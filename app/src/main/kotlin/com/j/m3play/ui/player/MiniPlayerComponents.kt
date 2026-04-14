/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.j.m3play.R
import com.j.m3play.service.PlayerConnection
import com.j.m3play.service.TogetherSessionState

// ------------------------------
// 🎧 MINI PLAYER MAIN
// ------------------------------

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()

    val isLoading = playbackState == Player.STATE_BUFFERING
    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🎵 Artwork + Progress
            MiniPlayerArtwork(
                mediaMetadata = mediaMetadata,
                position = position,
                duration = duration,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 🎶 Title + Artist
            mediaMetadata?.let {
                MiniPlayerInfo(mediaMetadata = it)
            } ?: Spacer(modifier = Modifier.weight(1f))

            // 👥 Together Badge
            if (togetherSessionState !is TogetherSessionState.Idle) {
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.all_inclusive),
                            contentDescription = stringResource(R.string.music_together),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ▶ Controls
            MiniPlayerTransportControls(
                isPlaying = isPlaying,
                playbackState = playbackState,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                playerConnection = playerConnection
            )
        }
    }
}

// ------------------------------
// 🎵 ARTWORK + PROGRESS
// ------------------------------

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: com.j.m3play.models.MediaMetadata?,
    position: Long,
    duration: Long,
    isLoading: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp)
    ) {
        CircularProgressIndicator(
            progress = {
                if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 3.dp
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val thumbnailUrl = mediaMetadata?.thumbnailUrl

            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ------------------------------
// ▶ TRANSPORT CONTROLS
// ------------------------------

@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playerConnection: PlayerConnection
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        MiniPlayerButton(
            icon = R.drawable.skip_previous,
            enabled = canSkipPrevious
        ) {
            playerConnection.player.seekToPreviousMediaItem()
        }

        MiniPlayerButton(
            icon = when {
                playbackState == Player.STATE_ENDED -> R.drawable.replay
                isPlaying -> R.drawable.pause
                else -> R.drawable.play
            },
            isPrimary = true
        ) {
            if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.player.togglePlayPause()
            }
        }

        MiniPlayerButton(
            icon = R.drawable.skip_next,
            enabled = canSkipNext
        ) {
            playerConnection.player.seekToNext()
        }
    }
}

// ------------------------------
// 🔘 BUTTON
// ------------------------------

@Composable
private fun MiniPlayerButton(
    icon: Int,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isPrimary) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(if (isPrimary) MaterialTheme.colorScheme.surface else Color.Transparent)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(if (isPrimary) 22.dp else 18.dp)
        )
    }
}                
