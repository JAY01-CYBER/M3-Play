/*
 * M3Play Project (2026)
 * Kòi Natsuko (github.com/JAY01-CYBER)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab>,
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer,
    ) {
        @Serializable
        data class TabRenderer(
            val title: String?,
            val content: Content?,
            val endpoint: NavigationEndpoint?,
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer?,
                val musicQueueRenderer: MusicQueueRenderer?,
            )
        }
    }
}
