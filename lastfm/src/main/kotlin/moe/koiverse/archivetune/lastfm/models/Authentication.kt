/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class Authentication(
    val session: Session
) {
    @Serializable
    data class Session(
        val name: String,       // Username
        val key: String,        // Session Key
        val subscriber: Int,    // Last.fm Pro?
    )
}

@Serializable
data class TokenResponse(
    val token: String
)

@Serializable
data class LastFmError(
    val error: Int,
    val message: String
)
