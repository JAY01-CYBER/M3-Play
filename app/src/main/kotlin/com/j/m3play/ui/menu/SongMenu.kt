/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.ui.menu

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.isDownloaded
import com.j.m3play.extensions.toggleLike
import com.j.m3play.models.toMediaItem
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.ui.component.LocalBottomSheetPageState
import kotlinx.coroutines.flow.first

@Composable
fun SongMenu(
    originalSong: Song,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloadUtil = LocalDownloadUtil.current
    val context = LocalContext.current

    val song by database.song(originalSong.id).collectAsState(initial = originalSong)

    var showPlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSleepTimerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

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

    if (showEditDialog) {
        EditMetadataDialog(
            song = song,
            onDismiss = { showEditDialog = false },
        )
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Row(modifier = Modifier.fillMaxWidth()) {
        if (isLandscape) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // YAHAN 128.dp PADDING ADD KI GAYI HAI TAARI SCROLL KABHI CUT NA HO!
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 128.dp 
            ),
            modifier = Modifier.weight(1f)
        ) {
            if (!isLandscape) {
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
                                .clip(RoundedCornerShape(16.dp)), // Pixel Style Squircle
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
                                    update(song.toggleLike())
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (song.inLibrary) R.drawable.favorite else R.drawable.favorite_border),
                                contentDescription = null,
                                tint = if (song.inLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            if (song.isLocal) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.play_offline), fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Icon(painterResource(R.drawable.offline_bolt), null, tint = MaterialTheme.colorScheme.tertiary)
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            playerConnection.forcePlayFromBeginning(song.toMediaItem())
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
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

            if (!song.isLocal) {
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

            if (song.isDownloaded(context)) {
                item {
                    var isRemoving by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.delete_from_device), fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            if (isRemoving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isRemoving) {
                            isRemoving = true
                            downloadUtil.removeDownload(song.id)
                            Toast.makeText(context, context.getString(R.string.removed_from_device), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            if (song.isLocal) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.edit_local_metadata), fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Icon(painterResource(R.drawable.edit), null)
                        },
                        modifier = Modifier.clickable {
                            showEditDialog = true
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
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
        }
    }
}
