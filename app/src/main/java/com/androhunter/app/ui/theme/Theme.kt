package com.androhunter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HunterGreen   = Color(0xFF00FF88)
val HunterBg      = Color(0xFF080C10)
val HunterSurface = Color(0xFF0D1117)
val HunterCard    = Color(0xFF161B22)
val HunterBorder  = Color(0xFF30363D)
val HunterRed     = Color(0xFFFF4444)
val HunterYellow  = Color(0xFFFFAA00)
val HunterBlue    = Color(0xFF58A6FF)
val HunterPurple  = Color(0xFFBC8CFF)
val HunterTextDim = Color(0xFF8B949E)
val HunterDim     = Color(0xFF8B949E)
val HunterText    = Color(0xFFE2E8F0)

private val DarkColors = darkColorScheme(
    primary        = HunterGreen,
    onPrimary      = HunterBg,
    background     = HunterBg,
    surface        = HunterSurface,
    onBackground   = HunterGreen,
    onSurface      = HunterGreen,
    secondary      = HunterBlue,
    error          = HunterRed,
)

@Composable
fun AndroHunterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
