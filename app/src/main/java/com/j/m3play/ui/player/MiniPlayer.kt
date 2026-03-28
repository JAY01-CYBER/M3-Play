package com.j.m3play.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DarkModeKey
import com.j.m3play.constants.DefaultMiniPlayerThumbnailShape
import com.j.m3play.constants.MiniPlayerThumbnailShapeKey
import com.j.m3play.constants.PureBlackKey
import com.j.m3play.constants.ThumbnailCornerRadius
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.utils.Haptics
import com.j.m3play.utils.getMiniPlayerThumbnailShape
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val miniPlayerThumbnailShapeState = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val miniPlayerThumbnailShape = remember(miniPlayerThumbnailShapeState.value, isPlaying) {
        if (isPlaying) {
            getMiniPlayerThumbnailShape(miniPlayerThumbnailShapeState.value)
        } else {
            MaterialShapes.Circle
        }
    }

    val currentThumbnailShape = remember(isPlaying, miniPlayerThumbnailShape) {
        if (isPlaying) miniPlayerThumbnailShape else MaterialShapes.Square
    }.toShape()

    val miniPlayerBackgroundColor = when {
        useDarkTheme && pureBlack -> MaterialTheme.colorScheme.surfaceContainer
        useDarkTheme -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var thresholdTriggered by remember { mutableStateOf(false) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.0f else 0.34f,
        label = "overlay_alpha",
        animationSpec = animationSpec
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    val autoSwipeThreshold = calculateAutoSwipeThreshold(0.73f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .padding(bottom = 10.dp)
            .background(Color.Transparent)
    ) {
        Surface(
            modifier = Modifier
                .then(
                    if (isTabletLandscape) {
                        Modifier
                            .width(480.dp)
                            .align(Alignment.CenterEnd)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(68.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false
                ),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(miniPlayerBackgroundColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable {
                        // full player open karna ho to yaha action laga
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                                thresholdTriggered = false
                                Haptics.tick(haptic = haptic, context = context)
                            },
                            onDragCancel = {
                                thresholdTriggered = false
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
                                    totalDragDistance += adjustedDragAmount.absoluteValue
                                    val nextOffset = offsetXAnimatable.value + adjustedDragAmount
                                    val crossedThreshold = nextOffset.absoluteValue > 72f

                                    if (crossedThreshold && !thresholdTriggered) {
                                        thresholdTriggered = true
                                        Haptics.longPress(haptic = haptic, context = context)
                                    } else if (!crossedThreshold && thresholdTriggered) {
                                        thresholdTriggered = false
                                    }

                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(nextOffset)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity =
                                    if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (0.73f * -8.25f) + 8.5f

                                val shouldChangeSong =
                                    (currentOffset.absoluteValue > minDistanceThreshold &&
                                        velocity > velocityThreshold) ||
                                        (currentOffset.absoluteValue > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0

                                    if (isRightSwipe && canSkipPrevious) {
                                        Haptics.success(context)
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        Haptics.success(context)
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                thresholdTriggered = false

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (duration > 0) {
                            CircularProgressIndicator(
                                progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                trackColor = Color.Transparent
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(currentThumbnailShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                    shape = currentThumbnailShape
                                )
                                .clickable {
                                    Haptics.click(haptic = haptic, context = context)
                                    if (playbackState == Player.STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        playerConnection.player.togglePlayPause()
                                    }
                                }
                        ) {
                            val thumbnailUrl = mediaMetadata?.thumbnailUrl

                            if (thumbnailUrl != null) {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(currentThumbnailShape)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = Color.Black.copy(alpha = overlayAlpha),
                                        shape = currentThumbnailShape
                                    )
                            )

                            androidx.compose.animation.AnimatedVisibility(
                                visible = playbackState == Player.STATE_ENDED || !isPlaying,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (playbackState == Player.STATE_ENDED) {
                                                R.drawable.replay
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = mediaMetadata?.title ?: "Unknown title",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "mini_title",
                        ) { title ->
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }

                        val subtitle = when {
                            error != null -> "Error playing"
                            mediaMetadata?.artists?.any { it.name.isNotBlank() } == true ->
                                mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
                            else -> "Unknown artist"
                        }

                        AnimatedContent(
                            targetState = subtitle,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "mini_artist",
                        ) { artists ->
                            Text(
                                text = artists,
                                color = if (error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                },
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            Haptics.click(haptic = haptic, context = context)
                            playerConnection.toggleLike()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (currentSong?.song?.liked == true) {
                                    R.drawable.favorite
                                } else {
                                    R.drawable.favorite_border
                                }
                            ),
                            contentDescription = if (currentSong?.song?.liked == true) "Unlike" else "Like",
                            tint = if (currentSong?.song?.liked == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            enabled = canSkipNext,
                            onClick = {
                                Haptics.click(haptic = haptic, context = context)
                                playerConnection.player.seekToNext()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (canSkipNext) 0.82f else 0.35f
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(
                        if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd
                    )
                    .padding(horizontal = 24.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) {
                            R.drawable.skip_previous
                        } else {
                            R.drawable.skip_next
                        }
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold)
                            .coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: androidx.media3.common.PlaybackException?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
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
    }
}
