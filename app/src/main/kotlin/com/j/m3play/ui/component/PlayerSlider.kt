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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
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
    amplitude: Dp = 2.5.dp,
    strokeWidth: Dp = 5.dp,
    animated: Boolean = true,
) {
    // Material 3 Expressive's LinearWavyProgressIndicator gives us the actual
    // premium wave rendering. WavySlider owns only gesture mapping; no external
    // slider AAR is involved.
    WavySlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        isPlaying = animated,
        strokeWidth = strokeWidth,
        thumbRadius = 7.dp,
    )
}

/**
 * Premium circular-bead slider. It keeps Material 3's Slider interaction/state,
 * while drawing a custom expressive track so no external slider AAR is needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3PlayCircularSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val state = rememberPlayerSliderState(value, valueRange)
    val transition = rememberInfiniteTransition(label = "player_circular_slider")
    val pulse = transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "thumb_pulse",
    )

    Slider(
        state = state,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        modifier = modifier,
        thumb = { Spacer(Modifier.size(0.dp)) },
        track = { sliderState ->
            val range = sliderState.trackRange
            val fraction = if (range.endInclusive == range.start) 0f else
                ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            val active = colors.activeTrackColor
            val inactive = colors.inactiveTrackColor
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val cy = size.height / 2f
                val x = size.width * fraction
                val radius = 7.dp.toPx()
                val rail = 3.dp.toPx()
                val glow = if (animated) pulse.value else 1f

                drawLine(
                    color = inactive.copy(alpha = 0.38f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = rail,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = active,
                    start = Offset(0f, cy),
                    end = Offset(x, cy),
                    strokeWidth = rail + 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Subtle bead rhythm: circular markers become denser toward the active side.
                val spacing = 14.dp.toPx()
                var marker = spacing
                while (marker < size.width) {
                    if (marker > x + radius || marker < x - radius) {
                        drawCircle(
                            color = inactive.copy(alpha = 0.55f),
                            radius = 1.35.dp.toPx(),
                            center = Offset(marker, cy),
                        )
                    }
                    marker += spacing
                }

                if (x in 0f..size.width) {
                    drawCircle(
                        color = active.copy(alpha = 0.18f * glow),
                        radius = radius * 1.95f * glow,
                        center = Offset(x, cy),
                    )
                    drawCircle(
                        color = active,
                        radius = radius,
                        center = Offset(x, cy),
                    )
                    drawCircle(
                        color = colors.thumbColor,
                        radius = radius * 0.42f,
                        center = Offset(x, cy),
                    )
                }
            }
        },
    )
}
