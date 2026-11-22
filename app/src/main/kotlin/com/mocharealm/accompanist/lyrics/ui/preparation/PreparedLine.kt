package com.mocharealm.accompanist.lyrics.ui.preparation

import androidx.compose.ui.text.TextLayoutResult
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine

class PreparedLine
internal constructor(
    val source: KaraokeLine,
    val runs: List<PreparedProfileRun>,
    val rows: List<PreparedRow>,
    val width: Float,
    val height: Float,
    val rightAligned: Boolean,
    val before: List<PreparedLine>,
    val after: List<PreparedLine>,
    val translation: TextLayoutResult?,
    val phonetic: TextLayoutResult?,
) {
    internal var visibilityStart: Int = source.start
        private set

    internal var visibilityEnd: Int = source.end
        private set

    internal var revealFromBottom: Boolean = false
        private set

    init {
        for (child in before + after) {
            child.visibilityStart = minOf(child.source.start, source.start)
            child.visibilityEnd = maxOf(child.source.end, source.end)
            child.revealFromBottom = child.source.start < source.start
        }
    }
}
