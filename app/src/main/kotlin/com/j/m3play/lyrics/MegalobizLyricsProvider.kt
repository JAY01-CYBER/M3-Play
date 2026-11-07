/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Optional fallback provider for line-synced lyrics.
 */
package com.j.m3play.lyrics

import android.content.Context
import com.j.m3play.constants.EnableMegalobizKey
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.Locale

object MegalobizLyricsProvider : LyricsProvider {
    override val name: String = "Megalobiz"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableMegalobizKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.trim()
            val cleanArtist = artist.trim()
            if (cleanTitle.isBlank() || cleanArtist.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Song title and artist are required"))
            }

            val query = URLEncoder.encode("$cleanArtist-$cleanTitle", "UTF-8")
            val searchUrl = "https://www.megalobiz.com/search/all?qry=$query&searchButton.x=0&searchButton.y=0"
            val searchPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MS)
                .get()

            val queryTokens = tokens("$cleanArtist $cleanTitle")
            val bestLink = searchPage.select("a[href]")
                .filter {
                    it.attr("href").startsWith("/lrc/maker/") &&
                        it.text().isNotBlank()
                }
                .maxByOrNull { link -> matchScore(queryTokens, tokens(link.text())) }
                ?: return@withContext Result.failure(IllegalStateException("No Megalobiz lyrics match found"))

            val confidence = matchScore(queryTokens, tokens(bestLink.text()))
            if (confidence < MINIMUM_MATCH_SCORE) {
                return@withContext Result.failure(IllegalStateException("Megalobiz result did not match the requested track"))
            }

            val href = bestLink.attr("href")
            val lyricsPageUrl = if (href.startsWith("http")) href else ROOT_URL + href
            val lyricsPage = Jsoup.connect(lyricsPageUrl)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MS)
                .get()

            val lyricId = extractLyricsId(href)
            val lyricContainer = lyricsPage.getElementById("lrc_${lyricId}_details")
                ?: lyricsPage.selectFirst("div[id^=lrc_][id$=_details]")
                ?: return@withContext Result.failure(IllegalStateException("Megalobiz lyrics content was not found"))

            val lyrics = lyricContainer.wholeText()
                .replace("\r", "")
                .trim()
            if (lyrics.isBlank()) {
                Result.failure(IllegalStateException("Megalobiz returned empty lyrics"))
            } else {
                Result.success(lyrics)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun extractLyricsId(href: String): String {
        val dottedSuffix = href.substringAfterLast('.', missingDelimiterValue = "")
        if (dottedSuffix.isNotBlank()) return dottedSuffix
        return href.substringAfterLast('/').substringBefore('?')
    }

    private fun tokens(value: String): Set<String> =
        value.lowercase(Locale.ROOT)
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length > 1 }
            .toSet()

    private fun matchScore(query: Set<String>, candidate: Set<String>): Double {
        if (query.isEmpty() || candidate.isEmpty()) return 0.0
        val intersection = query.intersect(candidate).size.toDouble()
        return intersection / query.size.toDouble()
    }

    private const val ROOT_URL = "https://www.megalobiz.com"
    private const val USER_AGENT = "M3Play Lyrics/1.0 (Android)"
    private const val REQUEST_TIMEOUT_MS = 10_000
    private const val MINIMUM_MATCH_SCORE = 0.45
}
