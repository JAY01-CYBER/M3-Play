/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    @SerialName("large_image")
    val largeImage: String?,
    @SerialName("small_image")
    val smallImage: String?,
    @SerialName("large_text")
    val largeText: String? = null,
    @SerialName("small_text")
    val smallText: String? = null,
)