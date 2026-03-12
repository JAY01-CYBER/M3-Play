import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamExtractorSelectionTest {

    @Test
    fun `selectPlayableAudioUrl prefers best codec then bitrate`() {
        val metadata = StreamMetadataDto(
            audioStreams = listOf(
                AudioStreamDto(url = "https://cdn/aac-256.m4a", codec = "aac", bitrate = 256000, mimeType = "audio/mp4"),
                AudioStreamDto(url = "https://cdn/opus-160.webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
                AudioStreamDto(url = "https://cdn/vorbis-320.webm", codec = "vorbis", bitrate = 320000, mimeType = "audio/webm")
            )
        )

        assertEquals("https://cdn/opus-160.webm", metadata.selectPlayableAudioUrl())
    }

    @Test
    fun `selectPlayableAudioUrl falls back to hls then dash`() {
        val withHls = StreamMetadataDto(audioStreams = emptyList(), hls = "https://cdn/master.m3u8", dash = "https://cdn/master.mpd")
        val withDashOnly = StreamMetadataDto(audioStreams = emptyList(), hls = null, dash = "https://cdn/master.mpd")

        assertEquals("https://cdn/master.m3u8", withHls.selectPlayableAudioUrl())
        assertEquals("https://cdn/master.mpd", withDashOnly.selectPlayableAudioUrl())
    }

    @Test
    fun `selectPlayableAudioUrl returns null when no playable sources`() {
        val metadata = StreamMetadataDto(
            audioStreams = listOf(AudioStreamDto(url = "", codec = "aac", bitrate = 128000)),
            hls = "",
            dash = null
        )

        assertNull(metadata.selectPlayableAudioUrl())
    }
}
