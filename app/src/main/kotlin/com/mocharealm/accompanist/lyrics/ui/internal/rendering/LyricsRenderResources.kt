package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow
import com.mocharealm.accompanist.lyrics.ui.internal.diagnostics.traceLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Line rasters are prepared on demand and retained in a bounded, viewport-friendly cache. */
internal class LyricsRenderResources(
    prepared: PreparedLyrics,
    val color: Color,
    private val density: Density,
    private val direction: LayoutDirection,
) {
    private val rasters: Map<PreparedLine, MutableState<PreparedLineRaster?>> =
        prepared.allLines.associateWith { mutableStateOf(null) }
    private val rows = prepared.allLines.flatMap { it.rows }.associateWith { RowRenderState(it) }
    private val rasterMutex = Mutex()
    private val cacheLock = LyricsResourceLock()
    private val rasterizationLock = LyricsResourceLock()
    private val displayUnitCache = DisplayUnitRasterCache()
    private val retainedLines = mutableMapOf<PreparedLine, Int>()
    private val recentlyUsed = mutableListOf<PreparedLine>()
    private val maxCachedLines = 24
    private val maxCachedBytes = 16L * 1024 * 1024
    private val rasterBytes = mutableMapOf<PreparedLine, Long>()
    private var cachedBytes = 0L

    fun rasterState(line: PreparedLine): State<PreparedLineRaster?> = rasters.getValue(line)

    /** Synchronous compatibility path for renderer-level callers; UI rendering uses prepareRaster. */
    fun raster(line: PreparedLine): PreparedLineRaster {
        val cached = cacheLock.withLock { rasters.getValue(line).value?.also { touch(line) } }
        if (cached != null) return cached
        val raster = rasterizationLock.withLock {
            prepareLineRaster(line, color, density, direction, displayUnitCache)
        }
        return cacheLock.withLock {
            rasters.getValue(line).value?.also { touch(line) } ?: raster.also { store(line, it) }
        }
    }

    suspend fun prepareRaster(line: PreparedLine): PreparedLineRaster = rasterMutex.withLock {
        val cached = cacheLock.withLock {
            rasters.getValue(line).value?.also { touch(line) }
        }
        if (cached != null) return@withLock traceLyrics("Lyrics.rasterLineCacheHit") { cached }
        val raster = withContext(Dispatchers.Default) {
            rasterizationLock.withLock {
                traceLyrics("Lyrics.rasterizeLine") {
                    prepareLineRaster(line, color, density, direction, displayUnitCache)
                }
            }
        }
        cacheLock.withLock {
            rasters.getValue(line).value?.also { touch(line) } ?: raster.also { store(line, it) }
        }
    }

    fun retain(line: PreparedLine) = cacheLock.withLock {
        retainedLines[line] = (retainedLines[line] ?: 0) + 1
        touch(line)
    }

    fun release(line: PreparedLine) = cacheLock.withLock {
        val count = retainedLines[line] ?: return@withLock
        if (count <= 1) retainedLines.remove(line) else retainedLines[line] = count - 1
        trim()
    }

    private fun touch(line: PreparedLine) {
        recentlyUsed.remove(line)
        recentlyUsed.add(line)
    }

    private fun store(line: PreparedLine, raster: PreparedLineRaster) {
        val state = rasters.getValue(line)
        rasterBytes.put(line, raster.byteCount)?.let { cachedBytes -= it }
        cachedBytes += raster.byteCount
        state.value = raster
        touch(line)
        trim()
    }

    private fun trim() {
        while (recentlyUsed.size > maxCachedLines || cachedBytes > maxCachedBytes) {
            val oldestUnused = recentlyUsed.indexOfFirst { (retainedLines[it] ?: 0) == 0 }
            if (oldestUnused < 0) return
            val line = recentlyUsed.removeAt(oldestUnused)
            rasters.getValue(line).value = null
            rasterBytes.remove(line)?.let { cachedBytes -= it }
        }
    }

    fun row(row: PreparedRow): RowRenderState = rows.getValue(row)
}
