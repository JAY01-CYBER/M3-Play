/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package com.j.m3play.betterlyrics.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnisonEntry(
    val id: Long,
    @SerialName("videoId") val videoId: String? = null,
    val song: String,
    val artist: String,
    val lyrics: String,
    val format: String,
    val syncType: String,
    val score: Double = 0.0,
    val effectiveScore: Double = 0.0,
    val voteCount: Int = 0,
    val confidence: String = "low",
    val language: String? = null,
)

@Serializable
data class UnisonSearchEntry(
    val id: Long,
    @SerialName("videoId") val videoId: String? = null,
    val song: String,
    val artist: String,
    val lyrics: String? = null,
    val format: String,
    val syncType: String,
    val score: Double = 0.0,
    val effectiveScore: Double = 0.0,
    val voteCount: Int = 0,
    val confidence: String = "low",
    val language: String? = null,
) {
    fun toEntry(): UnisonEntry? {
        val nonBlankLyrics = lyrics?.takeIf { it.isNotBlank() } ?: return null
        return UnisonEntry(
            id = id,
            videoId = videoId,
            song = song,
            artist = artist,
            lyrics = nonBlankLyrics,
            format = format,
            syncType = syncType,
            score = score,
            effectiveScore = effectiveScore,
            voteCount = voteCount,
            confidence = confidence,
            language = language,
        )
    }
}

@Serializable
data class UnisonResponse(
    val success: Boolean,
    val data: UnisonEntry? = null,
)

@Serializable
data class UnisonSearchResponse(
    val success: Boolean,
    val data: List<UnisonSearchEntry>? = null,
)
