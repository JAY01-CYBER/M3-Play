package com.mocharealm.accompanist.lyrics.ui.internal.effects

import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDotsDefaults

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/** Static phase boundaries ported from feature-text-engine renderer/draw.rs. */
internal class PreparedBreathingDots(
    startMs: Int,
    endMs: Int,
    defaults: KaraokeBreathingDotsDefaults,
) {
    val number = defaults.number.coerceIn(1, 8)
    val duration = (endMs.toLong() - startMs).coerceAtLeast(1).toFloat()
    private val start = startMs
    private val valid = endMs > startMs
    private val enter = defaults.enterDurationMs.coerceAtLeast(1).toFloat()
    private val dip = defaults.preExitDipAndRiseDuration.coerceAtLeast(1).toFloat()
    private val still = defaults.preExitStillDuration.coerceAtLeast(0).toFloat()
    private val exit = defaults.exitDurationMs.coerceAtLeast(1).toFloat()
    private val factor = (duration / (enter + dip + still + exit)).coerceAtMost(1f)
    val enterEnd = enter * factor
    val exitStart = duration - exit * factor
    val stillStart = exitStart - still * factor
    val dipStart = stillStart - dip * factor
    private val breathingDuration = (dipStart - enterEnd).coerceAtLeast(0f)
    private val hasBreathing = breathingDuration > 16f
    private val halfCycles =
        (breathingDuration / 1500f).roundToInt().coerceAtLeast(1).let {
            if (it % 2 == 0) it + 1 else it
        }
    private val period = 2f * breathingDuration / halfCycles
    private val dotSpan = (exitStart - enterEnd).coerceAtLeast(1f) / number

    fun elapsed(timeMs: Int) = (timeMs.toLong() - start).toFloat()

    fun scale(time: Float): Float =
        when {
            !valid -> 0f
            time < enterEnd -> smooth(time / enterEnd) * if (hasBreathing) 0.8f else 1f
            hasBreathing && time < dipStart ->
                0.9f - 0.1f * cos((time - enterEnd) / period * 2f * PI.toFloat())
            time < dipStart -> 1f
            time < stillStart ->
                0.8f +
                    0.2f *
                        cos(
                            ((time - dipStart) / (stillStart - dipStart).coerceAtLeast(0.000001f))
                                .coerceIn(0f, 1f) * 2f * PI.toFloat()
                        )
            time < exitStart -> 1f
            else -> smooth((duration - time) / (duration - exitStart).coerceAtLeast(0.000001f))
        }

    fun alpha(time: Float): Float =
        when {
            !valid -> 0f
            time < enterEnd -> smooth(time / enterEnd)
            time < exitStart -> 1f
            else -> smooth((duration - time) / (duration - exitStart).coerceAtLeast(0.000001f))
        }

    fun dotAlpha(index: Int, time: Float) =
        0.2f + 0.8f * ((time - enterEnd - dotSpan * index) / dotSpan).coerceIn(0f, 1f)

    fun visibility(time: Float): Float =
        if (!valid || time < 0f || time >= duration) 0f
        else minOf(smooth(time / 220f), smooth((duration - time) / 220f))

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
