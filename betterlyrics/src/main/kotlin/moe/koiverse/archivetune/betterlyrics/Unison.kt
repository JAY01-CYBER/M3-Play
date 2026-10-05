/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.betterlyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Public read API: https://docs.betterlyrics.org/unison. No submission credentials required. */
object Unison {
    private val client by lazy {
        UnisonClient(HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 4_000
                socketTimeoutMillis = 8_000
            }
        })
    }

    suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> =
        try {
            Result.success(client.getLyrics(id, title, artist, album, duration))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
}

internal class UnisonClient(
    private val client: HttpClient,
    private val baseUrl: String = "https://unison.boidu.dev",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): String {
        if (id.matches(Regex("[A-Za-z0-9_-]{11}"))) {
            val response = client.get("$baseUrl/lyrics") { parameter("v", id) }
            if (response.status.isSuccess()) return decode(response.bodyAsText())
            // A missing video may still be indexed under its song metadata. Do not retry rate limits.
            check(response.status == HttpStatusCode.NotFound) { "Unison: HTTP ${response.status.value}" }
        }
        require(title.isNotBlank() && artist.isNotBlank()) { "Song metadata is missing" }
        val response = client.get("$baseUrl/lyrics") {
            parameter("song", title)
            parameter("artist", artist)
            album?.takeIf { it.isNotBlank() }?.let { parameter("album", it) }
            if (duration > 0) parameter("duration", duration)
        }
        check(response.status.isSuccess()) { "Unison: HTTP ${response.status.value}" }
        return decode(response.bodyAsText())
    }

    internal fun decode(body: String): String {
        val response = json.decodeFromString<Envelope>(body)
        val data = response.data
        check(response.success && data != null) { "No Unison lyrics found" }
        check(data.format in setOf("ttml", "lrc", "plain") && data.lyrics.isNotBlank()) { "Unsupported or empty Unison lyrics" }
        return data.lyrics
    }

    @Serializable
    private data class Envelope(val success: Boolean = false, val data: Entry? = null)
    @Serializable
    private data class Entry(val lyrics: String = "", val format: String = "")
}
