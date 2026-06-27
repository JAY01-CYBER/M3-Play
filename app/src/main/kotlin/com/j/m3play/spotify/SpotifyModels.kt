package com.j.m3play.spotify

data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String? = null
)

// User ki khud ki saari playlists lene ke liye
data class UserPlaylistsResponse(
    val items: List<UserPlaylist>
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val tracks: PlaylistTracksRef?
)

data class PlaylistTracksRef(
    val total: Int
)

// Playlist ke andar ke gaane lene ke liye
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
