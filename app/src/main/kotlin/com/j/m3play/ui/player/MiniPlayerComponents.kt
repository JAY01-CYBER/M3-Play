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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.j.m3play.R
import com.j.m3play.constants.MiniPlayerHeight
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.together.TogetherSessionState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.j.m3play.LocalDatabase
import com.j.m3play.db.entities.ArtistEntity

@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    // Swipe vibrate
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    val isRightSwipe = currentOffset > 0
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                        if (com.j.m3play.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.j.m3play.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                        if (com.j.m3play.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.j.m3play.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        content(offsetXAnimatable.value)

        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

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
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()

    val isLoading = playbackState == Player.STATE_BUFFERING
    val isLiked = currentSong?.song?.liked == true

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            ModernMiniPlayerArtwork(
                mediaMetadata = mediaMetadata,
                position = position,
                duration = duration
            )

            Spacer(modifier = Modifier.width(12.dp))

            mediaMetadata?.let {
                ModernMiniPlayerInfo(mediaMetadata = it)
            } ?: Spacer(Modifier.weight(1f))

            if (togetherSessionState !is TogetherSessionState.Idle) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.all_inclusive),
                        contentDescription = stringResource(R.string.music_together),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp).size(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            ModernLikeButton(isLiked = isLiked, onLikeClick = playerConnection::toggleLike)

            Spacer(modifier = Modifier.width(12.dp))

            ModernPlayPauseControl(
                isPlaying = isPlaying,
                playbackState = playbackState,
                isLoading = isLoading,
                playerConnection = playerConnection
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

// YAHAN PAR MAINE UPDATE KIYA HAI - CUSTOM CANVAS HATA DIYA
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernMiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(56.dp)
    ) {
        val progressVal = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        
        // Official Material 3 Wavy Progress Indicator
        CircularWavyProgressIndicator(
            progress = { progressVal },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        )

        Box(
            modifier = Modifier
                .padding(5.dp) // Thoda extra padding takki wavy indicator clear dikhe
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = mediaMetadata?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernPlayPauseControl(
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    playerConnection: PlayerConnection
) {
    val haptic = LocalHapticFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp) 
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (playbackState == Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.player.togglePlayPause()
                }
            }
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                painter = painterResource(
                    if (playbackState == Player.STATE_ENDED) {
                        R.drawable.replay
                    } else if (isPlaying) {
                        R.drawable.pause
                    } else {
                        R.drawable.play
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RowScope.ModernMiniPlayerInfo(mediaMetadata: MediaMetadata) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun ModernLikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isLiked) MaterialTheme.colorScheme.errorContainer 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLikeClick()
            }
    ) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
            ),
            contentDescription = null,
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -----------------------------------------------------------------
// LEGACY & ORIGINAL COMPONENTS
// -----------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerPlayPauseButton(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    playerConnection: PlayerConnection
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (playbackState == Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.player.togglePlayPause()
                }
            }
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                painter = painterResource(
                    if (playbackState == Player.STATE_ENDED) {
                        R.drawable.replay
                    } else if (isPlaying) {
                        R.drawable.pause
                    } else {
                        R.drawable.play
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun MiniPlayerActionButtons(
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLikeClick()
            }
    ) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
            ),
            contentDescription = null,
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MiniPlayerSubscribeButton(mediaMetadata: MediaMetadata) {
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    
    mediaMetadata.artists.firstOrNull()?.let { artistInfo ->
        artistInfo.id?.let { artistId ->
            val libraryArtist by database.artist(artistId).collectAsState(initial = null)
            val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isSubscribed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isSubscribed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        database.transaction {
                            val artist = libraryArtist?.artist
                            if (artist != null) {
                                update(artist.toggleLike())
                            } else {
                                insert(
                                    ArtistEntity(
                                        id = artistId,
                                        name = artistInfo.name,
                                        channelId = null,
                                        thumbnailUrl = null,
                                    ).toggleLike()
                                )
                            }
                        }
                    }
            ) {
                Icon(
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = null,
                    tint = if (isSubscribed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
