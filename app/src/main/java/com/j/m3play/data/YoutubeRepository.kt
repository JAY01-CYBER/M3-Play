import java.util.LinkedHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val PIPED_BASE_URL = "https://pipedapi.kavin.rocks/"
private const val MAX_QUERY_CACHE_SIZE = 20

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Empty(val message: String = "No results found") : DataResult<Nothing>()
    data class Error(val message: String, val cause: Throwable? = null) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}

interface PipedApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs"
    ): List<SearchItemDto>
}

data class SearchItemDto(
    val type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val uploaderName: String? = null,
    val uploader: String? = null,
    val thumbnail: String? = null,
    val thumbnailUrl: String? = null,
    val url: String? = null
)

data class Song(
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val streamUrl: String
)

internal fun SearchItemDto.toSongOrNull(): Song? {
    val resolvedTitle = title ?: name ?: return null
    val resolvedArtist = uploaderName ?: uploader ?: "Unknown artist"
    val resolvedThumbnail = thumbnailUrl ?: thumbnail ?: ""
    val videoId = extractVideoId(url)

    return Song(
        title = resolvedTitle,
        artist = resolvedArtist,
        thumbnailUrl = resolvedThumbnail,
        streamUrl = videoId ?: ""
    )
}

internal fun extractVideoId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val watchToken = "v="
    return when {
        url.contains(watchToken) -> url.substringAfter(watchToken).substringBefore('&')
        url.startsWith("/watch") && url.contains("?") -> url.substringAfter("?").substringAfter(watchToken, "")
            .substringBefore('&')
            .ifBlank { null }

        else -> url.substringAfterLast('/').ifBlank { null }
    }
}

private fun pipedApiService(
    baseUrl: String = PIPED_BASE_URL
): PipedApiService {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PipedApiService::class.java)
}

class YoutubeRepository(
    private val service: PipedApiService = pipedApiService()
) {
    private val cacheMutex = Mutex()
    private val queryCache = object : LinkedHashMap<String, List<Song>>(MAX_QUERY_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Song>>?): Boolean {
            return size > MAX_QUERY_CACHE_SIZE
        }
    }

    suspend fun searchSongs(query: String): DataResult<List<Song>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return DataResult.Empty("Type something to search")

        cacheMutex.withLock {
            queryCache[normalizedQuery]?.let { return DataResult.Success(it) }
        }

        return try {
            val songs = service.search(normalizedQuery)
                .asSequence()
                .mapNotNull { it.toSongOrNull() }
                .toList()

            if (songs.isEmpty()) {
                DataResult.Empty("No songs found for \"$normalizedQuery\"")
            } else {
                cacheMutex.withLock {
                    queryCache[normalizedQuery] = songs
                }
                DataResult.Success(songs)
            }
        } catch (t: Throwable) {
            DataResult.Error(
                message = "Unable to search songs right now. Please try again.",
                cause = t
            )
        }
    }
}
