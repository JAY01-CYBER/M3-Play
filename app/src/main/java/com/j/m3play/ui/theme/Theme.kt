package com.j.m3play.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.j.m3play.ui.designsystem.harmonize

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    secondary = Color(0xFF00B894),
    tertiary = Color(0xFFE17055),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA29BFE),
    secondary = Color(0xFF55EFC4),
    tertiary = Color(0xFFFFB8A8),
)

@Composable
fun M3PlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors
    val harmonized = base.copy(
        secondary = base.secondary.harmonize(base.primary, 0.35f),
        tertiary = base.tertiary.harmonize(base.primary, 0.2f),
    )

    MaterialTheme(
        colorScheme = harmonized,
        content = content,
    )
}
