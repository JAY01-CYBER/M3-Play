package com.mocharealm.accompanist.lyrics.ui.internal.diagnostics

internal fun lyricsTraceEnabled(): Boolean = android.os.Trace.isEnabled()

internal fun beginLyricsTrace(name: String) = android.os.Trace.beginSection(name)

internal fun endLyricsTrace() = android.os.Trace.endSection()

internal fun setLyricsTraceCounter(name: String, value: Long) =
    android.os.Trace.setCounter(name, value)
