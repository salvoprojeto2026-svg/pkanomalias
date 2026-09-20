package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorBright
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerLowest
import kotlin.math.sin

@Composable
fun SpectralOscilloscopeView(
  phase: Float,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, GlassBorderCyan, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = "Audio Icon",
          tint = SecondaryCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
          text = "ÁUDIO ESPECTRAL EVP",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "414 Hz",
          color = SecondaryCyan,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier
            .background(Color(0xFF272A31), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(
          text = "-12.4 dB",
          color = Color.LightGray,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Wave Display Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(SurfaceContainerLowest)
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Grid lines
        drawLine(
          color = Color.White.copy(alpha = 0.06f),
          start = Offset(0f, midY),
          end = Offset(width, midY),
          strokeWidth = 1.dp.toPx()
        )
        drawLine(
          color = Color.White.copy(alpha = 0.04f),
          start = Offset(0f, midY - 12.dp.toPx()),
          end = Offset(width, midY - 12.dp.toPx()),
          strokeWidth = 1.dp.toPx()
        )
        drawLine(
          color = Color.White.copy(alpha = 0.04f),
          start = Offset(0f, midY + 12.dp.toPx()),
          end = Offset(width, midY + 12.dp.toPx()),
          strokeWidth = 1.dp.toPx()
        )

        // Sine wave path
        val path = Path()
        val step = 4f
        var first = true

        var x = 0f
        while (x <= width) {
          val normalizedX = x / width
          val waveAmp = 12.dp.toPx() * (0.8f + 0.3f * sin(phase * 1.8f))
          val y = midY + sin(normalizedX * 4 * Math.PI.toFloat() + phase) * waveAmp
          if (first) {
            path.moveTo(x, y)
            first = false
          } else {
            path.lineTo(x, y)
          }
          x += step
        }

        drawPath(
          path = path,
          color = SecondaryCyan,
          style = Stroke(width = 2.dp.toPx())
        )
      }

      // Recording badge
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(ErrorBright)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
          text = "GRAVANDO",
          color = ErrorBright,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Subtitle Info
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "VALÊNCIA: -8.7 EV (NEGATIVA)",
        color = Color.LightGray,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "ECO POLIFÔNICO DETECTADO",
        color = SecondaryCyan,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
