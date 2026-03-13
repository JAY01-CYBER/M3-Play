package com.j.m3play.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val M3PlayColorScheme = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF11131A),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF131721),
    surfaceVariant = Color(0xFF1E2432)
)

@Composable
fun M3PlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = M3PlayColorScheme,
        content = content
    )
}
