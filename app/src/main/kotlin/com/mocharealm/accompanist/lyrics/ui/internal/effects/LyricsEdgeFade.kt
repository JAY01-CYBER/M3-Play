package com.mocharealm.accompanist.lyrics.ui.internal.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade

/** Apply inside an offscreen layer so the masks affect only the lyrics. */
internal fun Modifier.lyricsEdgeFade(top: LyricsFade, bottom: LyricsFade, anchor: Dp) =
    drawWithCache {
        fun LyricsFade.length(): Float = when (this) {
            is LyricsFade.Fixed -> length.toPx()
            is LyricsFade.Fraction -> size.height * fraction
            is LyricsFade.ToAnchor -> (anchor - inset).toPx()
        }.coerceIn(0f, size.height)

        val topLength = top.length()
        val bottomLength = bottom.length()
        val topBrush = if (topLength > 0f) Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black), startY = 0f, endY = topLength,
        ) else null
        val bottomBrush = if (bottomLength > 0f) Brush.verticalGradient(
            listOf(Color.Black, Color.Transparent),
            startY = size.height - bottomLength, endY = size.height,
        ) else null
        onDrawWithContent {
            drawContent()
            topBrush?.let { drawRect(it, blendMode = BlendMode.DstIn) }
            bottomBrush?.let { drawRect(it, blendMode = BlendMode.DstIn) }
        }
    }
