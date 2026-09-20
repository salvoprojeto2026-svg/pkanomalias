package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TacticalColorScheme = darkColorScheme(
  primary = PrimaryNeonGreen,
  onPrimary = OnPrimary,
  primaryContainer = PrimaryNeonGreen,
  onPrimaryContainer = OnPrimaryContainer,
  secondary = SecondaryCyan,
  onSecondary = OnSecondary,
  secondaryContainer = SecondaryDimCyan,
  tertiary = TertiaryAmber,
  onTertiary = OnTertiary,
  tertiaryContainer = TertiaryContainer,
  background = SurfaceDark,
  onBackground = OnSurface,
  surface = SurfaceDark,
  onSurface = OnSurface,
  surfaceVariant = SurfaceContainerHigh,
  onSurfaceVariant = OnSurfaceVariant,
  surfaceContainer = SurfaceContainer,
  surfaceContainerLow = SurfaceContainerLow,
  surfaceContainerLowest = SurfaceContainerLowest,
  surfaceContainerHigh = SurfaceContainerHigh,
  surfaceContainerHighest = SurfaceContainerHighest,
  error = ErrorRed,
  onError = OnError,
  errorContainer = ErrorContainer,
  outline = OutlineTactical,
  outlineVariant = OutlineVariant
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TacticalColorScheme,
    typography = Typography,
    content = content
  )
}
