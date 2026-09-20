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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.SessionState
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.TertiaryBright

@Composable
fun TacticalTopHeader(
  sessionState: SessionState,
  sessionDurationFormatted: String,
  locationStatus: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(GlassBackground)
      .border(1.dp, Color.White.copy(alpha = 0.05f))
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Lado esquerdo: Nome do aplicativo e Projeto Jairo Bahia
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          val statusColor = when (sessionState) {
            SessionState.MONITORANDO -> PrimaryNeonGreen
            SessionState.CALIBRANDO -> TertiaryBright
            SessionState.PAUSADO -> Color.Yellow
            SessionState.PARADO -> Color.Gray
          }

          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Spacer(modifier = Modifier.size(6.dp))
          Text(
            text = "PROJETO JAIRO BAHIA",
            color = Color.LightGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "PKE Scanner Brasil",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }

        Text(
          text = locationStatus,
          color = SecondaryCyan,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      // Lado direito: Estado da Sessão e Cronômetro
      Column(horizontalAlignment = Alignment.End) {
        val (stateText, stateColor) = when (sessionState) {
          SessionState.MONITORANDO -> "MONITORANDO" to PrimaryNeonGreen
          SessionState.CALIBRANDO -> "CALIBRANDO" to TertiaryBright
          SessionState.PAUSADO -> "PAUSADO" to Color.Yellow
          SessionState.PARADO -> "PARADO" to Color.Gray
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceContainerHigh)
            .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = stateText,
            color = stateColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = sessionDurationFormatted,
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}
