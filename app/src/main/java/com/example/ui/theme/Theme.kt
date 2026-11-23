package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RpgDarkColorScheme = darkColorScheme(
  primary = RelicGold,
  onPrimary = VoidNavy,
  primaryContainer = AstralPanel,
  onPrimaryContainer = StarlightWhite,
  secondary = MythrilCyan,
  onSecondary = VoidNavy,
  secondaryContainer = Color(0xFF0E2F44),
  onSecondaryContainer = MythrilCyan,
  tertiary = SoulBreakMagenta,
  onTertiary = StarlightWhite,
  tertiaryContainer = Color(0xFF3E132D),
  onTertiaryContainer = Color(0xFFFF80AB),
  background = VoidNavy,
  onBackground = StarlightWhite,
  surface = DeepCrystalBlue,
  onSurface = StarlightWhite,
  surfaceVariant = AstralPanel,
  onSurfaceVariant = MutedSilver,
  outline = AstralBorder,
  error = DangerCrimson,
  onError = VoidNavy
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = RpgDarkColorScheme,
    typography = Typography,
    content = content
  )
}
