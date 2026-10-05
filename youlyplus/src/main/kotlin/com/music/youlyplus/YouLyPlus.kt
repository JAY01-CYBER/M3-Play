/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.music.youlyplus

import com.music.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * YouLyPlus / LyricsPlus KPoe API client.
 *
 * This replicates the multi-server fetch strategy from the YouLyPlus browser
 * extension (ibratabian17/YouLyPlus), querying community-hosted instances of
 * the open-source LyricsPlus backend (ibratabian17/lyricsplus).
 *
 * API endpoint: GET {server}/v2/lyrics/get?title=...&artist=...&duration=...
 */
object YouLyPlus {

    private const val BASE_URL = "https://lyricsplus.binimum.org"

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 3_000
                requestTimeoutMillis = 8_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            expectSuccess = true
        }
    }

    /** A single maintained mirror avoids duplicate upstream work and stays in the caller's job. */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): Result<String> = runCatching {
        val response = fetchFromServer(BASE_URL, title, artist, duration, album, isrc)
        response?.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: response?.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
            ?: response?.plainLyrics?.takeIf { it.isNotBlank() }
            ?: error("No LyricsPlus lyrics found")
    }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album, id, isrc).onSuccess(callback)
    }

    /**
     * Converts a list of LyricsItem (with millisecond 'time') to a standard 
     * [mm:ss.xxx]LRC string.
     * Supports word-by-word rich sync if syllables are present.
     */
    internal fun List<com.music.youlyplus.models.LyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L
            
            // Check if any syllable or the item itself is marked as background
            val isBg = item.syllabus?.any { it.isBackground == true } == true
            val lineTimestamp = formatTime(lineTime)
            val bgMarker = if (isBg) "{bg}" else ""
            
            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(bgMarker)
                syllabus.forEach { syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                }
                syllabus.lastOrNull()?.let { last ->
                    if (last.time != null && last.duration != null) {
                        sb.append(formatTime(last.time + last.duration, isSyllable = true))
                    }
                }
                sb.toString().trim()
            } else {
                lineTimestamp + bgMarker + (item.text ?: "")
            }
        }
    }

    private fun formatTime(timeMs: Long, isSyllable: Boolean = false): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format(java.util.Locale.ROOT, "%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }

    private suspend fun fetchFromServer(
        baseUrl: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        isrc: String? = null,
    ): LyricsResponse? = runCatching {
        val url = baseUrl.let { if (it.endsWith("/")) it else "$it/" } + "v2/lyrics/get"
        client.get(url) {
            header("User-Agent", "M3Play/3.2.0")
            parameter("title", title)
            parameter("artist", artist)
            if (duration > 0) parameter("duration", duration)
            if (album != null) parameter("album", album)
            if (isrc != null) parameter("isrc", isrc)
        }.body<LyricsResponse>()
    }.onFailure {
        if (it is kotlinx.coroutines.CancellationException) throw it
        System.err.println("YouLyPlus: Failed to fetch from $baseUrl: ${it.message}")
        it.printStackTrace()
    }.getOrNull()
}
