/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsSectionRenderer(
    val contents: List<Content>,
) {
    @Serializable
    data class Content(
        val searchSuggestionRenderer: SearchSuggestionRenderer?,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    ) {
        @Serializable
        data class SearchSuggestionRenderer(
            val suggestion: Runs,
            val navigationEndpoint: NavigationEndpoint,
        )
    }
}
