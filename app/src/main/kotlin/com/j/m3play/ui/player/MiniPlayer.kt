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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.MiniPlayerHeight
import com.j.m3play.constants.SwipeSensitivityKey
import com.j.m3play.constants.ThumbnailCornerRadius
import com.j.m3play.constants.MiniPlayerStyle
import com.j.m3play.constants.MiniPlayerStyleKey
import com.j.m3play.constants.CropThumbnailToSquareKey
import com.j.m3play.constants.PlayerBackgroundStyle
import com.j.m3play.db.entities.ArtistEntity
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.rememberEnumPreference
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val miniPlayerStyle by rememberEnumPreference(MiniPlayerStyleKey, MiniPlayerStyle.MODERN)

    when (miniPlayerStyle) {
        MiniPlayerStyle.MODERN -> NewMiniPlayer(position, duration, modifier, pureBlack)
        MiniPlayerStyle.LEGACY -> LegacyMiniPlayer(position, duration, modifier, pureBlack)
        MiniPlayerStyle.MINIMAL -> MinimalMiniPlayer(position, duration, modifier, pureBlack)
        MiniPlayerStyle.FLOATING -> FloatingPillMiniPlayer(position, duration, modifier, pureBlack)
        MiniPlayerStyle.APPLE_MUSIC -> AppleMusicMiniPlayer(position, duration, modifier, pureBlack)
    }
}

// 🔥 EXACT APPLE MUSIC LIQUID GLASS PILL STYLE 🔥
@Composable
private fun AppleMusicMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.j.m3play.constants.SwipeThumbnailKey, true)
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isLoading = playbackState == Player.STATE_BUFFERING
    val haptic = LocalHapticFeedback.current
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    SwipeableMiniPlayerBox(
        // iOS style margins
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp) 
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .shadow(
                    elevation = 12.dp, 
                    shape = CircleShape, // PILL SHAPE
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    ambientColor = Color.Black.copy(alpha = 0.1f)
                )
                .clip(CircleShape) // PILL SHAPE CLIP
                // 🔥 LIQUID GLASS EFFECT (TRANSLUCENT) 🔥
                .background(
                    if (pureBlack) Color.DarkGray.copy(alpha = 0.4f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) 
                )
                // Faint border edge reflection
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        ) {
            // Subtle progress bar
            LinearProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).alpha(0.6f),
                color = MaterialTheme.colorScheme.onSurface,
                trackColor = Color.Transparent,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                // Square Artwork with rounded corners
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track Info Left Aligned
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = mediaMetadata?.title ?: "Unknown",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600, fontSize = 15.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                // Clean Play/Pause Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(
                                if (playbackState == Player.STATE_ENDED) R.drawable.replay
                                else if (isPlaying) R.drawable.pause
                                else R.drawable.play
                            ),
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Clean Next Button
                IconButton(
                    enabled = canSkipNext,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerConnection.player.seekToNext()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canSkipNext) 1f else 0.3f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun NewMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.j.m3play.constants.SwipeThumbnailKey, true)
    
    val miniPlayerBackground by rememberEnumPreference(
        stringPreferencesKey("mini_player_background_style"), 
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val (gradientColors, onGradientColorsChange) = remember { mutableStateOf<List<Color>>(emptyList()) }

    MiniPlayerColorExtractor(
        mediaMetadata = mediaMetadata,
        miniPlayerBackground = miniPlayerBackground,
        onGradientColorsChange = onGradientColorsChange
    )

    SwipeableMiniPlayerBox(
        modifier = modifier.padding(bottom = 8.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .clip(RoundedCornerShape(34.dp))
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            MiniPlayerBackgroundLayer(
                style = miniPlayerBackground,
                mediaMetadata = mediaMetadata,
                gradientColors = gradientColors
            )

            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection
            )
        }
    }
}

@Composable
private fun LegacyMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    
    val isLoading = playbackState == STATE_BUFFERING
    
    val currentView = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.j.m3play.constants.SwipeThumbnailKey, true)
    
    val miniPlayerBackground by rememberEnumPreference(
        stringPreferencesKey("mini_player_background_style"), 
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val (gradientColors, onGradientColorsChange) = remember { mutableStateOf<List<Color>>(emptyList()) }

    MiniPlayerColorExtractor(
        mediaMetadata = mediaMetadata,
        miniPlayerBackground = miniPlayerBackground,
        onGradientColorsChange = onGradientColorsChange
    )

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

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
            .background(
                if (pureBlack) 
                    Color.Black 
                else 
                    MaterialTheme.colorScheme.surfaceContainer
            )
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
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    val isRightSwipe = currentOffset > 0
                                    
                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
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
        MiniPlayerBackgroundLayer(
            style = miniPlayerBackground,
            mediaMetadata = mediaMetadata,
            gradientColors = gradientColors
        )

        LinearProgressIndicator(
            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    LegacyMiniMediaInfo(
                        mediaMetadata = it,
                        error = error,
                        pureBlack = pureBlack,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
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
                            },
                        ),
                        contentDescription = null,
                    )
                }
            }

            IconButton(
                enabled = canSkipNext,
                onClick = playerConnection::seekToNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                )
            }
        }
        
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
private fun MinimalMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.j.m3play.constants.SwipeThumbnailKey, true)
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isLoading = playbackState == Player.STATE_BUFFERING
    val haptic = LocalHapticFeedback.current

    SwipeableMiniPlayerBox(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = mediaMetadata?.title ?: "Unknown",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                IconButton(
                    onClick = {
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(
                                if (playbackState == Player.STATE_ENDED) R.drawable.replay
                                else if (isPlaying) R.drawable.pause
                                else R.drawable.play
                            ),
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun FloatingPillMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.j.m3play.constants.SwipeThumbnailKey, true)
    
    val miniPlayerBackground by rememberEnumPreference(
        stringPreferencesKey("mini_player_background_style"), 
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val (gradientColors, onGradientColorsChange) = remember { mutableStateOf<List<Color>>(emptyList()) }

    MiniPlayerColorExtractor(
        mediaMetadata = mediaMetadata,
        miniPlayerBackground = miniPlayerBackground,
        onGradientColorsChange = onGradientColorsChange
    )

    SwipeableMiniPlayerBox(
        modifier = modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            MiniPlayerBackgroundLayer(
                style = miniPlayerBackground,
                mediaMetadata = mediaMetadata,
                gradientColors = gradientColors
            )
            
            LinearProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )

            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection
            )
        }
    }
}

@Composable
private fun LegacyMiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val cropThumbnailToSquare by rememberPreference(CropThumbnailToSquareKey, false)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (cropThumbnailToSquare) it.aspectRatio(1f) else it }
                    .graphicsLayer(
                        renderEffect = BlurEffect(
                            radiusX = 75f,
                            radiusY = 75f
                        ),
                        alpha = 0.5f
                    )
            )

            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = if (cropThumbnailToSquare) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (cropThumbnailToSquare) it.aspectRatio(1f) else it }
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { title ->
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }

            AnimatedContent(
                targetState = mediaMetadata.artists.joinToString { it.name },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { artists ->
                Text(
                    text = artists,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// ISOLATED BACKGROUND EXTRACTOR COMPOSABLES
// ============================================================================

@Composable
fun MiniPlayerColorExtractor(
    mediaMetadata: MediaMetadata?,
    miniPlayerBackground: PlayerBackgroundStyle,
    onGradientColorsChange: (List<Color>) -> Unit
) {
    val context = LocalContext.current
    val fallbackColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
        if (miniPlayerBackground == PlayerBackgroundStyle.GRADIENT || 
            miniPlayerBackground == PlayerBackgroundStyle.GLOW_ANIMATED ||
            miniPlayerBackground == PlayerBackgroundStyle.GLOW) {
            
            val currentMetadata = mediaMetadata
            if (currentMetadata?.thumbnailUrl != null) {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = if (miniPlayerBackground == PlayerBackgroundStyle.GLOW_ANIMATED || miniPlayerBackground == PlayerBackgroundStyle.GLOW) {
                                listOfNotNull(
                                    palette.getVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getLightVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getDarkVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getMutedColor(fallbackColor).let { Color(it) },
                                    palette.getLightMutedColor(fallbackColor).let { Color(it) },
                                    palette.getDarkMutedColor(fallbackColor).let { Color(it) }
                                ).distinct()
                            } else {
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor
                                )
                            }
                            withContext(Dispatchers.Main) { onGradientColorsChange(extractedColors) }
                        }
                    }
                }
            }
        } else {
            onGradientColorsChange(emptyList())
        }
    }
}

@Composable
fun MiniPlayerBackgroundLayer(
    style: PlayerBackgroundStyle,
    mediaMetadata: MediaMetadata?,
    gradientColors: List<Color>
) {
    val context = LocalContext.current
    
    when (style) {
        PlayerBackgroundStyle.BLUR -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaMetadata?.thumbnailUrl)
                    .size(100, 100)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(100.dp)
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        }
        PlayerBackgroundStyle.BLUR_GRADIENT -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaMetadata?.thumbnailUrl)
                    .size(100, 100)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(100.dp)
            )
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.6f),
                    0.4f to Color.Black.copy(alpha = 0.3f),
                    1.0f to Color.Black.copy(alpha = 0.7f)
                )
            ))
        }
        PlayerBackgroundStyle.APPLE_MUSIC -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaMetadata?.thumbnailUrl)
                    .size(128, 128)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(150.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }
        PlayerBackgroundStyle.LIVE_MESH -> {
            val infiniteTransition = rememberInfiniteTransition(label = "liveMesh")
            val rotation = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(60000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.5f
                        scaleY = 1.5f
                    }
            ) {
                val matrix = remember { ColorMatrix().apply { setToSaturation(1.6f) } }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .size(128, 128)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(matrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp)
                        .graphicsLayer { rotationZ = rotation.value }
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }
        }
        PlayerBackgroundStyle.GRADIENT -> {
            if (gradientColors.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(gradientColors))
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            }
        }
        PlayerBackgroundStyle.GLOW -> {
            if (gradientColors.isNotEmpty()) {
                val colors = gradientColors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val width = size.width
                            val height = size.height
                            
                            val c1 = colors.getOrElse(0) { Color.DarkGray }
                            val c2 = colors.getOrElse(1) { c1 }

                            val b1 = Brush.radialGradient(
                                colors = listOf(c1.copy(alpha = 0.8f), Color.Transparent),
                                center = Offset(width * 0.2f, height * 0.5f),
                                radius = width * 1.2f
                            )
                            val b2 = Brush.radialGradient(
                                colors = listOf(c2.copy(alpha = 0.7f), Color.Transparent),
                                center = Offset(width * 0.8f, height * 0.5f),
                                radius = width * 1.0f
                            )
                            
                            drawRect(Color(0xFF050505))
                            drawRect(b1)
                            drawRect(b2)
                        }
                )
            }
        }
        PlayerBackgroundStyle.GLOW_ANIMATED -> {
            if (gradientColors.isNotEmpty()) {
                val infiniteTransition = rememberInfiniteTransition(label = "GlowAnimation")
                val progress = infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "glowProgress"
                )

                val colors = gradientColors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val p = progress.value
                            val width = size.width
                            val height = size.height
                            
                            fun rotatedColorAt(index: Int): Color {
                                val size = colors.size
                                val idx = index.toFloat() + p * size
                                val a = kotlin.math.floor(idx).toInt() % size
                                val b = (a + 1) % size
                                val frac = idx - kotlin.math.floor(idx)
                                return androidx.compose.ui.graphics.lerp(colors[a], colors[b], frac)
                            }

                            fun oscillate(min: Float, max: Float, phase: Float): Float {
                                val v = kotlin.math.sin(2f * Math.PI.toFloat() * (p + phase))
                                return min + (max - min) * ((v + 1f) * 0.5f)
                            }

                            val c1 = rotatedColorAt(0)
                            val c2 = rotatedColorAt(1)

                            val o1x = oscillate(0.0f, 1.0f, 0.0f)
                            val o1y = oscillate(0.0f, 0.5f, 0.1f)
                            val o2x = oscillate(1.0f, 0.0f, 0.2f)
                            val o2y = oscillate(0.5f, 1.0f, 0.3f)

                            val b1 = Brush.radialGradient(
                                colors = listOf(c1.copy(alpha = 0.8f), Color.Transparent),
                                center = Offset(width * o1x, height * o1y),
                                radius = width * 1.2f
                            )
                            val b2 = Brush.radialGradient(
                                colors = listOf(c2.copy(alpha = 0.7f), Color.Transparent),
                                center = Offset(width * o2x, height * o2y),
                                radius = width * 1.0f
                            )
                            
                            drawRect(Color(0xFF050505))
                            drawRect(b1)
                            drawRect(b2)
                        }
                )
            }
        }
        else -> {}
    }
}
