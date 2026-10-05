package com.mocharealm.accompanist.lyrics.ui.composable.list

import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsHeightIndex
import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsScrollChainState

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Remeasurement
import kotlin.math.roundToInt
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.first

@Immutable
data class LyricsListItem(
    val key: Any,
    val estimatedHeightPx: Int,
    val settledHeightPx: (() -> Int)? = null,
    val focusOffsetPx: (() -> Int)? = null,
    val preserveAnchorOnHeightChange: Boolean = true,
) {
    init {
        require(estimatedHeightPx >= 0)
    }
}

/** A lyrics-specific vertical lazy list. Positive scroll moves towards later items. */
@Stable
class LyricsLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
) : ScrollableState {
    init {
        require(initialFirstVisibleItemIndex >= 0 && initialFirstVisibleItemScrollOffset >= 0)
    }

    var firstVisibleItemIndex by mutableIntStateOf(initialFirstVisibleItemIndex)
        private set

    var firstVisibleItemScrollOffset by mutableIntStateOf(initialFirstVisibleItemScrollOffset)
        private set

    internal val chain = LyricsScrollChainState()
    private var followingChain = false
    private var predictingFollow = false
    internal var followAnchorIndex = -1
        private set

    /** Item whose leading interlude dots are currently changing height. */
    internal var interludeItemIndex = -1
    private var clearInterludeAfterMeasure = false
    internal var anchoringContentChange = false
        private set

    private var contentCoordinateShift = 0.0

    /** Reflow around the followed aggregate without displacing its screen position or spring. */
    internal fun preserveFollowAnchorForContentChange() {
        if (!isManualScrolling && followAnchorIndex in items.indices) anchoringContentChange = true
    }

    internal fun setInterludeItem(index: Int) {
        if (index >= 0) {
            interludeItemIndex = index
            clearInterludeAfterMeasure = false
        } else if (interludeItemIndex >= 0) {
            // Keep the old index through the removal measure so the collapsing gap follows the
            // same direct-plus-one-spring path as its appearance.
            clearInterludeAfterMeasure = true
        }
    }

    internal fun finishInterludeMeasure() {
        if (clearInterludeAfterMeasure) {
            interludeItemIndex = -1
            clearInterludeAfterMeasure = false
        }
    }

    internal fun compensateContentHeightChange(delta: Int) {
        position += delta
        contentCoordinateShift += delta
    }

    /** True only while the global follow animation is still changing the scroll position. */
    internal val predictedFollowActive
        get() = predictingFollow && followingChain

    internal val hasPredictedScrollGeometry
        get() = predictingFollow && (followingChain || chain.active)

    internal var retainedFirst = 0
    internal var retainedEnd = 0
    private var suppressChain = false

    internal fun suspendChain() {
        anchoringContentChange = false
        suppressChain = true
        predictingFollow = false
        chain.reset(position)
    }

    internal suspend fun animateFollowToItem(
        index: Int,
        focusEnd: Int,
        cascade: Boolean,
        animationSpec: AnimationSpec<Float>,
    ) {
        if (followAnchorIndex != index) anchoringContentChange = false
        followAnchorIndex = index
        animateToItem(index, 0, animationSpec, cascade, focusEnd, preserveChain = true)
    }

    internal var position by mutableDoubleStateOf(0.0)
    internal var maxPosition by mutableDoubleStateOf(0.0)
    internal var ready by mutableStateOf(false)
    internal var remeasurement: Remeasurement? = null
    internal var tryPlacementScroll: (() -> Boolean)? = null
    internal var onScrollPrefetch: ((Float) -> Unit)? = null
    internal var measurePasses = 0
        private set

    internal fun recordMeasure() {
        measurePasses++
    }

    internal var heights = LyricsHeightIndex(intArrayOf(), 0)
    internal var items: List<LyricsListItem> = emptyList()
    private var configuredWidth = -1
    private var configuredSpacing = -1
    private var keys: Map<Any, Int> = emptyMap()
    private var pendingIndex: Int? = initialFirstVisibleItemIndex
    private var pendingOffset = initialFirstVisibleItemScrollOffset
    val interactionSource = MutableInteractionSource()
    /** True throughout dragging, flinging and the delay before automatic following resumes. */
    var isManualScrolling by mutableStateOf(false)
        internal set

    internal var resumeRequest by mutableIntStateOf(0)
        private set

    /** Resume following immediately after a lyric click or a caller's explicit seek. */
    private var clickPosition: Int? = null
    private var clickTime = TimeSource.Monotonic.markNow()

    fun resumeAutoScroll(seekPosition: Int? = null) {
        clickPosition = seekPosition
        clickTime = TimeSource.Monotonic.markNow()
        resumeRequest++
    }

    internal fun consumeClickSeek(time: Int): Boolean {
        val expected = clickPosition ?: return false
        if (clickTime.elapsedNow().inWholeMilliseconds > 1500) {
            clickPosition = null
            return false
        }
        if (kotlin.math.abs(time.toLong() - expected) > 600) return false
        clickPosition = null
        return true
    }

    private val scrollState = ScrollableState { delta ->
        val previous = position
        position = (previous + delta).coerceIn(0.0, maxPosition)
        if (position != previous) {
            chain.moveTo(position, followingChain && !suppressChain)
            if (tryPlacementScroll?.invoke() != true) remeasurement?.forceRemeasure()
            onScrollPrefetch?.invoke((position - previous).toFloat())
        }
        (position - previous).toFloat()
    }
    override val isScrollInProgress
        get() = scrollState.isScrollInProgress

    override val canScrollForward
        get() = position < maxPosition

    override val canScrollBackward
        get() = position > 0.0

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) = scrollState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float) = scrollState.dispatchRawDelta(delta)

    internal fun configure(
        newItems: List<LyricsListItem>,
        width: Int,
        spacing: Int,
        top: Int,
        bottom: Int,
        viewport: Int,
    ) {
        if (items !== newItems || configuredWidth != width || configuredSpacing != spacing) {
            val anchorKey = items.getOrNull(firstVisibleItemIndex)?.key
            val anchorOffset = firstVisibleItemScrollOffset
            keys = newItems.withIndex().associate { it.value.key to it.index }
            require(keys.size == newItems.size) { "Lyrics list keys must be unique" }
            items = newItems
            heights =
                LyricsHeightIndex(IntArray(items.size) { items[it].estimatedHeightPx }, spacing)
            configuredWidth = width
            configuredSpacing = spacing
            chain.reset(position)
            if (pendingIndex == null) {
                val anchor =
                    (keys[anchorKey] ?: firstVisibleItemIndex).coerceIn(
                        0,
                        (items.size - 1).coerceAtLeast(0),
                    )
                position = heights.top(anchor) + anchorOffset
            }
        }
        updateRange(top, bottom, viewport)
        pendingIndex?.let {
            if (items.isNotEmpty()) {
                position =
                    (heights.top(it.coerceAtMost(items.lastIndex)) + pendingOffset).coerceIn(
                        0.0,
                        maxPosition,
                    )
                pendingIndex = null
            }
        }
        ready = true
    }

    internal fun updateRange(top: Int, bottom: Int, viewport: Int) {
        maxPosition =
            if (heights.size == 0) 0.0
            else (top + heights.total + bottom - viewport).coerceAtLeast(0.0)
        position = position.coerceIn(0.0, maxPosition)
        firstVisibleItemIndex = heights.itemAt(position)
        firstVisibleItemScrollOffset =
            (position - heights.top(firstVisibleItemIndex)).coerceAtLeast(0.0).roundToInt()
    }

    internal fun indexOf(key: Any): Int = keys[key] ?: -1

    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0) {
        require(index >= 0)
        snapshotFlow { ready }.first { it }
        if (items.isEmpty()) return
        scroll {
            suspendChain()
            position =
                (heights.top(index.coerceAtMost(items.lastIndex)) + scrollOffset).coerceIn(
                    0.0,
                    maxPosition,
                )
            chain.reset(position)
            remeasurement?.forceRemeasure()
        }
    }

    /**
     * Duration/easing (tween) or a caller-supplied spring; new scroll mutations cancel this one.
     */
    suspend fun animateScrollToItem(
        index: Int,
        scrollOffset: Int = 0,
        animationSpec: AnimationSpec<Float> = tween(650, easing = FastOutSlowInEasing),
    ) {
        animateToItem(index, scrollOffset, animationSpec, false, index)
    }

    private suspend fun animateToItem(
        index: Int,
        scrollOffset: Int,
        animationSpec: AnimationSpec<Float>,
        cascade: Boolean,
        focusEnd: Int,
        preserveChain: Boolean = false,
    ) {
        require(index >= 0)
        snapshotFlow { ready }.first { it }
        if (items.isEmpty()) return
        scroll {
            followingChain = preserveChain || cascade
            suppressChain = !followingChain
            chain.follow(glide = preserveChain && !cascade)
            chain.focusAt(focusEnd)
            if (!followingChain) chain.reset(position)
            try {
                val targetIndex = index.coerceAtMost(items.lastIndex)
                val start = position
                val initialCoordinateShift = contentCoordinateShift
                predictingFollow =
                    preserveChain &&
                        items[targetIndex].settledHeightPx != null &&
                        targetIndex in retainedFirst until retainedEnd
                val settledTarget =
                    if (predictingFollow) {
                        var destination = heights.top(targetIndex)
                        for (i in retainedFirst until minOf(retainedEnd, targetIndex)) {
                            items[i].settledHeightPx?.let {
                                destination += it() - heights.height(i)
                            }
                        }
                        destination += items[targetIndex].focusOffsetPx?.invoke() ?: 0
                        destination.coerceIn(0.0, maxPosition)
                    } else null
                fun targetPosition() =
                    settledTarget
                        ?: (heights.top(targetIndex) + scrollOffset).coerceIn(0.0, maxPosition)
                if (kotlin.math.abs(targetPosition() - start) < 0.5) return@scroll
                // Re-read measured target geometry without restarting the animation's
                // duration/curve.
                val distance = targetPosition() - start
                // Spring visibility thresholds are physical pixels, never normalized progress.
                animate(0f, distance.toFloat(), animationSpec = animationSpec) { travelled, _ ->
                    val fraction = travelled / distance
                    val shift = contentCoordinateShift - initialCoordinateShift
                    val shiftedStart = start + shift
                    val target = targetPosition() + if (settledTarget != null) shift else 0.0
                    scrollBy(
                        (shiftedStart + (target - shiftedStart) * fraction - position).toFloat()
                    )
                }
                val target =
                    targetPosition() +
                        if (settledTarget != null) contentCoordinateShift - initialCoordinateShift
                        else 0.0
                scrollBy((target - position).toFloat())
            } finally {
                followingChain = false
            }
        }
    }

    companion object {
        val Saver =
            listSaver<LyricsLazyListState, Int>(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = { LyricsLazyListState(it[0], it[1]) },
            )
    }
}

@Composable
fun rememberLyricsLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LyricsLazyListState =
    rememberSaveable(saver = LyricsLazyListState.Saver) {
        LyricsLazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
