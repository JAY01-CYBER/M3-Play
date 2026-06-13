package com.j.m3play.spotify

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

interface SpotifyApiService {
    
    // Login Code ko Access Token mein badalna (REAL URL ADDED)
    @FormUrlEncoded
    @POST
    suspend fun getUserToken(
        @Url url: String = "https://accounts.spotify.com/api/token", 
        @Header("Authorization") authHeader: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<TokenResponse>

    // User ki saari playlists lene ke liye
    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Header("Authorization") token: String
    ): Response<UserPlaylistsResponse>

    // Kisi ek playlist ke andar ke gaane lene ke liye
    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String,
        @Header("Authorization") token: String
    ): Response<SpotifyPlaylistResponse>
}
