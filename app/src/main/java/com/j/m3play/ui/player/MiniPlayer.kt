package com.j.m3play.ui.player

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DarkModeKey
import com.j.m3play.constants.PureBlackKey
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt

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

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val liftedOffsetY = with(density) { (-10).dp.roundToPx() }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var swipeThresholdTriggered by remember { mutableStateOf(false) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    val autoSwipeThreshold = calculateAutoSwipeThreshold(0.73f)

    val rawProgress = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mini_progress"
    )

    val titleText = mediaMetadata?.title?.takeIf { it.isNotBlank() } ?: "Unknown title"
    val artistText = mediaMetadata?.artists
        ?.joinToString { it.name }
        ?.takeIf { it.isNotBlank() } ?: "Unknown artist"

    val containerColor by animateColorAsState(
        targetValue = if (useDarkTheme && pureBlack) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "mini_container"
    )

    val artworkBgColor = if (useDarkTheme) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progressColor = MaterialTheme.colorScheme.primary
    val progressTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val playButtonColor = MaterialTheme.colorScheme.primary
    val playIconColor = MaterialTheme.colorScheme.onPrimary
    val nextButtonColor = MaterialTheme.colorScheme.secondaryContainer
    val nextIconColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 14.dp, vertical = 0.dp)
    ) {
        Surface(
            modifier = Modifier
                .then(
                    if (isTabletLandscape) {
                        Modifier
                            .width(520.dp)
                            .align(Alignment.CenterEnd)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(78.dp)
                .offset {
                    IntOffset(
                        offsetXAnimatable.value.roundToInt(),
                        liftedOffsetY
                    )
                }
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canSkipNext, canSkipPrevious, layoutDirection) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                                swipeThresholdTriggered = false
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            },
                            onDragCancel = {
                                swipeThresholdTriggered = false
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = springSpec
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

                                    if (crossedThreshold && !swipeThresholdTriggered) {
                                        swipeThresholdTriggered = true
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    } else if (!crossedThreshold && swipeThresholdTriggered) {
                                        swipeThresholdTriggered = false
                                    }

                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(nextOffset)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) {
                                    totalDragDistance / dragDuration
                                } else {
                                    0f
                                }

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
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                swipeThresholdTriggered = false

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = springSpec
                                    )
                                }
                            }
                        )
                    }
                    .clickable {
                        // Full player open action yaha laga
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        color = progressColor,
                        trackColor = progressTrackColor
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(artworkBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val thumbnailUrl = mediaMetadata?.thumbnailUrl

                            if (thumbnailUrl != null) {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedContent(
                                targetState = titleText,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "mini_title"
                            ) { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedContent(
                                    targetState = if (error != null) "Error playing" else artistText,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "mini_artist"
                                ) { subtitle ->
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = subtitleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .basicMarquee()
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                MiniVisualizer(
                                    isPlaying = isPlaying,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(playButtonColor),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (playbackState == Player.STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        playerConnection.player.togglePlayPause()
                                    }
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying && playbackState != Player.STATE_ENDED) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = playIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(nextButtonColor),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    playerConnection.player.seekToNext()
                                },
                                enabled = canSkipNext,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    tint = if (canSkipNext) {
                                        nextIconColor
                                    } else {
                                        nextIconColor.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(
                        if (offsetXAnimatable.value > 0) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        }
                    )
                    .padding(horizontal = 22.dp)
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
                        alpha = (
                            offsetXAnimatable.value.absoluteValue / autoSwipeThreshold
                        ).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mini_visualizer")

    val bar1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 460, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val passiveAlpha = 0.35f

    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        VisualizerBar(
            heightFraction = if (isPlaying) bar1 else passiveAlpha,
            color = color
        )
        VisualizerBar(
            heightFraction = if (isPlaying) bar2 else 0.55f,
            color = color
        )
        VisualizerBar(
            heightFraction = if (isPlaying) bar3 else passiveAlpha,
            color = color
        )
    }
}

@Composable
private fun VisualizerBar(
    heightFraction: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(heightFraction.coerceIn(0.2f, 1f))
            .clip(RoundedCornerShape(99.dp))
            .background(color)
    )
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
                    .clip(RoundedCornerShape(14.dp)),
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
                            shape = RoundedCornerShape(14.dp),
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
