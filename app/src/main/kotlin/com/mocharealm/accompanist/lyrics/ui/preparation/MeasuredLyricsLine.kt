package com.mocharealm.accompanist.lyrics.ui.preparation

import androidx.compose.ui.text.TextLayoutResult
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileAnimationUnit

/** Initial unlimited-width measurement supplied by the system before profile processing. */
class MeasuredLyricsLine(val layout: TextLayoutResult, val syllables: List<KaraokeSyllable>) {
    val ranges: List<MeasuredSyllable> = buildList {
        var offset = 0
        for (source in syllables) {
            add(
                MeasuredSyllable(
                    source,
                    offset,
                    offset + source.content.length,
                    ProfileAnimationUnit(source.start, source.end),
                )
            )
            offset += source.content.length
        }
    }
    private val orderedRanges =
        ranges.zipWithNext().all { (a, b) ->
            a.source.start <= b.source.start && a.source.end <= b.source.end
        }

    internal fun sourceFor(start: Int, end: Int): MeasuredSyllable? {
        if (!orderedRanges)
            return ranges.firstOrNull { start >= it.source.start && end <= it.source.end }
        var low = 0
        var high = ranges.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (ranges[middle].source.end < end) low = middle + 1 else high = middle
        }
        return ranges.getOrNull(low)?.takeIf { start >= it.source.start }
    }
}
