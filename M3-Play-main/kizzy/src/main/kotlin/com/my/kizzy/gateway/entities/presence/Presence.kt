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
data class Presence(
    @SerialName("activities")
    val activities: List<Activity?>?,
    @SerialName("afk")
    val afk: Boolean? = false,
    @SerialName("since")
    val since: Long? = null,
    @SerialName("status")
    val status: String? = "online",
)