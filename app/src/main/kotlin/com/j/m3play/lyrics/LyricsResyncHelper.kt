/**
 * M3Play Project
 */
package com.j.m3play.lyrics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LyricsResyncHelper {
    private val _resyncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resyncTrigger: SharedFlow<Unit> = _resyncTrigger.asSharedFlow()

    fun triggerResync() {
        _resyncTrigger.tryEmit(Unit)
    }
}
