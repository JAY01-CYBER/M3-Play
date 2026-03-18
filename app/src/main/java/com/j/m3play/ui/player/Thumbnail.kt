package com.j.m3play.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.C
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.AppConfig
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val isAppleMusicStyle = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        else -> Color.White
    }

    val thumbnailLazyGridState = rememberLazyGridState()
    val timeline = playerConnection.player.currentTimeline
    val shuffleModeEnabled = playerConnection.player.shuffleModeEnabled

    val currentMediaItemIndex by playerConnection
        .currentMediaItemIndex
        .collectAsState()

    val mediaItems by remember(
        currentMediaItemIndex,
        timeline,
        shuffleModeEnabled,
        swipeThumbnail
    ) {
        derivedStateOf {

            val currentItem = playerConnection.player.currentMediaItem

            val previous = if (swipeThumbnail && !timeline.isEmpty) {
                val index = timeline.getPreviousWindowIndex(
                    currentMediaItemIndex,
                    Player.REPEAT_MODE_OFF,
                    shuffleModeEnabled
                )
                if (index != C.INDEX_UNSET)
                    playerConnection.player.getMediaItemAt(index)
                else null
            } else null

            val next = if (swipeThumbnail && !timeline.isEmpty) {
                val index = timeline.getNextWindowIndex(
                    currentMediaItemIndex,
                    Player.REPEAT_MODE_OFF,
                    shuffleModeEnabled
                )
                if (index != C.INDEX_UNSET)
                    playerConnection.player.getMediaItemAt(index)
                else null
            } else null

            listOfNotNull(previous, currentItem, next)
        }
    }

    val currentMediaIndex by remember(mediaItems) {
        derivedStateOf {
            mediaItems.indexOf(playerConnection.player.currentMediaItem)
        }
    }

    // FIX 1: Reaccionar tanto a currentMediaItemIndex como a mediaItems
    // para evitar la race condition donde mediaItems aún no se actualizó
    LaunchedEffect(currentMediaItemIndex, mediaItems) {
        val index = mediaItems.indexOf(playerConnection.player.currentMediaItem)
        if (index != -1) {
            thumbnailLazyGridState.animateScrollToItem(index)
        }
    }

    // FIX 2: Detectar cuando el usuario termina de swipear y notificar al player
    LaunchedEffect(thumbnailLazyGridState.isScrollInProgress) {
        if (!thumbnailLazyGridState.isScrollInProgress && mediaItems.isNotEmpty()) {
            val firstVisible = thumbnailLazyGridState.firstVisibleItemIndex
            val safeCurrentIndex = currentMediaIndex.takeIf { it != -1 } ?: return@LaunchedEffect
            when {
                firstVisible > safeCurrentIndex && canSkipNext -> playerConnection.seekToNext()
                firstVisible < safeCurrentIndex && canSkipPrevious -> playerConnection.seekToPrevious()
            }
        }
    }

    val snapProvider = remember(thumbnailLazyGridState) {
        SnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState
        ) { layoutSize, itemSize ->
            layoutSize / 2f - itemSize / 2f
        }
    }

    Box(modifier = modifier) {

        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (!isAppleMusicStyle) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.playing_from),
                            style = MaterialTheme.typography.titleMedium,
                            color = textBackgroundColor
                        )

                        val playingFrom = queueTitle ?: mediaMetadata?.album?.title
                        if (!playingFrom.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = playingFrom,
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val size = maxWidth - PlayerHorizontalPadding * 2

                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(1),
                        state = thumbnailLazyGridState,
                        flingBehavior = rememberSnapFlingBehavior(snapProvider),
                        userScrollEnabled = swipeThumbnail && isPlayerExpanded,
                        modifier = Modifier.fillMaxSize()
                    ) {

                        items(
                            items = mediaItems,
                            key = { it.mediaId }
                        ) { item ->

                            Box(
                                modifier = Modifier
                                    .width(maxWidth)
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { onOpenFullscreenLyrics() },
                                            onDoubleTap = { offset ->
                                                if (offset.x < size.toPx() / 2)
                                                    playerConnection.player.seekBack()
                                                else
                                                    playerConnection.player.seekForward()
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {

                                if (isAppleMusicStyle) {
                                    Box(Modifier.size(size))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(size)
                                            .clip(RoundedCornerShape(thumbnailCornerRadius.dp * 2))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        // FIX 3: crossfade para transición visual suave
                                        // y evitar que Coil muestre la imagen cacheada anterior
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(item.mediaMetadata.artworkUri)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .networkCachePolicy(CachePolicy.ENABLED)
                                                .crossfade(300)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        var showSeekEffect by remember { mutableStateOf(false) }
        var seekDirection by remember { mutableStateOf("") }

        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}