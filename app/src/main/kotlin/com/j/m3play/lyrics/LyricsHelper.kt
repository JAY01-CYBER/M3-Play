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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
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
            BinimumLyricsProvider,
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            BetterLyricsPortatoProvider,
            PaxsenixLyricsProvider,
            PaxsenixAppleMusicLyricsProvider,
            PaxsenixSpotifyLyricsProvider,
            PaxsenixMusixmatchLyricsProvider,
            MegalobizLyricsProvider,
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
            if (!networkConnectivity.isCurrentlyConnected()) {
                return@withContext LyricsResult("Offline", LYRICS_NOT_FOUND)
            }

            val ordered = orderedProviders().filter { it.isEnabled(context) }
            // Preferred-only must never silently switch to another provider just because
            // the user's chosen provider is disabled.
            val providers = if (preferredProviderOnly) {
                val preferred = orderedProviders().firstOrNull()
                ordered.filter { it === preferred }
            } else ordered
            if (providers.isEmpty()) return@withContext LyricsResult("Unknown", LYRICS_NOT_FOUND)

            val artist = mediaMetadata.artists.joinToString { it.name }
            val album = mediaMetadata.album?.title
            val key = cacheKey(
                mediaMetadata.id,
                mediaMetadata.title,
                artist,
                album,
                mediaMetadata.duration,
                providers,
                false,
            )
            cache.get(key)?.firstOrNull()?.let { return@withContext it }

            data class Candidate(
                val order: Int,
                val result: LyricsResult,
                val quality: LyricsQualityEvaluator.Assessment,
            )

            // Ask several sources at once, but cap concurrency to avoid hammering lyric APIs.
            val semaphore = kotlinx.coroutines.sync.Semaphore(PROVIDER_CONCURRENCY)
            val candidates = coroutineScope {
                providers.mapIndexed { index, provider ->
                    async {
                        semaphore.withPermit {
                            try {
                                val response = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                                    provider.getLyrics(
                                        mediaMetadata.id,
                                        mediaMetadata.title,
                                        artist,
                                        album,
                                        mediaMetadata.duration,
                                    )
                                } ?: return@withPermit null

                                response.exceptionOrNull()?.let { error ->
                                    if (error is kotlinx.coroutines.CancellationException) throw error
                                }
                                val lyrics = response.getOrNull()?.trim()
                                    ?: return@withPermit null
                                if (!isMeaningfulLyrics(lyrics)) return@withPermit null

                                val quality = LyricsQualityEvaluator.assess(lyrics, mediaMetadata.duration)
                                if (quality.score < MINIMUM_LYRICS_QUALITY_SCORE) return@withPermit null

                                Candidate(index, LyricsResult(provider.name, lyrics), quality)
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                reportException(error)
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            // Prefer actual word timestamps over line-only lyrics when quality is close.
            // A provider's textual match alone should not override richer timing.
            val best = candidates.maxWithOrNull(
                compareBy<Candidate> { it.quality.isWordSynced }
                    .thenBy { it.quality.score }
                    .thenBy { -it.order },
            )?.result ?: LyricsResult("Unknown", LYRICS_NOT_FOUND)

            if (best.providerName != "Unknown") cache.put(key, listOf(best))
            best
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
        if (providers.isEmpty()) return@withContext
        val key = cacheKey(mediaId, songTitle, songArtists, songAlbum, duration, providers, true)
        cache.get(key)?.let { cached ->
            cached.forEach(callback)
            return@withContext
        }

        val job = currentCoroutineContext()[Job]
        currentLyricsJob = job
        try {
            val semaphore = kotlinx.coroutines.sync.Semaphore(PROVIDER_CONCURRENCY)
            val providerResults = coroutineScope {
                providers.mapIndexed { index, provider ->
                    async {
                        semaphore.withPermit {
                            val local = mutableListOf<Pair<Int, LyricsResult>>()
                            try {
                                withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                                    provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) { lyrics ->
                                        if (isMeaningfulLyrics(lyrics)) {
                                            val trimmed = lyrics.trim()
                                            val quality = LyricsQualityEvaluator.assess(trimmed, duration)
                                            if (quality.score >= MINIMUM_LYRICS_QUALITY_SCORE) {
                                                synchronized(local) {
                                                    if (local.none { it.second.lyrics == trimmed }) {
                                                        local += index to LyricsResult(provider.name, trimmed)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                reportException(error)
                            }
                            synchronized(local) { local.toList() }
                        }
                    }
                }.awaitAll()
            }

            // De-duplicate identical lyrics across providers, retaining the highest-ranked source.
            data class RankedCandidate(
                val providerOrder: Int,
                val result: LyricsResult,
                val score: Int,
            )

            val unique = linkedMapOf<String, RankedCandidate>()
            providerResults.flatten().forEach { item ->
                val normalized = normalizeForDeduplication(item.second.lyrics)
                val candidate = RankedCandidate(
                    providerOrder = item.first,
                    result = item.second,
                    score = LyricsQualityEvaluator.assess(item.second.lyrics, duration).score,
                )
                val existing = unique[normalized]
                if (
                    existing == null ||
                    candidate.score > existing.score ||
                    (candidate.score == existing.score && candidate.providerOrder < existing.providerOrder)
                ) {
                    unique[normalized] = candidate
                }
            }

            val ranked = unique.values.sortedWith(
                compareByDescending<RankedCandidate> { it.score }.thenBy { it.providerOrder },
            ).map { it.result }

            if (ranked.isNotEmpty()) cache.put(key, ranked)
            ranked.forEach(callback)
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

    private fun normalizeForDeduplication(lyrics: String): String =
        lyrics.lowercase()
            .replace(INVISIBLE_CHARS_REGEX, "")
            .replace(TIMESTAMP_REGEX, "")
            .replace(Regex("""\\s+"""), " ")
            .trim()

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
        private const val PROVIDER_CONCURRENCY = 4
        private const val MINIMUM_LYRICS_QUALITY_SCORE = 8
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
