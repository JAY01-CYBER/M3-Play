package com.arturo254.innertube.pages

import com.arturo254.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
