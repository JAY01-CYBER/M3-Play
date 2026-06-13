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
    
    // Spotify se Access Token lene ki request
    @FormUrlEncoded
    @POST
    suspend fun getAccessToken(
        @Url url: String = "https://accounts.spotify.com/api/token",
        @Header("Authorization") authHeader: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<TokenResponse>

    // Playlist ka data lene ki request
    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String,
        @Header("Authorization") token: String
    ): Response<SpotifyPlaylistResponse>
}
