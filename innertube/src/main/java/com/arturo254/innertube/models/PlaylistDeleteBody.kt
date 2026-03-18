package com.arturo254.innertube.models.body

import com.arturo254.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
