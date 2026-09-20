package com.example.data.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SensorReadingState(
  // Campo Magnético (µT)
  val magneticFieldTotal: Float? = null,
  val magneticFieldX: Float? = null,
  val magneticFieldY: Float? = null,
  val magneticFieldZ: Float? = null,
  val magneticAccuracy: String = "Aguardando leitura",
  val hasMagneticSensor: Boolean = false,

  // Aceleração e Movimento (m/s²)
  val accelerationTotal: Float? = null,
  val accelerationX: Float? = null,
  val accelerationY: Float? = null,
  val accelerationZ: Float? = null,
  val dynamicMovementDelta: Float = 0f,
  val hasAccelerometer: Boolean = false,

  // Giroscópio (rad/s)
  val gyroX: Float? = null,
  val gyroY: Float? = null,
  val gyroZ: Float? = null,
  val hasGyroscope: Boolean = false,

  // Orientação e Bússola
  val azimuthDegrees: Float? = null,
  val cardinalDirection: String = "---",
  val pitchDegrees: Float? = null,
  val rollDegrees: Float? = null,

  // Luminosidade (lx)
  val lightLux: Float? = null,
  val hasLightSensor: Boolean = false,

  // Pressão Atmosférica (hPa)
  val pressureHpa: Float? = null,
  val hasPressureSensor: Boolean = false,

  // Proximidade (cm)
  val proximityCm: Float? = null,
  val hasProximitySensor: Boolean = false,

  // Temperatura e Umidade dedicadas (se disponíveis)
  val ambientTempCelsius: Float? = null,
  val hasAmbientTempSensor: Boolean = false,
  val relativeHumidityPercent: Float? = null,
  val hasHumiditySensor: Boolean = false
)

data class HardwareSensorInfo(
  val name: String,
  val vendor: String,
  val version: Int,
  val typeName: String,
  val powerMa: Float,
  val resolution: Float,
  val maxRange: Float,
  val isPresent: Boolean,
  val notes: String = ""
)

class SensorTracker(context: Context) : SensorEventListener {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val _readingState = MutableStateFlow(SensorReadingState())
  val readingState: StateFlow<SensorReadingState> = _readingState.asStateFlow()

  // Referências aos sensores
  private val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
  private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
  private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
  private val pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
  private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
  private val tempSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
  private val humiditySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

  // Matrizes de rotação para cálculo de azimute
  private val gravityMatrix = FloatArray(3)
  private val geomagneticMatrix = FloatArray(3)
  private var hasGravity = false
  private var hasGeomagnetic = false

  private var isListening = false

  init {
    _readingState.value = _readingState.value.copy(
      hasMagneticSensor = magneticSensor != null,
      hasAccelerometer = accelerometerSensor != null,
      hasGyroscope = gyroscopeSensor != null,
      hasLightSensor = lightSensor != null,
      hasPressureSensor = pressureSensor != null,
      hasProximitySensor = proximitySensor != null,
      hasAmbientTempSensor = tempSensor != null,
      hasHumiditySensor = humiditySensor != null
    )
  }

  fun start() {
    if (isListening || sensorManager == null) return
    isListening = true

    val delay = SensorManager.SENSOR_DELAY_UI

    magneticSensor?.let { sensorManager.registerListener(this, it, delay) }
    accelerometerSensor?.let { sensorManager.registerListener(this, it, delay) }
    gyroscopeSensor?.let { sensorManager.registerListener(this, it, delay) }
    rotationVectorSensor?.let { sensorManager.registerListener(this, it, delay) }
    lightSensor?.let { sensorManager.registerListener(this, it, delay) }
    pressureSensor?.let { sensorManager.registerListener(this, it, delay) }
    proximitySensor?.let { sensorManager.registerListener(this, it, delay) }
    tempSensor?.let { sensorManager.registerListener(this, it, delay) }
    humiditySensor?.let { sensorManager.registerListener(this, it, delay) }
  }

  fun stop() {
    if (!isListening || sensorManager == null) return
    isListening = false
    sensorManager.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    when (event.sensor.type) {
      Sensor.TYPE_MAGNETIC_FIELD -> {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val total = sqrt(x * x + y * y + z * z)

        System.arraycopy(event.values, 0, geomagneticMatrix, 0, 3)
        hasGeomagnetic = true
        updateOrientationFallback()

        _readingState.value = _readingState.value.copy(
          magneticFieldTotal = total,
          magneticFieldX = x,
          magneticFieldY = y,
          magneticFieldZ = z,
          magneticAccuracy = when (event.accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Alta"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Média"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Baixa (calibre movendo em 8)"
            else -> "Não calibrado"
          }
        )
      }

      Sensor.TYPE_ACCELEROMETER -> {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val total = sqrt(x * x + y * y + z * z)
        val dynamicDelta = kotlin.math.abs(total - 9.81f)

        System.arraycopy(event.values, 0, gravityMatrix, 0, 3)
        hasGravity = true
        updateOrientationFallback()

        _readingState.value = _readingState.value.copy(
          accelerationTotal = total,
          accelerationX = x,
          accelerationY = y,
          accelerationZ = z,
          dynamicMovementDelta = dynamicDelta
        )
      }

      Sensor.TYPE_GYROSCOPE -> {
        _readingState.value = _readingState.value.copy(
          gyroX = event.values[0],
          gyroY = event.values[1],
          gyroZ = event.values[2]
        )
      }

      Sensor.TYPE_ROTATION_VECTOR -> {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f

        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

        _readingState.value = _readingState.value.copy(
          azimuthDegrees = azimuthDeg,
          cardinalDirection = degToCardinal(azimuthDeg),
          pitchDegrees = pitchDeg,
          rollDegrees = rollDeg
        )
      }

      Sensor.TYPE_LIGHT -> {
        _readingState.value = _readingState.value.copy(
          lightLux = event.values[0]
        )
      }

      Sensor.TYPE_PRESSURE -> {
        _readingState.value = _readingState.value.copy(
          pressureHpa = event.values[0]
        )
      }

      Sensor.TYPE_PROXIMITY -> {
        _readingState.value = _readingState.value.copy(
          proximityCm = event.values[0]
        )
      }

      Sensor.TYPE_AMBIENT_TEMPERATURE -> {
        _readingState.value = _readingState.value.copy(
          ambientTempCelsius = event.values[0]
        )
      }

      Sensor.TYPE_RELATIVE_HUMIDITY -> {
        _readingState.value = _readingState.value.copy(
          relativeHumidityPercent = event.values[0]
        )
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
      _readingState.value = _readingState.value.copy(
        magneticAccuracy = when (accuracy) {
          SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Alta"
          SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Média"
          SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Baixa (calibre movendo em 8)"
          else -> "Não calibrado"
        }
      )
    }
  }

  private fun updateOrientationFallback() {
    if (rotationVectorSensor == null && hasGravity && hasGeomagnetic) {
      val r = FloatArray(9)
      val i = FloatArray(9)
      if (SensorManager.getRotationMatrix(r, i, gravityMatrix, geomagneticMatrix)) {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)
        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f
        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

        _readingState.value = _readingState.value.copy(
          azimuthDegrees = azimuthDeg,
          cardinalDirection = degToCardinal(azimuthDeg),
          pitchDegrees = pitchDeg,
          rollDegrees = rollDeg
        )
      }
    }
  }

  private fun degToCardinal(deg: Float): String {
    val normalized = (deg % 360 + 360) % 360
    return when {
      normalized >= 337.5 || normalized < 22.5 -> "N (0°)"
      normalized < 67.5 -> "NE (45°)"
      normalized < 112.5 -> "L (90°)"
      normalized < 157.5 -> "SE (135°)"
      normalized < 202.5 -> "S (180°)"
      normalized < 247.5 -> "SO (225°)"
      normalized < 292.5 -> "O (270°)"
      else -> "NO (315°)"
    }
  }

  fun getHardwareInventory(): List<HardwareSensorInfo> {
    if (sensorManager == null) return emptyList()

    val targetTypes = listOf(
      Sensor.TYPE_MAGNETIC_FIELD to "Magnetômetro (Campo Magnético)",
      Sensor.TYPE_ACCELEROMETER to "Acelerômetro (Movimento / Força G)",
      Sensor.TYPE_GYROSCOPE to "Giroscópio (Rotação Angular)",
      Sensor.TYPE_ROTATION_VECTOR to "Vetor de Rotação (Fusão de Sensores)",
      Sensor.TYPE_LIGHT to "Sensor de Luminosidade",
      Sensor.TYPE_PRESSURE to "Barômetro (Pressão Atmosférica)",
      Sensor.TYPE_PROXIMITY to "Sensor de Proximidade Frontal",
      Sensor.TYPE_AMBIENT_TEMPERATURE to "Termômetro Ambiente Dedicado",
      Sensor.TYPE_RELATIVE_HUMIDITY to "Higrômetro (Umidade Relativa)"
    )

    return targetTypes.map { (type, typeName) ->
      val sensor = sensorManager.getDefaultSensor(type)
      if (sensor != null) {
        val notes = if (type == Sensor.TYPE_PROXIMITY) {
          "Sensor de proximidade frontal para desligamento de tela em chamadas; não detecta pessoas ou objetos à distância."
        } else ""

        HardwareSensorInfo(
          name = sensor.name,
          vendor = sensor.vendor,
          version = sensor.version,
          typeName = typeName,
          powerMa = sensor.power,
          resolution = sensor.resolution,
          maxRange = sensor.maximumRange,
          isPresent = true,
          notes = notes
        )
      } else {
        val notes = when (type) {
          Sensor.TYPE_AMBIENT_TEMPERATURE -> "Hardware não possui sensor térmico ambiente dedicado (temperatura de bateria não é ambiente)."
          Sensor.TYPE_PRESSURE -> "Aparelho não possui barômetro integrado."
          Sensor.TYPE_RELATIVE_HUMIDITY -> "Aparelho não possui higrômetro ambiental."
          else -> "Sensor não disponível neste modelo de dispositivo."
        }

        HardwareSensorInfo(
          name = "Não disponível",
          vendor = "N/A",
          version = 0,
          typeName = typeName,
          powerMa = 0f,
          resolution = 0f,
          maxRange = 0f,
          isPresent = false,
          notes = notes
        )
      }
    }
  }
}
