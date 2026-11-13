package com.j.m3play.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.palette.graphics.Palette

/** Artwork-derived immersive backdrop, following SimpMusic's dominant-swatch treatment. */
fun Palette?.toImmersiveBackground(): Color {
    val palette = this ?: return Color.Black
    val rgb = palette.getDominantColor(0).takeIf { it != 0 }
        ?: palette.getMutedColor(0).takeIf { it != 0 }
        ?: palette.getVibrantColor(0).takeIf { it != 0 }
        ?: return Color.Black
    val base = Color(rgb)
    val luminance = 0.299f * base.red + 0.587f * base.green + 0.114f * base.blue
    val darkenFactor = 0.35f + 0.45f * luminance
    return androidx.compose.ui.graphics.lerp(base, Color.Black, darkenFactor)
}

/** Hold the last resolved palette while KMPalette recalculates, then animate the color change. */
@Composable
fun rememberPaletteColor(
    palette: Palette?,
    fallback: Color = Color.Black,
    pick: (Palette) -> Color = { it.toImmersiveBackground() },
): Color {
    var held by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(palette) { palette?.let { held = pick(it).toArgb() } }
    val target = held?.let { Color(it) } ?: fallback
    return animateColorAsState(targetValue = target, animationSpec = tween(500), label = "playlistPaletteColor").value
}
