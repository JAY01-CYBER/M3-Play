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

package com.j.m3play.lyrics

import android.content.Context
import com.j.paxsenix.Paxsenix
import com.j.m3play.BuildConfig
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    private const val TAG = "PaxsenixProvider"
    
    init {
        // Configure the provider before its first request.
        Paxsenix.setApiKey(BuildConfig.PAXSENIX_API_KEY)
        Paxsenix.setUserAgent("M3Play", BuildConfig.VERSION_NAME)
    }

    override val name = "Paxsenix"

    // Hamesha enabled rakha hai taaki extra settings key na banani pade
    override fun isEnabled(context: Context): Boolean = BuildConfig.PAXSENIX_API_KEY.isNotBlank()

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> {
        Timber.tag(TAG).d("getLyrics called: title='$title', artist='$artist', duration=$duration")
        
        try {
            val result = Paxsenix.getLyrics(title, artist, duration)
            
            result.onSuccess { lyrics ->
                Timber.tag(TAG).i("Success! Got ${lyrics.length} chars of lyrics")
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to get lyrics")
            }
            
            return result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception in getLyrics")
            return Result.failure(e)
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        Timber.tag(TAG).d("getAllLyrics called")
        try {
            Paxsenix.getAllLyrics(title, artist, duration, callback)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching lyrics from Paxsenix")
            callback("")
        }
    }
}
