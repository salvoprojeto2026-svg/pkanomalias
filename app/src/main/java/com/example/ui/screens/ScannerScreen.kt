package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionState
import com.example.ui.components.EnvironmentalLineChart
import com.example.ui.components.TacticalCompassView
import com.example.ui.components.TacticalTopHeader
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorderGreen
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TertiaryBright
import com.example.ui.viewmodel.PkeScannerViewModel
import com.example.ui.viewmodel.ScannerUiState
import java.util.Locale

@Composable
fun ScannerScreen(
  viewModel: PkeScannerViewModel,
  uiState: ScannerUiState,
  modifier: Modifier = Modifier
) {
  var showMarkDialog by remember { mutableStateOf(false) }
  var markNotes by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SurfaceDark)
  ) {
    // Cabeçalho Superior
    TacticalTopHeader(
      sessionState = uiState.sessionState,
      sessionDurationFormatted = uiState.formattedDuration,
      locationStatus = uiState.locationReading.statusDescription
    )

    // Corpo Rolável
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Banner de feedback / avisos
      if (uiState.feedbackMessage.isNotBlank()) {
        FeedbackBanner(
          message = uiState.feedbackMessage,
          onDismiss = { viewModel.clearFeedbackMessage() }
        )
      }

      // Painel de Controle da Sessão e Calibração
      SessionControlCard(
        sessionState = uiState.sessionState,
        calibrationSeconds = uiState.calibrationRemainingSeconds,
        calibrationProgress = uiState.calibrationProgress,
        onStart = { viewModel.startMonitoring() },
        onPause = { viewModel.pauseMonitoring() },
        onResume = { viewModel.resumeMonitoring() },
        onStop = { viewModel.stopMonitoring() },
        onMarkOccurrence = { showMarkDialog = true }
      )

      // Bússola e Orientação Real
      TacticalCompassView(
        azimuthDegrees = uiState.sensorReading.azimuthDegrees,
        cardinalDirection = uiState.sensorReading.cardinalDirection,
        pitchDegrees = uiState.sensorReading.pitchDegrees,
        rollDegrees = uiState.sensorReading.rollDegrees,
        magneticAccuracy = uiState.sensorReading.magneticAccuracy
      )

      // Painel de Sensores em Tempo Real (Valores & Unidades)
      Text(
        text = "SENSORES FÍSICOS EM TEMPO REAL",
        color = Color.LightGray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      // Grade de Leituras de Sensores
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SensorValueCard(
          title = "CAMPO MAGNÉTICO",
          mainValue = uiState.sensorReading.magneticFieldTotal?.let { String.format(Locale.US, "%.1f", it) } ?: "---",
          unit = "µT",
          subDetails = "X:${formatF(uiState.sensorReading.magneticFieldX)} Y:${formatF(uiState.sensorReading.magneticFieldY)} Z:${formatF(uiState.sensorReading.magneticFieldZ)}",
          status = uiState.sensorReading.magneticAccuracy,
          icon = Icons.Default.Sensors,
          accentColor = PrimaryNeonGreen,
          modifier = Modifier.weight(1f)
        )

        SensorValueCard(
          title = "NÍVEL ACÚSTICO",
          mainValue = if (uiState.audioReading.currentDbfs > -90f) {
            String.format(Locale.US, "%.1f", uiState.audioReading.currentDbfs)
          } else "SILÊNCIO",
          unit = "dBFS",
          subDetails = "Pico: ${formatDbfs(uiState.audioReading.peakDbfs)}",
          status = if (uiState.audioReading.isAudioCapturingActive) "Microfone ativo" else "Parado",
          icon = Icons.Default.GraphicEq,
          accentColor = SecondaryCyan,
          modifier = Modifier.weight(1f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val dynamicMovement = uiState.sensorReading.dynamicMovementDelta
        val movementState = if (dynamicMovement < 0.8f) "Aparelho em repouso" else "Em movimento (${String.format(Locale.US, "%.1f", dynamicMovement)} m/s²)"

        SensorValueCard(
          title = "ACELERAÇÃO TOTAL",
          mainValue = uiState.sensorReading.accelerationTotal?.let { String.format(Locale.US, "%.2f", it) } ?: "---",
          unit = "m/s²",
          subDetails = movementState,
          status = "Eixos X, Y, Z ativos",
          icon = Icons.Default.MotionPhotosOn,
          accentColor = TertiaryBright,
          modifier = Modifier.weight(1f)
        )

        SensorValueCard(
          title = "LUMINOSIDADE",
          mainValue = uiState.sensorReading.lightLux?.let { String.format(Locale.US, "%.0f", it) } ?: "---",
          unit = "lx",
          subDetails = if (uiState.sensorReading.hasLightSensor) "Sensor frontal" else "Hardware ausente",
          status = if (uiState.sensorReading.hasLightSensor) "Operante" else "Indisponível",
          icon = Icons.Default.WbSunny,
          accentColor = Color(0xFFFFD54F),
          modifier = Modifier.weight(1f)
        )
      }

      // Card de Pressão Barométrica e GPS
      BarometerAndLocationCard(
        pressureHpa = uiState.sensorReading.pressureHpa,
        hasPressureSensor = uiState.sensorReading.hasPressureSensor,
        location = uiState.locationReading
      )

      // 3 Gráficos com Dados Reais
      Text(
        text = "MONITORAMENTO GRÁFICO (DADOS REAIS)",
        color = Color.LightGray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      // Gráfico 1: Campo Magnético
      EnvironmentalLineChart(
        title = "1. CAMPO MAGNÉTICO (FLUXO TOTAL)",
        currentValueFormatted = uiState.sensorReading.magneticFieldTotal?.let { String.format(Locale.US, "%.1f", it) } ?: "---",
        baselineFormatted = String.format(Locale.US, "%.1f µT", uiState.baseline.magneticFieldUtd),
        unit = "µT",
        historyPoints = uiState.magneticHistory,
        lineColor = PrimaryNeonGreen,
        minValue = 20f,
        maxValue = 80f
      )

      // Gráfico 2: Nível de Áudio
      EnvironmentalLineChart(
        title = "2. NÍVEL DE ÁUDIO (MICROFONE REAL)",
        currentValueFormatted = if (uiState.audioReading.currentDbfs > -90f) {
          String.format(Locale.US, "%.1f", uiState.audioReading.currentDbfs)
        } else "-90",
        baselineFormatted = String.format(Locale.US, "%.1f dBFS", uiState.baseline.audioDbfs),
        unit = "dBFS",
        historyPoints = uiState.audioHistory,
        lineColor = SecondaryCyan,
        minValue = -90f,
        maxValue = 0f
      )

      // Gráfico 3: Movimento / Vibração
      EnvironmentalLineChart(
        title = "3. MOVIMENTO / VIBRAÇÃO MECÂNICA",
        currentValueFormatted = String.format(Locale.US, "%.2f", uiState.sensorReading.dynamicMovementDelta),
        baselineFormatted = "0.00 m/s² (Repouso)",
        unit = "m/s²",
        historyPoints = uiState.movementHistory,
        lineColor = TertiaryBright,
        minValue = 0f,
        maxValue = 10f
      )

      Spacer(modifier = Modifier.height(70.dp))
    }
  }

  // Diálogo para Marcar Ocorrência Manual
  if (showMarkDialog) {
    AlertDialog(
      onDismissRequest = { showMarkDialog = false },
      title = {
        Text(
          text = "MARCAR OCORRÊNCIA",
          color = PrimaryNeonGreen,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column {
          Text(
            text = "As medições instantâneas de todos os sensores físicos, orientação e coordenadas GPS serão gravadas localmente no banco de dados.",
            color = Color.LightGray,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = markNotes,
            onValueChange = { markNotes = it },
            label = { Text("Anotações do observador (opcional)") },
            placeholder = { Text("Ex.: Som de estalo metálico ouvido próximo à parede sul.") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryNeonGreen,
              unfocusedBorderColor = Color.Gray,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.markOccurrence(userNotes = markNotes)
            markNotes = ""
            showMarkDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen)
        ) {
          Text("REGISTRAR AGORA", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showMarkDialog = false }) {
          Text("CANCELAR", color = Color.Gray)
        }
      },
      containerColor = SurfaceContainerHigh
    )
  }
}

@Composable
private fun SessionControlCard(
  sessionState: SessionState,
  calibrationSeconds: Int,
  calibrationProgress: Float,
  onStart: () -> Unit,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStop: () -> Unit,
  onMarkOccurrence: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(GlassBackground)
      .border(1.dp, GlassBorderGreen, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "CONTROLE DA SESSÃO",
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = when (sessionState) {
              SessionState.PARADO -> "Pronto para iniciar medições"
              SessionState.CALIBRANDO -> "Calibrando linha de base ($calibrationSeconds s)..."
              SessionState.MONITORANDO -> "Monitoramento ativo e contínuo"
              SessionState.PAUSADO -> "Sessão pausada temporariamente"
            },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Botão de Marcar Ocorrência em Destaque
        Button(
          onClick = onMarkOccurrence,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.BookmarkAdd,
            contentDescription = "Marcar Ocorrência",
            tint = Color(0xFF003919),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "REGISTRAR",
            color = Color(0xFF003919),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Barra de progresso da calibração
      if (sessionState == SessionState.CALIBRANDO) {
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
          progress = { calibrationProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = TertiaryBright,
          trackColor = SurfaceContainerHighest
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Botões de Início, Pausa e Encerramento
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        when (sessionState) {
          SessionState.PARADO -> {
            Button(
              onClick = onStart,
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
              Spacer(modifier = Modifier.width(6.dp))
              Text("INICIAR MONITORAMENTO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }

          SessionState.MONITORANDO, SessionState.CALIBRANDO -> {
            Button(
              onClick = onPause,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Yellow)
              Spacer(modifier = Modifier.width(4.dp))
              Text("PAUSAR", color = Color.White, fontSize = 11.sp)
            }

            Button(
              onClick = onStop,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1D1D)),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("ENCERRAR", color = Color.White, fontSize = 11.sp)
            }
          }

          SessionState.PAUSADO -> {
            Button(
              onClick = onResume,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
              Spacer(modifier = Modifier.width(4.dp))
              Text("RETOMAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
              onClick = onStop,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1D1D)),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("ENCERRAR", color = Color.White, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SensorValueCard(
  title: String,
  mainValue: String,
  unit: String,
  subDetails: String,
  status: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          color = Color.Gray,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(14.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = mainValue,
          color = Color.White,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = " $unit",
          color = accentColor,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(bottom = 1.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = subDetails,
        color = Color.LightGray,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )

      Text(
        text = "Estado: $status",
        color = accentColor.copy(alpha = 0.8f),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
private fun BarometerAndLocationCard(
  pressureHpa: Float?,
  hasPressureSensor: Boolean,
  location: com.example.data.hardware.LocationReadingState
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Barômetro
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "PRESSÃO ATMOSFÉRICA",
          color = Color.Gray,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = if (hasPressureSensor && pressureHpa != null) {
            "${String.format(Locale.US, "%.1f", pressureHpa)} hPa"
          } else "Hardware não disponível no aparelho",
          color = if (hasPressureSensor) Color.White else Color.Gray,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      // Localização GPS
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = SecondaryCyan,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "GPS / LOCALIZAÇÃO REAL",
            color = SecondaryCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = location.timestampFormatted,
          color = Color.Gray,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (location.latitude != null && location.longitude != null) {
            "${String.format(Locale.US, "%.5f", location.latitude)}°, ${String.format(Locale.US, "%.5f", location.longitude)}°"
          } else "Aguardando sinal...",
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )

        Text(
          text = location.accuracyMeters?.let { "Precisão: ±${it.toInt()}m" } ?: "Precisão N/D",
          color = Color.LightGray,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
private fun FeedbackBanner(message: String, onDismiss: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(SurfaceContainerHigh)
      .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = PrimaryNeonGreen,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = message,
          color = Color.White,
          fontSize = 11.sp,
          fontFamily = FontFamily.SansSerif
        )
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Fechar",
          tint = Color.Gray,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

private fun formatF(v: Float?): String = v?.let { String.format(Locale.US, "%.0f", it) } ?: "0"
private fun formatDbfs(v: Float?): String = v?.let { String.format(Locale.US, "%.0f dBFS", it) } ?: "---"
