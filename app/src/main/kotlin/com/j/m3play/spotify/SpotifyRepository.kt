package com.j.m3play.spotify

import android.util.Base64
import com.j.m3play.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyRepository {

    // Aapke BuildConfig se keys automatically yahan aayengi
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET

    // Retrofit ka setup
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SpotifyApiService::class.java)

    // Token generate karne ka function
    private suspend fun getAuthToken(): String? {
        val authString = "$clientId:$clientSecret"
        val base64Auth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
        
        return try {
            val response = api.getAccessToken(authHeader = "Basic $base64Auth")
            if (response.isSuccessful) response.body()?.access_token else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Link daalkar gaano ki list nikalne ka function
    suspend fun fetchPlaylistTracks(playlistUrl: String): List<String> {
        val extractedTracks = mutableListOf<String>()
        
        try {
            // 1. Link se id nikalna
            val regex = "playlist/([a-zA-Z0-9]+)".toRegex()
            val matchResult = regex.find(playlistUrl)
            val playlistId = matchResult?.groupValues?.get(1) ?: return emptyList()

            // 2. Token lena
            val token = getAuthToken() ?: return emptyList()

            // 3. Gaane fetch karna
            val response = api.getPlaylistTracks(playlistId, "Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.items?.forEach { item ->
                    item.track?.let { track ->
                        val trackName = track.name
                        val artistName = track.artists.firstOrNull()?.name ?: ""
                        // Format: "Song Name Artist Name"
                        extractedTracks.add("$trackName $artistName") 
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return extractedTracks
    }
}
