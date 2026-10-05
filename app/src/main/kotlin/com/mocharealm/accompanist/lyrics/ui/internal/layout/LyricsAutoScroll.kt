package com.mocharealm.accompanist.lyrics.ui.internal.layout

import com.mocharealm.accompanist.lyrics.ui.composable.list.*

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Manual interaction suspends following; the latest target wins, including during an animation. */
@Composable
internal fun lyricsAutoScroll(
    state: LyricsLazyListState,
    targetIndex: () -> Int,
    animationSpec: AnimationSpec<Float>,
    resumeDelayMillis: Long,
    focusEndIndex: () -> Int = targetIndex,
    playbackPosition: (() -> Int)? = null,
): Modifier {
    require(resumeDelayMillis >= 0)
    val target by rememberUpdatedState(targetIndex)
    val focusEnd by rememberUpdatedState(focusEndIndex)
    val playback by rememberUpdatedState(playbackPosition)
    var seekRevision by remember(state) { mutableIntStateOf(0) }
    DisposableEffect(state) { onDispose { state.isManualScrolling = false } }
    var dragging by remember(state) { mutableStateOf(false) }
    var interaction by remember(state) { mutableIntStateOf(0) }
    val connection =
        remember(state) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) {
                        state.suspendChain()
                        state.isManualScrolling = true
                        interaction++
                    }
                    return Offset.Zero
                }
            }
        }
    LaunchedEffect(state) {
        state.interactionSource.interactions.collect {
            when (it) {
                is DragInteraction.Start -> {
                    state.suspendChain()
                    dragging = true
                    state.isManualScrolling = true
                    interaction++
                }
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> {
                    dragging = false
                    interaction++
                }
            }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.resumeRequest }.collect { state.isManualScrolling = false }
    }
    LaunchedEffect(state, resumeDelayMillis) {
        snapshotFlow { Triple(interaction, dragging, state.isScrollInProgress) }
            .collectLatest {
                if (state.isManualScrolling && !it.second && !it.third) {
                    delay(resumeDelayMillis)
                    state.isManualScrolling = false
                }
            }
    }
    LaunchedEffect(state) {
        var previous: Int? = null
        snapshotFlow { playback?.invoke() }
            .collect { time ->
                val last = previous
                if (
                    time != null &&
                        last != null &&
                        (time.toLong() - last < -1 || time.toLong() - last > 600)
                ) {
                    if (!state.consumeClickSeek(time)) seekRevision++
                }
                previous = time
            }
    }
    LaunchedEffect(state, animationSpec) {
        var previousSeek = seekRevision
        var previousResume = state.resumeRequest
        snapshotFlow {
                FollowTarget(
                    if (state.isManualScrolling) -1 else target(),
                    focusEnd(),
                    seekRevision,
                    state.resumeRequest,
                )
            }
            .collectLatest { request ->
                val cascade = request.seek == previousSeek || request.resume != previousResume
                previousSeek = request.seek
                previousResume = request.resume
                if (request.index >= 0) {
                    state.animateFollowToItem(request.index, request.end, cascade, animationSpec)
                }
            }
    }
    return Modifier.nestedScroll(connection)
}

private data class FollowTarget(val index: Int, val end: Int, val seek: Int, val resume: Int)
