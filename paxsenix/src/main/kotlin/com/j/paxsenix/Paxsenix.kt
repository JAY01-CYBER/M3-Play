package com.j.paxsenix

import android.util.Log
import com.j.m3play.betterlyrics.TTMLParser
import com.j.paxsenix.models.AppleMusicSearchResponse
import com.j.paxsenix.models.LyricsResponse
import com.j.paxsenix.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs

object Paxsenix {
    private val httpClient: HttpClient by lazy {
        Log.d("Paxsenix", "Initializing Paxsenix HTTP client")
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            defaultRequest {
                url("https://lyrics.paxsenix.org")
                header("User-Agent", "M3Play/1.0")
            }
            expectSuccess = true
        }
    }

    private const val APPLE_MUSIC_API_BASE = "https://amp-api.music.apple.com/v1/catalog/us"

    private val appleJson = Json { ignoreUnknownKeys = true }
    
    @Volatile
    private var appleTokenManager: AppleTokenManager? = null
    private val tokenManager: AppleTokenManager
        get() = appleTokenManager ?: synchronized(this) {
            appleTokenManager ?: AppleTokenManager(httpClient).also { appleTokenManager = it }
        }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        Log.d("Paxsenix", "Searching Apple Music for: $query")
        
        val token = tokenManager.getToken()
        return@runCatching searchWithToken(token, query)
    }.getOrElse { e ->
        if (e is ClientRequestException && e.response.status.value == 401) {
            tokenManager.clearToken()
            return@getOrElse runCatching {
                val newToken = tokenManager.getToken()
                searchWithToken(newToken, query)
            }.getOrElse { e2 ->
                Log.e("Paxsenix", "Search retry error: ${e2.message}", e2)
                emptyList()
            }
        }
        Log.e("Paxsenix", "Search error: ${e.message}", e)
        emptyList()
    }
    
    private suspend fun searchWithToken(token: String, query: String): List<SearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        val response = httpClient.get("$APPLE_MUSIC_API_BASE/search?term=$encodedQuery&types=songs&limit=25&l=en-US&platform=web&format[resources]=map&include[songs]=artists&extend=artistUrl") {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0")
            header("Accept", "application/json")
            header("Accept-Language", "en-US,en;q=0.5")
            header("x-apple-renewal", "true")
        }
        
        val body = try {
            appleJson.decodeFromString<AppleMusicSearchResponse>(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("Paxsenix", "Failed to parse Apple Music search response", e)
            return emptyList()
        }
        
        val songs = body.results.songs?.data ?: return emptyList()
        
        return songs.mapNotNull { songData ->
            val detail = body.resources?.songs?.get(songData.id) ?: return@mapNotNull null
            val attr = detail.attributes
            SearchResult(
                id = songData.id,
                trackName = attr.name,
                artistName = attr.artistName,
                albumName = attr.albumName,
                duration = attr.durationInMillis?.toInt()?.div(1000),
                artwork = attr.artwork?.url?.replace("{w}", "100")?.replace("{h}", "100")?.replace("{f}", "png")
            )
        }.also { results ->
            Log.d("Paxsenix", "Apple Music search results count: ${results.size}")
            results.forEach { result ->
                Log.v("Paxsenix", "  - ${result.displayName} by ${result.displayArtist} (ID: ${result.id}, Duration: ${result.duration})")
            }
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        Log.d("Paxsenix", "getLyrics called: title='$title', artist='$artist', duration=$duration, album=$album")
        
        val searchQueries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
        }
        
        var allResults: List<Pair<SearchResult, Double>> = emptyList()
        
        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                Log.d("Paxsenix", "Trying search query: $query")
                val searchResults = search(query)
                
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, title, artist, duration)
                }
            }
        }
        
        if (allResults.isEmpty()) {
            Log.w("Paxsenix", "No tracks found for any query")
            throw IllegalStateException("No tracks found on Paxsenix")
        }

        var bestLyrics: String? = null
        var bestQuality = 0

        for ((result, score) in allResults.take(10)) {
            Log.d("Paxsenix", "Trying: ${result.displayName} (ID: ${result.id}, dur: ${result.duration}, score: $score)")
            val lrc = fetchLyricsForTrack(result.id).getOrNull() ?: continue
            if (lrc.isEmpty()) continue
            
            val quality = getQuality(lrc)
            
            if (quality > bestQuality) {
                bestQuality = quality
                bestLyrics = lrc
            }
            
            if (bestQuality == 3) break
        }

        bestLyrics?.let {
            Log.d("Paxsenix", "Using Paxsenix lyrics with quality $bestQuality")
            return Result.success(it)
        }
        
        Log.w("Paxsenix", "No lyrics content from Paxsenix for matched tracks")
        return Result.failure(IllegalStateException("No lyrics available from Paxsenix"))
    }
    
    private fun getQuality(lrc: String): Int {
        if (lrc.isBlank()) return 0
        if (lrc.contains("<tt", ignoreCase = true) || lrc.contains("http://www.w3.org/ns/ttml", ignoreCase = true)) return 3
        val hasWordTimings = (lrc.contains("<") && lrc.contains(">") && (lrc.contains("|") || lrc.contains(":"))) ||
                lrc.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"))
        
        if (hasWordTimings) return 3
        
        val hasLineTimings = lrc.contains(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]")) || lrc.contains(Regex("^\\[bg:.*\\]", RegexOption.MULTILINE))
        
        if (hasLineTimings) return 2
        return 1
    }

    private fun scoreAndFilterResults(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<SearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")
        
        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()
        
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)
        
        return results.map { result ->
            var score = 0.0
            
            val resultTitle = result.displayName
            val resultArtist = result.displayArtist
            
            result.duration?.let { d ->
                val diff = abs(d - durationMs)
                when {
                    diff <= 2000 -> score += 100 
                    diff <= 5000 -> score += 50  
                    diff <= 10000 -> score += 10 
                    else -> score -= 50          
                }
            }
            
            val resultTitleCleaned = resultTitle.replace(cleanupRegex, "").lowercase().trim()
            
            when {
                resultTitleCleaned == cleanedTitle -> score += 80
                resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) -> score += 40
            }
            
            val resultIsMixed = resultTitle.contains("mixed", ignoreCase = true)
            val resultIsRemix = resultTitle.contains("remix", ignoreCase = true)
            
            if (resultIsMixed && !targetIsMixed) score -= 60
            if (resultIsRemix && !targetIsRemix) score -= 40
            
            val resultArtistLower = resultArtist.lowercase()
            val targetArtistPrimary = cleanedArtist
            
            when {
                resultArtistLower.contains(targetArtistPrimary) -> score += 50
                else -> {
                    val artistWords = targetArtistPrimary.split(Regex("\\s+")).filter { it.length > 2 }
                    if (artistWords.any { resultArtistLower.contains(it) }) {
                        score += 25
                    }
                }
            }
            
            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        Log.d("Paxsenix", "Fetching lyrics for track ID: $id")
        
        val response = httpClient.get("/apple-music/lyrics") {
            parameter("id", id)
        }.body<LyricsResponse>()
        
        val lyricsType = response.type
        Log.d("Paxsenix", "Lyrics response: type=$lyricsType")
        
        if (!response.ttmlContent.isNullOrBlank()) {
            Log.d("Paxsenix", "Returning raw TTML content for perfect word sync")
            return@runCatching response.ttmlContent
        }

        if (!response.elrcMultiPerson.isNullOrBlank()) {
            Log.d("Paxsenix", "Using elrcMultiPerson as fallback")
            return@runCatching response.elrcMultiPerson
        }
        if (!response.elrc.isNullOrBlank()) {
            Log.d("Paxsenix", "Using elrc as fallback")
            return@runCatching response.elrc
        }

        if (!response.plain.isNullOrBlank()) {
            Log.d("Paxsenix", "Using plain lyrics field")
            return@runCatching response.plain
        }

        if (response.content.isEmpty()) {
            throw IllegalStateException("No lyrics found")
        }
        
        val hasWordLevel = lyricsType == "Syllable"
        
        if (!hasWordLevel) {
            val plain = response.content
                .map { line -> line.text.joinToString(" ") { it.text } }
                .filter { it.isNotBlank() }
                .joinToString("\n")
            return@runCatching plain
        }

        val lrc = buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10

                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }

                val lineText = line.text.joinToString(" ") { it.text }

                if (lineText.isNotBlank()) {
                    appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agent, lineText))

                    if (line.text.isNotEmpty()) {
                        val wordsData = line.text.joinToString("|") { word ->
                            "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
                        }
                        if (wordsData.isNotEmpty()) {
                            appendLine("<$wordsData>")
                        }
                    }
                }
            }
        }

        return@runCatching lrc
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val searchQueries = listOf(
            "$cleanedTitle $cleanedArtist",
            cleanedTitle
        )

        var scoredResults: List<Pair<SearchResult, Double>> = emptyList()
        searchLoop@ for (query in searchQueries) {
            val results = search(query)
            if (results.isEmpty()) continue

            val filtered = scoreAndFilterResults(results, title, artist, duration)
            if (filtered.isNotEmpty()) {
                scoredResults = filtered
                break@searchLoop
            }
        }

        val collectedLyrics = mutableListOf<Pair<String, Int>>()

        for ((result, _) in scoredResults.take(5)) {
            Log.d("Paxsenix", "Trying lyrics for: ${result.displayName}")
            val lrc = fetchLyricsForTrack(result.id).getOrNull() ?: continue
            if (lrc.isNotEmpty()) {
                val quality = getQuality(lrc)
                collectedLyrics.add(lrc to quality)
                if (quality == 3) break
            }
        }

        collectedLyrics.sortedByDescending { it.second }.forEach { (lrc, _) ->
            callback(lrc)
        }
    }
    
    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val parsedLines = TTMLParser.parseTTML(ttml)
            toLRC(parsedLines) 
        } catch (e: Exception) {
            Log.e("Paxsenix", "TTML conversion failed: ${e.message}", e)
            ""
        }
    }

    private fun toLRC(lines: List<TTMLParser.ParsedLine>): String {
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10

                val prefix = when {
                    line.isBackground -> "{bg}"
                    line.agent == "v2" -> "{agent:v2}"
                    line.agent == "v1" -> "{agent:v1}"
                    else -> ""
                }

                appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, prefix, line.text))

                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }
            }
        }
    }

    private class AppleTokenManager(private val httpClient: HttpClient) {
        private var cachedToken: String? = null
        private val mutex = Mutex()

        suspend fun getToken(): String = mutex.withLock {
            cachedToken?.let { return it }

            try {
                val mainPageResponse = httpClient.get("https://beta.music.apple.com")
                val mainPageBody = mainPageResponse.bodyAsText()

                val indexJsRegex = Regex("""/assets/index~[^/]+\.js""")
                val indexJsMatch = indexJsRegex.find(mainPageBody)
                    ?: throw Exception("Could not find index JS URL")

                val indexJsUri = indexJsMatch.value

                val indexJsResponse = httpClient.get("https://beta.music.apple.com$indexJsUri")
                val indexJsBody = indexJsResponse.bodyAsText()

                val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                val tokenMatch = tokenRegex.find(indexJsBody)
                    ?: throw Exception("Could not find token")

                val token = tokenMatch.value
                cachedToken = token
                Log.d("Paxsenix", "Fetched new Apple Music token")
                return token
            } catch (e: Exception) {
                Log.e("Paxsenix", "Error fetching Apple Music token", e)
                throw Exception("Error fetching Apple Music token: ${e.message}", e)
            }
        }

        fun clearToken() {
            cachedToken = null
            Log.d("Paxsenix", "Cleared cached Apple Music token")
        }
    }
}
