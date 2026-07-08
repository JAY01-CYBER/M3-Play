/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab>? = null
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer? = null
    ) {
        @Serializable
        data class TabRenderer(
            val title: String? = null,
            val selected: Boolean? = null,
            val content: Content? = null,
            val endpoint: NavigationEndpoint? = null
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer? = null,
                val musicQueueRenderer: MusicQueueRenderer? = null
            ) {
                @Serializable
                data class SectionListRenderer(
                    val contents: List<Content>? = null,
                    val continuations: List<Continuation>? = null,
                    val header: Header? = null
                ) {
                    @Serializable
                    data class Content(
                        val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
                        
                        
                        val musicShelfRenderer: MusicShelfRenderer? = null, 
                        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
                        val gridRenderer: GridRenderer? = null
                    )
                    
                    @Serializable
                    data class Header(
                        val chipCloudRenderer: ChipCloudRenderer? = null
                    )
                }
            }
        }
    }
}
