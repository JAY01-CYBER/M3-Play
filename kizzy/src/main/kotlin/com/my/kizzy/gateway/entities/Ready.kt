/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.my.kizzy.gateway.entities


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ready(
    @SerialName("resume_gateway_url")
    val resumeGatewayUrl: String? = null,
    @SerialName("session_id")
    val sessionId: String? = null,
)