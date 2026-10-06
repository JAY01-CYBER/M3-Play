/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Component Module
 *
 * Reusable UI building block
 * Signature: M3PLAY::COMPONENT::V1
 */

package com.j.m3play.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.trackRange
    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        drawTrack(
            stepsToTickFractions(sliderState.steps),
            0f,
            calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
            ),
            inactiveTrackColor,
            activeTrackColor,
            inactiveTickColor,
            activeTickColor,
            trackHeight
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()
    drawLine(
        inactiveTrackColor,
        sliderStart,
        sliderEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    val sliderValueEnd = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        center.y
    )
    val sliderValueStart = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeStart,
        center.y
    )
    drawLine(
        activeTrackColor,
        sliderValueStart,
        sliderValueEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

/** Keep the slider in sync with playback and recreate its bounds when the track changes. */
@Composable
fun rememberPlayerSliderState(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
): SliderState = androidx.compose.runtime.remember(valueRange, steps) {
    SliderState(value = value, steps = steps, trackRange = valueRange)
}.also { it.value = value }


/**
 * Material 3 Slider compatibility wrapper for M3Play.
 *
 * Material3 1.5.x moved Slider to the stateful API. Keeping all legacy-style
 * call sites behind this wrapper prevents binary ABI mismatches with libraries
 * compiled against older Material3 Slider overloads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3PlaySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = { _ ->
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled,
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState,
        )
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val state = rememberPlayerSliderState(value, valueRange, steps)
    Slider(
        state = state,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )
}

/**
 * Local Material3-1.5-compatible wavy player slider.
 * This replaces the old squigglyslider AAR, whose bytecode was compiled
 * against Material3 1.2.0 and therefore could call a removed Slider overload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3PlayWavySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    modifier: Modifier = Modifier,
    amplitude: Dp = 2.dp,
    strokeWidth: Dp = 6.dp,
) {
    val state = rememberPlayerSliderState(value, valueRange)
    Slider(
        state = state,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        modifier = modifier,
        thumb = { Spacer(Modifier.size(0.dp)) },
        track = { sliderState ->
            val range = sliderState.trackRange
            val fraction = if (range.endInclusive == range.start) {
                0f
            } else {
                ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            }
            val activeColor = colors.activeTrackColor
            val inactiveColor = colors.inactiveTrackColor
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(strokeWidth + 8.dp)
            ) {
                val centerY = size.height / 2f
                val amplitudePx = amplitude.toPx()
                val strokePx = strokeWidth.toPx()
                val cycles = maxOf(1f, size.width / 22.dp.toPx())
                val activeEnd = size.width * fraction

                fun drawWave(startX: Float, endX: Float, color: Color) {
                    if (endX <= startX) return
                    val path = Path()
                    val samples = maxOf(24, ((endX - startX) / 3f).toInt())
                    for (index in 0..samples) {
                        val t = index.toFloat() / samples.toFloat()
                        val x = startX + (endX - startX) * t
                        val y = centerY + amplitudePx * sin(t * cycles * 2f * PI).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }

                drawWave(0f, activeEnd, activeColor)
                drawWave(activeEnd, size.width, inactiveColor)
            }
        },
    )
}
