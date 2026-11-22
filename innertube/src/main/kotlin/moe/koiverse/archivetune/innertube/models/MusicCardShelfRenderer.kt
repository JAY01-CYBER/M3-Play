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
data class MusicCardShelfRenderer(
    val title: Runs,
    val subtitle: Runs,
    val thumbnail: ThumbnailRenderer,
    val header: Header?,
    val contents: List<Content>?,
    val buttons: List<Button>,
    val onTap: NavigationEndpoint,
    val subtitleBadges: List<Badges>?,
) {
    @Serializable
    data class Header(
        val musicCardShelfHeaderBasicRenderer: MusicCardShelfHeaderBasicRenderer,
    ) {
        @Serializable
        data class MusicCardShelfHeaderBasicRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    )
}
