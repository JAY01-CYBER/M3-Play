package com.mocharealm.accompanist.lyrics.ui.internal.playback

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine

/** One playback owner per view; prepared geometry may be shared by independent clocks. */
internal class LyricsPlaybackState(prepared: PreparedLyrics) {
    private val rows = prepared.allLines.flatMap { it.rows }.associateWith { RowPlaybackState() }
    private val lines = prepared.allLines.associateWith {
        LinePlaybackState(it.source !is KaraokeLine.AccompanimentKaraokeLine)
    }

    fun row(row: PreparedRow): RowPlaybackState = rows.getValue(row)
    fun line(line: PreparedLine): LinePlaybackState = lines.getValue(line)
}

internal class RowPlaybackState {
    val time = mutableIntStateOf(Int.MIN_VALUE)
}

internal class LinePlaybackState(visible: Boolean) {
    val visible = mutableStateOf(visible)
}
