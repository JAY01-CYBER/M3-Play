package com.mocharealm.accompanist.lyrics.ui.internal.diagnostics

/**
 * Android/JVM implementation for the vendored Accompanist lyrics UI.
 *
 * The upstream source is Kotlin Multiplatform and declares these as expect
 * functions. M3-Play is a regular Android module, so expect/actual cannot be
 * compiled here. Diagnostics are intentionally no-op in the Android app.
 */
internal fun lyricsTraceEnabled(): Boolean = false

internal fun beginLyricsTrace(name: String) = Unit

internal fun endLyricsTrace() = Unit

internal inline fun <T> traceLyrics(name: String, block: () -> T): T {
    if (!lyricsTraceEnabled()) return block()
    beginLyricsTrace(name)
    try {
        return block()
    } finally {
        endLyricsTrace()
    }
}
