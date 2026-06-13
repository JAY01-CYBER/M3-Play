package com.j.m3play.spotify

import android.util.Base64
import com.j.m3play.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class SpotifyRepository {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val redirectUri = "m3play://callback" // Dashboard wala deep link

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SpotifyApiService::class.java)

    // Step 1: Login ke liye Spotify ka Browser URL generate karna
    fun getLoginUrl(): String {
        val scopes = "playlist-read-private playlist-read-collaborative"
        val encodedScopes = URLEncoder.encode(scopes, "UTF-8")
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        return "https://accounts.spotify.com/authorize?client_id=$clientId&response_type=code&redirect_uri=$encodedRedirect&scope=$encodedScopes"
    }

    // Step 2: Browser se mile hue Code se Access Token generate karna
    suspend fun exchangeCodeForToken(code: String): String? {
        val authString = "$clientId:$clientSecret"
        val base64Auth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
        
        return try {
            val response = api.getUserToken(
                authHeader = "Basic $base64Auth",
                code = code,
                redirectUri = redirectUri
            )
            if (response.isSuccessful) response.body()?.access_token else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Step 3: Token use karke User ki saari Playlists lana
    suspend fun fetchUserPlaylists(userToken: String): List<UserPlaylist> {
        return try {
            val response = api.getUserPlaylists("Bearer $userToken")
            if (response.isSuccessful) {
                response.body()?.items ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Step 4: List me se kisi ek Playlist ke gaane M3Play search ke liye nikalna
    suspend fun fetchTracksFromPlaylist(playlistId: String, userToken: String): List<String> {
        val extractedTracks = mutableListOf<String>()
        try {
            val response = api.getPlaylistTracks(playlistId, "Bearer $userToken")
            if (response.isSuccessful) {
                response.body()?.items?.forEach { item ->
                    item.track?.let { track ->
                        val trackName = track.name
                        val artistName = track.artists.firstOrNull()?.name ?: ""
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
