package com.j.m3play.ui.player

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.models.MediaMetadata
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.ui.component.*
import com.j.m3play.extensions.togglePlayPause

@Composable
fun ApplePlayerStyle(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(model = mediaMetadata.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)), startY = 600f)))
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Bottom) {
            Text(text = mediaMetadata.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = mediaMetadata.artists.joinToString { it.name }, color = Color.White.copy(0.6f), fontSize = 18.sp, maxLines = 1)
            Spacer(Modifier.height(32.dp))
            Slider(
                value = if (duration > 0) position.toFloat() / duration else 0f,
                onValueChange = { onSeek((it * duration).toLong()) },
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.White)
            )
            Spacer(Modifier.height(40.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(painterResource(R.drawable.apple_prev), null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    if (isLoading) CircularProgressIndicator(color = Color.White)
                    else Icon(painterResource(if (isPlaying) R.drawable.apple_pause else R.drawable.apple_play), null, tint = Color.White, modifier = Modifier.fillMaxSize(0.7f))
                }
                IconButton(onClick = onNext) { Icon(painterResource(R.drawable.apple_next), null, tint = Color.White, modifier = Modifier.size(44.dp)) }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    sliderStyle: SliderStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit
) {
    if (playerDesignStyle == PlayerDesignStyle.APPLE) {
        ApplePlayerStyle(mediaMetadata, isPlaying, isLoading, position, duration, 
            { playerConnection.player.togglePlayPause() }, playerConnection::seekToNext, playerConnection::seekToPrevious, onSliderValueChange)
        return
    }
    // Purana logic (Simplified for Build)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        PlayerTitleSection(mediaMetadata, textBackgroundColor, navController, state, clipboardManager, context)
        StyledPlaybackSlider(sliderStyle, (sliderPosition ?: position).toFloat(), 0f..duration.toFloat(), { onSliderValueChange(it.toLong()) }, onSliderValueChangeFinished, textButtonColor, isPlaying)
        PlayerPlaybackControls(playerDesignStyle, playbackState, isPlaying, isLoading, repeatMode, canSkipPrevious, canSkipNext, textButtonColor, iconButtonColor, textBackgroundColor, icBackgroundColor, 24.dp, playerConnection, false)
    }
}

@Composable
fun StyledPlaybackSlider(sliderStyle: SliderStyle, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit, activeColor: Color, isPlaying: Boolean, modifier: Modifier = Modifier) {
    Slider(value = value.coerceIn(valueRange), onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, valueRange = valueRange, modifier = modifier)
}

@Composable
fun PlayerBackground(playerBackground: PlayerBackgroundStyle, mediaMetadata: MediaMetadata?, gradientColors: List<Color>, disableBlur: Boolean, playerCustomImageUri: String, playerCustomBlur: Float, playerCustomContrast: Float, playerCustomBrightness: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (mediaMetadata?.thumbnailUrl != null) {
            AsyncImage(model = mediaMetadata.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(if (disableBlur) 0.dp else 40.dp))
        }
    }
}
