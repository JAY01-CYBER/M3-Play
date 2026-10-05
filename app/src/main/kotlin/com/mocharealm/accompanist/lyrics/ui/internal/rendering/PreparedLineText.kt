package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.internal.effects.LyricsReveal
import com.mocharealm.accompanist.lyrics.ui.internal.effects.LyricsRevealSpring
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealAlpha
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealBlurIndex
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealScale
import com.mocharealm.accompanist.lyrics.ui.internal.diagnostics.traceLyrics
import com.mocharealm.accompanist.lyrics.ui.internal.playback.LyricsPlaybackState
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.LyricsRenderResources
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.RowGlowLayers
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.RowPaints
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.drawPreparedRow
import kotlin.math.roundToInt

@Composable
internal fun PreparedLineText(
    prepared: PreparedLine,
    playback: LyricsPlaybackState,
    resources: LyricsRenderResources,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 8.dp,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    showDebugRectangles: Boolean = false,
) {
    val density = LocalDensity.current
    val activeColor = resources.color
    val raster = resources.raster(prepared)
    val paints = remember(activeColor) { RowPaints(activeColor) }
    val alignment = if (prepared.rightAligned) Alignment.End else Alignment.Start
    val phoneticProgress =
        animateFloatAsState(
            if (showPhonetic) 1f else 0f,
            LyricsRevealSpring,
            label = "inlinePhonetic",
        )
    LyricsReveal(
        visible = playback.line(prepared).visible.value,
        animateInitial = prepared.source is KaraokeLine.AccompanimentKaraokeLine,
        origin =
            if (prepared.source is KaraokeLine.AccompanimentKaraokeLine)
                androidx.compose.ui.graphics.TransformOrigin(
                    if (prepared.rightAligned) 1f else 0f,
                    if (prepared.revealFromBottom) 1f else 0f,
                )
            else androidx.compose.ui.graphics.TransformOrigin.Center,
    ) {
        Column(
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = verticalPadding,
                    horizontal =
                        if (prepared.source is KaraokeLine.AccompanimentKaraokeLine) 0.dp else 16.dp,
                ),
            horizontalAlignment = alignment,
        ) {
            for (line in prepared.before) PreparedLineText(
                line,
                playback = playback,
                resources = resources,
                showTranslation = showTranslation,
                showPhonetic = showPhonetic,
                showDebugRectangles = showDebugRectangles,
            )
            // Separate draw scopes mean a ticking row cannot invalidate its static neighbours.
            Column(Modifier.fillMaxWidth()) {
                for ((index, row) in prepared.rows.withIndex()) {
                    val clock = playback.row(row).time
                    val renderState = resources.row(row)
                    Box(
                        Modifier.fillMaxWidth()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val hidden =
                                    row.phoneticHeight * (1f - phoneticProgress.value.coerceIn(0f, 1f))
                                layout(
                                    placeable.width,
                                    (placeable.height - hidden).roundToInt().coerceAtLeast(0),
                                ) {
                                    placeable.place(0, 0)
                                }
                            }
                            .height(with(density) { row.height.toDp() })
                            .drawWithCache {
                                val layers = raster.rows[index]
                                val glows =
                                    layers
                                        .takeIf { it.hasGlow || it.hasPhonetics }
                                        ?.let {
                                            traceLyrics("Lyrics.layerCache") { RowGlowLayers(this, it) }
                                        }
                                onDrawBehind {
                                    val progress = phoneticProgress.value.coerceIn(0f, 1f)
                                    translate(
                                        top = -row.top - row.phoneticHeight * (1f - progress)
                                    ) {
                                        drawPreparedRow(
                                            row,
                                            clock.intValue,
                                            renderState,
                                            activeColor,
                                            paints,
                                            showDebugRectangles,
                                            layers,
                                            glows,
                                            progress,
                                        )
                                    }
                                }
                            }
                    )
                }
            }
            if (prepared.translation != null || prepared.phonetic != null) {
                val captionGap =
                    animateFloatAsState(
                        if (
                            (showTranslation && prepared.translation != null) ||
                                (showPhonetic && prepared.phonetic != null)
                        )
                            8f
                        else 0f,
                        LyricsRevealSpring,
                        label = "captionGap",
                    )
                Spacer(
                    Modifier.layout { _, constraints ->
                        layout(
                            constraints.minWidth,
                            constraints.constrainHeight(captionGap.value.dp.roundToPx()),
                        ) {}
                    }
                )
            }
            prepared.translation?.let { layout ->
                LyricsReveal(showTranslation) {
                    Canvas(
                        Modifier.size(
                            with(density) { layout.size.width.toDp() },
                            with(density) { layout.size.height.toDp() },
                        )
                    ) {
                        drawText(layout, paints.phoneticColor)
                    }
                }
            }
            prepared.phonetic?.let { layout ->
                LyricsReveal(showPhonetic) {
                    Canvas(
                        Modifier.size(
                            with(density) { layout.size.width.toDp() },
                            with(density) { layout.size.height.toDp() },
                        )
                    ) {
                        drawText(layout, paints.phoneticColor)
                    }
                }
            }
            for (line in prepared.after) PreparedLineText(
                line,
                playback = playback,
                resources = resources,
                showTranslation = showTranslation,
                showPhonetic = showPhonetic,
                showDebugRectangles = showDebugRectangles,
            )
        }
    }
}
