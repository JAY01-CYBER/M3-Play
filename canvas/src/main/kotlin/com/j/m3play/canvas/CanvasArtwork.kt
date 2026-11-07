/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val albumName: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
