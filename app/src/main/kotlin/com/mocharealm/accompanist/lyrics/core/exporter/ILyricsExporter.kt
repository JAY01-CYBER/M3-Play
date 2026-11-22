package com.mocharealm.accompanist.lyrics.core.exporter

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

interface ILyricsExporter {
    fun export(lyrics: SyncedLyrics): String
}