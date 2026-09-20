package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalCompassView(
  azimuthDegrees: Float?,
  cardinalDirection: String,
  pitchDegrees: Float?,
  rollDegrees: Float?,
  magneticAccuracy: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(GlassBackground)
      .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
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
            text = "ORIENTAÇÃO & BÚSSOLA REAL",
            color = PrimaryNeonGreen,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "Calibração: $magneticAccuracy",
            color = Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerHigh)
            .border(1.dp, SecondaryCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (azimuthDegrees != null) "${azimuthDegrees.toInt()}° $cardinalDirection" else "CALCULANDO...",
            color = SecondaryCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        // Mostrador Circular da Bússola
        Box(
          modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(Color(0xFF041009))
            .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.3f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          val heading = azimuthDegrees ?: 0f

          Canvas(modifier = Modifier.size(120.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 8.dp.toPx()

            // Círculos concêntricos e marcações dos eixos
            drawCircle(
              color = PrimaryNeonGreen.copy(alpha = 0.15f),
              radius = radius,
              center = center,
              style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
              color = PrimaryNeonGreen.copy(alpha = 0.08f),
              radius = radius * 0.6f,
              center = center,
              style = Stroke(width = 1.dp.toPx())
            )

            // Graduações a cada 30 graus
            for (angle in 0 until 360 step 30) {
              val rad = Math.toRadians((angle - heading - 90).toDouble())
              val isCardinal = angle % 90 == 0
              val markLen = if (isCardinal) 10.dp.toPx() else 5.dp.toPx()
              val p1 = Offset(
                (center.x + (radius - markLen) * cos(rad)).toFloat(),
                (center.y + (radius - markLen) * sin(rad)).toFloat()
              )
              val p2 = Offset(
                (center.x + radius * cos(rad)).toFloat(),
                (center.y + radius * sin(rad)).toFloat()
              )
              drawLine(
                color = if (angle == 0) Color.Red else PrimaryNeonGreen.copy(alpha = if (isCardinal) 0.6f else 0.25f),
                start = p1,
                end = p2,
                strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
              )
            }

            // Agulha Norte (Vermelha) apontando para o Norte Magnético relativo
            val northRad = Math.toRadians((-heading - 90).toDouble())
            val southRad = Math.toRadians((-heading + 90).toDouble())
            val needleLen = radius * 0.75f

            val northPoint = Offset(
              (center.x + needleLen * cos(northRad)).toFloat(),
              (center.y + needleLen * sin(northRad)).toFloat()
            )
            val southPoint = Offset(
              (center.x + needleLen * cos(southRad)).toFloat(),
              (center.y + needleLen * sin(southRad)).toFloat()
            )

            // Linha Sul (Verde claro / ciano)
            drawLine(
              color = SecondaryCyan.copy(alpha = 0.8f),
              start = center,
              end = southPoint,
              strokeWidth = 3.dp.toPx(),
              cap = StrokeCap.Round
            )

            // Linha Norte (Vermelho)
            drawLine(
              color = Color(0xFFFF4444),
              start = center,
              end = northPoint,
              strokeWidth = 3.dp.toPx(),
              cap = StrokeCap.Round
            )

            // Pivô central
            drawCircle(
              color = PrimaryNeonGreen,
              radius = 3.dp.toPx(),
              center = center
            )
          }

          // Letra N estática no topo do display
          Text(
            text = "N",
            color = Color(0xFFFF5555),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 2.dp)
          )
        }

        // Informações angulares complementares
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OrientationRow(label = "RUMO", value = cardinalDirection)
          OrientationRow(
            label = "AZIMUTE",
            value = azimuthDegrees?.let { "${String.format("%.1f", it)}°" } ?: "---"
          )
          OrientationRow(
            label = "INCLINAÇÃO",
            value = pitchDegrees?.let { "${String.format("%.1f", it)}°" } ?: "---"
          )
          OrientationRow(
            label = "ROLAGEM",
            value = rollDegrees?.let { "${String.format("%.1f", it)}°" } ?: "---"
          )
        }
      }
    }
  }
}

@Composable
private fun OrientationRow(label: String, value: String) {
  Row(
    modifier = Modifier.padding(vertical = 1.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$label: ",
      color = Color.Gray,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}
