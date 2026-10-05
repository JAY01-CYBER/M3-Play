package com.mocharealm.accompanist.lyrics.ui.internal.playback

import androidx.compose.runtime.mutableStateOf
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow

/** Sorted boundary cursor: constant work between events, binary search on seeks. */
internal class BoundaryCursor(private val times: IntArray) {
    var index = -1
        private set

    private var previous = Int.MIN_VALUE

    fun advance(time: Int): Boolean {
        val old = index
        if (time < previous || time.toLong() - previous > 1000L) {
            var low = 0
            var high = times.size
            while (low < high) {
                val mid = (low + high) ushr 1
                if (times[mid] <= time) low = mid + 1 else high = mid
            }
            index = low - 1
        } else {
            while (index + 1 < times.size && times[index + 1] <= time) index++
        }
        previous = time
        return old != index
    }
}

private class TimelineBoundary {
    val startingLines = mutableListOf<Int>()
    val endingLines = mutableListOf<Int>()
    val startingRows = mutableListOf<PreparedRow>()
    val endingRows = mutableListOf<PreparedRow>()
    val showingLines = mutableListOf<PreparedLine>()
    val hidingLines = mutableListOf<PreparedLine>()
    val startingInterludes = mutableListOf<Int>()
    val endingInterludes = mutableListOf<Int>()
}

internal class LyricsPlaybackTimeline(lyrics: SyncedLyrics, prepared: PreparedLyrics) {
    val state = LyricsPlaybackState(prepared)
    private val boundaries: IntArray
    private val focusStates: List<FocusState>
    private val activeRows: List<List<PreparedRow>>
    private val endingRows: List<List<PreparedRow>>
    private val visibleLines: List<List<PreparedLine>>
    private val hidingLines: List<List<PreparedLine>>
    private val accompaniment =
        prepared.allLines.filter { it.source is KaraokeLine.AccompanimentKaraokeLine }
    private val allRows = prepared.rows
    private val showingLines: List<List<PreparedLine>>
    private var previousRows: List<PreparedRow> = emptyList()
    private val cursor: BoundaryCursor
    private var previousTime = Int.MIN_VALUE
    private var initialized = false
    val focus = mutableStateOf(FocusState(0, emptyList(), null, false))
    val introEnd: Int
    val interludeEnds: IntArray
    val interludeStarts: IntArray

    init {
        val lines = lyrics.lines
        val starts =
            IntArray(lines.size) { index ->
                val line = lines[index]
                minOf(
                    line.start,
                    (line as? KaraokeLine.MainKaraokeLine)?.accompanimentLines?.minOfOrNull {
                        it.start
                    } ?: line.start,
                )
            }
        val ends =
            IntArray(lines.size) { index ->
                val line = lines[index]
                maxOf(
                    line.end,
                    (line as? KaraokeLine.MainKaraokeLine)?.accompanimentLines?.maxOfOrNull {
                        it.end
                    } ?: line.end,
                )
            }
        interludeEnds = starts
        introEnd = starts.firstOrNull() ?: 0
        interludeStarts = IntArray(lines.size) { if (it > 0) ends[it - 1] else 0 }
        val events = sortedMapOf<Int, TimelineBoundary>()
        fun at(time: Int) = events.getOrPut(time) { TimelineBoundary() }
        at(Int.MIN_VALUE)
        at(0)
        val embedded = BooleanArray(lines.size) { prepared.lines[it] in prepared.embeddedLines }
        for (index in lines.indices) {
            // Nested vocals share the owner's focus lifetime and list item. Their own end
            // must not change the list's focus range or restart automatic scrolling.
            if (embedded[index]) continue
            at(starts[index])
            if (ends[index] > starts[index]) {
                at(starts[index]).startingLines.add(index)
                at(ends[index]).endingLines.add(index)
            }
            if (index > 0 && starts[index].toLong() - ends[index - 1] > 5000) {
                at(ends[index - 1]).startingInterludes.add(index)
                at(starts[index]).endingInterludes.add(index)
            }
        }
        for (row in allRows) for (window in row.windows) {
            at(window.start).startingRows.add(row)
            at(window.end).endingRows.add(row)
        }
        for (line in accompaniment) {
            at(line.visibilityStart).showingLines.add(line)
            at(line.visibilityEnd).hidingLines.add(line)
        }

        // Resolve accompaniment anchors once; nested lines already extend their parent's interval.
        val mainIndices =
            lines.indices
                .filter { lines[it] !is KaraokeLine.AccompanimentKaraokeLine }
                .sortedBy { lines[it].start }
        val anchors = IntArray(lines.size) { it }
        for (index in lines.indices) if (
            lines[index] is KaraokeLine.AccompanimentKaraokeLine && mainIndices.isNotEmpty()
        ) {
            var low = 0
            var high = mainIndices.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (lines[mainIndices[middle]].start < lines[index].start) low = middle + 1
                else high = middle
            }
            val before = mainIndices[(low - 1).coerceAtLeast(0)]
            val after = mainIndices[low.coerceAtMost(mainIndices.lastIndex)]
            anchors[index] =
                if (
                    kotlin.math.abs(lines[before].start.toLong() - lines[index].start) <=
                        kotlin.math.abs(lines[after].start.toLong() - lines[index].start)
                )
                    before
                else after
        }
        val future = lines.indices.filter { !embedded[it] }.sortedBy { starts[it] }
        var next = 0
        val focused = sortedSetOf<Int>()
        val counts = IntArray(lines.size)
        fun changeFocus(index: Int, delta: Int) {
            counts[index] += delta
            if (counts[index] > 0) focused.add(index) else focused.remove(index)
        }
        val rows = linkedSetOf<PreparedRow>()
        val visible = linkedSetOf<PreparedLine>()
        val interludes = sortedSetOf<Int>()
        val focusIntervals = mutableListOf<FocusState>()
        val rowIntervals = mutableListOf<List<PreparedRow>>()
        val visibleIntervals = mutableListOf<List<PreparedLine>>()
        var focusedIndices: List<Int> = emptyList()
        var rowSnapshot: List<PreparedRow> = emptyList()
        var visibleSnapshot: List<PreparedLine> = emptyList()
        for ((time, event) in events) {
            for (index in event.endingLines) {
                changeFocus(index, -1)
                if (anchors[index] != index) changeFocus(anchors[index], -1)
            }
            for (index in event.startingLines) {
                changeFocus(index, 1)
                if (anchors[index] != index) changeFocus(anchors[index], 1)
            }
            if (event.endingLines.isNotEmpty() || event.startingLines.isNotEmpty())
                focusedIndices = focused.toList()
            while (next < future.size && starts[future[next]] <= time) next++
            interludes.removeAll(event.endingInterludes.toSet())
            interludes.addAll(event.startingInterludes)
            val first =
                focused.firstOrNull() ?: future.getOrNull(next) ?: lines.lastIndex.coerceAtLeast(0)
            focusIntervals.add(
                FocusState(
                    first,
                    focusedIndices,
                    interludes.firstOrNull(),
                    introEnd > 5000 && time >= 0 && time < introEnd,
                )
            )
            if (event.startingRows.isNotEmpty() || event.endingRows.isNotEmpty()) {
                rows.removeAll(event.endingRows.toSet())
                rows.addAll(event.startingRows)
                rowSnapshot = rows.toList()
            }
            rowIntervals.add(rowSnapshot)
            if (event.showingLines.isNotEmpty() || event.hidingLines.isNotEmpty()) {
                visible.removeAll(event.hidingLines.toSet())
                visible.addAll(event.showingLines)
                visibleSnapshot = visible.toList()
            }
            visibleIntervals.add(visibleSnapshot)
        }
        boundaries = events.keys.toIntArray()
        focusStates = focusIntervals
        activeRows = rowIntervals
        visibleLines = visibleIntervals
        endingRows = events.values.map { it.endingRows }
        hidingLines = events.values.map { it.hidingLines }
        showingLines = events.values.map { it.showingLines }
        cursor = BoundaryCursor(boundaries)
    }

    fun update(time: Int) {
        val seek = !initialized || time < previousTime || time.toLong() - previousTime > 1000L
        val previousIndex = cursor.index
        if (cursor.advance(time)) {
            val index = cursor.index.coerceAtLeast(0)
            if (seek) {
                // Seeking changes static rows too, including rows skipped entirely.
                for (row in allRows) state.row(row).time.intValue = staticTime(row, time)
                for (line in accompaniment) state.line(line).visible.value = false
                for (line in visibleLines[index]) state.line(line).visible.value = true
            } else {
                for (boundary in previousIndex + 1..index) {
                    for (row in endingRows[boundary]) state.row(row).time.intValue = staticTime(row, time)
                    for (line in hidingLines[boundary]) state.line(line).visible.value = false
                    for (line in showingLines[boundary]) state.line(line).visible.value = true
                }
            }
            previousRows = activeRows[index]
            focus.value = focusStates[index]
        }
        for (index in previousRows.indices) state.row(previousRows[index]).time.intValue = time
        previousTime = time
        initialized = true
    }

    private fun staticTime(row: PreparedRow, time: Int): Int =
        when {
            time < row.start -> Int.MIN_VALUE
            time >= row.end -> Int.MAX_VALUE
            else -> time
        }
}
