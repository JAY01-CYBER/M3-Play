/**
 * M3Play Project
 */
package com.j.m3play.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.j.m3play.constants.PreferredLyricsProvider
import com.j.m3play.constants.PreferredLyricsProviderKey
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.extensions.toEnum
import com.j.m3play.models.MediaMetadata
import com.j.m3play.utils.GlobalLog
import com.j.m3play.utils.NetworkConnectivityObserver
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class LyricsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders = listOf(
        SimpMusicLyricsProvider, BetterLyricsProvider, PaxsenixLyricsProvider,
        LrcLibLyricsProvider, KuGouLyricsProvider, YouTubeSubtitleLyricsProvider,
        YouTubeLyricsProvider, YouLyPlusLyricsProvider
    )

    private val cache = LruCache<String, List<LyricsResult>>(3)
    private var currentLyricsJob: Job? = null

    // Glossy's Fast Concurrent Provider Job
    private fun CoroutineScope.launchProviderJob(
        provider: LyricsProvider, index: Int, channel: Channel<Pair<Int, LyricsResult?>>, mediaMetadata: MediaMetadata
    ) = launch {
        try {
            if (!provider.isEnabled(context)) {
                channel.send(Pair(index, null))
                return@launch
            }
            val result = provider.getLyrics(mediaMetadata.id, mediaMetadata.title, mediaMetadata.artists.joinToString { it.name }, mediaMetadata.album?.title, mediaMetadata.duration)
            result.onSuccess { lyrics ->
                if (isMeaningfulLyrics(lyrics)) {
                    channel.send(Pair(index, LyricsResult(provider.name, lyrics)))
                } else channel.send(Pair(index, null))
            }.onFailure { channel.send(Pair(index, null)) }
        } catch (e: Exception) { channel.send(Pair(index, null)) }
    }

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): LyricsResult {
        currentLyricsJob?.cancel()
        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) return cached

        val isNetworkAvailable = try { networkConnectivity.isCurrentlyConnected() } catch (e: Exception) { true }
        if (!isNetworkAvailable) return LyricsResult("Offline", LYRICS_NOT_FOUND)

        val ordered = orderedProviders()
        val providers = if (preferredProviderOnly) listOf(ordered.first()) else ordered

        // Glossy Tier-based Racing Algorithm
        val result = withTimeoutOrNull(30000L) {
            val channel = Channel<Pair<Int, LyricsResult?>>(capacity = providers.size)
            val launchedJobs = mutableListOf<Job>()
            val TIER_SIZE = 2

            for (i in 0 until minOf(TIER_SIZE, providers.size)) {
                launchedJobs += launchProviderJob(providers[i], i, channel, mediaMetadata)
            }

            var nextTierIndex = TIER_SIZE
            var bestIndex = Int.MAX_VALUE
            var bestResult: LyricsResult? = null
            val remaining = (0 until providers.size).toMutableSet()

            val collectJob = launch {
                for ((index, res) in channel) {
                    remaining.remove(index)
                    if (res != null && index < bestIndex) { bestIndex = index; bestResult = res }
                    if (remaining.none { it < bestIndex }) { channel.cancel(); break }
                }
            }

            while (nextTierIndex < providers.size && collectJob.isActive) {
                delay(4000L)
                if (bestResult == null && collectJob.isActive) {
                    for (i in nextTierIndex until minOf(nextTierIndex + TIER_SIZE, providers.size)) {
                        launchedJobs += launchProviderJob(providers[i], i, channel, mediaMetadata)
                    }
                    nextTierIndex += TIER_SIZE
                } else break
            }
            collectJob.join()
            launchedJobs.forEach { it.cancel() }
            bestResult ?: LyricsResult("Unknown", LYRICS_NOT_FOUND)
        }
        return result ?: LyricsResult("Unknown", LYRICS_NOT_FOUND)
    }

    suspend fun getAllLyrics(mediaId: String, songTitle: String, songArtists: String, songAlbum: String?, duration: Int, callback: (LyricsResult) -> Unit) {
        currentLyricsJob?.cancel()
        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results -> results.forEach { callback(it) }; return }

        val isNetworkAvailable = try { networkConnectivity.isCurrentlyConnected() } catch (e: Exception) { true }
        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val callbackMutex = Mutex()
            val jobs = providers.map { provider ->
                launch {
                    if (provider.isEnabled(context)) {
                        try {
                            provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) { lyrics ->
                                if (isMeaningfulLyrics(lyrics)) {
                                    val result = LyricsResult(provider.name, lyrics)
                                    launch { callbackMutex.withLock { allResult += result; callback(result) } }
                                }
                            }
                        } catch (e: Exception) { reportException(e) }
                    }
                }
            }
            jobs.forEach { it.join() }
            cache.put(cacheKey, allResult)
        }
        currentLyricsJob?.join()
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val preferred = context.dataStore.data.first()[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
        val first = when (preferred) {
            PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
            PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
            PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
            PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
            PreferredLyricsProvider.YOULYPLUS -> YouLyPlusLyricsProvider
            PreferredLyricsProvider.PAXSENIX -> PaxsenixLyricsProvider
        }
        return listOf(first) + baseProviders.filterNot { it == first }
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized = lyrics.replace("\uFEFF", "").replace(INVISIBLE_CHARS_REGEX, "").trim { it.isWhitespace() || it == '\u00A0' }
        if (normalized.isEmpty() || normalized == LYRICS_NOT_FOUND) return false
        val remaining = TIMESTAMP_REGEX.replace(normalized, "").replace(INVISIBLE_CHARS_REGEX, "").trim { it.isWhitespace() || it == '\u00A0' }
        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() { currentLyricsJob?.cancel(); currentLyricsJob = null }

    companion object {
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(val providerName: String, val lyrics: String)
