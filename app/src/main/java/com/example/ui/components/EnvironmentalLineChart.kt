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

@Composable
fun EnvironmentalLineChart(
  title: String,
  currentValueFormatted: String,
  baselineFormatted: String? = null,
  unit: String,
  historyPoints: List<Float>,
  lineColor: Color = PrimaryNeonGreen,
  minValue: Float = 0f,
  maxValue: Float = 100f,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, lineColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = title,
            color = lineColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          if (baselineFormatted != null) {
            Text(
              text = "Ref. Calibrada: $baselineFormatted",
              color = Color.Gray,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = currentValueFormatted,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = " $unit",
            color = lineColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Canvas do Gráfico Real
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(65.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF020905))
      ) {
        val w = size.width
        val h = size.height

        // Linhas de grade sutis
        val gridLines = 3
        for (i in 1..gridLines) {
          val y = (h / (gridLines + 1)) * i
          drawLine(
            color = lineColor.copy(alpha = 0.1f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
          )
        }

        if (historyPoints.size >= 2) {
          // Determina escalas mínima e máxima reais com margem
          var actualMin = historyPoints.minOrNull() ?: minValue
          var actualMax = historyPoints.maxOrNull() ?: maxValue
          if (actualMin == actualMax) {
            actualMin -= 1f
            actualMax += 1f
          }
          val range = actualMax - actualMin

          val path = Path()
          val stepX = w / (historyPoints.size - 1).coerceAtLeast(1)

          historyPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = ((value - actualMin) / range).coerceIn(0f, 1f)
            val y = h - (normalized * (h - 10.dp.toPx())) - 5.dp.toPx()

            if (index == 0) {
              path.moveTo(x, y)
            } else {
              path.lineTo(x, y)
            }
          }

          // Traça linha do sinal
          drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
          )

          // Ponto final (leitura mais recente)
          val lastX = (historyPoints.size - 1) * stepX
          val lastVal = historyPoints.last()
          val lastNorm = ((lastVal - actualMin) / range).coerceIn(0f, 1f)
          val lastY = h - (lastNorm * (h - 10.dp.toPx())) - 5.dp.toPx()

          drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(lastX, lastY)
          )
          drawCircle(
            color = lineColor,
            radius = 2.dp.toPx(),
            center = Offset(lastX, lastY)
          )
        } else {
          // Linha guia basal enquanto acumula leituras
          drawLine(
            color = lineColor.copy(alpha = 0.4f),
            start = Offset(0f, h / 2),
            end = Offset(w, h / 2),
            strokeWidth = 1.5.dp.toPx()
          )
        }
      }
    }
  }
}
