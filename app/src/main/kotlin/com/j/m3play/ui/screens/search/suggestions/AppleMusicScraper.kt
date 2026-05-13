/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.search.suggestions

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.TimeUnit

object AppleMusicScraper {
    private const val TAG = "AppleMusicScraper"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun executeGet(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing GET request for $url", e)
            null
        }
    }

    fun fetchTopSongs(countryCode: String = "in"): List<SuggestionTrack> {
        val tracks = mutableListOf<SuggestionTrack>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/100/songs.json"
            val responseStr = executeGet(url) ?: return emptyList()
            val json = JSONObject(responseStr)
            val results = json.getJSONObject("feed").getJSONArray("results")

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100").replace("100x100", "500x500")
                val appleUrl = item.getString("url")
                tracks.add(SuggestionTrack(rank, title, artist, artwork, appleUrl))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Songs for $countryCode", e)
        }
        return tracks
    }

    fun fetchTopAlbums(countryCode: String = "in"): List<SuggestionAlbum> {
        val albums = mutableListOf<SuggestionAlbum>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/50/albums.json"
            val responseStr = executeGet(url) ?: return emptyList()
            val json = JSONObject(responseStr)
            val results = json.getJSONObject("feed").getJSONArray("results")

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100").replace("100x100", "500x500")
                val appleUrl = item.getString("url")
                albums.add(SuggestionAlbum(rank, title, artist, artwork, appleUrl))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Albums for $countryCode", e)
        }
        return albums
    }

    fun fetchTopVideos(countryCode: String = "in"): List<SuggestionTrack> {
        val videos = mutableListOf<SuggestionTrack>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/50/music-videos.json"
            val responseStr = executeGet(url) ?: return emptyList()
            val json = JSONObject(responseStr)
            val results = json.getJSONObject("feed").getJSONArray("results")

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100").replace("100x100", "1920x1080")
                val appleUrl = item.getString("url")
                videos.add(SuggestionTrack(rank, title, artist, artwork, appleUrl))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Videos for $countryCode", e)
        }
        return videos
    }

    fun getTrendingArtists(tracks: List<SuggestionTrack>): List<SuggestionArtist> {
        val artistCounts = mutableMapOf<String, Int>()
        val artistImages = mutableMapOf<String, String?>()
        
        tracks.forEach { track ->
            val mainArtist = track.artist.split(",", "&", "feat.", "ft.").first().trim()
            artistCounts[mainArtist] = (artistCounts[mainArtist] ?: 0) + 1
            if (artistImages[mainArtist] == null) {
                artistImages[mainArtist] = track.thumbnailUrl?.replace("1920x1080", "500x500")
            }
        }
        
        return artistCounts.toList()
            .sortedByDescending { it.second }
            .take(15)
            .mapIndexed { index, pair ->
                SuggestionArtist(index + 1, pair.first, artistImages[pair.first])
            }
    }
}
