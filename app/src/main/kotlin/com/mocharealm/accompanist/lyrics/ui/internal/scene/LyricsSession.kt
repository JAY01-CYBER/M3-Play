package com.mocharealm.accompanist.lyrics.ui.internal.scene

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.ui.internal.playback.LyricsPlaybackTimeline
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLyrics

/** Build the event index and item mapping on the preparation worker, once per layout. */
internal class LyricsSession(source: SyncedLyrics, val lyrics: PreparedLyrics) {
    val timeline = LyricsPlaybackTimeline(source, lyrics)
    val items = LyricsItemMapping(lyrics)
}
