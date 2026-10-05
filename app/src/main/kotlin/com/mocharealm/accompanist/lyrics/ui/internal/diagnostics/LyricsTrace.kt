package com.mocharealm.accompanist.lyrics.ui.internal.diagnostics

internal expect fun lyricsTraceEnabled(): Boolean

internal expect fun beginLyricsTrace(name: String)

internal expect fun endLyricsTrace()

internal inline fun <T> traceLyrics(name: String, block: () -> T): T {
    if (!lyricsTraceEnabled()) return block()
    beginLyricsTrace(name)
    try {
        return block()
    } finally {
        endLyricsTrace()
    }
}
