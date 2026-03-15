package com.j.m3play.models

import com.arturo254.innertube.models.YTItem
import com.j.m3play.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
