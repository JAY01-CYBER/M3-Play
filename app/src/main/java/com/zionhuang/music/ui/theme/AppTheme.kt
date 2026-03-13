package com.zionhuang.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InnerTuneDarkColors = darkColorScheme(
    primary = Color(0xFFB88BFF),
    secondary = Color(0xFF7ED6C2),
    tertiary = Color(0xFFFFB4A2),
    background = Color(0xFF0E1016),
    surface = Color(0xFF171A22),
    onPrimary = Color(0xFF1F1238),
    onSecondary = Color(0xFF0C2D29),
    onBackground = Color(0xFFE9EAF0),
    onSurface = Color(0xFFE9EAF0),
    onSurfaceVariant = Color(0xFFB7B9C4)
)

private val InnerTuneLightColors = lightColorScheme(
    primary = Color(0xFF6A3EC4),
    background = Color(0xFFF8F7FF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF14151B),
    onSurface = Color(0xFF14151B)
)

@Composable
fun InnerTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) InnerTuneDarkColors else InnerTuneLightColors,
        content = content
    )
}
