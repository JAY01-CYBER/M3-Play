package com.mocharealm.accompanist.lyrics.ui.internal.layout

import androidx.compose.runtime.*
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsScrollChain
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

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
    private var glide = false
    private var focus = 0
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
    }

    fun retain(from: Int, until: Int) {
        for (i in from until until) if (i < first || i >= end) {
            positions[i] = base
            layoutTops[i] = Double.NaN
            velocities[i] = 0f
            offsets[i].floatValue = 0f
        }
        first = from
        end = until
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
        if (settings == null || !previous.isFinite() || !preserveScreenPosition) return
        val delta = top - previous - anchorCorrection
        if (delta == 0.0) return
        positions[index] += delta
        offsets[index].floatValue = (base - positions[index]).toFloat()
        active = true
    }

    /** Apply one measured content-height change to every following retained item exactly once. */
    fun applyContentShift(afterIndex: Int, delta: Double) {
        if (delta == 0.0) return
        for (index in maxOf(first, afterIndex + 1) until end) {
            positions[index] += delta
            if (layoutTops[index].isFinite()) layoutTops[index] += delta
            offsets[index].floatValue = (base - positions[index]).toFloat()
        }
    }

    fun rebase(position: Double) {
        val delta = position - base
        if (delta == 0.0) return
        for (i in first until end) positions[i] += delta
        base = position
    }

    fun follow(glide: Boolean) {
        this.glide = glide
    }

    fun focusAt(index: Int) {
        focus = index
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
    }

    fun moveTo(position: Double, cascade: Boolean) {
        if (!cascade || settings == null) {
            reset(position)
            return
        }
        val delta = position - base
        if (delta == 0.0) return
        base = position
        var moving = false
        for (i in first until end) {
            // The focus and preceding items ride the configured scroll exactly. Preserve any
            // residual offset when a previously trailing item becomes focused; let it settle.
            if (!glide && i <= focus) positions[i] += delta
            val offset = (base - positions[i]).toFloat().coerceIn(-limit, limit)
            positions[i] = base - offset
            offsets[i].floatValue = offset
            if (abs(offset) > 0.08f || abs(velocities[i]) > 0.08f) moving = true
        }
        active = moving
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
                val distance =
                    if (glide) 0 else (i - focus).coerceAtLeast(0).coerceAtMost(stiffness.lastIndex)
                val neighbour =
                    if (!glide && i > focus && i > first) positions[i - 1] - base else 0.0
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
    }
}
