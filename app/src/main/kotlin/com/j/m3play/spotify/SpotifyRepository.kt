package com.j.m3play.spotify

import android.util.Base64
import com.j.m3play.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class SpotifyRepository {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val redirectUri = "m3play://callback"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SpotifyApiService::class.java)

    // --- LOGIN SYSTEM FUNCTIONS ---
    fun getLoginUrl(): String {
        return "https://accounts.spotify.com/authorize?response_type=code&client_id=$clientId&scope=$scopes&redirect_uri=$redirectUri"
    }

    suspend fun exchangeCodeForToken(code: String): String? {
        val authString = "$clientId:$clientSecret"
        val base64Auth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
        return try {
            val response = api.getUserToken(authHeader = "Basic $base64Auth", code = code, redirectUri = redirectUri)
            if (response.isSuccessful) response.body()?.access_token else null
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun fetchUserPlaylists(userToken: String): List<UserPlaylist> {
        return try {
            val response = api.getUserPlaylists("Bearer $userToken")
            if (response.isSuccessful) response.body()?.items ?: emptyList() else emptyList()
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // --- URL IMPORT FUNCTION (Yeh missing tha) ---
    suspend fun fetchPlaylistTracks(playlistUrl: String): List<String> {
        val extractedTracks = mutableListOf<String>()
        val regex = "playlist/([a-zA-Z0-9]+)".toRegex()
        val matchResult = regex.find(playlistUrl)
        val playlistId = matchResult?.groupValues?.get(1) ?: return emptyList()

        try {
            // Public track fetch ke liye hum token ke bina ya standard method se karenge
            // Agar aapke paas client token hai toh wo yahan use karein
            val response = api.getPlaylistTracks(playlistId, "Bearer " + "YOUR_CLIENT_TOKEN_IF_NEEDED")
            if (response.isSuccessful) {
                response.body()?.items?.forEach { item ->
                    item.track?.let { track ->
                        val trackName = track.name
                        val artistName = track.artists.firstOrNull()?.name ?: ""
                        extractedTracks.add("$trackName $artistName") 
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return extractedTracks
    }
}
