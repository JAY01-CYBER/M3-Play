package com.j.m3play.spotify

import com.j.m3play.spotify.models.SpotifyAlbum
import com.j.m3play.spotify.models.SpotifyArtist
import com.j.m3play.spotify.models.SpotifyImage
import com.j.m3play.spotify.models.SpotifyPaging
import com.j.m3play.spotify.models.SpotifyPlaylist
import com.j.m3play.spotify.models.SpotifyPlaylistOwner
import com.j.m3play.spotify.models.SpotifyPlaylistTrack
import com.j.m3play.spotify.models.SpotifyPlaylistTracksRef
import com.j.m3play.spotify.models.SpotifyRecommendations
import com.j.m3play.spotify.models.SpotifySavedTrack
import com.j.m3play.spotify.models.SpotifySearchResult
import com.j.m3play.spotify.models.SpotifySimpleAlbum
import com.j.m3play.spotify.models.SpotifySimpleArtist
import com.j.m3play.spotify.models.SpotifyTrack
import com.j.m3play.spotify.models.SpotifyUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object Spotify {
    @Volatile
    var accessToken: String? = null

    private const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v1/query"

    private fun randomUserAgent(): String {
        val osOptions = arrayOf(
            "Windows NT 10.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "X11; Linux x86_64",
        )
        val chromeBase = 140
        val chromeMajor = chromeBase - (0..4).random()
        val chromePatch = (0..499).random()
        val os = osOptions.random()
        return "Mozilla/5.0 ($os) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chromeMajor.0.$chromePatch.0 Safari/537.36"
    }

    private val json =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private val restClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url("https://api.spotify.com/v1/")
                header("User-Agent", randomUserAgent())
                header("app-platform", "WebPlayer")
                header("Origin", "https://open.spotify.com")
                header("Referer", "https://open.spotify.com/")
            }
            expectSuccess = false
        }
    }

    private val gqlClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
            defaultRequest {
                header("User-Agent", randomUserAgent())
                header("app-platform", "WebPlayer")
                header("Origin", "https://open.spotify.com")
                header("Referer", "https://open.spotify.com/")
                header("Accept", "application/json")
            }
            expectSuccess = false
        }
    }

    class SpotifyException(
        val statusCode: Int,
        override val message: String,
        val retryAfterSec: Long = 0,
    ) : Exception(message)

    @Volatile
    var logger: ((level: String, message: String) -> Unit)? = null

    private fun log(level: String, message: String) {
        logger?.invoke(level, message)
    }

    private fun JsonObject.obj(key: String): JsonObject? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonObject } catch (_: Exception) { null }

    private fun JsonObject.str(key: String): String? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null }

    private fun JsonObject.int(key: String): Int? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull } catch (_: Exception) { null }

    private fun JsonObject.arr(key: String): JsonArray? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonArray } catch (_: Exception) { null }

    @Volatile
    var onHashExpired: ((operationName: String) -> Unit)? = null

    private suspend fun graphqlPost(
        operationName: String,
        variables: JsonObject = buildJsonObject {},
    ): JsonObject {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")

        val primaryHash = SpotifyHashProvider.getHash(operationName)
        val hashCandidates = buildList {
            add(primaryHash)
            SpotifyHashProvider.getPreviousHash(operationName)?.let { prev ->
                if (prev != primaryHash) add(prev)
            }
        }

        for ((hashIdx, sha256Hash) in hashCandidates.withIndex()) {
            val body = buildGqlBody(operationName, sha256Hash, variables)
            val result = executeGqlWithRetries(operationName, token, body)

            if (result.isPersistedQueryNotFound) {
                if (hashIdx < hashCandidates.lastIndex) continue
                onHashExpired?.invoke(operationName)
                throw SpotifyException(412, "PersistedQueryNotFound for $operationName")
            }
            return result.json!!
        }
        throw SpotifyException(412, "No valid hash found for $operationName")
    }

    private fun buildGqlBody(
        operationName: String,
        sha256Hash: String,
        variables: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("variables", variables)
            put("operationName", operationName)
            putJsonObject("extensions") {
                putJsonObject("persistedQuery") {
                    put("version", 1)
                    put("sha256Hash", sha256Hash)
                }
            }
        }

    private class GqlResult(
        val json: JsonObject?,
        val isPersistedQueryNotFound: Boolean,
    )

    private suspend fun executeGqlWithRetries(
        operationName: String,
        token: String,
        body: JsonObject,
    ): GqlResult {
        val maxRetries = 3
        for (attempt in 0 until maxRetries) {
            val response = gqlClient.post(GQL_URL) {
                header("Authorization", "Bearer $token")
                setBody(TextContent(body.toString(), ContentType.Application.Json.withParameter("charset", "UTF-8")))
            }

            if (response.status == HttpStatusCode.Unauthorized) throw SpotifyException(401, "Token expired or invalid")
            if (response.status == HttpStatusCode.TooManyRequests) {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                if (attempt < maxRetries - 1) {
                    delay(retryAfter * 1000)
                    continue
                }
                throw SpotifyException(429, "Rate limited", retryAfterSec = retryAfter)
            }
            if (response.status == HttpStatusCode.PreconditionFailed) return GqlResult(null, true)
            if (response.status.value !in 200..299) {
                throw SpotifyException(response.status.value, "GraphQL error")
            }

            val responseJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val errors = responseJson.arr("errors")
            if (errors != null && errors.isNotEmpty()) {
                val errorMsg = errors[0].jsonObject.str("message") ?: "Unknown GraphQL error"
                if (errorMsg.contains("PersistedQueryNotFound", ignoreCase = true)) {
                    return GqlResult(null, true)
                }
                throw SpotifyException(400, "GraphQL: $errorMsg")
            }
            return GqlResult(responseJson, false)
        }
        throw SpotifyException(429, "Rate limited after retries")
    }

    private suspend inline fun <reified T> authenticatedGet(
        endpoint: String,
        failFastOn429: Boolean = false,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")
        val maxRetries = if (failFastOn429) 1 else 3
        
        for (attempt in 0 until maxRetries) {
            val response = restClient.get(endpoint) {
                header("Authorization", "Bearer $token")
                block()
            }
            if (response.status == HttpStatusCode.Unauthorized) throw SpotifyException(401, "Token expired")
            if (response.status == HttpStatusCode.TooManyRequests) {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                if (failFastOn429 || retryAfter > 3L) throw SpotifyException(429, "Rate limited", retryAfterSec = retryAfter)
                if (attempt < maxRetries - 1) {
                    delay(retryAfter * 1000)
                    continue
                }
                throw SpotifyException(429, "Rate limited", retryAfterSec = retryAfter)
            }
            if (response.status.value !in 200..299) throw SpotifyException(response.status.value, "API error")
            return response.body()
        }
        throw SpotifyException(429, "Rate limited")
    }

    private fun parseGqlImage(source: JsonObject): SpotifyImage? {
        val url = source.str("url") ?: return null
        return SpotifyImage(url = url, height = source.int("height"), width = source.int("width"))
    }

    private fun parseGqlImages(sources: JsonArray?): List<SpotifyImage> =
        sources?.mapNotNull { parseGqlImage(it.jsonObject) } ?: emptyList()

    private fun parseGqlSimpleArtist(artistObj: JsonObject): SpotifySimpleArtist? {
        val uri = artistObj.str("uri") ?: return null
        return SpotifySimpleArtist(
            id = uri.substringAfterLast(":"),
            name = artistObj.obj("profile")?.str("name") ?: "",
            uri = uri,
        )
    }

    private fun parseGqlTrack(
        trackData: JsonObject,
        albumOverride: SpotifySimpleAlbum? = null,
        uriOverride: String? = null,
    ): SpotifyTrack {
        val uri = uriOverride ?: trackData.str("uri") ?: trackData.str("_uri") ?: ""
        val trackId = uri.substringAfterLast(":")

        val artists = trackData.obj("artists")?.arr("items")?.mapNotNull { elem ->
            parseGqlSimpleArtist(elem.jsonObject)
        } ?: emptyList()

        val album = albumOverride ?: run {
            val albumData = trackData.obj("albumOfTrack")
            val albumUri = albumData?.str("uri") ?: ""
            val albumId = albumUri.substringAfterLast(":")
            SpotifySimpleAlbum(
                id = albumId,
                name = albumData?.str("name") ?: "",
                images = parseGqlImages(albumData?.obj("coverArt")?.arr("sources")),
                uri = albumUri.ifEmpty { null },
            )
        }

        return SpotifyTrack(
            id = trackId,
            name = trackData.str("name") ?: "",
            artists = artists,
            album = album,
            durationMs = parseGqlTrackDurationMs(trackData),
            uri = uri.ifEmpty { null },
        )
    }

    private fun parseGqlTrackDurationMs(trackData: JsonObject): Int {
        trackData.obj("duration")?.int("totalMilliseconds")?.let { if (it > 0) return it }
        trackData.int("durationMs")?.let { if (it > 0) return it }
        trackData.int("duration_ms")?.let { if (it > 0) return it }
        trackData.int("duration")?.let { sec -> if (sec > 0) return sec * 1000 }
        return 0
    }

    private fun parseGqlPlaylistImages(imagesObj: JsonObject?): List<SpotifyImage> =
        imagesObj?.arr("items")?.flatMap { imageGroup ->
            parseGqlImages(imageGroup.jsonObject.arr("sources"))
        } ?: emptyList()

    suspend fun me(): Result<SpotifyUser> = runCatching {
        try {
            val response = graphqlPost(operationName = "profileAttributes")
            val profile = response.obj("data")?.obj("me")?.obj("profile") ?: throw Exception("Invalid profile")
            val uri = profile.str("uri") ?: ""
            SpotifyUser(
                id = uri.substringAfterLast(":"),
                displayName = profile.str("name"),
                email = null,
                images = parseGqlImages(profile.obj("avatar")?.arr("sources")),
            )
        } catch (e: Exception) {
            authenticatedGet<SpotifyUser>("me")
        }
    }

    suspend fun myPlaylists(limit: Int = 50, offset: Int = 0): Result<SpotifyPaging<SpotifyPlaylist>> = runCatching {
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Playlists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") { add("LIKED_SONGS"); add("YOUR_EPISODES_V2"); add("PRERELEASES"); add("EVENTS") }
            put("limit", limit)
            put("offset", offset)
            put("flatten", true)
            putJsonArray("expandedFolders") {}
            put("folderUri", null as String?)
            put("includeFoldersWhenFlattening", false)
        }
        val response = graphqlPost("libraryV3", vars)
        val libraryData = response.obj("data")?.obj("me")?.obj("libraryV3") ?: throw Exception("Invalid response")

        val playlists = libraryData.arr("items")?.mapNotNull { itemElem ->
            val wrapper = itemElem.jsonObject.obj("item") ?: return@mapNotNull null
            if (wrapper.str("__typename") != "PlaylistResponseWrapper") return@mapNotNull null
            parsePlaylistWrapper(wrapper)
        } ?: emptyList()

        SpotifyPaging(
            items = playlists,
            total = libraryData.int("totalCount") ?: 0,
            limit = libraryData.obj("pagingInfo")?.int("limit") ?: limit,
            offset = libraryData.obj("pagingInfo")?.int("offset") ?: offset,
        )
    }

    private fun parsePlaylistWrapper(wrapper: JsonObject): SpotifyPlaylist? {
        val data = wrapper.obj("data") ?: return null
        if (data.str("__typename") != "Playlist") return null
        val playlistUri = wrapper.str("_uri") ?: return null
        val ownerData = data.obj("ownerV2")?.obj("data")
        return SpotifyPlaylist(
            id = playlistUri.substringAfterLast(":"),
            name = data.str("name") ?: "",
            description = data.str("description"),
            images = parseGqlPlaylistImages(data.obj("images")),
            owner = SpotifyPlaylistOwner(
                id = ownerData?.str("uri")?.substringAfterLast(":") ?: ownerData?.str("id") ?: "",
                displayName = ownerData?.str("name"),
                uri = ownerData?.str("uri"),
            ),
            tracks = SpotifyPlaylistTracksRef(total = data.obj("content")?.int("totalCount")),
            uri = playlistUri,
        )
    }

    suspend fun playlistTracks(playlistId: String, limit: Int = 100, offset: Int = 0): Result<SpotifyPaging<SpotifyPlaylistTrack>> = runCatching {
        val vars = buildJsonObject {
            put("uri", "spotify:playlist:$playlistId")
            put("offset", offset)
            put("limit", limit)
            put("enableWatchFeedEntrypoint", false)
        }
        val response = graphqlPost("fetchPlaylist", vars)
        val content = response.obj("data")?.obj("playlistV2")?.obj("content") ?: throw Exception("No content")

        val tracks = content.arr("items")?.mapNotNull { elem ->
            val itemWrapper = elem.jsonObject.obj("itemV2") ?: return@mapNotNull null
            val itemData = itemWrapper.obj("data") ?: return@mapNotNull null
            val wrapperUri = itemWrapper.str("_uri") ?: itemWrapper.str("uri")
            SpotifyPlaylistTrack(
                track = parseGqlTrack(itemData, uriOverride = wrapperUri),
                uid = elem.jsonObject.str("uid") ?: itemWrapper.str("uid"),
            )
        } ?: emptyList()

        SpotifyPaging(
            items = tracks,
            total = content.int("totalCount") ?: 0,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun topTracks(timeRange: String = "medium_term", limit: Int = 50, offset: Int = 0): Result<SpotifyPaging<SpotifyTrack>> = runCatching {
        authenticatedGet("me/top/tracks", failFastOn429 = true) {
            parameter("time_range", timeRange)
            parameter("limit", limit)
            parameter("offset", offset)
        }
    }

    suspend fun topArtists(timeRange: String = "medium_term", limit: Int = 50, offset: Int = 0): Result<SpotifyPaging<SpotifyArtist>> = runCatching {
        authenticatedGet("me/top/artists", failFastOn429 = true) {
            parameter("time_range", timeRange)
            parameter("limit", limit)
            parameter("offset", offset)
        }
    }

    fun isAuthenticated(): Boolean = accessToken != null
}

@kotlinx.serialization.Serializable
data class ArtistTopTracksResponse(val tracks: List<SpotifyTrack> = emptyList())
@kotlinx.serialization.Serializable
data class RelatedArtistsResponse(val artists: List<SpotifyArtist> = emptyList())
@kotlinx.serialization.Serializable
data class NewReleasesResponse(val albums: SpotifyPaging<SpotifyAlbum>? = null)
