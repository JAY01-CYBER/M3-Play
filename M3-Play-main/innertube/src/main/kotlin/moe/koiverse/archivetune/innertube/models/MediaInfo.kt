/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models

data class MediaInfo(
    val videoId: String,
    val title: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val authorThumbnail: String? = null,
    val description: String? = null,
    val uploadDate: String? = null,
    val subscribers: String? = null,
    val viewCount: Int? = null,
    val like: Int? = null,
    val dislike: Int? = null,
)