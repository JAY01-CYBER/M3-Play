/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.simpmusic.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsData(
    val id: String? = null,
    val videoId: String? = null,
    @SerialName("songTitle")
    val title: String? = null,
    @SerialName("artistName")
    val artist: String? = null,
    @SerialName("albumName")
    val album: String? = null,
    @SerialName("durationSeconds")
    val duration: Int? = null,
    val syncedLyrics: String? = null,
    @SerialName("plainLyric")
    val plainLyrics: String? = null,
    val richSyncLyrics: String? = null,
    val vote: Int? = null,
)

@Serializable
data class SimpMusicApiResponse(
    val type: String? = null,
    val success: Boolean? = null,
    val data: List<LyricsData> = emptyList(),
) {
    val isSuccessful: Boolean
        get() = success ?: (type == "success")
}
