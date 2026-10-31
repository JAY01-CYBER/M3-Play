/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.spotify

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import com.j.m3play.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class SpotifyRepository {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    @Volatile private var pendingCodeVerifier: String? = null
    private val redirectUri = "m3play://callback"

    // REAL SPOTIFY BASE API URL
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SpotifyApiService::class.java)

    // REAL LOGIN URL GENERATOR
    fun getLoginUrl(): String {
        val scopes = "playlist-read-private playlist-read-collaborative"
        val encodedScopes = URLEncoder.encode(scopes, "UTF-8")
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val verifierBytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.encodeToString(
            verifierBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        pendingCodeVerifier = verifier
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val encodedChallenge = URLEncoder.encode(challenge, "UTF-8")

        return "https://accounts.spotify.com/authorize?client_id=$clientId&response_type=code&redirect_uri=$encodedRedirect&scope=$encodedScopes&code_challenge_method=S256&code_challenge=$encodedChallenge"
    }

    // Code se Token lena
    suspend fun exchangeCodeForToken(code: String): String? {
        val verifier = pendingCodeVerifier ?: return null
        pendingCodeVerifier = null

        return try {
            val response = api.getUserToken(
                clientId = clientId,
                code = code,
                redirectUri = redirectUri,
                codeVerifier = verifier
            )
            if (response.isSuccessful) response.body()?.access_token else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Playlists fetch karna
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

    // URL Import function
    suspend fun fetchPlaylistTracks(playlistUrl: String, userToken: String): List<String> {
        if (userToken.isBlank()) return emptyList()
        val extractedTracks = mutableListOf<String>()
        val regex = "playlist/([a-zA-Z0-9]+)".toRegex()
        val matchResult = regex.find(playlistUrl)
        val playlistId = matchResult?.groupValues?.get(1) ?: return emptyList()

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
        } catch (e: Exception) { e.printStackTrace() }
        return extractedTracks
    }
}
