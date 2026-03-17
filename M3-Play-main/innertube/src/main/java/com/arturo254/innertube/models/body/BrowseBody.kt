package com.arturo254.innertube.models.body

import com.arturo254.innertube.models.Context
import com.arturo254.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
