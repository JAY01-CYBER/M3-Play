/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package com.j.m3play.betterlyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import com.j.m3play.betterlyrics.models.UnisonEntry
import com.j.m3play.betterlyrics.models.UnisonResponse
import com.j.m3play.betterlyrics.models.UnisonSearchResponse

object Unison {
    private const val API_BASE_URL = "https://unison.boidu.dev/"
    private const val MAX_SEARCH_RESULTS = 5
    private const val MAX_LOG_BODY_LENGTH = 200

    private val jsonFormat by lazy {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonFormat)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 20000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 20000
            }

            defaultRequest {
                url(API_BASE_URL)
            }

            expectSuccess = false
        }
    }

    var logger: ((String) -> Unit)? = null

    private suspend fun fetchEntry(
        videoId: String?,
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
    ): UnisonEntry? {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()

        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return null

        val byMetadata = searchEntries(cleanTitle, cleanArtist, album?.trim(), durationSeconds).firstOrNull()
        if (byMetadata != null) return byMetadata

        val cleanVideoId = videoId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        logger?.invoke("No metadata match, fetching Unison lyrics by videoId: $cleanVideoId")
        return fetchByVideoId(cleanVideoId)
    }

    private suspend fun fetchByVideoId(videoId: String): UnisonEntry? {
        return try {
            val response =
                client.get("lyrics") {
                    parameter("v", videoId)
                }
            logger?.invoke("Unison videoId response status: ${response.status}")
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            logger?.invoke("Unison videoId raw response: ${body.take(MAX_LOG_BODY_LENGTH)}")
            val parsed = runCatching { jsonFormat.decodeFromString<UnisonResponse>(body) }.getOrNull()
            parsed?.takeIf { it.success }?.data?.takeIf { it.lyrics.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger?.invoke("Unison videoId fetch error: ${e.message}")
            null
        }
    }

    private suspend fun fetchById(id: Long): UnisonEntry? {
        return try {
            val response = client.get("lyrics/$id")
            logger?.invoke("Unison ID response status: ${response.status}")
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            logger?.invoke("Unison ID raw response: ${body.take(MAX_LOG_BODY_LENGTH)}")
            val parsed = runCatching { jsonFormat.decodeFromString<UnisonResponse>(body) }.getOrNull()
            parsed?.takeIf { it.success }?.data?.takeIf { it.lyrics.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger?.invoke("Unison ID fetch error: ${e.message}")
            null
        }
    }

    private suspend fun fetchVariants(videoId: String): List<UnisonEntry> {
        return try {
            val response = client.get("lyrics/variants/$videoId")
            logger?.invoke("Unison variants response status: ${response.status}")
            if (!response.status.isSuccess()) return emptyList()
            val body = response.bodyAsText()
            logger?.invoke("Unison variants raw response: ${body.take(MAX_LOG_BODY_LENGTH)}")
            val parsed = runCatching { jsonFormat.decodeFromString<UnisonSearchResponse>(body) }.getOrNull()
            parsed
                ?.takeIf { it.success }
                ?.data
                .orEmpty()
                .asSequence()
                .mapNotNull { it.toEntry() }
                .take(MAX_SEARCH_RESULTS)
                .toList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger?.invoke("Unison variants fetch error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchEntries(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
    ): List<UnisonEntry> {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return emptyList()

        logger?.invoke("Searching Unison lyrics: title=$cleanTitle, artist=$cleanArtist")
        return try {
            val response =
                client.get("lyrics/search") {
                    parameter("song", cleanTitle)
                    parameter("artist", cleanArtist)
                    if (!album.isNullOrBlank()) parameter("album", album.trim())
                    if (durationSeconds > 0) parameter("duration", durationSeconds)
                    parameter("limit", MAX_SEARCH_RESULTS)
                }
            logger?.invoke("Unison search response status: ${response.status}")
            if (!response.status.isSuccess()) return emptyList()
            val body = response.bodyAsText()
            logger?.invoke("Unison search raw response: ${body.take(MAX_LOG_BODY_LENGTH)}")
            val parsed = runCatching { jsonFormat.decodeFromString<UnisonSearchResponse>(body) }.getOrNull()
            val summaries = parsed?.takeIf { it.success }?.data.orEmpty()
            val entries = mutableListOf<UnisonEntry>()
            for (summary in summaries) {
                currentCoroutineContext().ensureActive()
                if (entries.size >= MAX_SEARCH_RESULTS) break
                val entry =
                    summary.toEntry()
                        ?: summary.videoId?.takeIf { it.isNotBlank() }?.let { fetchByVideoId(it) }
                        ?: fetchById(summary.id)
                if (entry != null) entries += entry
            }
            entries
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger?.invoke("Unison search fetch error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLyrics(
        videoId: String? = null,
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1,
    ): Result<String> =
        try {
            require(title.isNotBlank() && artist.isNotBlank()) { "Song title and artist are required" }
            val entry =
                fetchEntry(videoId, title, artist, album, durationSeconds)
                    ?: throw IllegalStateException("Lyrics unavailable")
            logger?.invoke("Unison got lyrics: format=${entry.format}, syncType=${entry.syncType}, length=${entry.lyrics.length}")
            Result.success(entry.lyrics)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun getAllLyrics(
        videoId: String? = null,
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1,
        callback: (String) -> Unit,
    ) {
        var results = searchEntries(title, artist, album, durationSeconds)
        if (results.isEmpty()) {
            val cleanVideoId = videoId?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanVideoId != null) {
                results = fetchVariants(cleanVideoId)
                if (results.isEmpty()) {
                    results = listOfNotNull(fetchByVideoId(cleanVideoId))
                }
            }
        }
        results.take(MAX_SEARCH_RESULTS).forEachIndexed { index, entry ->
            currentCoroutineContext().ensureActive()
            logger?.invoke("Unison search result ${index + 1}: format=${entry.format}, syncType=${entry.syncType}")
            callback(entry.lyrics)
        }
    }
}
