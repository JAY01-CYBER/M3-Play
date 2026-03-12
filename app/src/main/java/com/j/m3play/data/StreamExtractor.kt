import java.util.LinkedHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private const val STREAM_BASE_URL = "https://pipedapi.kavin.rocks/"
private const val MAX_STREAM_CACHE_SIZE = 50

interface StreamApiService {
    @GET("streams/{videoId}")
    suspend fun streamMetadata(@Path("videoId") videoId: String): StreamMetadataDto
}

data class StreamMetadataDto(
    val audioStreams: List<AudioStreamDto>? = null,
    val hls: String? = null,
    val dash: String? = null
)

data class AudioStreamDto(
    val url: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    val mimeType: String? = null,
    val quality: String? = null
)

internal fun StreamMetadataDto.selectPlayableAudioUrl(): String? {
    val sortedAudio = audioStreams.orEmpty()
        .filter { !it.url.isNullOrBlank() }
        .sortedWith(
            compareByDescending<AudioStreamDto> { stream ->
                val codecScore = when {
                    stream.codec?.contains("opus", ignoreCase = true) == true -> 4
                    stream.codec?.contains("aac", ignoreCase = true) == true -> 3
                    stream.codec?.contains("mp4a", ignoreCase = true) == true -> 3
                    stream.codec?.contains("vorbis", ignoreCase = true) == true -> 2
                    else -> 1
                }
                val mimeBoost = if (stream.mimeType?.contains("audio", ignoreCase = true) == true) 1 else 0
                codecScore + mimeBoost
            }.thenByDescending { it.bitrate ?: 0 }
        )

    return sortedAudio.firstOrNull()?.url
        ?: hls?.takeIf { it.isNotBlank() }
        ?: dash?.takeIf { it.isNotBlank() }
}

private fun streamApiService(
    baseUrl: String = STREAM_BASE_URL
): StreamApiService {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StreamApiService::class.java)
}

object StreamExtractor {
    private val service: StreamApiService = streamApiService()
    private val cacheMutex = Mutex()
    private val streamCache = object : LinkedHashMap<String, String>(MAX_STREAM_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > MAX_STREAM_CACHE_SIZE
        }
    }

    suspend fun getAudioStream(videoId: String): DataResult<String> {
        val normalizedVideoId = videoId.trim()
        if (normalizedVideoId.isBlank()) {
            return DataResult.Error("Invalid video id")
        }

        cacheMutex.withLock {
            streamCache[normalizedVideoId]?.let { return DataResult.Success(it) }
        }

        return try {
            val metadata = service.streamMetadata(normalizedVideoId)
            val selectedUrl = metadata.selectPlayableAudioUrl()

            if (selectedUrl.isNullOrBlank()) {
                DataResult.Empty("No playable stream found")
            } else {
                cacheMutex.withLock {
                    streamCache[normalizedVideoId] = selectedUrl
                }
                DataResult.Success(selectedUrl)
            }
        } catch (t: Throwable) {
            DataResult.Error(
                message = "Unable to load stream right now. Please try again.",
                cause = t
            )
        }
    }
}
