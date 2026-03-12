import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeRepositoryMappingTest {

    @Test
    fun `toSongOrNull maps dto with title and uploaderName`() {
        val dto = SearchItemDto(
            title = "Song Title",
            uploaderName = "Artist Name",
            thumbnailUrl = "https://img.example/1.jpg",
            url = "/watch?v=abc123"
        )

        val song = dto.toSongOrNull()

        requireNotNull(song)
        assertEquals("Song Title", song.title)
        assertEquals("Artist Name", song.artist)
        assertEquals("https://img.example/1.jpg", song.thumbnailUrl)
        assertEquals("abc123", song.streamUrl)
    }

    @Test
    fun `toSongOrNull falls back to name and unknown artist`() {
        val dto = SearchItemDto(
            name = "Fallback Name",
            thumbnail = "https://img.example/2.jpg",
            url = "https://youtube.com/watch?v=vid999&t=10"
        )

        val song = dto.toSongOrNull()

        requireNotNull(song)
        assertEquals("Fallback Name", song.title)
        assertEquals("Unknown artist", song.artist)
        assertEquals("https://img.example/2.jpg", song.thumbnailUrl)
        assertEquals("vid999", song.streamUrl)
    }

    @Test
    fun `toSongOrNull returns null when title and name missing`() {
        val dto = SearchItemDto(url = "/watch?v=abc123")
        assertNull(dto.toSongOrNull())
    }
}
