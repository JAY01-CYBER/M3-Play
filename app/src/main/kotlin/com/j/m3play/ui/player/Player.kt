package com.j.m3play.ui.player

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.navigation.NavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.*
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(state: BottomSheetState, navController: NavController, modifier: Modifier = Modifier, pureBlack: Boolean) {
    val context = LocalContext.current; val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; val menuState = LocalMenuState.current; val bottomSheetPageState = LocalBottomSheetPageState.current; val playerConnection = LocalPlayerConnection.current ?: return
    val playerDesignStyle by rememberEnumPreference(key = PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = true); val (showCodecOnPlayer) = rememberPreference(booleanPreferencesKey("show_codec_on_player"), false)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO); val useDarkTheme = remember(darkTheme, isSystemInDarkTheme()) { if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme() else darkTheme == DarkMode.ON }
    val playbackState by playerConnection.playbackState.collectAsState(); val isPlaying by playerConnection.isPlaying.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState(); val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState(); val canSkipNext by playerConnection.canSkipNext.collectAsState(); val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)
    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.currentPosition) }; var duration by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.duration) }; var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }; val isLoading = playbackState == STATE_BUFFERING || sliderPosition != null
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }; val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground != PlayerBackgroundStyle.DEFAULT && mediaMetadata?.thumbnailUrl != null) {
            val result = runCatching { withContext(Dispatchers.IO) { context.imageLoader.execute(ImageRequest.Builder(context).data(mediaMetadata!!.thumbnailUrl).allowHardware(false).build()) } }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap -> val palette = Palette.from(bitmap).generate(); gradientColors = PlayerColorExtractor.extractGradientColors(palette, MaterialTheme.colorScheme.surface.toArgb()) }
        }
    }

    val TextBackgroundColor = if (playerDesignStyle == PlayerDesignStyle.APPLE) Color.White else Color.White; val icBackgroundColor = if (playerDesignStyle == PlayerDesignStyle.APPLE) Color.Black else Color.Black
    val playerButtonsStyle by rememberEnumPreference(key = PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT); val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) { PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor); else -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary) }

    LaunchedEffect(mediaMetadata?.id, playbackState) { if (playbackState == STATE_READY) { while (isActive) { position = playerConnection.player.currentPosition; duration = playerConnection.player.duration; delay(100) } } }

    val dynamicQueuePeekHeight = if (playerDesignStyle == PlayerDesignStyle.V5 || playerDesignStyle == PlayerDesignStyle.APPLE) 0.dp else QueuePeekHeight
    val queueSheetState = rememberBottomSheetState(dismissedBound = dynamicQueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(), expandedBound = state.expandedBound, collapsedBound = dynamicQueuePeekHeight + 1.dp, initialAnchor = 1); val lyricsSheetState = rememberBottomSheetState(dismissedBound = 0.dp, expandedBound = state.expandedBound, collapsedBound = 0.dp, initialAnchor = 1)

    BottomSheet(state = state, modifier = modifier.focusRequester(remember { FocusRequester() }).focusable(), backgroundColor = if (useDarkTheme && pureBlack) Color.Black else MaterialTheme.colorScheme.surface, onDismiss = { playerConnection.service.stopAndClearPlayback() }, collapsedContent = { MiniPlayer(position = position, duration = duration, pureBlack = pureBlack) }) {
        if (!state.isCollapsed && playerDesignStyle != PlayerDesignStyle.V5 && playerDesignStyle != PlayerDesignStyle.APPLE) { PlayerBackground(playerBackground, mediaMetadata, gradientColors, disableBlur, "", 0f, 1f, 1f) }
        val onSliderValueChange: (Long) -> Unit = { sliderPosition = it }; val onSliderValueChangeFinished: () -> Unit = { sliderPosition?.let { playerConnection.player.seekTo(it) }; sliderPosition = null }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
            if (playerDesignStyle != PlayerDesignStyle.APPLE && playerDesignStyle != PlayerDesignStyle.V5) { Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) { Thumbnail(sliderPositionProvider = { sliderPosition }, isPlayerExpanded = state.isExpanded) } } else if (playerDesignStyle != PlayerDesignStyle.APPLE) { Spacer(Modifier.weight(1f)) }
            mediaMetadata?.let { metadata -> PlayerControlsContent(mediaMetadata = metadata, playerDesignStyle = playerDesignStyle, sliderStyle = sliderStyle, playbackState = playbackState, isPlaying = isPlaying, isLoading = isLoading, repeatMode = repeatMode, canSkipPrevious = canSkipPrevious, canSkipNext = canSkipNext, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, textBackgroundColor = TextBackgroundColor, icBackgroundColor = icBackgroundColor, sliderPosition = sliderPosition, position = position, duration = duration, playerConnection = playerConnection, navController = navController, state = state, menuState = menuState, bottomSheetPageState = bottomSheetPageState, clipboardManager = clipboardManager, context = context, onSliderValueChange = onSliderValueChange, onSliderValueChangeFinished = onSliderValueChangeFinished) }
            if (playerDesignStyle != PlayerDesignStyle.APPLE) Spacer(Modifier.height(30.dp))
        }
        Queue(state = queueSheetState, playerBottomSheetState = state, navController = navController, backgroundColor = if (useDarkTheme && pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer, onBackgroundColor = if (useDarkTheme && pureBlack) Color.White else MaterialTheme.colorScheme.onSurface, TextBackgroundColor = TextBackgroundColor, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, onShowLyrics = { lyricsSheetState.expandSoft() }, pureBlack = pureBlack)
    }
}
