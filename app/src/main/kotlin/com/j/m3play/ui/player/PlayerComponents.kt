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

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.PlayerMenu
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.ui.theme.PlayerBackgroundColorUtils
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ShowMediaInfo
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerDesignStyle by rememberEnumPreference(key = PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)

    val (playerCustomImageUri) = rememberPreference(PlayerCustomImageUriKey, "")
    val (playerCustomBlur) = rememberPreference(PlayerCustomBlurKey, 0f)
    val (playerCustomContrast) = rememberPreference(PlayerCustomContrastKey, 1f)
    val (playerCustomBrightness) = rememberPreference(PlayerCustomBrightnessKey, 1f)
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showCodecOnPlayer) = rememberPreference(booleanPreferencesKey("show_codec_on_player"), false)

    val playerButtonsStyle by rememberEnumPreference(key = PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme()) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme() else darkTheme == DarkMode.ON
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)

    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }
    var isUserSeeking by remember(mediaMetadata?.id) { mutableStateOf(false) }
    val isLoading = playbackState == STATE_BUFFERING || sliderPosition != null

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground != PlayerBackgroundStyle.DEFAULT && mediaMetadata?.thumbnailUrl != null) {
            val cached = gradientColorsCache[mediaMetadata!!.id]
            if (cached != null) {
                gradientColors = cached
            } else {
                val request = ImageRequest.Builder(context).data(mediaMetadata!!.thumbnailUrl).allowHardware(false).build()
                val result = runCatching { withContext(Dispatchers.IO) { context.imageLoader.execute(request) } }.getOrNull()
                result?.image?.toBitmap()?.let { bitmap ->
                    val palette = Palette.from(bitmap).generate()
                    val extracted = PlayerColorExtractor.extractGradientColors(palette, MaterialTheme.colorScheme.surface.toArgb())
                    gradientColorsCache[mediaMetadata!!.id] = extracted
                    gradientColors = extracted
                }
            }
        }
    }

    val TextBackgroundColor = if (playerDesignStyle == PlayerDesignStyle.APPLE) Color.White else Color.White
    val icBackgroundColor = if (playerDesignStyle == PlayerDesignStyle.APPLE) Color.Black else Color.Black

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
    }

    LaunchedEffect(mediaMetadata?.id, playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                delay(100)
            }
        }
    }

    val dynamicQueuePeekHeight = if (playerDesignStyle == PlayerDesignStyle.V5 || playerDesignStyle == PlayerDesignStyle.APPLE) 0.dp else if (showCodecOnPlayer) 88.dp else QueuePeekHeight
    val dismissedBound = dynamicQueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(dismissedBound = dismissedBound, expandedBound = state.expandedBound, collapsedBound = dismissedBound + 1.dp, initialAnchor = 1)
    val lyricsSheetState = rememberBottomSheetState(dismissedBound = 0.dp, expandedBound = state.expandedBound, collapsedBound = 0.dp, initialAnchor = 1)

    BottomSheet(
        state = state,
        modifier = modifier.focusRequester(remember { FocusRequester() }).focusable(),
        backgroundColor = if (useDarkTheme && pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
        onDismiss = { playerConnection.service.stopAndClearPlayback() },
        collapsedContent = { MiniPlayer(position = position, duration = duration, pureBlack = pureBlack) },
    ) {
        // BACKGROUND
        if (!state.isCollapsed && playerDesignStyle != PlayerDesignStyle.V5 && playerDesignStyle != PlayerDesignStyle.APPLE) {
            PlayerBackground(
                playerBackground = playerBackground,
                mediaMetadata = mediaMetadata,
                gradientColors = gradientColors,
                disableBlur = disableBlur,
                playerCustomImageUri = playerCustomImageUri,
                playerCustomBlur = playerCustomBlur,
                playerCustomContrast = playerCustomContrast,
                playerCustomBrightness = playerCustomBrightness
            )
        }

        val onSliderValueChange: (Long) -> Unit = { isUserSeeking = true; sliderPosition = it }
        val onSliderValueChangeFinished: () -> Unit = { 
            sliderPosition?.let { playerConnection.player.seekTo(it) }
            isUserSeeking = false 
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        ) {
            // THUMBNAIL (Hided for Apple Style)
            if (playerDesignStyle != PlayerDesignStyle.APPLE && playerDesignStyle != PlayerDesignStyle.V5) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                    Thumbnail(
                        sliderPositionProvider = { sliderPosition },
                        modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                        isPlayerExpanded = state.isExpanded
                    )
                }
            } else if (playerDesignStyle != PlayerDesignStyle.APPLE) {
                Spacer(Modifier.weight(1f))
            }

            // CONTROLS
            mediaMetadata?.let { metadata ->
                PlayerControlsContent(
                    mediaMetadata = metadata,
                    playerDesignStyle = playerDesignStyle,
                    sliderStyle = sliderStyle,
                    playbackState = playbackState,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    repeatMode = repeatMode,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    textBackgroundColor = TextBackgroundColor,
                    icBackgroundColor = icBackgroundColor,
                    sliderPosition = sliderPosition,
                    position = position,
                    duration = duration,
                    playerConnection = playerConnection,
                    navController = navController,
                    state = state,
                    menuState = menuState,
                    bottomSheetPageState = bottomSheetPageState,
                    clipboardManager = clipboardManager,
                    context = context,
                    onSliderValueChange = onSliderValueChange,
                    onSliderValueChangeFinished = onSliderValueChangeFinished,
                )
            }

            if (playerDesignStyle != PlayerDesignStyle.APPLE) {
                Spacer(Modifier.height(30.dp))
            }
        }

        val queueOnBackgroundColor = if (useDarkTheme && pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor = if (useDarkTheme && pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
            onBackgroundColor = queueOnBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            onShowLyrics = { lyricsSheetState.expandSoft() },
            pureBlack = pureBlack,
        )
    }
}

@Composable
private fun LittlePlayerContent(
    mediaMetadata: MediaMetadata,
    sliderPosition: Long?,
    positionMs: Long,
    durationMs: Long,
    textColor: Color,
    liked: Boolean,
    onCollapse: () -> Unit,
    onToggleLike: () -> Unit,
    onExpandQueue: () -> Unit,
    onMenuClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / 420.dp, maxHeight / 260.dp).coerceIn(0.78f, 1.15f)
        val displayPositionMs = sliderPosition ?: positionMs

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = (18f * scale).dp, vertical = (10f * scale).dp)) {
            Spacer(Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mediaMetadata.title, color = textColor, fontSize = (56f * scale).sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(text = mediaMetadata.artists.joinToString { it.name }, color = textColor.copy(0.6f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Text(text = makeTimeString(displayPositionMs), color = textColor, fontSize = (44f * scale).sp, textAlign = TextAlign.End)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) { Icon(painterResource(R.drawable.expand_more), null, tint = textColor) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleLike) { Icon(painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (liked) Color.Red else textColor) }
                IconButton(onClick = onExpandQueue) { Icon(painterResource(R.drawable.queue_music), null, tint = textColor) }
                IconButton(onClick = onMenuClick) { Icon(painterResource(R.drawable.more_vert), null, tint = textColor) }
            }
        }
    }
}

private fun Modifier.littlePlayerOverlayGestures(
    seekEnabled: Boolean,
    durationMs: Long,
    progressFraction: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeekToPositionMs: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
): Modifier = pointerInput(seekEnabled, durationMs) {
    awaitEachGesture {
        val down = awaitFirstDown()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            // Logic for seeking simplified
            change.consume()
        }
        onSeekFinished()
    }
}
