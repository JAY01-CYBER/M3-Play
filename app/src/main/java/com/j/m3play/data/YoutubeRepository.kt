package com.j.m3play.data

import com.j.m3play.model.Song

class YoutubeRepository {
    suspend fun searchSongs(query: String): List<Song> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return SampleCatalog.trendingSongs

        return SampleCatalog.trendingSongs.filter {
            it.title.lowercase().contains(normalized) ||
                it.artist.lowercase().contains(normalized)
        }
    }
}
