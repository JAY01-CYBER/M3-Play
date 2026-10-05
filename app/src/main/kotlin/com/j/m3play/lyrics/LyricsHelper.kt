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
import android.util.LruCache
import com.j.m3play.constants.PreferredLyricsProvider
import com.j.m3play.constants.PreferredLyricsProviderKey
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.extensions.toEnum
import com.j.m3play.models.MediaMetadata
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.reportException
import com.j.m3play.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
            UnisonLyricsProvider,
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            PaxsenixLyricsProvider, 
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
            YouLyPlusLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): LyricsResult =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (!networkConnectivity.isCurrentlyConnected()) return@withContext LyricsResult("Offline", LYRICS_NOT_FOUND)
            val ordered = orderedProviders()
            val providers = (if (preferredProviderOnly) ordered.take(1) else ordered)
                .filter { it.isEnabled(context) }
            val key = cacheKey(mediaMetadata.id, mediaMetadata.title, mediaMetadata.artists.joinToString { it.name }, mediaMetadata.album?.title, mediaMetadata.duration, providers, false)
            cache.get(key)?.firstOrNull()?.let { return@withContext it }
            for (provider in providers) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                try {
                    val result = kotlinx.coroutines.withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(mediaMetadata.id, mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name }, mediaMetadata.album?.title, mediaMetadata.duration)
                    }
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val error = result?.exceptionOrNull()
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    val lyrics = result?.getOrNull()
                    if (lyrics != null && isMeaningfulLyrics(lyrics)) {
                        val found = LyricsResult(provider.name, lyrics)
                        cache.put(key, listOf(found))
                        return@withContext found
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    reportException(error)
                }
            }
            LyricsResult("Unknown", LYRICS_NOT_FOUND)
        }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (!networkConnectivity.isCurrentlyConnected()) return@withContext
        val providers = orderedProviders().filter { it.isEnabled(context) }
        val key = cacheKey(mediaId, songTitle, songArtists, songAlbum, duration, providers, true)
        cache.get(key)?.let { results ->
            results.forEach(callback)
            return@withContext
        }
        val job = kotlinx.coroutines.currentCoroutineContext()[Job]
        currentLyricsJob = job
        val results = mutableListOf<LyricsResult>()
        try {
            for (provider in providers) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                try {
                    kotlinx.coroutines.withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) { lyrics ->
                            if (isMeaningfulLyrics(lyrics) && results.none { it.lyrics == lyrics }) {
                                val result = LyricsResult(provider.name, lyrics)
                                results += result
                                callback(result)
                            }
                        }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    reportException(error)
                }
            }
            if (results.isNotEmpty()) cache.put(key, results.toList())
        } finally {
            if (currentLyricsJob === job) currentLyricsJob = null
        }
    }

    private fun cacheKey(id: String, title: String, artist: String, album: String?, duration: Int,
                         providers: List<LyricsProvider>, all: Boolean): String =
        listOf(id, title, artist, album.orEmpty(), duration.toString(), providers.joinToString { it.name }, all.toString())
            .joinToString("\u0000")

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val preferred =
            context.dataStore.data
                .first()[PreferredLyricsProviderKey]
                .toEnum(PreferredLyricsProvider.LRCLIB)

        val first =
            when (preferred) {
                PreferredLyricsProvider.UNISON -> UnisonLyricsProvider
                PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
                PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
                PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
                PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
                PreferredLyricsProvider.YOULYPLUS -> YouLyPlusLyricsProvider
                PreferredLyricsProvider.PAXSENIX -> PaxsenixLyricsProvider
            }

        return listOf(first) + baseProviders.filterNot { provider -> provider == first }
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 16
        private const val PROVIDER_TIMEOUT_MS = 12_000L
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
