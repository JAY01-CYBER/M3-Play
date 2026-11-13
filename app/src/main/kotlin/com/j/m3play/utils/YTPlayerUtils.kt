/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.j.m3play.constants.AudioQuality
import com.j.m3play.constants.PlayerStreamClient
import com.j.m3play.innertube.NewPipeUtils
import com.j.m3play.innertube.YouTube
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.innertube.models.YouTubeClient
import com.j.m3play.innertube.models.YouTubeClient.Companion.IOS
import com.j.m3play.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.j.m3play.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.j.m3play.innertube.models.response.PlayerResponse
import com.zemer.cipher.CipherDeobfuscator
import com.zemer.cipher.potoken.PoTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_TESTSUITE
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_UNPLUGGED
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.j.m3play.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.j.m3play.innertube.models.YouTubeClient.Companion.IPADOS
import com.j.m3play.innertube.models.YouTubeClient.Companion.IOS_MUSIC
import com.j.m3play.innertube.models.YouTubeClient.Companion.MOBILE
import com.j.m3play.innertube.models.YouTubeClient.Companion.TVHTML5
import com.j.m3play.innertube.models.YouTubeClient.Companion.VISIONOS
import com.j.m3play.innertube.models.YouTubeClient.Companion.WEB
import com.j.m3play.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val FAILED_CLIENT_BACKOFF_MS = 10 * 60 * 1000L

    class LoginRequiredForPlaybackException(
        val videoId: String,
        val targetUrl: String,
        reason: String?,
    ) : IllegalStateException(reason)

    private data class PlaybackGateFailure(
        val clientName: String,
        val status: String,
        val reason: String?,
    )

    @Volatile private var streamClientPair: Pair<java.net.Proxy?, OkHttpClient>? = null

    private fun currentStreamClient(): OkHttpClient {
        val current = YouTube.streamProxy
        streamClientPair?.let { (proxy, client) ->
            if (proxy == current) return client
        }
        val client = OkHttpClient.Builder()
            .proxy(current)
            .build()
        streamClientPair = current to client
        return client
    }
    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.j.m3play.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IOS,
        MOBILE,
        ANDROID_MUSIC,
        IOS_MUSIC,
        ANDROID_VR_NO_AUTH,
        ANDROID_VR_1_61_48,
        ANDROID_VR_1_43_32,
        ANDROID_CREATOR,
        ANDROID_TESTSUITE,
        ANDROID_UNPLUGGED,
        IPADOS,
        VISIONOS,
        TVHTML5,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        WEB,
        WEB_CREATOR,
        WEB_REMIX
    )
    private data class CachedStreamUrl(
        val url: String,
        val expiresAtMs: Long,
    )

    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private val failedStreamClientsUntil = ConcurrentHashMap<String, Long>()

    fun invalidateCachedStreamUrls(videoId: String) {
        val prefix = "$videoId:"
        streamUrlCache.keys.removeIf { it.startsWith(prefix) }
    }

    fun markStreamClientFailed(videoId: String, clientKey: String?, httpStatusCode: Int?) {
        if (httpStatusCode != 403) return
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return
        failedStreamClientsUntil[buildFailedClientKey(videoId, normalizedClientKey)] =
            System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
    }

    fun markPreferredClientFailed(videoId: String, client: PlayerStreamClient, httpStatusCode: Int?) {
        markStreamClientFailed(videoId, client.name, httpStatusCode)
    }

    private fun isStreamClientTemporarilyBlocked(videoId: String, clientKey: String?): Boolean {
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return false
        val key = buildFailedClientKey(videoId, normalizedClientKey)
        val until = failedStreamClientsUntil[key] ?: return false
        if (until <= System.currentTimeMillis()) {
            failedStreamClientsUntil.remove(key)
            return false
        }
        return true
    }

    private fun normalizeStreamClientKey(clientKey: String?): String {
        return clientKey?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.US).orEmpty()
    }

    private fun buildFailedClientKey(videoId: String, clientKey: String): String {
        return "$videoId:${normalizeStreamClientKey(clientKey)}"
    }

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient = PlayerStreamClient.ANDROID_VR,
        // if provided, this preference overrides ConnectivityManager.isActiveNetworkMetered
        networkMetered: Boolean? = null,
        avoidCodecs: Set<String> = emptySet(),
    ): Result<PlaybackData> = runCatching {
        val attempts =
            when (audioQuality) {
                AudioQuality.HIGHEST -> listOf(AudioQuality.HIGHEST, AudioQuality.HIGH)
                AudioQuality.AUTO -> listOf(AudioQuality.AUTO, AudioQuality.HIGH)
                else -> listOf(audioQuality)
            }.distinct()

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val attemptResult =
                runCatching {
                    playerResponseForPlaybackOnce(
                        videoId = videoId,
                        playlistId = playlistId,
                        audioQuality = attempt,
                        connectivityManager = connectivityManager,
                        preferredStreamClient = preferredStreamClient,
                        networkMetered = networkMetered,
                        avoidCodecs = avoidCodecs,
                    )
                }
            if (attemptResult.isSuccess) return@runCatching attemptResult.getOrThrow()
            lastError = attemptResult.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("Failed to resolve stream")
    }

    private suspend fun playerResponseForPlaybackOnce(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient,
        networkMetered: Boolean?,
        avoidCodecs: Set<String>,
    ): PlaybackData {
        Timber.tag(logTag).i("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).v("Signature timestamp: $signatureTimestamp")

        // A non-null cookie string is not sufficient: only a cookie jar containing
        // SAPISID can produce the SAPISIDHASH header used by login-capable clients.
        val isLoggedIn = YouTube.cookie
            ?.let(::parseCookieString)
            ?.get("SAPISID")
            ?.isNotBlank() == true
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        Timber.tag(logTag).i(
            "YouTube auth state: ${if (isLoggedIn) "SAPISID available" else "anonymous"}; " +
                "dataSyncIdAvailable=${!YouTube.dataSyncId.isNullOrBlank()}, visitorDataAvailable=${!YouTube.visitorData.isNullOrBlank()}"
        )

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var streamClientUsed: YouTubeClient? = null

        val orderedFallbackClients =
            (
                if (isLoggedIn) {
                    STREAM_FALLBACK_CLIENTS.filter { it.loginSupported } + STREAM_FALLBACK_CLIENTS.filterNot { it.loginSupported }
                } else {
                    STREAM_FALLBACK_CLIENTS.toList()
                }
                ).distinct()

        val preferredYouTubeClient =
            when (preferredStreamClient) {
                // ANDROID_VR_NO_AUTH cannot use the signed-in account session. When
                // SAPISID is available, prefer WEB_REMIX just as Glossy does for
                // authenticated playback; keep VR as the anonymous fallback.
                PlayerStreamClient.ANDROID_VR -> if (isLoggedIn) WEB_REMIX else ANDROID_VR_NO_AUTH
                PlayerStreamClient.WEB_REMIX -> WEB_REMIX
                PlayerStreamClient.IOS -> IOS
                PlayerStreamClient.TVHTML5 -> TVHTML5
                PlayerStreamClient.ANDROID_MUSIC -> ANDROID_MUSIC
            }

        val metadataClient =
            if (isLoggedIn) MAIN_CLIENT
            else preferredYouTubeClient.takeIf { preferredStreamClient == PlayerStreamClient.ANDROID_VR } ?: MAIN_CLIENT

        // Generate a video-bound Web PoToken at most once for this playback resolution.
        // If BotGuard/WebView is unavailable, keep the existing NewPipe/client fallback path.
        var zemerStsAttempted = false
        var zemerSignatureTimestamp: Int? = null
        suspend fun getZemerSignatureTimestamp(): Int? {
            if (!zemerStsAttempted) {
                zemerStsAttempted = true
                zemerSignatureTimestamp = ZemerPoTokenProvider.signatureTimestamp()
                Timber.tag(logTag).i(
                    if (zemerSignatureTimestamp != null)
                        "Using Zemer player signature timestamp across player clients: $zemerSignatureTimestamp"
                    else
                        "Zemer player signature timestamp unavailable; using existing STS fallback"
                )
            }
            return zemerSignatureTimestamp
        }

        var zemerTokenAttempted = false
        var zemerTokens: PoTokenResult? = null
        suspend fun getZemerTokensForWebClient(): PoTokenResult? {
            if (!zemerTokenAttempted) {
                zemerTokenAttempted = true
                val visitor = YouTube.visitorData
                    ?: YouTube.visitorData().getOrNull()?.also { YouTube.visitorData = it }
                if (!visitor.isNullOrBlank()) {
                    zemerTokens = ZemerPoTokenProvider.generate(videoId, visitor)
                }
            }
            return zemerTokens
        }

        Timber.tag(logTag).i("Fetching metadata response using client: ${metadataClient.clientName}")
        val metadataPoToken = if (requiresWebPoToken(metadataClient)) {
            getZemerTokensForWebClient()?.playerRequestPoToken
        } else null
        // Zemer's cipher engine uses this exact player.js. Keep every client player
        // request on the same STS so any cipher returned by that response matches it.
        val metadataSts = getZemerSignatureTimestamp() ?: signatureTimestamp
        val metadataPlayerResponse =
            YouTube.player(videoId, playlistId, metadataClient, metadataSts, metadataPoToken).getOrThrow()
        val audioConfig = metadataPlayerResponse.playerConfig?.audioConfig
        val videoDetails = metadataPlayerResponse.videoDetails
        val playbackTracking = metadataPlayerResponse.playbackTracking
        val expectedDurationMs = videoDetails?.lengthSeconds?.toLongOrNull()?.takeIf { it > 0 }?.times(1000L)

        val streamClients =
            buildList {
                add(preferredYouTubeClient)
                addAll(orderedFallbackClients)
                if (preferredYouTubeClient != MAIN_CLIENT) add(MAIN_CLIENT)
            }.distinct().filterNot { client ->
                val blocked = isStreamClientTemporarilyBlocked(videoId, client.clientName)
                if (blocked) {
                    Timber.tag(logTag).w("Temporarily blocked stream client for $videoId: ${client.clientName}")
                }
                blocked
            }

        val botDetectedClients = mutableSetOf<String>()
        var gateFailure: PlaybackGateFailure? = null

        for ((index, client) in streamClients.withIndex()) {
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            streamPlayerResponse = null
            streamClientUsed = null

            Timber.tag(logTag).v(
                "Trying ${if (client == MAIN_CLIENT) "MAIN_CLIENT" else "fallback client"} ${index + 1}/${streamClients.size}: ${client.clientName}"
            )

            if (client != MAIN_CLIENT && client.loginRequired && !isLoggedIn) {
                Timber.tag(logTag).w("Skipping client ${client.clientName} - requires login but user is not logged in")
                continue
            }

            streamPlayerResponse =
                if (client == metadataClient) {
                    metadataPlayerResponse
                } else {
                    Timber.tag(logTag).i("Fetching player response for fallback client: ${client.clientName}")
                    val clientPoToken = if (requiresWebPoToken(client)) {
                        getZemerTokensForWebClient()?.playerRequestPoToken
                    } else null
                    val clientSts = getZemerSignatureTimestamp() ?: signatureTimestamp
                    YouTube.player(videoId, playlistId, client, clientSts, clientPoToken).getOrNull()
                }

            if (streamPlayerResponse == null) continue

            if (streamPlayerResponse.playabilityStatus.status != "OK") {
                val reason = streamPlayerResponse.playabilityStatus.reason.orEmpty()
                val isLoginRecovery = isLoginRecoveryError(reason)
                val isBotDetection = isBotDetectionError(reason)
                Timber.tag(logTag).w(
                    "Player response status not OK: ${streamPlayerResponse.playabilityStatus.status}, reason: $reason, loginRecovery: $isLoginRecovery, botDetection: $isBotDetection"
                )
                if (isLoginRecovery) {
                    gateFailure = PlaybackGateFailure(
                        clientName = client.clientName,
                        status = streamPlayerResponse.playabilityStatus.status,
                        reason = streamPlayerResponse.playabilityStatus.reason,
                    )
                } else if (isBotDetection) {
                    botDetectedClients.add(client.clientName)
                }
                continue
            }

            val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
            val candidates =
                selectAudioFormatCandidates(
                    streamPlayerResponse,
                    audioQuality,
                    isMetered,
                    avoidCodecs = avoidCodecs,
                )

            if (candidates.isEmpty()) continue

            var selectedFormat: PlayerResponse.StreamingData.Format? = null
            var selectedUrl: String? = null

            for (candidate in candidates.asSequence().take(6)) {
                if (isLoggedIn && expectedDurationMs != null && isLikelyPreview(candidate, expectedDurationMs)) continue
                val cacheKey = buildCacheKey(videoId, candidate.itag, client.clientName)
                val cached = streamUrlCache[cacheKey]
                val baseCandidateUrl =
                    if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
                        cached.url
                    } else {
                        findUrlOrNull(candidate, videoId, client)
                    } ?: continue
                val candidateUrl = if (requiresWebPoToken(client)) {
                    getZemerTokensForWebClient()?.streamingDataPoToken?.let { token ->
                        appendPoToken(baseCandidateUrl, token)
                    } ?: baseCandidateUrl
                } else baseCandidateUrl
                selectedFormat = candidate
                selectedUrl = candidateUrl
                break
            }

            if (selectedFormat == null || selectedUrl == null) continue

            format = selectedFormat
            streamUrl = selectedUrl
            streamClientUsed = client
            streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds

            if (streamExpiresInSeconds == null) continue

            Timber.tag(logTag).i("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")
            Timber.tag(logTag).v("Stream expires in: $streamExpiresInSeconds seconds")

            val valid = validateStatus(streamUrl, client.userAgent)
            if (valid) {
                Timber.tag(logTag).i("Stream validated successfully with client: ${client.clientName}")
                break
            }

            Timber.tag(logTag).w("Stream validation failed with client: ${client.clientName}, trying next fallback")
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            streamPlayerResponse = null
        }

        if (streamPlayerResponse == null) {
            gateFailure?.let { failure ->
                Timber.tag(logTag).w(
                    "Playback requires login recovery for $videoId via ${failure.clientName} (${failure.status}): ${failure.reason.orEmpty()}"
                )
                throw LoginRequiredForPlaybackException(
                    videoId = videoId,
                    targetUrl = "https://music.youtube.com/watch?v=$videoId",
                    reason = failure.reason,
                )
            }
            if (botDetectedClients.isNotEmpty()) {
                Timber.tag(logTag).e("Bot detection triggered on clients: $botDetectedClients - all clients failed")
                throw PlaybackException(
                    "Sign in to confirm you're not a bot",
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            if (isLoginRecoveryError(errorReason.orEmpty())) {
                Timber.tag(logTag).w("Playback requires login recovery for $videoId: $errorReason")
                throw LoginRequiredForPlaybackException(
                    videoId = videoId,
                    targetUrl = "https://music.youtube.com/watch?v=$videoId",
                    reason = errorReason,
                )
            }
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find suitable format for quality: $audioQuality. Available formats from last client: ${streamPlayerResponse.streamingData?.adaptiveFormats?.filter { it.isAudio }?.map { "${it.mimeType} @ ${it.bitrate}bps (itag: ${it.itag})" }}")
            throw Exception("Could not find format for quality: $audioQuality")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url for format: ${format.mimeType}, itag: ${format.itag}")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).i("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")

        streamUrlCache[buildCacheKey(videoId, format.itag, streamClientUsed?.clientName ?: "unknown")] =
            CachedStreamUrl(
                url = removePoToken(streamUrl),
                expiresAtMs = System.currentTimeMillis() + (streamExpiresInSeconds * 1000L),
            )

        return PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).i("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")

        // Keep metadata-only requests aligned with the same player.js generation and
        // web-client integrity context used by normal playback requests. Previously
        // this helper omitted both Zemer STS and the video-bound player PoToken.
        val sts = ZemerPoTokenProvider.signatureTimestamp()
            ?: getSignatureTimestampOrNull(videoId)
        val visitor = YouTube.visitorData
            ?: YouTube.visitorData().getOrNull()?.also { YouTube.visitorData = it }
        val tokens = if (requiresWebPoToken(MAIN_CLIENT) && !visitor.isNullOrBlank()) {
            ZemerPoTokenProvider.generate(videoId, visitor)
        } else null

        Timber.tag(logTag).d(
            "Metadata request context: client=${MAIN_CLIENT.clientName}, stsAvailable=${sts != null}, " +
                "visitorAvailable=${!visitor.isNullOrBlank()}, playerPoTokenAvailable=${!tokens?.playerRequestPoToken.isNullOrBlank()}"
        )

        return YouTube.player(
            videoId = videoId,
            playlistId = playlistId,
            client = MAIN_CLIENT,
            signatureTimestamp = sts,
            poToken = tokens?.playerRequestPoToken,
        ).mapCatching { response ->
            val status = response.playabilityStatus.status
            if (status != "OK") {
                val reason = response.playabilityStatus.reason.orEmpty()
                Timber.tag(logTag).w(
                    "Metadata player response rejected: client=${MAIN_CLIENT.clientName}, status=$status, " +
                        "botDetection=${isBotDetectionError(reason)}, reason=$reason"
                )
                throw IllegalStateException("Metadata player response rejected ($status): $reason")
            }
            Timber.tag(logTag).d(
                "Successfully fetched metadata: videoDetails=${response.videoDetails != null}, " +
                    "playbackTracking=${response.playbackTracking != null}"
            )
            response
        }.onFailure { error ->
            Timber.tag(logTag).e(error, "Failed to fetch metadata for videoId=$videoId using ${MAIN_CLIENT.clientName}")
        }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        // optional override from user preference; if non-null, use this instead of ConnectivityManager
        networkMetered: Boolean? = null,
        avoidCodecs: Set<String> = emptySet(),
    ): PlayerResponse.StreamingData.Format? {
        val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
        return selectAudioFormatCandidates(
            playerResponse,
            audioQuality,
            isMetered,
            avoidCodecs = avoidCodecs,
        ).firstOrNull()
    }

    private fun selectAudioFormatCandidates(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        networkMetered: Boolean,
        avoidCodecs: Set<String> = emptySet(),
    ): List<PlayerResponse.StreamingData.Format> {
        Timber.tag(logTag).i("Finding format with audioQuality: $audioQuality, network metered: $networkMetered")

        val audioFormats =
            playerResponse.streamingData?.adaptiveFormats
                ?.asSequence()
                ?.filter { it.isAudio && it.bitrate > 0 }
                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                ?.filter { format ->
                    val codec = extractCodec(format.mimeType)?.lowercase()
                    codec == null || codec !in avoidCodecs
                }
                ?.toList()
                .orEmpty()

        if (audioFormats.isEmpty()) return emptyList()

        val effectiveQuality =
            when (audioQuality) {
                AudioQuality.AUTO -> if (networkMetered) AudioQuality.HIGH else AudioQuality.HIGHEST
                else -> audioQuality
            }

        val targetBitrateBps =
            when (effectiveQuality) {
                AudioQuality.LOW -> 70_000
                AudioQuality.HIGH -> 160_000
                AudioQuality.HIGHEST -> 320_000
                AudioQuality.AUTO -> null
            }

        val preferHigher =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenByDescending { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val preferLowerAboveTarget =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenBy { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val candidates =
            if (targetBitrateBps == null) {
                audioFormats.sortedWith(preferHigher)
            } else {
                val belowOrEqual = audioFormats.filter { it.bitrate <= targetBitrateBps }
                if (belowOrEqual.isNotEmpty()) {
                    belowOrEqual.sortedWith(preferHigher)
                } else {
                    val aboveOrEqual = audioFormats.filter { it.bitrate >= targetBitrateBps }
                    if (aboveOrEqual.isNotEmpty()) aboveOrEqual.sortedWith(preferLowerAboveTarget)
                    else audioFormats.sortedWith(preferHigher)
                }
            }

        Timber.tag(logTag)
            .v(
                "Available audio formats: ${
                    candidates.take(12).map {
                        val codec = extractCodec(it.mimeType)
                        val direct = if (it.url != null) "direct" else "cipher"
                        "${it.mimeType} ($direct, codec=${codec ?: "unknown"}) @ ${it.bitrate}bps"
                    }
                }"
            )

        return candidates
    }

    private fun extractCodec(mimeType: String): String? {
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues.getOrNull(1)?.split(",")?.firstOrNull()?.trim()
    }

    private fun isCipheredFormat(format: PlayerResponse.StreamingData.Format): Boolean {
        return format.url == null && (format.signatureCipher != null || format.cipher != null)
    }

    private fun codecRank(codec: String?): Int =
        when {
            codec.isNullOrBlank() -> 0
            codec.contains("opus", ignoreCase = true) -> 3
            codec.contains("mp4a", ignoreCase = true) -> 2
            else -> 1
        }
    private fun isLikelyPreview(
        format: PlayerResponse.StreamingData.Format,
        expectedDurationMs: Long,
    ): Boolean {
        val approx = format.approxDurationMs?.toLongOrNull() ?: return false
        if (expectedDurationMs < 90_000L) return false
        return approx in 1L..(minOf(90_000L, (expectedDurationMs * 9L) / 10L))
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String, userAgent: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val httpUrl = url.toHttpUrlOrNull()
            val clientParam = httpUrl?.queryParameter("c")?.trim().orEmpty()

            val resolvedUserAgent = StreamClientUtils.resolveUserAgent(clientParam).ifEmpty { userAgent }
            val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

            val probeRanges =
                if (StreamClientUtils.isWebClient(clientParam)) {
                    listOf("bytes=0-0", "bytes=262144-262145", "bytes=1048576-1048577")
                } else {
                    listOf("bytes=0-0")
                }

            for (range in probeRanges) {
                val rangeRequest =
                    okhttp3.Request.Builder()
                        .get()
                        .header("User-Agent", resolvedUserAgent)
                        .header("Range", range)
                        .apply {
                            originReferer.origin?.let { header("Origin", it) }
                            originReferer.referer?.let { header("Referer", it) }
                        }.url(url)
                        .build()

                val code = currentStreamClient().newCall(rangeRequest).execute().use { response -> response.code }
                if (code == 403) return false
                if (code !in 200..399 && code != 416) return false
            }

            return true
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).i("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).i("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }
    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions.
     * Also patches cver to match the client version.
     */
    private suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")

        // Direct URLs need no signature decipher, but still pass through the n-parameter
        // transformer just like Glossy's playback path.
        var url = format.url?.takeIf { it.isNotBlank() }

        // Match Glossy's order: use Zemer for signatureCipher first, then NewPipe as a
        // compatibility fallback. This avoids NewPipe's parser masking a working Zemer path.
        if (url == null && client != null) {
            val cipher = format.signatureCipher ?: format.cipher
            if (!cipher.isNullOrBlank()) {
                Timber.tag(logTag).i("Trying Zemer signature decipher for ${client.clientName}")
                url = try {
                    withContext(Dispatchers.IO) {
                        CipherDeobfuscator.deobfuscateStreamUrl(cipher, videoId)
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.tag(logTag).w(error, "Zemer cipher failed for ${client.clientName}")
                    null
                }
            }
        }

        // NewPipe remains a fallback for formats/configurations Zemer cannot yet parse.
        if (url == null) {
            url = NewPipeUtils.getStreamUrl(format, videoId, client)
                .onSuccess { Timber.tag(logTag).d("NewPipe URL fallback succeeded") }
                .onFailure { Timber.tag(logTag).w(it, "NewPipe URL fallback failed") }
                .getOrNull()
        }

        val resolvedUrl = url ?: return null

        // Apply n-transform consistently, regardless of whether the URL was direct,
        // produced by Zemer, or produced by NewPipe.
        var finalUrl = try {
            withContext(Dispatchers.IO) { CipherDeobfuscator.transformNParamInUrl(resolvedUrl) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.tag(logTag).w(error, "Zemer n-parameter transform failed; keeping resolved URL")
            resolvedUrl
        }

        // Keep the URL's client version aligned with the client that produced the response.
        if (client != null) {
            finalUrl = StreamClientUtils.patchClientVersion(finalUrl, client.clientVersion)
        }
        return finalUrl
    }

    /**
     * YouTube's InnerTube layer requires Service Integrity PoTokens for these
     * web/TV clients. Android and iOS clients must not receive Web PoTokens.
     */
    private fun requiresWebPoToken(client: YouTubeClient): Boolean =
        client.clientName.uppercase(Locale.US) in setOf(
            "WEB",
            "WEB_REMIX",
            "WEB_CREATOR",
            "MWEB",
            "WEB_EMBEDDED_PLAYER",
            "TVHTML5",
            "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            "TVHTML5_SIMPLY",
        )

    private fun appendPoToken(url: String, token: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        return parsed.newBuilder()
            .setQueryParameter("pot", token)
            .build()
            .toString()
    }

    private fun removePoToken(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        return parsed.newBuilder()
            .removeAllQueryParameters("pot")
            .build()
            .toString()
    }

    private fun buildCacheKey(videoId: String, itag: Int, clientName: String): String {
        // Stream URLs are client-bound (cver, PoToken and authorization may differ).
        // Never reuse a URL obtained from one client for another client.
        return "$videoId:$clientName:$itag"
    }

    private fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase(Locale.US)
        return "bot" in lower ||
            "unusual traffic" in lower ||
            "automated" in lower ||
            "confirm" in lower && "not a" in lower ||
            "not a robot" in lower ||
            "verify" in lower && "human" in lower
    }

    private fun isLoginRecoveryError(reason: String): Boolean {
        val lower = reason.lowercase(Locale.US)
        return "confirm your age" in lower ||
            "age-restricted" in lower ||
            "age restricted" in lower ||
            "inappropriate for some users" in lower ||
            "mature audiences" in lower ||
            "adult" in lower && "sign in" in lower ||
            "allow" in lower && "youtube music" in lower
    }

    fun isBotDetectionException(error: PlaybackException): Boolean {
        val message = error.message.orEmpty()
        if (isBotDetectionError(message)) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }
}
