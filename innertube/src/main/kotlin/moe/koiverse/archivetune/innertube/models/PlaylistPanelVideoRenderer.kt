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

package com.j.m3play.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: Runs?,
    val lengthText: Runs?,
    val longBylineText: Runs?,
    val shortBylineText: Runs?,
    val badges: List<Badges>?,
    val videoId: String?,
    val playlistSetVideoId: String?,
    val selected: Boolean,
    val thumbnail: Thumbnails,
    val unplayableText: Runs?,
    val menu: Menu?,
    val navigationEndpoint: NavigationEndpoint,
)
