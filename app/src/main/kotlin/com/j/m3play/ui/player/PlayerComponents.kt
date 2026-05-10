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
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.FormatEntity
import com.j.m3play.models.MediaMetadata
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.PlayerMenu
import com.j.m3play.ui.theme.PlayerSliderColors
import com.j.m3play.ui.utils.ShowMediaInfo
import com.j.m3play.utils.makeTimeString

@Composable
fun PlayerTitleSection(mediaMetadata: MediaMetadata, textBackgroundColor: Color, navController: NavController, state: BottomSheetState, clipboardManager: ClipboardManager, context: Context) {
    Column {
        Text(text = mediaMetadata.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, color = textBackgroundColor, modifier = Modifier.basicMarquee())
        Text(text = mediaMetadata.artists.joinToString { it.name }, style = MaterialTheme.typography.titleMedium, color = textBackgroundColor.copy(alpha = 0.7f), maxLines = 1, modifier = Modifier.basicMarquee())
    }
}

@Composable
fun PlayerTopActions(mediaMetadata: MediaMetadata, playerDesignStyle: PlayerDesignStyle, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, playerConnection: PlayerConnection, navController: NavController, menuState: MenuState, state: BottomSheetState, bottomSheetPageState: BottomSheetPageState, context: Context, currentSongLiked: Boolean) {
    if (playerDesignStyle == PlayerDesignStyle.V1) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(onClick = { playerConnection.toggleLike() }, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (currentSongLiked) Color.Red else Color.White, modifier = Modifier.size(18.dp)) }
            }
            Surface(onClick = { menuState.show { PlayerMenu(mediaMetadata, navController, state, { bottomSheetPageState.show { ShowMediaInfo(mediaMetadata.id) } }, { menuState.dismiss() }) } }, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.more_horiz), null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
    } else {
        // Original Actions for other styles
    }
}

@Composable
fun PlayerTimeLabel(sliderPosition: Long?, position: Long, duration: Long, textBackgroundColor: Color, currentFormat: FormatEntity? = null, playerDesignStyle: PlayerDesignStyle) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)) {
        Text(makeTimeString(sliderPosition ?: position), style = MaterialTheme.typography.labelMedium, color = textBackgroundColor.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterStart))
        if (playerDesignStyle == PlayerDesignStyle.V1 && currentFormat != null) {
            Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.align(Alignment.Center)) {
                Text("Lossless", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        val remaining = duration - (sliderPosition ?: position)
        Text(if (duration > 0) "-${makeTimeString(remaining)}" else "", style = MaterialTheme.typography.labelMedium, color = textBackgroundColor.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun PlayerPlaybackControls(playerDesignStyle: PlayerDesignStyle, playbackState: Int, isPlaying: Boolean, isLoading: Boolean, repeatMode: Int, canSkipPrevious: Boolean, canSkipNext: Boolean, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, icBackgroundColor: Color, playPauseRoundness: Dp, playerConnection: PlayerConnection, currentSongLiked: Boolean) {
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    if (playerDesignStyle == PlayerDesignStyle.V1) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.shuffle), null, tint = Color.White.copy(alpha = if (shuffleModeEnabled) 1f else 0.4f), modifier = Modifier.size(24.dp).clickable { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled })
            Icon(painterResource(R.drawable.skip_previous), null, tint = Color.White, modifier = Modifier.size(48.dp).clickable { playerConnection.seekToPrevious() })
            Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = Color.White, modifier = Modifier.size(72.dp).clickable { playerConnection.player.togglePlayPause() })
            Icon(painterResource(R.drawable.skip_next), null, tint = Color.White, modifier = Modifier.size(48.dp).clickable { playerConnection.seekToNext() })
            Icon(painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat), null, tint = Color.White.copy(alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f), modifier = Modifier.size(24.dp).clickable { playerConnection.player.toggleRepeatMode() })
        }
    } else {
        // Original Controls for other styles
    }
}

@Composable
fun PlayerControlsContent(mediaMetadata: MediaMetadata, playerDesignStyle: PlayerDesignStyle, sliderStyle: SliderStyle, playbackState: Int, isPlaying: Boolean, isLoading: Boolean, repeatMode: Int, canSkipPrevious: Boolean, canSkipNext: Boolean, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, icBackgroundColor: Color, sliderPosition: Long?, position: Long, duration: Long, playerConnection: PlayerConnection, navController: NavController, state: BottomSheetState, menuState: MenuState, bottomSheetPageState: BottomSheetPageState, clipboardManager: ClipboardManager, context: Context, onSliderValueChange: (Long) -> Unit, onSliderValueChangeFinished: () -> Unit, currentFormat: FormatEntity? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PlayerTitleSection(mediaMetadata, textBackgroundColor, navController, state, clipboardManager, context)
            PlayerTopActions(mediaMetadata, playerDesignStyle, textButtonColor, iconButtonColor, textBackgroundColor, playerConnection, navController, menuState, state, bottomSheetPageState, context, currentSongLiked = false)
        }
        Spacer(Modifier.height(16.dp))
        PlayerSlider(SliderStyle.Standard, sliderPosition, position, duration, isPlaying, textButtonColor, onSliderValueChange, onSliderValueChangeFinished)
        PlayerTimeLabel(sliderPosition, position, duration, textBackgroundColor, currentFormat, playerDesignStyle)
        Spacer(Modifier.height(16.dp))
        PlayerPlaybackControls(playerDesignStyle, playbackState, isPlaying, isLoading, repeatMode, canSkipPrevious, canSkipNext, textButtonColor, iconButtonColor, textBackgroundColor, icBackgroundColor, 24.dp, playerConnection, false)
    }
}

@Composable
fun PlayerSlider(style: SliderStyle, pos: Long?, p: Long, dur: Long, isP: Boolean, c: Color, onC: (Long) -> Unit, onF: () -> Unit) {
    Slider(value = (pos ?: p).toFloat(), onValueChange = { onC(it.toLong()) }, onValueChangeFinished = onF, valueRange = 0f..dur.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding))
}
