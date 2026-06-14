
package com.j.m3play.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j.m3play.R
import com.j.m3play.constants.ListThumbnailSize
import com.j.m3play.constants.ThumbnailCornerRadius
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.spotify.SpotifyMapper
import com.j.m3play.spotify.models.SpotifyPlaylist
import com.j.m3play.spotify.models.SpotifyTrack
import com.j.m3play.ui.utils.resize
import com.j.m3play.utils.joinByBullet
import com.j.m3play.utils.makeTimeString

@Composable
fun SpotifyLibraryPlaylistListItem(
    playlist: SpotifyPlaylist,
    navController: NavController,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
) {
    val libraryPlaylist = remember(playlist) { playlist.toLibraryPlaylist() }
    val openPlaylist = {
        navController.navigate("spotify_playlist/${playlist.id}")
    }
    val trailing: @Composable RowScope.() -> Unit = {
        Icon(
            painter = painterResource(R.drawable.spotify_icon),
            contentDescription = stringResource(R.string.spotify_account),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }

    Card(
    shape = shape,
    modifier = modifier
        .fillMaxWidth()
        .focusable()
        .clickable(onClick = openPlaylist)
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.playlist.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        trailing()
    }
}
}

@Composable
fun SpotifyTrackListItem(
    track: SpotifyTrack,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (track.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showSongIconPlaceholder: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val duration = track.durationMs.takeIf { it > 0 }?.toLong()?.let(::makeTimeString)
    val subtitle = joinByBullet(
        track.artists.joinToString { it.name },
        duration,
    )

    ListItem(
        title = track.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = SpotifyMapper.getTrackThumbnail(track)?.resize(200, 200),
                albumIndex = albumIndex,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                placeholderIconRes = if (showSongIconPlaceholder) R.drawable.music_note else null,
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

private fun SpotifyPlaylist.toLibraryPlaylist(): Playlist =
    Playlist(
        playlist = PlaylistEntity(
            id = "SPOTIFY_PLAYLIST_$id",
            name = name,
            thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(this),
            remoteSongCount = tracks?.total ?: 0,
            isEditable = false,
        ),
        songCount = tracks?.total ?: 0,
        songThumbnails = images.map { it.url },
    )
