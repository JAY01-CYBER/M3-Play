package com.arturo254.innertube.pages

import com.arturo254.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
