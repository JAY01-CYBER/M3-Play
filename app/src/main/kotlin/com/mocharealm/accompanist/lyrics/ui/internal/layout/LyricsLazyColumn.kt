/*
 * Copyright 2026 Accompanist contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mocharealm.accompanist.lyrics.ui.internal.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsListItem
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsScrollChain
import com.mocharealm.accompanist.lyrics.ui.internal.diagnostics.traceLyrics
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Own vertical virtual-list measurement and scrolling, built on Compose's slot-recycling harness.
 * Item heights come from prepared content, then track actual measured size (e.g. expanding backing
 * vocals). Keys must be unique and saveable on the target platform. No LazyColumn/LazyListState is
 * used.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsLazyColumn(
    items: List<LyricsListItem>,
    state: LyricsLazyListState,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondBounds: Dp = 100.dp,
    userScrollEnabled: Boolean = true,
    scrollChain: LyricsScrollChain? = null,
    itemContent: @Composable (Int) -> Unit,
) {
    require(itemSpacing.value.isFinite() && itemSpacing >= 0.dp)
    require(beyondBounds.value.isFinite() && beyondBounds >= 0.dp)
    val scope = rememberCoroutineScope()
    val content by rememberUpdatedState(itemContent)
    val prefetch = remember(items, state) { LyricsItemPrefetch(LazyLayoutPrefetchState()) }
    DisposableEffect(prefetch, state) {
        state.onScrollPrefetch = prefetch::onScroll
        onDispose {
            prefetch.cancel()
            state.onScrollPrefetch = null
        }
    }
    val provider =
        remember(items, state) {
            val indices = items.withIndex().associate { it.value.key to it.index }
            object : LazyLayoutItemProvider {
                override val itemCount
                    get() = items.size

                override fun getKey(index: Int) = items[index].key

                override fun getIndex(key: Any) = indices[key] ?: -1

                @Composable
                override fun Item(index: Int, key: Any) {
                    Box(
                        Modifier.graphicsLayer { translationY = state.chain.offset(index) }
                            .semantics { collectionItemInfo = CollectionItemInfo(index, 1, 0, 1) }
                    ) {
                        content(index)
                    }
                }
            }
        }
    val remeasureModifier =
        remember(state) {
            object : RemeasurementModifier {
                override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                    state.remeasurement = remeasurement
                }
            }
        }
    DisposableEffect(state) {
        onDispose {
            state.remeasurement = null
            state.tryPlacementScroll = null
            state.ready = false
        }
    }
    LaunchedEffect(state, scrollChain) {
        snapshotFlow { state.chain.active }
            .collectLatest { active ->
                if (active) {
                    var previous = withFrameNanos { it }
                    while (state.chain.active) {
                        withFrameNanos { now ->
                            state.chain.advance((now - previous) / 1_000_000_000f)
                            previous = now
                        }
                    }
                }
            }
    }
    LazyLayout(
        itemProvider = { provider },
        prefetchState = prefetch.state,
        modifier =
            modifier
                .then(remeasureModifier)
                .clipToBounds()
                .scrollable(
                    state,
                    Orientation.Vertical,
                    enabled = userScrollEnabled,
                    reverseDirection = true,
                    interactionSource = state.interactionSource,
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                )
                .semantics {
                    isTraversalGroup = true
                    collectionInfo = CollectionInfo(items.size, 1)
                    verticalScrollAxisRange =
                        ScrollAxisRange(
                            value = { state.position.toFloat() },
                            maxValue = { state.maxPosition.toFloat() },
                        )
                    if (userScrollEnabled) {
                        scrollBy { _, y ->
                            scope.launch { state.animateScrollBy(y) }
                            true
                        }
                        scrollToIndex { index ->
                            if (index !in items.indices) false
                            else {
                                scope.launch { state.scrollToItem(index) }
                                true
                            }
                        }
                    }
                },
        measurePolicy =
            LazyLayoutMeasurePolicy { constraints ->
                traceLyrics("Lyrics.measure") {
                    require(constraints.hasBoundedHeight && constraints.hasBoundedWidth) {
                        "LyricsLazyColumn needs bounded width and height"
                    }
                    state.recordMeasure()
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight
                    val left = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                    val right = contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                    val top = contentPadding.calculateTopPadding().roundToPx()
                    val bottom = contentPadding.calculateBottomPadding().roundToPx()
                    val spacing = itemSpacing.roundToPx()
                    val buffer =
                        beyondBounds.roundToPx() +
                            if (scrollChain != null && state.chain.active) height else 0
                    val childWidth = (width - left - right).coerceAtLeast(0)
                    Snapshot.withoutReadObservation {
                        state.configure(items, childWidth, spacing, top, bottom, height)
                        state.chain.configure(items.size, scrollChain, state.position, height)
                    }
                    val position = Snapshot.withoutReadObservation { state.position }
                    val startIndex =
                        state.heights.itemAt((position - top - buffer).coerceAtLeast(0.0))
                    val measured = ArrayList<Placeable>()
                    val anchor =
                        Snapshot.withoutReadObservation {
                            if (state.anchoringContentChange) state.followAnchorIndex
                            else state.firstVisibleItemIndex
                        }
                    val startTop = state.heights.top(startIndex)
                    var itemTop = startTop
                    var y = top + itemTop - position
                    var index = startIndex
                    val childConstraints = Constraints(minWidth = childWidth, maxWidth = childWidth)
                    while (index < items.size && y < height + buffer) {
                        val placeable = compose(index).single().measure(childConstraints)
                        val previousHeight = state.heights.height(index)
                        state.heights.update(index, placeable.height)
                        val heightDelta = placeable.height - previousHeight
                        if (index == state.interludeItemIndex && heightDelta != 0) {
                            // Dots are leading content of this item. Shift every later item as a
                            // single geometry update; do not feed the same delta into each spring.
                            state.chain.applyContentShift(index, heightDelta.toDouble())
                        }
                        // Expansion above the visible anchor must not shove the reader's current
                        // line.
                        if (index < anchor && state.anchoringContentChange) {
                            Snapshot.withoutReadObservation {
                                state.compensateContentHeightChange(
                                    placeable.height - previousHeight
                                )
                            }
                        } else if (
                            index < anchor &&
                                items[index].preserveAnchorOnHeightChange &&
                                !state.hasPredictedScrollGeometry
                        )
                            Snapshot.withoutReadObservation {
                                state.position += placeable.height - previousHeight
                            }
                        measured.add(placeable)
                        index++
                        itemTop += placeable.height.toDouble() + spacing
                        y = top + itemTop - Snapshot.withoutReadObservation { state.position }
                    }
                    Snapshot.withoutReadObservation {
                        state.updateRange(top, bottom, height)
                        state.chain.rebase(state.position)
                    }
                    state.chain.retain(startIndex, index)
                    state.finishInterludeMeasure()
                    state.retainedFirst = startIndex
                    state.retainedEnd = index
                    val anchorCorrection =
                        Snapshot.withoutReadObservation { state.position } - position
                    // Adjacent tops are prefix + measured extents: one tree lookup per range,
                    // rather than one lookup per item in each of three passes.
                    val positions = DoubleArray(measured.size)
                    itemTop = startTop
                    // A predicted follow already includes settled heights in its target scroll.
                    // Keep those height changes in the deterministic layout instead of seeding
                    // the same displacement into the per-item spring a second time.
                    for (i in measured.indices) {
                        // Prepared lyric items own caption visibility. Keep the current anchor
                        // and its preceding content on the new layout directly; only items below
                        // it enter the spring. Generic items retain predicted-height suppression.
                        val itemIndex = startIndex + i
                        val preserveScreenPosition =
                            (state.anchoringContentChange || !state.predictedFollowActive) &&
                                (items[itemIndex].preserveAnchorOnHeightChange ||
                                    itemIndex > anchor)
                        state.chain.layoutAt(
                            startIndex + i,
                            itemTop,
                            anchorCorrection,
                            preserveScreenPosition,
                        )
                        positions[i] = top + itemTop
                        itemTop += measured[i].height.toDouble() + spacing
                    }
                    val measuredEnd = index
                    val endTop = itemTop
                    val chainWasActive = state.chain.active
                    state.tryPlacementScroll = {
                        val current = state.position
                        val first =
                            state.heights.itemAt((current - top - buffer).coerceAtLeast(0.0))
                        val covered =
                            measuredEnd == items.size || top + endTop >= current + height + buffer
                        if (
                            state.items === items &&
                                first == startIndex &&
                                covered &&
                                state.chain.active == chainWasActive
                        ) {
                            state.updateRange(top, bottom, height)
                            true
                        } else false
                    }
                    prefetch.measured(
                        startIndex,
                        measuredEnd,
                        items.size,
                        childConstraints,
                        ((endTop - startTop) / measured.size.coerceAtLeast(1)).toFloat(),
                        height.toFloat(),
                    )
                    layout(width, height) {
                        val current = state.position
                        for (i in measured.indices) measured[i].place(
                            left,
                            (positions[i] - current).roundToInt(),
                        )
                    }
                }
            },
    )
}
