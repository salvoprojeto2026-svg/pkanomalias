package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorderGreen
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.viewmodel.TacticalNavDestination

@Composable
fun TacticalBottomBar(
  currentTab: TacticalNavDestination,
  onTabSelected: (TacticalNavDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(GlassBackground)
        .border(1.dp, GlassBorderGreen, RoundedCornerShape(16.dp))
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      NavItem(
        title = "MONITORAR",
        icon = Icons.Default.Sensors,
        isSelected = currentTab == TacticalNavDestination.MONITORAR,
        onClick = { onTabSelected(TacticalNavDestination.MONITORAR) }
      )

      NavItem(
        title = "CÂMERA",
        icon = Icons.Default.CameraAlt,
        isSelected = currentTab == TacticalNavDestination.CAMERA,
        onClick = { onTabSelected(TacticalNavDestination.CAMERA) }
      )

      NavItem(
        title = "REGISTROS",
        icon = Icons.Default.Description,
        isSelected = currentTab == TacticalNavDestination.REGISTROS,
        onClick = { onTabSelected(TacticalNavDestination.REGISTROS) }
      )

      NavItem(
        title = "EQUIPAMENTO",
        icon = Icons.Default.Memory,
        isSelected = currentTab == TacticalNavDestination.EQUIPAMENTO,
        onClick = { onTabSelected(TacticalNavDestination.EQUIPAMENTO) }
      )
    }
  }
}

@Composable
private fun NavItem(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val contentColor = if (isSelected) PrimaryNeonGreen else Color.LightGray.copy(alpha = 0.6f)
  val bgColor = if (isSelected) SurfaceContainerHigh.copy(alpha = 0.9f) else Color.Transparent

  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .then(
        if (isSelected) {
          Modifier.border(1.dp, PrimaryNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        } else {
          Modifier
        }
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = contentColor,
      modifier = Modifier.size(20.dp)
    )
    Text(
      text = title,
      color = contentColor,
      fontSize = 9.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 0.5.sp
    )
  }
}
