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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.*
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerDesignStyle by rememberEnumPreference(PlayerDesignStyleKey, PlayerDesignStyle.V4)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    
    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(0L) }
    var duration by rememberLongStateOf(0L)
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val isUserSeeking = remember { mutableStateOf(false) }

    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val queueSheetState = rememberBottomSheetState(dismissedBound = dismissedBound, expandedBound = state.expandedBound, collapsedBound = dismissedBound + 1.dp, initialAnchor = 1)
    val lyricsSheetState = rememberBottomSheetState(dismissedBound = 0.dp, expandedBound = state.expandedBound, collapsedBound = 0.dp, initialAnchor = 1)

    
    val globalScale by animateFloatAsState(
        targetValue = if (queueSheetState.progress > 0.1f || lyricsSheetState.progress > 0.1f) 0.92f else 1f,
        animationSpec = tween(400, easing = LinearEasing), label = "globalScale"
    )
    val globalAlpha by animateFloatAsState(
        targetValue = if (queueSheetState.progress > 0.5f || lyricsSheetState.progress > 0.5f) 0.6f else 1f,
        label = "globalAlpha"
    )

    val TextBackgroundColor = Color.White
    val icBackgroundColor = Color.Black

    BottomSheet(
        state = state,
        backgroundColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
        onDismiss = { playerConnection.service.stopAndClearPlayback() },
        collapsedContent = { MiniPlayer(position = position, duration = duration, pureBlack = pureBlack) },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { metadata ->
            PlayerControlsContent(
                mediaMetadata = metadata, playerDesignStyle = playerDesignStyle, sliderStyle = sliderStyle, playbackState = playbackState,
                isPlaying = isPlaying, isLoading = false, repeatMode = repeatMode, canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext, textButtonColor = TextBackgroundColor, iconButtonColor = icBackgroundColor,
                textBackgroundColor = TextBackgroundColor, icBackgroundColor = icBackgroundColor, sliderPosition = sliderPosition,
                position = position, duration = duration, playerConnection = playerConnection, navController = navController,
                state = state, menuState = menuState, bottomSheetPageState = bottomSheetPageState, clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager,
                context = context, onSliderValueChange = { sliderPosition = it }, onSliderValueChangeFinished = { playerConnection.player.seekTo(sliderPosition ?: 0L); sliderPosition = null }, currentFormat = currentFormat
            )
        }

        if (playerDesignStyle == PlayerDesignStyle.V1) {
            
            Box(modifier = Modifier.fillMaxSize().padding(bottom = queueSheetState.collapsedBound)) {
                // LAYER 1: Background Blur
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(100.dp)
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

                // LAYER 2: Scalable Content Layer (Shrinks when sheets open)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = globalScale; scaleY = globalScale; alpha = globalAlpha }
                ) {
                    Spacer(Modifier.height(20.dp))
                    // Artwork Section
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Thumbnail(sliderPositionProvider = { sliderPosition }, isPlayerExpanded = state.isExpanded)
                    }
                    
                    
                    Column(modifier = Modifier.padding(bottom = 32.dp)) {
                        mediaMetadata?.let { controlsContent(it) }
                    }
                }
                
                
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))))
            }
        } else {
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(bottom = queueSheetState.collapsedBound)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Thumbnail(sliderPositionProvider = { sliderPosition }, isPlayerExpanded = state.isExpanded)
                }
                mediaMetadata?.let { controlsContent(it) }
                Spacer(Modifier.height(30.dp))
            }
        }

        Queue(state = queueSheetState, playerBottomSheetState = state, navController = navController, backgroundColor = Color.Black, onBackgroundColor = Color.White, TextBackgroundColor = Color.White, textButtonColor = Color.White, iconButtonColor = Color.Black, onShowLyrics = { lyricsSheetState.expandSoft() }, pureBlack = pureBlack)

        mediaMetadata?.let { metadata ->
            BottomSheet(state = lyricsSheetState, backgroundColor = Color.Unspecified, onDismiss = { }, collapsedContent = { }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = lyricsSheetState.progress))) {
                    LyricsScreen(mediaMetadata = metadata, onBackClick = { lyricsSheetState.collapseSoft() }, navController = navController)
                }
            }
        }
    }
}
