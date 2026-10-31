package com.mocharealm.accompanist.lyrics.ui.internal.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.time.TimeSource

private fun prefetchClock(): () -> Long {
    val start = TimeSource.Monotonic.markNow()
    return { start.elapsedNow().inWholeNanoseconds }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LyricsItemPrefetch(
    val state: LazyLayoutPrefetchState,
    private val nowNanos: () -> Long = prefetchClock(),
    private val request: (Int, Constraints) -> (() -> Unit) = { index, constraints ->
        val handle = state.schedulePrecompositionAndPremeasure(index, constraints)
        val cancel: () -> Unit = { handle.cancel() }
        cancel
    },
) {
    private var first = 0
    private var end = 0
    private var count = 0
    private var forward = true
    private var constraints = Constraints()
    private var averageExtent = 200f
    private var viewport = 600f
    private var velocity = 0f
    private var lastSample = Long.MIN_VALUE
    private var hasDirection = false
    private var pendingReverseDistance = 0f
    private var scheduledNext = Int.MIN_VALUE
    private var scheduledDirection = 0
    private var scheduledDesired = -1
    private val requested = IntArray(3) { -1 }
    private val cancellations = arrayOfNulls<() -> Unit>(3)

    fun measured(
        first: Int,
        end: Int,
        count: Int,
        constraints: Constraints,
        averageExtent: Float = 200f,
        viewport: Float = 600f,
    ) {
        if (this.constraints != constraints) cancel()
        this.first = first
        this.end = end
        this.count = count
        this.constraints = constraints
        this.averageExtent = averageExtent.coerceAtLeast(1f)
        this.viewport = viewport.coerceAtLeast(1f)
        if (pendingReverseDistance == 0f) schedule()
    }

    fun onScroll(delta: Float, retainDirectionOnSmallReversal: Boolean = false) {
        if (delta == 0f) return
        val now = nowNanos()
        val elapsedMs = if (lastSample == Long.MIN_VALUE) 16f else (now - lastSample) / 1_000_000f
        val newForward = delta > 0f
        var directionChanged = false
        if (!hasDirection) {
            directionChanged = newForward != forward
            forward = newForward
            hasDirection = true
        } else if (newForward != forward) {
            if (retainDirectionOnSmallReversal) {
                pendingReverseDistance += abs(delta)
                if (pendingReverseDistance < maxOf(24f, averageExtent * 0.25f)) {
                    // Follow springs often cross zero for only a few pixels. Keep the current
                    // prefetch window until the reversal has enough travel to be meaningful.
                    velocity = 0f
                    lastSample = now
                    return
                }
            }
            forward = newForward
            pendingReverseDistance = 0f
            velocity = 0f
            directionChanged = true
        } else {
            pendingReverseDistance = 0f
        }
        if (elapsedMs > 250f) velocity = 0f
        // Several deltas in the same frame must not imply an unbounded velocity.
        val sample = abs(delta) / elapsedMs.coerceAtLeast(8f)
        velocity = if (velocity == 0f) sample else velocity * 0.65f + sample * 0.35f
        lastSample = now
        if (directionChanged || desiredCount() != scheduledDesired) schedule()
    }

    private fun schedule() {
        val desired = desiredCount()
        val next = if (forward) end else first - 1
        val direction = if (forward) 1 else -1
        if (
            next == scheduledNext &&
                direction == scheduledDirection &&
                desired == scheduledDesired
        ) return
        scheduledNext = next
        scheduledDirection = direction
        scheduledDesired = desired
        // Keep overlapping requests when the measured range advances; cancel only obsolete work.
        for (slot in requested.indices) {
            val distance = (requested[slot] - next) * direction
            if (
                requested[slot] >= 0 &&
                    (distance !in 0 until desired || requested[slot] !in 0 until count)
            ) {
                cancellations[slot]?.invoke()
                cancellations[slot] = null
                requested[slot] = -1
            }
        }
        for (distance in 0 until desired) {
            val index = next + distance * direction
            if (index !in 0 until count || index in requested) continue
            val slot = requested.indexOf(-1)
            if (slot < 0) break
            requested[slot] = index
            cancellations[slot] = request(index, constraints)
        }
    }

    private fun desiredCount(): Int {
        val stale = lastSample != Long.MIN_VALUE && nowNanos() - lastSample > 250_000_000L
        if (stale) velocity = 0f
        val horizon = (velocity * 120f).coerceAtMost(viewport)
        return kotlin.math.ceil(horizon / averageExtent).toInt().coerceIn(1, 3)
    }

    fun cancel() {
        for (slot in requested.indices) {
            cancellations[slot]?.invoke()
            cancellations[slot] = null
            requested[slot] = -1
        }
        velocity = 0f
        lastSample = Long.MIN_VALUE
        hasDirection = false
        pendingReverseDistance = 0f
        scheduledNext = Int.MIN_VALUE
        scheduledDirection = 0
        scheduledDesired = -1
    }
}
