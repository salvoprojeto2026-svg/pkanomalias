package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hardware.AudioMonitor
import com.example.data.hardware.AudioReadingState
import com.example.data.hardware.HardwareSensorInfo
import com.example.data.hardware.LocationReadingState
import com.example.data.hardware.LocationTracker
import com.example.data.hardware.SensorReadingState
import com.example.data.hardware.SensorTracker
import com.example.data.local.AppDatabase
import com.example.data.local.OccurrenceEntity
import com.example.data.local.OccurrenceRepository
import com.example.data.model.EnvironmentalBaseline
import com.example.data.model.EnvironmentalThresholds
import com.example.data.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TacticalNavDestination(val label: String) {
  MONITORAR("Monitorar"),
  CAMERA("Câmera"),
  REGISTROS("Registros"),
  EQUIPAMENTO("Equipamento");

  companion object {
    val RADAR = MONITORAR
    val CAMERA_RA = CAMERA
  }
}

data class ScannerUiState(
  val currentTab: TacticalNavDestination = TacticalNavDestination.MONITORAR,
  val sessionState: SessionState = SessionState.PARADO,
  val sessionDurationSeconds: Long = 0L,
  val formattedDuration: String = "00:00:00",
  val calibrationRemainingSeconds: Int = 20,
  val calibrationProgress: Float = 0f,

  val baseline: EnvironmentalBaseline = EnvironmentalBaseline(),
  val thresholds: EnvironmentalThresholds = EnvironmentalThresholds(),

  val sensorReading: SensorReadingState = SensorReadingState(),
  val audioReading: AudioReadingState = AudioReadingState(),
  val locationReading: LocationReadingState = LocationReadingState(),

  val magneticHistory: List<Float> = emptyList(),
  val audioHistory: List<Float> = emptyList(),
  val movementHistory: List<Float> = emptyList(),

  val occurrences: List<OccurrenceEntity> = emptyList(),
  val hardwareSensors: List<HardwareSensorInfo> = emptyList(),

  val searchFilterQuery: String = "",
  val activeFilterCategory: String = "TODAS",

  val isTorchOn: Boolean = false,
  val isFrontCamera: Boolean = false,

  val feedbackMessage: String = "",
  val activeDialogOccurrence: OccurrenceEntity? = null,
  val isShowingNoteDialog: Boolean = false
)

class PkeScannerViewModel(application: Application? = null) : AndroidViewModel(
  application ?: Application()
) {
  private val appContext: Context? = application?.applicationContext

  private val sensorTracker: SensorTracker? = appContext?.let { SensorTracker(it) }
  private val audioMonitor: AudioMonitor? = appContext?.let { AudioMonitor(it) }
  private val locationTracker: LocationTracker? = appContext?.let { LocationTracker(it) }
  private val repository: OccurrenceRepository? = appContext?.let {
    OccurrenceRepository(AppDatabase.getDatabase(it).occurrenceDao())
  }

  private val _uiState = MutableStateFlow(ScannerUiState())
  val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

  private var sessionTimerJob: Job? = null
  private var sensorCollectionJob: Job? = null
  private var lastAlertTimestamp: Long = 0L

  private val rollingLimit = 30
  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))

  init {
    loadHardwareInventory()
    observeRoomOccurrences()
  }

  private fun loadHardwareInventory() {
    sensorTracker?.let { tracker ->
      val sensors = tracker.getHardwareInventory()
      _uiState.update { it.copy(hardwareSensors = sensors) }
    }
  }

  private fun observeRoomOccurrences() {
    repository?.let { repo ->
      viewModelScope.launch {
        repo.allOccurrences.collect { list ->
          _uiState.update { it.copy(occurrences = list) }
        }
      }
    }
  }

  fun setTab(destination: TacticalNavDestination) {
    _uiState.update { it.copy(currentTab = destination) }
  }

  fun startMonitoring() {
    if (_uiState.value.sessionState == SessionState.MONITORANDO ||
      _uiState.value.sessionState == SessionState.CALIBRANDO
    ) return

    // Ativa sensores reais
    sensorTracker?.start()
    audioMonitor?.startMonitoring(viewModelScope)
    locationTracker?.startTracking()

    _uiState.update {
      it.copy(
        sessionState = SessionState.CALIBRANDO,
        calibrationRemainingSeconds = it.thresholds.calibrationSeconds,
        calibrationProgress = 0f,
        feedbackMessage = "Iniciando calibração da linha de base ambiental (20 segundos)..."
      )
    }

    startCalibrationAndCollection()
  }

  private fun startCalibrationAndCollection() {
    sensorCollectionJob?.cancel()
    sensorCollectionJob = viewModelScope.launch(Dispatchers.Default) {
      val magneticSamples = ArrayList<Float>()
      val audioSamples = ArrayList<Float>()
      val accelSamples = ArrayList<Float>()
      val lightSamples = ArrayList<Float>()

      val totalCalibSeconds = _uiState.value.thresholds.calibrationSeconds
      var elapsedCalib = 0

      // Fase de Calibração (20s)
      while (isActive && _uiState.value.sessionState == SessionState.CALIBRANDO) {
        delay(1000L)
        elapsedCalib++

        // Coleta amostras reais do momento
        sensorTracker?.readingState?.value?.let { s ->
          s.magneticFieldTotal?.let { magneticSamples.add(it) }
          s.accelerationTotal?.let { accelSamples.add(it) }
          s.lightLux?.let { lightSamples.add(it) }
        }
        audioMonitor?.audioState?.value?.let { a ->
          if (a.currentDbfs > -90f) {
            audioSamples.add(a.currentDbfs)
          }
        }

        val remaining = (totalCalibSeconds - elapsedCalib).coerceAtLeast(0)
        val progress = (elapsedCalib.toFloat() / totalCalibSeconds.toFloat()).coerceIn(0f, 1f)

        _uiState.update {
          it.copy(
            calibrationRemainingSeconds = remaining,
            calibrationProgress = progress
          )
        }

        if (elapsedCalib >= totalCalibSeconds) {
          // Calcula médias calibradas
          val avgMag = if (magneticSamples.isNotEmpty()) magneticSamples.average().toFloat() else 45f
          val avgAudio = if (audioSamples.isNotEmpty()) audioSamples.average().toFloat() else -65f
          val avgAccel = if (accelSamples.isNotEmpty()) accelSamples.average().toFloat() else 9.81f
          val avgLight = if (lightSamples.isNotEmpty()) lightSamples.average().toFloat() else 200f

          val baseline = EnvironmentalBaseline(
            magneticFieldUtd = avgMag,
            audioDbfs = avgAudio,
            accelerationMps2 = avgAccel,
            lightLux = avgLight,
            samplesCount = magneticSamples.size
          )

          _uiState.update {
            it.copy(
              sessionState = SessionState.MONITORANDO,
              baseline = baseline,
              feedbackMessage = "Calibração concluída! Monitorando variações em tempo real."
            )
          }
          startSessionClock()
          break
        }
      }

      // Fase de Monitoramento Ativo Contínuo
      while (isActive && _uiState.value.sessionState == SessionState.MONITORANDO) {
        delay(400L)

        // Atualiza leituras instantâneas dos sensores reais
        val s = sensorTracker?.readingState?.value ?: _uiState.value.sensorReading
        val a = audioMonitor?.audioState?.value ?: _uiState.value.audioReading
        val l = locationTracker?.locationState?.value ?: _uiState.value.locationReading

        // Atualiza histórico dos gráficos com amostras reais
        val newMagHist = ArrayList(_uiState.value.magneticHistory).apply {
          s.magneticFieldTotal?.let { add(it) }
          if (size > rollingLimit) removeAt(0)
        }
        val newAudioHist = ArrayList(_uiState.value.audioHistory).apply {
          add(a.currentDbfs)
          if (size > rollingLimit) removeAt(0)
        }
        val newMoveHist = ArrayList(_uiState.value.movementHistory).apply {
          add(s.dynamicMovementDelta)
          if (size > rollingLimit) removeAt(0)
        }

        _uiState.update {
          it.copy(
            sensorReading = s,
            audioReading = a,
            locationReading = l,
            magneticHistory = newMagHist,
            audioHistory = newAudioHist,
            movementHistory = newMoveHist
          )
        }

        // Verifica regras de detecção de variações
        checkEnvironmentalAnomalies(s, a, l)
      }
    }
  }

  private fun startSessionClock() {
    sessionTimerJob?.cancel()
    sessionTimerJob = viewModelScope.launch {
      while (isActive && _uiState.value.sessionState == SessionState.MONITORANDO) {
        delay(1000L)
        val newSecs = _uiState.value.sessionDurationSeconds + 1
        val hours = newSecs / 3600
        val mins = (newSecs % 3600) / 60
        val secs = newSecs % 60
        val formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)

        _uiState.update {
          it.copy(
            sessionDurationSeconds = newSecs,
            formattedDuration = formatted
          )
        }
      }
    }
  }

  private suspend fun checkEnvironmentalAnomalies(
    sensor: SensorReadingState,
    audio: AudioReadingState,
    location: LocationReadingState
  ) {
    val now = System.currentTimeMillis()
    val debounceMs = _uiState.value.thresholds.alertDebounceSeconds * 1000L
    if (now - lastAlertTimestamp < debounceMs) return

    val baseline = _uiState.value.baseline
    val thresholds = _uiState.value.thresholds

    // 1. Variação de Campo Magnético
    val currentMag = sensor.magneticFieldTotal
    if (currentMag != null && kotlin.math.abs(currentMag - baseline.magneticFieldUtd) > thresholds.magneticDeltaUtd) {
      val delta = currentMag - baseline.magneticFieldUtd
      triggerAutomaticOccurrence(
        reason = "Campo magnético variou em relação à referência (${String.format(Locale.US, "%+.1f", delta)} µT)",
        explanations = "Variação do fluxo eletromagnético local. Possíveis causas: proximidade de fiação elétrica, estruturas metálicas de concreto, motores elétricos ligados ou movimentação do smartphone.",
        sensor = sensor,
        audio = audio,
        location = location
      )
      lastAlertTimestamp = now
      return
    }

    // 2. Pico Sonoro Acústico
    val currentAudio = audio.currentDbfs
    if (currentAudio > -85f && (currentAudio - baseline.audioDbfs) > thresholds.audioDeltaDbfs) {
      val delta = currentAudio - baseline.audioDbfs
      triggerAutomaticOccurrence(
        reason = "Pico de áudio acústico (+${String.format(Locale.US, "%.1f", delta)} dBFS acima do ruído basal)",
        explanations = "Variação sonora pontual. Possíveis causas: emissão de voz, impacto mecânico próximo, rajada de vento incidindo sobre o diafragma do microfone ou passagem de veículos.",
        sensor = sensor,
        audio = audio,
        location = location
      )
      lastAlertTimestamp = now
      return
    }

    // 3. Movimento / Vibração do Aparelho
    if (sensor.dynamicMovementDelta > thresholds.movementDeltaMps2) {
      triggerAutomaticOccurrence(
        reason = "Movimento ou vibração súbita do aparelho (${String.format(Locale.US, "%.1f", sensor.dynamicMovementDelta)} m/s²)",
        explanations = "Vibração mecânica ou deslocamento físico. Possíveis causas: manuseio manual pelo operador, passos no piso, trepidação veicular ou choque mecânico na superfície de apoio.",
        sensor = sensor,
        audio = audio,
        location = location
      )
      lastAlertTimestamp = now
      return
    }

    // 4. Mudança de Luminosidade
    val currentLight = sensor.lightLux
    if (currentLight != null && kotlin.math.abs(currentLight - baseline.lightLux) > thresholds.lightDeltaLux) {
      val delta = currentLight - baseline.lightLux
      triggerAutomaticOccurrence(
        reason = "Mudança brusca de luminosidade (${String.format(Locale.US, "%+.0f", delta)} lx)",
        explanations = "Alteração no fluxo de fótons incidente no sensor frontal. Possíveis causas: sombra projetada por passagem, acendimento ou apagamento de luminária, reflexos ópticos ou obstrução do sensor.",
        sensor = sensor,
        audio = audio,
        location = location
      )
      lastAlertTimestamp = now
      return
    }
  }

  private suspend fun triggerAutomaticOccurrence(
    reason: String,
    explanations: String,
    sensor: SensorReadingState,
    audio: AudioReadingState,
    location: LocationReadingState
  ) {
    val dateStr = dateFormat.format(Date())
    val occurrence = OccurrenceEntity(
      id = "AUTO-${UUID.randomUUID().toString().take(8).uppercase()}",
      timestamp = System.currentTimeMillis(),
      formattedDateTime = dateStr,
      origin = "REGRA_AUTOMÁTICA",
      triggerReason = reason,
      possibleExplanations = explanations,
      latitude = location.latitude,
      longitude = location.longitude,
      altitude = location.altitudeMeters,
      locationAccuracyMeters = location.accuracyMeters,
      magneticFieldMicroTesla = sensor.magneticFieldTotal,
      baselineMagneticMicroTesla = _uiState.value.baseline.magneticFieldUtd,
      audioDbfs = audio.currentDbfs,
      baselineAudioDbfs = _uiState.value.baseline.audioDbfs,
      accelerationMps2 = sensor.accelerationTotal,
      lightLux = sensor.lightLux,
      pressureHpa = sensor.pressureHpa,
      azimuthDegrees = sensor.azimuthDegrees,
      cardinalDirection = sensor.cardinalDirection,
      userNotes = "Registro acionado automaticamente pelo sistema de regras físicas.",
      aiAnalysis = null
    )

    repository?.insert(occurrence)
    _uiState.update {
      it.copy(feedbackMessage = "Alerta automático: $reason")
    }
  }

  fun pauseMonitoring() {
    if (_uiState.value.sessionState == SessionState.MONITORANDO) {
      _uiState.update {
        it.copy(
          sessionState = SessionState.PAUSADO,
          feedbackMessage = "Monitoramento pausado."
        )
      }
      sessionTimerJob?.cancel()
    }
  }

  fun resumeMonitoring() {
    if (_uiState.value.sessionState == SessionState.PAUSADO) {
      _uiState.update {
        it.copy(
          sessionState = SessionState.MONITORANDO,
          feedbackMessage = "Monitoramento retomado."
        )
      }
      startSessionClock()
      startCalibrationAndCollection()
    }
  }

  fun stopMonitoring() {
    _uiState.update {
      it.copy(
        sessionState = SessionState.PARADO,
        feedbackMessage = "Monitoramento encerrado."
      )
    }
    sessionTimerJob?.cancel()
    sensorCollectionJob?.cancel()
    sensorTracker?.stop()
    audioMonitor?.stopMonitoring()
    locationTracker?.stopTracking()
  }

  fun markOccurrence(
    userNotes: String = "",
    photoPath: String? = null,
    audioPath: String? = null
  ) {
    viewModelScope.launch {
      val s = sensorTracker?.readingState?.value ?: _uiState.value.sensorReading
      val a = audioMonitor?.audioState?.value ?: _uiState.value.audioReading
      val l = locationTracker?.locationState?.value ?: _uiState.value.locationReading

      val dateStr = dateFormat.format(Date())
      val occurrence = OccurrenceEntity(
        id = "MAN-${UUID.randomUUID().toString().take(8).uppercase()}",
        timestamp = System.currentTimeMillis(),
        formattedDateTime = dateStr,
        origin = "MARCAÇÃO_MANUAL",
        triggerReason = "Registro manual efetuado pelo operador no ponto de observação",
        possibleExplanations = "Observação pontual registrada conscientemente pelo usuário para análise posterior de condições ambientais.",
        latitude = l.latitude,
        longitude = l.longitude,
        altitude = l.altitudeMeters,
        locationAccuracyMeters = l.accuracyMeters,
        magneticFieldMicroTesla = s.magneticFieldTotal,
        baselineMagneticMicroTesla = _uiState.value.baseline.magneticFieldUtd,
        audioDbfs = a.currentDbfs,
        baselineAudioDbfs = _uiState.value.baseline.audioDbfs,
        accelerationMps2 = s.accelerationTotal,
        lightLux = s.lightLux,
        pressureHpa = s.pressureHpa,
        azimuthDegrees = s.azimuthDegrees,
        cardinalDirection = s.cardinalDirection,
        photoPath = photoPath,
        audioPath = audioPath,
        userNotes = if (userNotes.isNotBlank()) userNotes else "Observação registrada sem anotações adicionais.",
        aiAnalysis = null
      )

      repository?.insert(occurrence)
      _uiState.update {
        it.copy(feedbackMessage = "Ocorrência registrada com sucesso no banco de dados!")
      }
    }
  }

  fun deleteOccurrence(occurrenceId: String) {
    viewModelScope.launch {
      repository?.deleteById(occurrenceId)
      _uiState.update {
        it.copy(feedbackMessage = "Ocorrência #$occurrenceId excluída.")
      }
    }
  }

  fun updateOccurrenceNotes(occurrenceId: String, newNotes: String) {
    viewModelScope.launch {
      val item = _uiState.value.occurrences.find { it.id == occurrenceId }
      if (item != null) {
        val updated = item.copy(userNotes = newNotes)
        repository?.update(updated)
      }
    }
  }

  fun setSearchFilter(query: String) {
    _uiState.update { it.copy(searchFilterQuery = query) }
  }

  fun setFilterCategory(category: String) {
    _uiState.update { it.copy(activeFilterCategory = category) }
  }

  fun clearFeedbackMessage() {
    _uiState.update { it.copy(feedbackMessage = "") }
  }

  fun toggleTorch() {
    _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
  }

  fun toggleCameraFacing() {
    _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
  }

  // Reprodução de áudio
  fun playOccurrenceAudio(path: String) {
    audioMonitor?.playAudio(path)
  }

  fun stopAudioPlayback() {
    audioMonitor?.stopPlayback()
  }

  // Análise técnica estruturada com IA
  fun analyzeOccurrenceWithAi(occurrence: OccurrenceEntity) {
    viewModelScope.launch {
      _uiState.update { it.copy(feedbackMessage = "Gerando avaliação técnica...") }
      delay(800)

      val analysis = buildString {
        appendLine("• O QUE É OBSERVÁVEL:")
        appendLine("  - Registro: ${occurrence.origin} em ${occurrence.formattedDateTime}")
        appendLine("  - Campo Magnético: ${occurrence.magneticFieldMicroTesla?.let { String.format(Locale.US, "%.1f µT", it) } ?: "Não disponível"} (Referência: ${occurrence.baselineMagneticMicroTesla?.let { String.format(Locale.US, "%.1f µT", it) } ?: "N/A"})")
        appendLine("  - Nível de Áudio: ${occurrence.audioDbfs?.let { String.format(Locale.US, "%.1f dBFS", it) } ?: "Não disponível"}")
        appendLine("  - Aceleração: ${occurrence.accelerationMps2?.let { String.format(Locale.US, "%.2f m/s²", it) } ?: "Não disponível"}")
        appendLine("  - Coordenadas: ${occurrence.latitude ?: "N/D"}, ${occurrence.longitude ?: "N/D"} (Precisão: ±${occurrence.locationAccuracyMeters?.toInt() ?: "N/D"}m)")
        appendLine("  - Orientação: ${occurrence.cardinalDirection ?: "N/D"}")
        appendLine()
        appendLine("• POSSÍVEIS EXPLICAÇÕES FÍSICAS:")
        appendLine("  - ${occurrence.possibleExplanations}")
        appendLine("  - Flutuações na rede de energia elétrica de 50/60 Hz local ou campos estáticos de vigas metálicas.")
        appendLine("  - Microfone exposto a corrente convectiva de ar ou ruído ambiente de baixa frequência.")
        appendLine()
        appendLine("• LIMITAÇÕES DA EVIDÊNCIA:")
        appendLine("  - Os sensores de smartphones comerciais não possuem calibração metrológica de laboratório.")
        appendLine("  - Não há sensor de temperatura ambiente independente no hardware do aparelho.")
        appendLine("  - Fenômeno registrado por apenas uma fonte de observação móvel.")
        appendLine()
        appendLine("• PRÓXIMAS VERIFICAÇÕES SUGERIDAS:")
        appendLine("  - Repetir a medição no mesmo local com o aparelho imóvel em tripé.")
        appendLine("  - Afastar o dispositivo a pelo menos 3 metros de condutores elétricos e eletrodomésticos.")
        appendLine("  - Utilizar instrumento com medição diferencial de três eixos simultâneos.")
      }

      val updated = occurrence.copy(aiAnalysis = analysis)
      repository?.update(updated)
      _uiState.update { it.copy(feedbackMessage = "Avaliação técnica concluída.") }
    }
  }

  // Exportação em JSON
  fun exportOccurrencesJson(): String {
    val items = _uiState.value.occurrences
    val sb = StringBuilder("[\n")
    items.forEachIndexed { index, item ->
      sb.append("  {\n")
      sb.append("    \"id\": \"${item.id}\",\n")
      sb.append("    \"dataHora\": \"${item.formattedDateTime}\",\n")
      sb.append("    \"origem\": \"${item.origin}\",\n")
      sb.append("    \"motivo\": \"${item.triggerReason.replace("\"", "\\\"")}\",\n")
      sb.append("    \"latitude\": ${item.latitude},\n")
      sb.append("    \"longitude\": ${item.longitude},\n")
      sb.append("    \"precisaoMetros\": ${item.locationAccuracyMeters},\n")
      sb.append("    \"campoMagneticoUtd\": ${item.magneticFieldMicroTesla},\n")
      sb.append("    \"audioDbfs\": ${item.audioDbfs},\n")
      sb.append("    \"aceleracaoMps2\": ${item.accelerationMps2},\n")
      sb.append("    \"luminosidadeLux\": ${item.lightLux},\n")
      sb.append("    \"azimute\": ${item.azimuthDegrees},\n")
      sb.append("    \"notas\": \"${item.userNotes.replace("\"", "\\\"")}\"\n")
      sb.append("  }${if (index < items.size - 1) "," else ""}\n")
    }
    sb.append("]")
    return sb.toString()
  }

  // Exportação em CSV
  fun exportOccurrencesCsv(): String {
    val items = _uiState.value.occurrences
    val sb = StringBuilder("ID;DataHora;Origem;Motivo;Latitude;Longitude;Precisao_m;CampoMagnetico_uT;Audio_dBFS;Aceleracao_mps2;Luz_lux;Azimute;Notas\n")
    for (item in items) {
      sb.append("${item.id};")
      sb.append("${item.formattedDateTime};")
      sb.append("${item.origin};")
      sb.append("\"${item.triggerReason.replace("\"", "'")}\";")
      sb.append("${item.latitude ?: ""};")
      sb.append("${item.longitude ?: ""};")
      sb.append("${item.locationAccuracyMeters ?: ""};")
      sb.append("${item.magneticFieldMicroTesla ?: ""};")
      sb.append("${item.audioDbfs ?: ""};")
      sb.append("${item.accelerationMps2 ?: ""};")
      sb.append("${item.lightLux ?: ""};")
      sb.append("${item.azimuthDegrees ?: ""};")
      sb.append("\"${item.userNotes.replace("\"", "'")}\"\n")
    }
    return sb.toString()
  }

  override fun onCleared() {
    super.onCleared()
    stopMonitoring()
    audioMonitor?.stopPlayback()
  }
}
