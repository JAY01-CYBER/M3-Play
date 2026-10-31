package com.mocharealm.accompanist.lyrics.ui.internal.layout

import androidx.compose.runtime.*
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsScrollChain
import com.mocharealm.accompanist.lyrics.ui.diagnostics.LyricsSpringTrace
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.time.TimeSource

/** Physics storage survives lazy item recycling. Only the retained viewport range is advanced. */
internal class LyricsScrollChainState {
    private var settings: LyricsScrollChain? = null
    private var layoutTops = DoubleArray(0)
    private var positions = DoubleArray(0)
    private var velocities = FloatArray(0)
    private var offsets by mutableStateOf(emptyArray<MutableFloatState>())
    private var stiffness = FloatArray(0)
    private var damping = FloatArray(0)
    private var first = 0
    private var end = 0
    private var base = 0.0
    private var limit = 1f
    private var focus = 0
    private val traceStart = TimeSource.Monotonic.markNow()
    private var traceSequence = 0
    var active by mutableStateOf(false)
        private set

    fun configure(count: Int, config: LyricsScrollChain?, position: Double, viewport: Int) {
        limit = viewport.coerceAtLeast(1).toFloat()
        if (positions.size == count && settings == config) return
        settings = config
        layoutTops = DoubleArray(count) { Double.NaN }
        positions = DoubleArray(count) { position }
        velocities = FloatArray(count)
        offsets = Array(count) { mutableFloatStateOf(0f) }
        stiffness = FloatArray(count + 1)
        damping = FloatArray(count + 1)
        if (config != null)
            for (distance in stiffness.indices) {
                val response =
                    (1f - distance * config.distanceFalloff).coerceIn(config.minResponse, 1f)
                stiffness[distance] = config.stiffness * response
                damping[distance] = config.damping * response.pow(0.3f)
            }
        first = 0
        end = 0
        base = position
        active = false
        traceState(
            LyricsSpringTraceEvent.CONFIGURE,
            eventIndex = count,
            eventValue = position,
            eventValue2 = viewport.toDouble(),
        )
    }

    fun retain(from: Int, until: Int) {
        if (first == from && end == until) return
        for (i in from until until) if (i < first || i >= end) {
            positions[i] = base
            layoutTops[i] = Double.NaN
            velocities[i] = 0f
            offsets[i].floatValue = 0f
        }
        first = from
        end = until
        traceState(LyricsSpringTraceEvent.RETAIN, eventIndex = from, eventValue = until.toDouble())
    }

    /**
     * Records the latest content-space top and optionally preserves the item's current screen
     * coordinate while its measured height changes.
     *
     * During a predicted follow the target scroll already includes the settled heights of the
     * preceding items. Injecting the same top delta into the spring would apply that geometry
     * change twice, so callers can update the cached top without perturbing the spring.
     */
    fun layoutAt(
        index: Int,
        top: Double,
        anchorCorrection: Double,
        preserveScreenPosition: Boolean = true,
    ) {
        val previous = layoutTops[index]
        layoutTops[index] = top
        val topChanged = !previous.isFinite() || top != previous
        val delta = if (previous.isFinite()) top - previous - anchorCorrection else Double.NaN
        if (settings == null || !previous.isFinite() || !preserveScreenPosition) {
            if (topChanged)
                traceState(
                    LyricsSpringTraceEvent.LAYOUT,
                    eventIndex = index,
                    eventValue = top,
                    eventValue2 = delta,
                    eventFloat = if (preserveScreenPosition) 1f else 0f,
                )
            return
        }
        if (delta == 0.0) return
        positions[index] += delta
        offsets[index].floatValue = (base - positions[index]).toFloat()
        active = true
        traceState(
            LyricsSpringTraceEvent.LAYOUT,
            eventIndex = index,
            eventValue = top,
            eventValue2 = delta,
            eventFloat = 1f,
        )
    }

    /** Apply one measured content-height change to every following retained item exactly once. */
    fun applyContentShift(afterIndex: Int, delta: Double) {
        if (delta == 0.0) return
        for (index in maxOf(first, afterIndex + 1) until end) {
            positions[index] += delta
            if (layoutTops[index].isFinite()) layoutTops[index] += delta
            offsets[index].floatValue = (base - positions[index]).toFloat()
        }
        traceState(LyricsSpringTraceEvent.CONTENT_SHIFT, eventIndex = afterIndex, eventValue = delta)
    }

    fun rebase(position: Double) {
        val delta = position - base
        if (delta == 0.0) return
        for (i in first until end) positions[i] += delta
        base = position
        traceState(LyricsSpringTraceEvent.REBASE, eventValue = position, eventValue2 = delta)
    }

    fun focusAt(index: Int, scrollVelocity: Float = 0f) {
        if (focus == index) return
        if (settings != null && scrollVelocity != 0f) {
            for (i in first until end) {
                val rodeScroll = i <= focus
                val ridesScroll = i <= index
                if (rodeScroll != ridesScroll) {
                    // A trailing row's velocity is absolute; a riding row's is relative to
                    // the scroll. Change that basis without changing its screen velocity.
                    velocities[i] += if (rodeScroll) scrollVelocity else -scrollVelocity
                    if (abs(velocities[i]) > 0.08f) active = true
                }
            }
        }
        focus = index
        traceState(LyricsSpringTraceEvent.FOCUS, eventIndex = index)
    }

    fun offset(index: Int): Float = offsets.getOrNull(index)?.floatValue ?: 0f

    val retainedCount
        get() = end - first

    fun reset(position: Double) {
        base = position
        for (i in first until end) {
            positions[i] = base
            velocities[i] = 0f
            offsets[i].floatValue = 0f
        }
        active = false
        traceState(LyricsSpringTraceEvent.RESET, eventValue = position)
    }

    /**
     * A non-follow scroll only changes the coordinate frame. It does not own spring lifetime;
     * callers that intentionally interrupt the chain must call [reset] at that transition.
     */
    fun followScrollTo(position: Double) {
        if (settings == null) {
            rebase(position)
            return
        }
        val delta = position - base
        if (delta == 0.0) return
        base = position
        var moving = false
        for (i in first until end) {
            // The focus and preceding items ride the configured scroll exactly. Preserve any
            // residual offset when a previously trailing item becomes focused; let it settle.
            if (i <= focus) positions[i] += delta
            val offset = (base - positions[i]).toFloat().coerceIn(-limit, limit)
            positions[i] = base - offset
            offsets[i].floatValue = offset
            if (abs(offset) > 0.08f || abs(velocities[i]) > 0.08f) moving = true
        }
        active = moving
        traceState(LyricsSpringTraceEvent.FOLLOW_SCROLL, eventValue = position, eventValue2 = delta)
    }

    fun advance(seconds: Float) {
        val config = settings ?: return
        if (!active || seconds <= 0f) return
        val dt = seconds.coerceAtMost(0.05f)
        val steps = ceil(dt / (1f / 240f)).toInt().coerceAtLeast(1)
        val step = dt / steps
        repeat(steps) {
            // Reverse traversal reads the previous substep's neighbour without a scratch array.
            for (i in end - 1 downTo first) {
                val distance = (i - focus).coerceAtLeast(0).coerceAtMost(stiffness.lastIndex)
                val neighbour =
                    if (i > focus && i > first) positions[i - 1] - base else 0.0
                val target = base + neighbour * config.coupling
                val acceleration =
                    -stiffness[distance] * (positions[i] - target) -
                        damping[distance] * velocities[i]
                velocities[i] += (acceleration * step).toFloat()
                positions[i] += velocities[i] * step
                if (abs(positions[i] - base) > limit) {
                    positions[i] =
                        base + (positions[i] - base).coerceIn(-limit.toDouble(), limit.toDouble())
                    velocities[i] = 0f
                }
            }
        }
        var moving = false
        for (i in first until end) {
            if (abs(positions[i] - base) > 0.08 || abs(velocities[i]) > 0.08f) moving = true
            offsets[i].floatValue = (base - positions[i]).toFloat()
        }
        if (!moving) reset(base)
        traceState(
            LyricsSpringTraceRecord.FRAME,
            requestedSeconds = seconds,
            integratedSeconds = dt,
            eventFloat = steps.toFloat(),
        )
    }

    internal fun traceMarker(
        eventCode: Int,
        eventIndex: Int = -1,
        eventValue: Double = Double.NaN,
        eventValue2: Double = Double.NaN,
        eventFloat: Float = Float.NaN,
    ) {
        traceState(eventCode, eventIndex, eventValue, eventValue2, eventFloat)
    }

    internal fun traceActor(
        eventCode: Int,
        eventIndex: Int,
        position: Double,
        velocity: Float,
        target: Double,
    ) {
        val sink = LyricsSpringTrace.currentSink() ?: return
        val bytes =
            LyricsSpringTraceRecord.state(
                type = LyricsSpringTraceRecord.SCROLL_ACTOR,
                sequence = traceSequence++,
                elapsedNanos = traceStart.elapsedNow().inWholeNanoseconds,
                requestedSeconds = 0f,
                integratedSeconds = 0f,
                eventCode = eventCode,
                eventIndex = eventIndex,
                eventValue = position,
                eventValue2 = target,
                eventFloat = velocity,
                base = base,
                limit = limit,
                focus = focus,
                first = first,
                end = end,
                active = active,
                settings = settings,
                rowCount = 0,
                row = { _, _ -> },
            )
        sink.onRecord(bytes)
    }

    private fun traceState(
        code: Int,
        eventIndex: Int = -1,
        eventValue: Double = Double.NaN,
        eventValue2: Double = Double.NaN,
        eventFloat: Float = Float.NaN,
        requestedSeconds: Float = 0f,
        integratedSeconds: Float = 0f,
    ) {
        val sink = LyricsSpringTrace.currentSink() ?: return
        val count = (end - first).coerceAtLeast(0)
        val bytes =
            LyricsSpringTraceRecord.state(
                type = if (code == LyricsSpringTraceRecord.FRAME) {
                    LyricsSpringTraceRecord.FRAME
                } else {
                    LyricsSpringTraceRecord.STATE_EVENT
                },
                sequence = traceSequence++,
                elapsedNanos = traceStart.elapsedNow().inWholeNanoseconds,
                requestedSeconds = requestedSeconds,
                integratedSeconds = integratedSeconds,
                eventCode = if (code == LyricsSpringTraceRecord.FRAME) 0 else code,
                eventIndex = eventIndex,
                eventValue = eventValue,
                eventValue2 = eventValue2,
                eventFloat = eventFloat,
                base = base,
                limit = limit,
                focus = focus,
                first = first,
                end = end,
                active = active,
                settings = settings,
                rowCount = count,
                row = { rowOffset, writer ->
                    val index = first + rowOffset
                    val distance = (index - focus).coerceAtLeast(0).coerceAtMost(stiffness.lastIndex)
                    val neighbour =
                        if (settings != null && index > focus && index > first)
                            positions[index - 1] - base
                        else 0.0
                    val targetDelta = neighbour * (settings?.coupling ?: 0f)
                    val acceleration =
                        if (settings != null)
                            -stiffness[distance] * (positions[index] - base - targetDelta) -
                                damping[distance] * velocities[index]
                        else 0.0
                    writer.putInt(index)
                    writer.putDouble(layoutTops[index])
                    writer.putDouble(positions[index] - base)
                    writer.putFloat(velocities[index])
                    writer.putFloat(offsets[index].floatValue)
                    writer.putDouble(targetDelta)
                    writer.putFloat(acceleration.toFloat())
                    writer.putFloat(stiffness.getOrElse(distance) { 0f })
                    writer.putFloat(damping.getOrElse(distance) { 0f })
                },
            )
        sink.onRecord(bytes)
    }
}
