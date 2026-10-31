package com.mocharealm.accompanist.lyrics.ui.internal.layout

import androidx.compose.animation.core.*

/** Springs already consume velocity. Duration-based scrolls need a continuous handoff too. */
internal fun lyricsFollowRetargetSpec(spec: AnimationSpec<Float>): AnimationSpec<Float> =
    if (spec is DurationBasedAnimationSpec<Float>) LyricsFollowRetargetSpec(spec) else spec

/**
 * Keep the caller's duration and curve instead of replacing every retarget with a fast spring.
 * A cubic correction carries incoming velocity, then vanishes with zero slope at the endpoint.
 * This belongs to the scroll actor; per-row springs retain their own configuration and state.
 */
internal class LyricsFollowRetargetSpec(source: DurationBasedAnimationSpec<Float>) : FloatAnimationSpec {
    private val native = source.vectorize(Float.VectorConverter)
    private var initial = Float.NaN
    private var target = Float.NaN
    private var velocity = Float.NaN
    private lateinit var initialVector: AnimationVector1D
    private lateinit var targetVector: AnimationVector1D
    private lateinit var velocityVector: AnimationVector1D
    private var duration = 0L
    private var durationSeconds = 0f
    private var velocityCorrection = 0f

    private fun bind(initialValue: Float, targetValue: Float, initialVelocity: Float) {
        if (initial == initialValue && target == targetValue && velocity == initialVelocity) return
        initial = initialValue
        target = targetValue
        velocity = initialVelocity
        initialVector = AnimationVector1D(initialValue)
        targetVector = AnimationVector1D(targetValue)
        velocityVector = AnimationVector1D(initialVelocity)
        duration = native.getDurationNanos(initialVector, targetVector, velocityVector)
        durationSeconds = duration / 1_000_000_000f
        val sampleTime = minOf(1_000_000L, duration)
        val start = native.getValueFromNanos(0L, initialVector, targetVector, velocityVector).value
        val next = native.getValueFromNanos(sampleTime, initialVector, targetVector, velocityVector).value
        val nativeStartVelocity = if (sampleTime > 0) (next - start) / (sampleTime / 1_000_000_000f) else 0f
        velocityCorrection = initialVelocity - nativeStartVelocity
    }

    override fun getDurationNanos(initialValue: Float, targetValue: Float, initialVelocity: Float): Long {
        bind(initialValue, targetValue, initialVelocity)
        return duration
    }

    override fun getValueFromNanos(playTimeNanos: Long, initialValue: Float, targetValue: Float, initialVelocity: Float): Float {
        bind(initialValue, targetValue, initialVelocity)
        val value = native.getValueFromNanos(playTimeNanos, initialVector, targetVector, velocityVector).value
        if (duration <= 0) return value
        val t = (playTimeNanos.toDouble() / duration).toFloat().coerceIn(0f, 1f)
        return value + velocityCorrection * durationSeconds * t * (1f - t) * (1f - t)
    }

    override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: Float, targetValue: Float, initialVelocity: Float): Float {
        bind(initialValue, targetValue, initialVelocity)
        if (playTimeNanos <= 0L && duration > 0L) return initialVelocity
        val value = native.getVelocityFromNanos(playTimeNanos, initialVector, targetVector, velocityVector).value
        if (duration <= 0) return value
        val t = (playTimeNanos.toDouble() / duration).toFloat().coerceIn(0f, 1f)
        return value + velocityCorrection * (1f - t) * (1f - 3f * t)
    }
}
