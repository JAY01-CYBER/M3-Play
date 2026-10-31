/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * These adapters expose distinct Paxsenix catalogue endpoints as separate candidates
 * so the shared lyrics-quality evaluator can compare their results.
 */
package com.j.m3play.lyrics

import android.content.Context
import com.j.m3play.BuildConfig
import com.j.paxsenix.Paxsenix

internal abstract class PaxsenixVariantLyricsProvider(
    final override val name: String,
) : LyricsProvider {
    override fun isEnabled(context: Context): Boolean =
        BuildConfig.PAXSENIX_API_KEY.isNotBlank()

    protected abstract suspend fun fetch(
        title: String,
        artist: String,
        duration: Int,
    ): Result<String>

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = fetch(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        fetch(title, artist, duration).onSuccess { lyrics ->
            if (lyrics.isNotBlank()) callback(lyrics)
        }
    }
}

internal object PaxsenixAppleMusicLyricsProvider : PaxsenixVariantLyricsProvider("Paxsenix · Apple Music") {
    override suspend fun fetch(title: String, artist: String, duration: Int): Result<String> =
        Paxsenix.getAppleMusicLyrics(title, artist, duration)
}

internal object PaxsenixSpotifyLyricsProvider : PaxsenixVariantLyricsProvider("Paxsenix · Spotify") {
    override suspend fun fetch(title: String, artist: String, duration: Int): Result<String> =
        Paxsenix.getSpotifyLyrics(title, artist, duration)
}

internal object PaxsenixMusixmatchLyricsProvider : PaxsenixVariantLyricsProvider("Paxsenix · Musixmatch") {
    override suspend fun fetch(title: String, artist: String, duration: Int): Result<String> =
        Paxsenix.getMusixmatchLyrics(title, artist, duration)
}
