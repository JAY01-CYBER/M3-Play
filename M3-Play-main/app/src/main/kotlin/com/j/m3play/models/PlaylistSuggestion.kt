/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.models

import com.j.m3play.innertube.models.YTItem

data class PlaylistSuggestion(
    val items: List<YTItem>,
    val continuation: String?,
    val currentQueryIndex: Int,
    val totalQueries: Int,
    val query: String,
    val hasMore: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlaylistSuggestionPage(
    val items: List<YTItem>,
    val continuation: String?
)

data class PlaylistSuggestionQuery(
    val query: String,
    val priority: Int
)