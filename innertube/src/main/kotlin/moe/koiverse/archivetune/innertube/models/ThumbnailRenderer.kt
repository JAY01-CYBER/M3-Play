/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl(): String? {
            // Sabse badi width wali image find karein
            val highestResUrl = thumbnail.thumbnails.maxByOrNull { it.width ?: 0 }?.url
                ?: thumbnail.thumbnails.lastOrNull()?.url

            // Agar URL mein size defined hai (=w120-h120 type), toh usko 512x512 se replace kar dein
            return highestResUrl?.let { url ->
                if (url.contains(Regex("=w\\d+-h\\d+"))) {
                    url.replace(Regex("=w\\d+-h\\d+"), "=w512-h512")
                } else {
                    url
                }
            }
        }
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
