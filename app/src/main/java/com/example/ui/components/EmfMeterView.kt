package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorBright
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorderGreen
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.TertiaryAmber

@Composable
fun EmfMeterView(
  emfValue: Double,
  modifier: Modifier = Modifier
) {
  val totalSegments = 16
  val maxScale = 10.0
  val activeCount = ((emfValue / maxScale) * totalSegments).toInt().coerceIn(0, totalSegments)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, GlassBorderGreen, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    // Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ElectricBolt,
          contentDescription = "EMF Icon",
          tint = PrimaryNeonGreen,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
          text = "FLUXO VETORIAL EMF",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif
        )
      }

      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = String.format("%.2f", emfValue),
          color = if (emfValue >= 9.0) ErrorBright else if (emfValue >= 5.0) TertiaryAmber else PrimaryNeonGreen,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.size(3.dp))
        Text(
          text = "mG",
          color = Color.LightGray,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 16-Segment LED Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      for (i in 0 until totalSegments) {
        val isActive = i < activeCount
        val segmentColor = when {
          !isActive -> SurfaceContainerHighest.copy(alpha = 0.35f)
          i < 10 -> PrimaryNeonGreen
          i < 13 -> TertiaryAmber
          else -> ErrorBright
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .height(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(segmentColor)
            .then(
              if (isActive) {
                Modifier.shadow(2.dp, RoundedCornerShape(2.dp), spotColor = segmentColor)
              } else {
                Modifier
              }
            )
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Threshold Marks
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "0.0 REFERÊNCIA",
        color = Color.Gray,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "5.0 LIMIAR DE PICO",
        color = TertiaryAmber,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "10.0 CRÍTICO",
        color = ErrorBright,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
