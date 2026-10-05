package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.internal.diagnostics.traceLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.LyricsRenderResources
import kotlinx.coroutines.flow.collectLatest

/** A bounded GPU warm-up window, independent of text preparation and lazy item lifetimes. */
@Composable
internal fun LyricsTexturePrefetch(
    lines: List<PreparedLine?>,
    resources: LyricsRenderResources,
    state: LyricsLazyListState,
    focusedItem: () -> Int,
) {
    val focus by rememberUpdatedState(focusedItem)
    LaunchedEffect(lines, resources, state) {
        val requested = mutableSetOf<ImageBitmap>()
        snapshotFlow {
                if (state.isManualScrolling) state.firstVisibleItemIndex
                else maxOf(state.firstVisibleItemIndex, focus())
            }
            .collectLatest { anchor ->
                val pages = linkedSetOf<ImageBitmap>()
                fun add(line: PreparedLine) {
                    line.before.forEach(::add)
                    resources.raster(line).rows.forEach { pages.addAll(it.pages) }
                    line.after.forEach(::add)
                }
                for (index in maxOf(0, anchor - 1)..minOf(lines.lastIndex, anchor + 6)) {
                    lines[index]?.let(::add)
                }
                requested.retainAll(pages)
                for (page in pages) if (page !in requested) {
                    // Schedule at most one page per frame. prepareToDraw is a platform hint,
                    // not a guarantee of residency; actual upload timing is checked in trace.
                    withFrameNanos {}
                    traceLyrics("Lyrics.prefetchTexture") { page.prepareToDraw() }
                    requested.add(page)
                }
            }
    }
}
