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

import com.my.kizzy.gateway.entities.op.OpCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Payload(
    @SerialName("t")
    val t: String? = null,
    @SerialName("s")
    val s: Int? = null,
    @SerialName("op")
    val op: OpCode? = null,
    @SerialName("d")
    val d: JsonElement? = null,
)