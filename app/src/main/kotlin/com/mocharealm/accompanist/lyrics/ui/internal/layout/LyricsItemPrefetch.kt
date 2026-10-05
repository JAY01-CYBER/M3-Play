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
        schedule()
    }

    fun onScroll(delta: Float) {
        if (delta == 0f) return
        val now = nowNanos()
        val elapsedMs = if (lastSample == Long.MIN_VALUE) 16f else (now - lastSample) / 1_000_000f
        val newForward = delta > 0f
        if (newForward != forward || elapsedMs > 250f) velocity = 0f
        forward = newForward
        // Several deltas in the same frame must not imply an unbounded velocity.
        val sample = abs(delta) / elapsedMs.coerceAtLeast(8f)
        velocity = if (velocity == 0f) sample else velocity * 0.65f + sample * 0.35f
        lastSample = now
        schedule()
    }

    private fun schedule() {
        val stale = lastSample != Long.MIN_VALUE && nowNanos() - lastSample > 250_000_000L
        if (stale) velocity = 0f
        val horizon = (velocity * 120f).coerceAtMost(viewport)
        val desired = kotlin.math.ceil(horizon / averageExtent).toInt().coerceIn(1, 3)
        val next = if (forward) end else first - 1
        val direction = if (forward) 1 else -1
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

    fun cancel() {
        for (slot in requested.indices) {
            cancellations[slot]?.invoke()
            cancellations[slot] = null
            requested[slot] = -1
        }
        velocity = 0f
        lastSample = Long.MIN_VALUE
    }
}
