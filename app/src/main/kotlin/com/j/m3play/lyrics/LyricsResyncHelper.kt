/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

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
