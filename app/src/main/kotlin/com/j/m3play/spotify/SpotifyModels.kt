package com.j.m3play.spotify

// Token fetch karne ke liye
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

// Playlist tracks ka response
data class SpotifyPlaylistResponse(
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val track: SpotifyTrack?
)

data class SpotifyTrack(
    val name: String,
    val artists: List<SpotifyArtist>
)

data class SpotifyArtist(
    val name: String
)
