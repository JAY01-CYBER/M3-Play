package com.zionhuang.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InnerTuneDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFB5CCFF),
    tertiary = Color(0xFFFFB59D),
    background = Color(0xFF0A0B10),
    surface = Color(0xFF12131A),
    surfaceContainer = Color(0xFF1A1D26),
    surfaceContainerLow = Color(0xFF141720),
    surfaceContainerHigh = Color(0xFF222633),
    surfaceContainerHighest = Color(0xFF2A3040),
    secondaryContainer = Color(0xFF2A2F45),
    onPrimary = Color(0xFF28124A),
    onSecondary = Color(0xFF1A2A4D),
    onBackground = Color(0xFFE8EAF2),
    onSurface = Color(0xFFE8EAF2),
    onSurfaceVariant = Color(0xFFB7BDCF)
)

private val InnerTuneLightColors = lightColorScheme(
    primary = Color(0xFF6A3EC4),
    secondary = Color(0xFF355EBC),
    tertiary = Color(0xFFB94E2A),
    background = Color(0xFFF5F4FF),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0EEFF),
    surfaceContainerLow = Color(0xFFF6F5FF),
    surfaceContainerHigh = Color(0xFFE8E5FF),
    surfaceContainerHighest = Color(0xFFDFDBFF),
    secondaryContainer = Color(0xFFE2E8FF),
    onBackground = Color(0xFF14151B),
    onSurface = Color(0xFF14151B),
    onSurfaceVariant = Color(0xFF51556B)
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
