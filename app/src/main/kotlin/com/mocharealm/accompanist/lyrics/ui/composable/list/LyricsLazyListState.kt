package com.mocharealm.accompanist.lyrics.ui.composable.list

import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsHeightIndex
import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsScrollChainState
import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsSpringTraceEvent
import com.mocharealm.accompanist.lyrics.ui.internal.layout.lyricsFollowRetargetSpec

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Remeasurement
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
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

internal data class LyricsFollowRequest(
    val index: Int,
    val focusEnd: Int,
)

private sealed interface LyricsFollowEvent {
    data class Request(val value: LyricsFollowRequest) : LyricsFollowEvent
    data object Geometry : LyricsFollowEvent
    data object AnimationFrame : LyricsFollowEvent
    data object Finished : LyricsFollowEvent
}

internal const val LyricsClickFollowTimeoutMillis = 1500L

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
    private var onCaptionGeometryChange: (() -> Unit)? = null
    private var captionReflowRunning = false

    /** Invalidates follow target calculations when measured list geometry changes. */
    internal var layoutRevision by mutableIntStateOf(0)
        private set

    internal fun recordLayoutChange() {
        layoutRevision++
    }

    /** Reflow around the followed aggregate without displacing its screen position or spring. */
    internal fun preserveFollowAnchorForContentChange() {
        captionReflowRunning = true
        if (!isManualScrolling && followAnchorIndex in items.indices) {
            anchoringContentChange = true
            onCaptionGeometryChange?.invoke()
        }
    }

    internal fun finishCaptionContentChange() {
        captionReflowRunning = false
        // The current follow keeps its compensated coordinate frame until a new request.
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
    /** User input owns the scroll base until auto-follow resumes; row springs keep their lifetime. */
    private var autoFollowSuspended = false

    internal fun suspendAutoFollow(reason: Int) {
        chain.traceMarker(
            LyricsSpringTraceEvent.AUTO_FOLLOW_SUSPENDED,
            eventValue = position,
            eventFloat = reason.toFloat(),
        )
        anchoringContentChange = false
        autoFollowSuspended = true
        predictingFollow = false
        clickTargetIndex = -1
        clickTargetRevisionState++
        // A user gesture takes control of the scroll coordinate, not of each row's spring.
        // scrollState.rebase() moves the coordinate frame while preserving offsets and velocity.
    }

    internal suspend fun animateFollowToItem(
        index: Int,
        focusEnd: Int,
        animationSpec: AnimationSpec<Float>,
    ) {
        if (followAnchorIndex != index) anchoringContentChange = captionReflowRunning
        followAnchorIndex = index
        animateToItem(index, 0, animationSpec, followChain = true, focusEnd = focusEnd)
    }

    /**
     * Keeps one scroll mutation alive while following changing targets. A new target retargets the
     * same scroll actor, preserving position, velocity and the last sampled frame time.
     */
    internal suspend fun animateFollowRequests(
        requests: ReceiveChannel<LyricsFollowRequest>,
        animationSpec: AnimationSpec<Float>,
    ) = coroutineScope {
        var interruptFollow: ((Int) -> Unit)? = null
        val previousImmediateFollowRequest = onImmediateFollowRequest
        val previousCaptionGeometryChange = onCaptionGeometryChange
        val immediateFollowRequest: (Int) -> Unit = { index ->
            previousImmediateFollowRequest?.invoke(index)
            interruptFollow?.invoke(index)
        }
        onImmediateFollowRequest = immediateFollowRequest
        try {
            while (isActive) {
                var initial = requests.receive()
                if (initial.index < 0) continue
                snapshotFlow { ready && items.isNotEmpty() }.first { it }
                while (true) {
                    val pending = requests.tryReceive().getOrNull() ?: break
                    initial = pending
                }
                if (initial.index < 0) continue

                val scrollJob =
                    launch {
                        try {
                            scroll {
                                val scrollScope = this
                                followingChain = true
                                autoFollowSuspended = false
                                var animator =
                                    AnimationState((position - contentCoordinateShift).toFloat())
                                var request by mutableStateOf(initial)
                                var requestCoordinateShift = contentCoordinateShift
                                var captionTargetOffset = 0.0
                                var measuredCaptionTarget = anchoringContentChange

                                fun updateRequest(next: LyricsFollowRequest, scrollVelocity: Float = animator.velocity) {
                                    if (followAnchorIndex != next.index || captionReflowRunning) {
                                        anchoringContentChange = captionReflowRunning
                                    }
                                    followAnchorIndex = next.index
                                    captionTargetOffset = 0.0
                                    measuredCaptionTarget = anchoringContentChange
                                    request = next
                                    requestCoordinateShift = contentCoordinateShift
                                    followingChain = true
                                    autoFollowSuspended = false
                                    chain.focusAt(next.focusEnd, scrollVelocity)
                                    chain.traceMarker(
                                        LyricsSpringTraceEvent.FOLLOW_TARGET,
                                        eventIndex = next.index,
                                        eventValue = next.focusEnd.toDouble(),
                                        eventValue2 = contentCoordinateShift,
                                    )
                                    val targetIndex = next.index.coerceAtMost(items.lastIndex)
                                    predictingFollow =
                                        items[targetIndex].settledHeightPx != null &&
                                            targetIndex in retainedFirst until retainedEnd
                                }

                                fun targetPosition(target: LyricsFollowRequest): Double {
                                    val targetIndex = target.index.coerceAtMost(items.lastIndex)
                                    var destination = heights.top(targetIndex)
                                    val predictSettledHeight =
                                        items[targetIndex].settledHeightPx != null &&
                                            targetIndex in retainedFirst until retainedEnd
                                    if (predictSettledHeight) {
                                        for (
                                            i in retainedFirst until minOf(retainedEnd, targetIndex)
                                        ) {
                                            items[i].settledHeightPx?.let {
                                                destination += it() - heights.height(i)
                                            }
                                        }
                                        destination +=
                                            items[targetIndex].focusOffsetPx?.invoke() ?: 0
                                        destination +=
                                            contentCoordinateShift - requestCoordinateShift
                                    }
                                    return destination.coerceIn(0.0, maxPosition)
                                }

                                fun logicalTarget(target: LyricsFollowRequest): Double {
                                    if (!measuredCaptionTarget) return targetPosition(target) - contentCoordinateShift
                                    val index = target.index.coerceAtMost(items.lastIndex)
                                    return heights.top(index) + (items[index].focusOffsetPx?.invoke() ?: 0) -
                                        contentCoordinateShift +
                                        if (target.index == request.index) captionTargetOffset else 0.0
                                }

                                updateRequest(initial)
                                coroutineScope {
                                    val geometryUpdates = Channel<Unit>(Channel.CONFLATED)
                                    val animationFrames = Channel<Unit>(Channel.CONFLATED)
                                    fun startAnimation(
                                        destination: Double,
                                        spec: AnimationSpec<Float>,
                                        initialVelocity: Float,
                                        retargeted: Boolean = false,
                                        carryFrameTime: Boolean = retargeted,
                                    ): Job =
                                        launch {
                                            // AnimationState retains its frame timestamp on cancellation.
                                            // A handoff starts at that sample, so the next frame advances
                                            // instead of repeating t=0 and momentarily stopping the list.
                                            animator = AnimationState(
                                                animator.value,
                                                initialVelocity,
                                                lastFrameTimeNanos = animator.lastFrameTimeNanos,
                                            )
                                            chain.traceMarker(
                                                LyricsSpringTraceEvent.ANIMATION_START_OR_RETARGET,
                                                eventIndex = request.index,
                                                eventValue = destination,
                                                eventValue2 = initialVelocity.toDouble(),
                                                eventFloat = if (retargeted) 1f else 0f,
                                            )
                                            animator.animateTo(
                                                destination.toFloat(),
                                                spec,
                                                sequentialAnimation = carryFrameTime,
                                            ) {
                                                scrollScope.scrollBy(
                                                    (value + contentCoordinateShift - position)
                                                        .toFloat()
                                                )
                                                chain.traceActor(
                                                    LyricsSpringTraceEvent.SCROLL_FRAME,
                                                    request.index,
                                                    value.toDouble() + contentCoordinateShift,
                                                    // Duration specs may report a small finite-difference
                                                    // residue at their endpoint; the actor is at rest then.
                                                    if (isRunning) animator.velocity else 0f,
                                                    destination + contentCoordinateShift,
                                                )
                                                animationFrames.trySend(Unit)
                                            }
                                        }

                                    val geometryObserver = launch {
                                        // Geometry events belong to completed measurements. Reading
                                        // caption/playback flags here can retarget before composition
                                        // rebases their coordinates and preserves the active target.
                                        snapshotFlow { layoutRevision }.collect {
                                            geometryUpdates.trySend(Unit)
                                        }
                                    }
                                    var animation =
                                        startAnimation(
                                            logicalTarget(initial),
                                            animationSpec,
                                            animator.velocity,
                                        )
                                    var animationDestination = logicalTarget(initial)
                                    val captionGeometryChange: () -> Unit = {
                                        // Caption visibility changes the layout coordinate frame.
                                        // Prefix measurements and contentCoordinateShift move together.
                                        // The measured target stays invariant even when lazy retention
                                        // changes; settled-height predictions use a different basis.
                                        // Keep the actor's destination, frame time and velocity intact.
                                        measuredCaptionTarget = true
                                        captionTargetOffset = 0.0
                                        captionTargetOffset = animationDestination - logicalTarget(request)
                                    }
                                    onCaptionGeometryChange = captionGeometryChange
                                    var handoffVelocity: Float? = null
                                    interruptFollow = { index ->
                                        val next = LyricsFollowRequest(index, index)
                                        if (
                                            animation.isActive &&
                                                kotlin.math.abs(
                                                    logicalTarget(next) - logicalTarget(request)
                                                ) >= 0.5
                                        ) {
                                            // Capture on the click callback's frame. The actor may
                                            // resume after the old animation has already ended.
                                            handoffVelocity = animator.velocity
                                            chain.traceMarker(
                                                LyricsSpringTraceEvent.CLICK_HANDOFF,
                                                eventIndex = index,
                                                eventValue = logicalTarget(next),
                                                eventValue2 = logicalTarget(request),
                                                eventFloat = animator.velocity,
                                            )
                                            animation.cancel()
                                        }
                                    }
                                    var finished = false
                                    try {
                                        while (!finished) {
                                            val event =
                                                select<LyricsFollowEvent> {
                                                    requests.onReceive {
                                                        LyricsFollowEvent.Request(it)
                                                    }
                                                    geometryUpdates.onReceive {
                                                        LyricsFollowEvent.Geometry
                                                    }
                                                    animationFrames.onReceive {
                                                        LyricsFollowEvent.AnimationFrame
                                                    }
                                                    // A click queues before the animation handoff.
                                                    // If both clauses are ready, consume that request
                                                    // into this same scroll mutation.
                                                    animation.onJoin {
                                                        requests.tryReceive().getOrNull()?.let {
                                                            LyricsFollowEvent.Request(it)
                                                        } ?: LyricsFollowEvent.Finished
                                                    }
                                                }
                                            when (event) {
                                                is LyricsFollowEvent.Request -> {
                                                    val next = event.value
                                                    if (next.index < 0 || items.isEmpty()) {
                                                        animation.cancelAndJoin()
                                                        finished = true
                                                    } else {
                                                        val carriedVelocity = handoffVelocity
                                                        handoffVelocity = null
                                                        val wasAnimating =
                                                            animation.isActive ||
                                                                carriedVelocity != null
                                                        val initialVelocity =
                                                            carriedVelocity ?: if (wasAnimating) animator.velocity else 0f
                                                        val previousDestination =
                                                            logicalTarget(request)
                                                        updateRequest(next, initialVelocity)
                                                        val destination = logicalTarget(next)
                                                        val destinationChanged =
                                                            kotlin.math.abs(
                                                                destination - previousDestination
                                                            ) >= 0.5
                                                        val clickCancelledAnimation =
                                                            carriedVelocity != null &&
                                                                !animation.isActive
                                                        if (
                                                            destinationChanged ||
                                                                clickCancelledAnimation
                                                        ) {
                                                            val retargetSpec =
                                                                if (wasAnimating)
                                                                    lyricsFollowRetargetSpec(animationSpec)
                                                                else animationSpec
                                                            // Finish the old frame writer before replacing
                                                            // the actor's animation state.
                                                            animation.cancelAndJoin()
                                                            animationDestination = destination
                                                            animation =
                                                                startAnimation(
                                                                    destination,
                                                                    retargetSpec,
                                                                    initialVelocity,
                                                                    retargeted = wasAnimating,
                                                                )
                                                        }
                                                    }
                                                }
                                                LyricsFollowEvent.Geometry -> {
                                                    val wasAnimating = animation.isActive
                                                    if (
                                                        wasAnimating &&
                                                            animationFrames.tryReceive().isFailure
                                                    ) {
                                                        // Preserve the last physical velocity sample
                                                        // before retargeting measured geometry.
                                                        select<Unit> {
                                                            animationFrames.onReceive { }
                                                            animation.onJoin { }
                                                        }
                                                    }
                                                    // Recompute because a conflated event can be stale
                                                    // by the time the animation actor handles it.
                                                    val destination = logicalTarget(request)
                                                    if (
                                                        kotlin.math.abs(
                                                            destination - animationDestination
                                                        ) >= 0.5
                                                    ) {
                                                        val initialVelocity =
                                                            if (wasAnimating) animator.velocity
                                                            else 0f
                                                        animation.cancelAndJoin()
                                                        animationDestination = destination
                                                        animation =
                                                            startAnimation(
                                                                destination,
                                                                lyricsFollowRetargetSpec(animationSpec),
                                                                initialVelocity,
                                                                retargeted = true,
                                                                carryFrameTime = wasAnimating,
                                                            )
                                                    }
                                                }
                                                LyricsFollowEvent.AnimationFrame -> Unit
                                                LyricsFollowEvent.Finished -> {
                                                    val destination = logicalTarget(request)
                                                    if (
                                                        kotlin.math.abs(
                                                            destination - animationDestination
                                                        ) >= 0.5
                                                    ) {
                                                        animationDestination = destination
                                                        animation =
                                                            startAnimation(
                                                                destination,
                                                                lyricsFollowRetargetSpec(animationSpec),
                                                                0f,
                                                                retargeted = true,
                                                                carryFrameTime = false,
                                                            )
                                                    } else {
                                                        finished = true
                                                    }
                                                }
                                            }
                                        }
                                    } finally {
                                        if (onCaptionGeometryChange === captionGeometryChange) {
                                            onCaptionGeometryChange = previousCaptionGeometryChange
                                        }
                                        interruptFollow = null
                                        geometryObserver.cancelAndJoin()
                                        geometryUpdates.close()
                                        animationFrames.close()
                                    }
                                }
                            }
                        } finally {
                            interruptFollow = null
                            followingChain = false
                            predictingFollow = false
                        }
                    }
                // A higher-priority user gesture may cancel this mutation. Keep the request
                // collector alive so automatic following can resume after the gesture ends.
                scrollJob.join()
            }
        } finally {
            interruptFollow = null
            if (onImmediateFollowRequest === immediateFollowRequest) {
                onImmediateFollowRequest = previousImmediateFollowRequest
            }
        }
    }

    internal var position by mutableDoubleStateOf(0.0)
    internal var maxPosition by mutableDoubleStateOf(0.0)
    internal var ready by mutableStateOf(false)
    internal var remeasurement: Remeasurement? = null
    internal var tryPlacementScroll: (() -> Boolean)? = null
    internal var onScrollPrefetch: ((Float, Boolean) -> Unit)? = null
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

    internal var onImmediateFollowRequest: ((Int) -> Unit)? = null

    internal val requestedClickTarget
        get() = clickTargetIndex

    internal val clickTargetRevision
        get() = clickTargetRevisionState

    /** Resume following immediately after a lyric click or a caller's explicit seek. */
    private var clickPosition: Int? = null
    private var clickTime = TimeSource.Monotonic.markNow()
    private var clickTargetIndex by mutableIntStateOf(-1)
    private var clickTargetRevisionState by mutableIntStateOf(0)

    fun resumeAutoScroll(seekPosition: Int? = null, targetIndex: Int? = null) {
        require(targetIndex == null || targetIndex >= 0)
        clickPosition = seekPosition
        clickTime = TimeSource.Monotonic.markNow()
        clickTargetIndex = targetIndex ?: -1
        clickTargetRevisionState++
        isManualScrolling = false
        resumeRequest++
        targetIndex?.let { onImmediateFollowRequest?.invoke(it) }
        chain.traceMarker(
            LyricsSpringTraceEvent.FOLLOW_REQUEST,
            eventIndex = targetIndex ?: -1,
            eventValue = seekPosition?.toDouble() ?: Double.NaN,
            eventValue2 = resumeRequest.toDouble(),
        )
    }

    internal fun clearClickTarget(revision: Int) {
        if (clickTargetRevisionState == revision) {
            clickTargetIndex = -1
            chain.traceMarker(LyricsSpringTraceEvent.CLICK_TARGET_CLEARED, eventIndex = revision)
        }
    }

    internal fun consumeClickSeek(time: Int): Boolean {
        val expected = clickPosition ?: return false
        if (clickTime.elapsedNow().inWholeMilliseconds > LyricsClickFollowTimeoutMillis) {
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
            if (followingChain && !autoFollowSuspended) chain.followScrollTo(position)
            else chain.rebase(position)
            if (tryPlacementScroll?.invoke() != true) remeasurement?.forceRemeasure()
            onScrollPrefetch?.invoke((position - previous).toFloat(), followingChain)
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
        newKeys: Map<Any, Int>,
        width: Int,
        spacing: Int,
        top: Int,
        bottom: Int,
        viewport: Int,
    ) {
        if (items !== newItems || configuredWidth != width || configuredSpacing != spacing) {
            val anchorKey = items.getOrNull(firstVisibleItemIndex)?.key
            val anchorOffset = firstVisibleItemScrollOffset
            keys = newKeys
            items = newItems
            heights =
                LyricsHeightIndex(IntArray(items.size) { items[it].estimatedHeightPx }, spacing)
            configuredWidth = width
            configuredSpacing = spacing
            recordLayoutChange()
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
        val previousMaxPosition = maxPosition
        maxPosition =
            if (heights.size == 0) 0.0
            else (top + heights.total + bottom - viewport).coerceAtLeast(0.0)
        if (maxPosition != previousMaxPosition) recordLayoutChange()
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
            suspendAutoFollow(reason = 3)
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
        animateToItem(
            index,
            scrollOffset,
            animationSpec,
            followChain = false,
            focusEnd = index,
        )
    }

    private suspend fun animateToItem(
        index: Int,
        scrollOffset: Int,
        animationSpec: AnimationSpec<Float>,
        followChain: Boolean,
        focusEnd: Int,
    ) {
        require(index >= 0)
        snapshotFlow { ready }.first { it }
        if (items.isEmpty()) return
        scroll {
            followingChain = followChain
            autoFollowSuspended = !followChain
            chain.focusAt(focusEnd)
            if (!followChain) {
                chain.traceMarker(
                    LyricsSpringTraceEvent.DIRECT_SCROLL_TARGET,
                    eventIndex = index,
                    eventValue = scrollOffset.toDouble(),
                )
            }
            if (!followingChain) chain.reset(position)
            try {
                val targetIndex = index.coerceAtMost(items.lastIndex)
                val start = position
                val initialCoordinateShift = contentCoordinateShift
                predictingFollow =
                    followChain &&
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
