package com.mocharealm.accompanist.lyrics.ui.internal.rendering

internal class LyricsResourceLock constructor() {
    private val monitor = Any()
    fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
