package com.beolddeok.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF38BDF8)
val Accent2 = Color(0xFF818CF8)
val DangerRed = Color(0xFFF43F5E)
val OkGreen = Color(0xFF34D399)

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = Accent2,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    error = DangerRed,
)

private val LightColors = lightColorScheme(
    primary = Accent,
    secondary = Accent2,
    error = DangerRed,
)

@Composable
fun BeolddeokTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
