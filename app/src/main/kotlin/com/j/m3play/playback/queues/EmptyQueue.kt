/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * ╭────────────────────────────────────────────╮
 * │            M3Play Core Engine              │
 * │--------------------------------------------│
 * │  Handles playback, audio pipeline & logic  │
 * │                                            │
 * │  Signature: M3PLAY::CORE::ENGINE::V1       │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.playback.queues

import androidx.media3.common.MediaItem
import com.j.m3play.models.MediaMetadata

object EmptyQueue : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus() = Queue.Status(null, emptyList(), -1)

    override fun hasNextPage() = false

    override suspend fun nextPage() = emptyList<MediaItem>()
}
