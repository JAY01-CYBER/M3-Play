/*
 * M3 Play Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.innertube.models.body

import com.j.m3play.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistBody(
    val context: Context,
    val title: String,
    val privacyStatus: String = PrivacyStatus.PRIVATE,
    val videoIds: List<String>? = null
) {
    object PrivacyStatus {
        const val PRIVATE = "PRIVATE"
        const val PUBLIC = "PUBLIC"
        const val UNLISTED = "UNLISTED"
    }
}
