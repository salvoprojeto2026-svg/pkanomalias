package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.hardware.HardwareSensorInfo
import com.example.ui.components.TacticalTopHeader
import com.example.ui.theme.GlassBackground
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
fun EquipmentScreen(
  viewModel: PkeScannerViewModel,
  uiState: ScannerUiState,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SurfaceDark)
  ) {
    TacticalTopHeader(
      sessionState = uiState.sessionState,
      sessionDurationFormatted = uiState.formattedDuration,
      locationStatus = uiState.locationReading.statusDescription
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Banner do Projeto
      ProjectInfoCard()

      // Esclarecimentos de Limitações Físicas
      PhysicalClarificationBanner()

      // Diagnóstico dos Subsistemas Principais
      Text(
        text = "DIAGNÓSTICO DE SUBSISTEMAS",
        color = Color.LightGray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      SubsystemDiagnosticsCard(uiState)

      // Inventário Técnico Completo de Sensores
      Text(
        text = "INVENTÁRIO DE SENSORES DO DISPOSITIVO",
        color = Color.LightGray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.hardwareSensors.forEach { sensorInfo ->
          SensorInventoryCard(sensorInfo)
        }
      }

      // Parâmetros e Limiares de Regras
      Text(
        text = "PARÂMETROS DE DETECÇÃO AMBIENTAL",
        color = Color.LightGray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      ThresholdsConfigCard(uiState)

      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun ProjectInfoCard() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
            text = "PKE SCANNER BRASIL",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Text(
            text = "Projeto Jairo Bahia • Versão 1.0",
            color = PrimaryNeonGreen,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "ANDROID NATIVO",
            color = SecondaryCyan,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Aplicativo para observação de grandezas físicas e ambientais em território brasileiro. Registra medições reais de magnetismo, acústica, movimento, luz e localização sem afirmações de cunho sobrenatural.",
        color = Color.LightGray,
        fontSize = 11.sp,
        lineHeight = 15.sp
      )
    }
  }
}

@Composable
private fun PhysicalClarificationBanner() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF06181E))
      .border(1.dp, SecondaryCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.Top) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = SecondaryCyan,
        modifier = Modifier
          .size(18.dp)
          .padding(top = 1.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "ESCLARECIMENTOS METROLÓGICOS IMPORTANTES",
          color = SecondaryCyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "• Sensor de proximidade frontal: atua exclusivamente no desligamento de tela ao aproximar o rosto em chamadas telefônicas; não detecta pessoas, objetos distantes ou obstáculos.\n• Ausência de radiação ionizante: o celular não possui contador Geiger-Müller e não mede raios gama, beta ou alfa.\n• Leituras em dBFS: expressam a amplitude de entrada do conversor digital do microfone em relação ao fundo de escala e não constituem medição dB SPL calibrada.",
          color = Color(0xFFC7E5F0),
          fontSize = 10.sp,
          lineHeight = 14.sp
        )
      }
    }
  }
}

@Composable
private fun SubsystemDiagnosticsCard(uiState: ScannerUiState) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Câmera
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryNeonGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Câmera / Óptica", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text("CameraX nativo (Traseira e Frontal)", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      }

      // Microfone
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Mic, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Microfone / Acústica", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text("PCM 16-bit 44.1 kHz Mono", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      }

      // Localização
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = TertiaryBright, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Posicionamento", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(uiState.locationReading.provider ?: "GPS Satélite / Rede", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

@Composable
private fun SensorInventoryCard(info: HardwareSensorInfo) {
  val statusColor = if (info.isPresent) PrimaryNeonGreen else Color.Gray

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(GlassBackground)
      .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (info.isPresent) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = info.typeName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Text(
          text = if (info.isPresent) "OPERANTE" else "INDISPONÍVEL",
          color = statusColor,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      if (info.isPresent) {
        Text(
          text = "Componente: ${info.name} • Fabricante: ${info.vendor} (v${info.version})",
          color = Color.LightGray,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "Consumo: ${String.format(Locale.US, "%.2f", info.powerMa)} mA • Alcance: ${info.maxRange} • Resolução: ${info.resolution}",
          color = Color.Gray,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      if (info.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = info.notes,
          color = if (info.isPresent) Color(0xFFFFCC80) else Color.Gray,
          fontSize = 9.sp,
          lineHeight = 12.sp
        )
      }
    }
  }
}

@Composable
private fun ThresholdsConfigCard(uiState: ScannerUiState) {
  val t = uiState.thresholds

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ConfigRow(label = "Tempo de Calibração Basal", value = "${t.calibrationSeconds} segundos")
      ConfigRow(label = "Gatilho de Variação Magnética", value = "± ${t.magneticDeltaUtd} µT")
      ConfigRow(label = "Gatilho de Pico de Áudio", value = "+ ${t.audioDeltaDbfs} dBFS acima da base")
      ConfigRow(label = "Gatilho de Aceleração Dinâmica", value = "> ${t.movementDeltaMps2} m/s²")
      ConfigRow(label = "Intervalo Mínimo Entre Alertas", value = "${t.alertDebounceSeconds} segundos")
    }
  }
}

@Composable
private fun ConfigRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      color = Color.LightGray,
      fontSize = 11.sp
    )
    Text(
      text = value,
      color = PrimaryNeonGreen,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}
