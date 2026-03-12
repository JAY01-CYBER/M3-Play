package com.j.m3play.data

class YoutubeRepository {
    suspend fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        return sampleSongs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val streamUrl: String,
)

val sampleSongs = listOf(
    Song("1", "Neon Skyline", "M3-Play", "https://picsum.photos/600?1", ""),
    Song("2", "Liquid Motion", "M3-Play", "https://picsum.photos/600?2", ""),
    Song("3", "Midnight Current", "M3-Play", "https://picsum.photos/600?3", ""),
    Song("4", "Soft Static", "M3-Play", "https://picsum.photos/600?4", ""),
)
