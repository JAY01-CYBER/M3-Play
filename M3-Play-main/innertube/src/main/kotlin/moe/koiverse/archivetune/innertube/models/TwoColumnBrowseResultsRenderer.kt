/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class TwoColumnBrowseResultsRenderer(
    val secondaryContents: SecondaryContents?,
    val tabs: List<Tabs.Tab>?
) {
    @Serializable
    data class SecondaryContents(
        val sectionListRenderer: SectionListRenderer?
    )

    @Serializable
    data class SectionListRenderer(
        val contents: List<Content>?,
        val continuations: List<Continuation>?,
    ) {
        @Serializable
        data class Content(
            val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
            val musicShelfRenderer: MusicShelfRenderer?
        )
    }
}
