package com.j.m3play.data

class YoutubeRepository {
    private val baseUrl = "https://pipedapi.kavin.rocks"

    suspend fun searchSongs(query: String): List<Song> {
        return emptyList()
    }
}

data class Song(
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val streamUrl: String
)
