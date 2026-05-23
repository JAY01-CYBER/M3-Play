/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.ExternalDownloaderEnabledKey
import com.j.m3play.constants.ExternalDownloaderPackageKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.isDownloaded
import com.j.m3play.extensions.toggleLike
import com.j.m3play.models.toMediaItem
import com.j.m3play.ui.component.LocalBottomSheetPageState
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.flow.first

@SuppressLint("StringFormatInvalid")
@Composable
fun YouTubeSongMenu(
    song: Song,
    navController: androidx.navigation.NavController,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloadUtil = LocalDownloadUtil.current
    val context = LocalContext.current

    var showPlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSleepTimerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(song.id) {
        val dbSong = database.song(song.id).first()
        isFavorite = dbSong?.song?.inLibrary == true
    }

    val (externalDownloaderEnabled, _) = rememberPreference(ExternalDownloaderEnabledKey, false)
    val (externalDownloaderPackage, _) = rememberPreference(ExternalDownloaderPackageKey, "")
    val bottomSheetPageState = LocalBottomSheetPageState.current

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            isVisible = showPlaylistDialog,
            onGetAddSongs = { listOf(song) },
            onDismiss = { showPlaylistDialog = false },
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            isVisible = showSleepTimerDialog,
            onDismiss = { showSleepTimerDialog = false },
        )
    }

    // YAHAN BHI 128.dp PADDING ADD KI GAYI HAI!
    LazyColumn(
        contentPadding = PaddingValues(
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 128.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp)
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)), // Premium Pixel Shape
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                IconButton(
                    onClick = {
                        database.transaction {
                            val dbSong = song(song.id).first() ?: song
                            update(dbSong.toggleLike())
                            isFavorite = !isFavorite
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.play_next), null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.enqueue), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.playlist_add), null, tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.player.addMediaItem(song.toMediaItem())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.start_radio), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.radio), null, tint = MaterialTheme.colorScheme.tertiary)
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playRadio(song.toMediaItem())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_playlist), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.queue_music), null)
                },
                modifier = Modifier.clickable {
                    showPlaylistDialog = true
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.share), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.share), null)
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        val downloadState by downloadUtil.getDownload(song.id).collectAsState(initial = null)

        if (!song.isLocal) {
            item {
                var isDownloading by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.download), fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                painter = painterResource(
                                    when (downloadState?.state) {
                                        Download.STATE_COMPLETED -> R.drawable.offline_bolt
                                        Download.STATE_DOWNLOADING -> R.drawable.downloading
                                        else -> R.drawable.download
                                    }
                                ),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable(enabled = !isDownloading) {
                        isDownloading = true
                        if (song.isDownloaded(context)) {
                            downloadUtil.removeDownload(song.id)
                            Toast.makeText(context, context.getString(R.string.removed_from_device), Toast.LENGTH_SHORT).show()
                        } else {
                            downloadUtil.addDownload(song)
                            Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                        }
                        isDownloading = false
                        onDismiss()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        if (song.album != null) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_album), fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.album), null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        navController.navigate("album/${song.album.id}")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        if (song.artists.isNotEmpty() && song.artists.first().id != null) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist), fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.artist), null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        navController.navigate("artist/${song.artists.first().id}")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.sleep_timer), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.timer), null)
                },
                modifier = Modifier.clickable {
                    showSleepTimerDialog = true
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.details), fontWeight = FontWeight.Medium) },
                leadingContent = {
                    Icon(painterResource(R.drawable.info), null)
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    bottomSheetPageState.show {
                        ShowMediaInfo(song.id)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        if (externalDownloaderEnabled) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.open_with_downloader), fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.download), null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        val url = "https://music.youtube.com/watch?v=${song.id}"
                        if (externalDownloaderPackage.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.external_downloader_not_configured), Toast.LENGTH_LONG).show()
                            return@clickable
                        }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setPackage(externalDownloaderPackage)
                            data = android.net.Uri.parse(url)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, context.getString(R.string.external_downloader_not_installed), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
