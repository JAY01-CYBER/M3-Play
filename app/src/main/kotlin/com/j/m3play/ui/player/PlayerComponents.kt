package com.j.m3play.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.PlayerBackgroundStyle
import com.j.m3play.constants.PlayerButtonsStyle
import com.j.m3play.constants.PlayerDesignStyle
import com.j.m3play.constants.PlayerHorizontalPadding
import com.j.m3play.constants.SliderStyle
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toggleRepeatMode
import com.j.m3play.models.MediaMetadata
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.ui.component.BottomSheetPageState
import com.j.m3play.ui.component.BottomSheetState
import com.j.m3play.ui.component.MenuState
import com.j.m3play.ui.component.PlayerSliderTrack
import com.j.m3play.ui.component.ResizableIconButton
import com.j.m3play.ui.menu.PlayerMenu
import com.j.m3play.ui.theme.PlayerBackgroundColorUtils
import com.j.m3play.ui.theme.PlayerSliderColors
import com.j.m3play.ui.utils.ShowMediaInfo
import com.j.m3play.utils.makeTimeString
import me.saket.squiggles.SquigglySlider

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
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.9f)),
                startY = 600f
            )
        ))
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mediaMetadata.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = mediaMetadata.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp, maxLines = 1)
                }
                Icon(painter = painterResource(R.drawable.favorite_border), contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
            val progress = if (duration > 0) position.toFloat() / duration else 0f
            Slider(
                value = progress,
                onValueChange = { onSeek((it * duration).toLong()) },
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(painterResource(R.drawable.apple_prev), null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                    else Icon(painterResource(if (isPlaying) R.drawable.apple_pause else R.drawable.apple_play), null, tint = Color.White, modifier = Modifier.fillMaxSize(0.7f))
                }
                IconButton(onClick = onNext) { Icon(painterResource(R.drawable.apple_next), null, tint = Color.White, modifier = Modifier.size(44.dp)) }
            }
            Spacer(modifier = Modifier.height(50.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(painterResource(R.drawable.history), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                Icon(painterResource(R.drawable.share), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                Icon(painterResource(R.drawable.library_music), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun PlayerTitleSection(mediaMetadata: MediaMetadata, textBackgroundColor: Color, navController: NavController, state: BottomSheetState, clipboardManager: ClipboardManager, context: Context) {
    AnimatedContent(targetState = mediaMetadata.title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { title ->
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textBackgroundColor,
            modifier = Modifier.basicMarquee().combinedClickable(onClick = { if (mediaMetadata.album != null) { state.snapTo(state.collapsedBound); navController.navigate("album/${mediaMetadata.album.id}") } }, onLongClick = { val clip = ClipData.newPlainText("Copied Title", title); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, "Copied Title", Toast.LENGTH_SHORT).show() }))
    }
    Spacer(Modifier.height(6.dp))
    val annotatedString = buildAnnotatedString { mediaMetadata.artists.forEachIndexed { index, artist -> pushStringAnnotation(tag = "artist_${artist.id.orEmpty()}", annotation = artist.id.orEmpty()); withStyle(SpanStyle(color = textBackgroundColor, fontSize = 16.sp)) { append(artist.name) }; pop(); if (index != mediaMetadata.artists.lastIndex) append(", ") } }
    Box(modifier = Modifier.fillMaxWidth().basicMarquee().padding(end = 12.dp)) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        Text(text = annotatedString, style = MaterialTheme.typography.titleMedium.copy(color = textBackgroundColor), maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { layoutResult = it },
            modifier = Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(); val tapPosition = event.changes.firstOrNull()?.position; if (tapPosition != null) { clickOffset = tapPosition } } } }.combinedClickable(onClick = { val tapPosition = clickOffset; val layout = layoutResult; if (tapPosition != null && layout != null) { val offset = layout.getOffsetForPosition(tapPosition); annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { ann -> val artistId = ann.item; if (artistId.isNotBlank()) { navController.navigate("artist/$artistId"); state.collapseSoft() } } } }, onLongClick = { val clip = ClipData.newPlainText("Copied Artist", annotatedString); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, "Copied Artist", Toast.LENGTH_SHORT).show() }))
    }
}

@Composable
fun PlayerTopActions(mediaMetadata: MediaMetadata, playerDesignStyle: PlayerDesignStyle, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, playerConnection: PlayerConnection, navController: NavController, menuState: MenuState, state: BottomSheetState, bottomSheetPageState: BottomSheetPageState, context: Context, currentSongLiked: Boolean) {
    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 10.dp, bottomEnd = 10.dp)).background(textButtonColor).clickable { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }) { Image(painter = painterResource(R.drawable.share), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(24.dp)) }
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 50.dp, bottomEnd = 50.dp)).background(textButtonColor).clickable { playerConnection.toggleLike() }) { Image(painter = painterResource(if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(24.dp)) }
            }
        }
        PlayerDesignStyle.V3, PlayerDesignStyle.V5 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).clickable { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = textBackgroundColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).clickable { playerConnection.toggleLike() }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (currentSongLiked) MaterialTheme.colorScheme.error.copy(alpha = 0.9f) else textBackgroundColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
        PlayerDesignStyle.V4 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }, shape = RoundedCornerShape(14.dp), color = textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(44.dp).width(44.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = textBackgroundColor, modifier = Modifier.size(22.dp)) } }
                Surface(onClick = { playerConnection.toggleLike() }, shape = RoundedCornerShape(14.dp), color = if (currentSongLiked) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(44.dp).width(44.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (currentSongLiked) MaterialTheme.colorScheme.error else textBackgroundColor, modifier = Modifier.size(22.dp)) } }
                Surface(onClick = { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }, shape = RoundedCornerShape(14.dp), color = textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(44.dp).width(44.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = textBackgroundColor, modifier = Modifier.size(22.dp)) } }
            }
        }
        PlayerDesignStyle.V1 -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }) { Image(painter = painterResource(R.drawable.share), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }) { Image(painter = painterResource(R.drawable.more_horiz), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor)) }
            }
        }
        PlayerDesignStyle.V6 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }, shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 6.dp, bottomEnd = 6.dp), color = textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(42.dp).width(42.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = textBackgroundColor, modifier = Modifier.size(20.dp)) } }
                Surface(onClick = { playerConnection.toggleLike() }, shape = RoundedCornerShape(50), color = if (currentSongLiked) MaterialTheme.colorScheme.error.copy(alpha = 0.18f) else textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(42.dp).width(42.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (currentSongLiked) MaterialTheme.colorScheme.error else textBackgroundColor, modifier = Modifier.size(20.dp)) } }
                Surface(onClick = { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }, shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 50.dp, bottomEnd = 50.dp), color = textBackgroundColor.copy(alpha = 0.12f), modifier = Modifier.height(42.dp).width(42.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = textBackgroundColor, modifier = Modifier.size(20.dp)) } }
            }
        }
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(sliderStyle: SliderStyle, sliderPosition: Long?, position: Long, duration: Long, isPlaying: Boolean, textButtonColor: Color, onValueChange: (Long) -> Unit, onValueChangeFinished: () -> Unit) {
    val safeDuration = if (duration <= 0L) 0f else duration.toFloat(); val safeValue = (sliderPosition ?: position).toFloat().coerceIn(0f, maxOf(0f, safeDuration))
    StyledPlaybackSlider(sliderStyle = sliderStyle, value = safeValue, valueRange = 0f..maxOf(1f, safeDuration), onValueChange = { onValueChange(it.toLong()) }, onValueChangeFinished = onValueChangeFinished, activeColor = textButtonColor, isPlaying = isPlaying, modifier = Modifier.padding(horizontal = PlayerHorizontalPadding))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledPlaybackSlider(sliderStyle: SliderStyle, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit, activeColor: Color, isPlaying: Boolean, modifier: Modifier = Modifier) {
    when (sliderStyle) {
        SliderStyle.Standard -> Slider(value = value, valueRange = valueRange, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, colors = PlayerSliderColors.standardSliderColors(activeColor), modifier = modifier)
        SliderStyle.Wavy -> SquigglySlider(value = value, valueRange = valueRange, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, colors = PlayerSliderColors.wavySliderColors(activeColor), modifier = modifier, squigglesSpec = SquigglySlider.SquigglesSpec(amplitude = if (isPlaying) 2.dp else 0.dp, strokeWidth = 6.dp))
        SliderStyle.Thick -> Slider(value = value, valueRange = valueRange, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, colors = PlayerSliderColors.thickSliderColors(activeColor), thumb = { Spacer(Modifier.size(0.dp)) }, track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.thickSliderColors(activeColor), trackHeight = 12.dp) }, modifier = modifier)
        SliderStyle.Circular -> SquigglySlider(value = value, valueRange = valueRange, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, colors = PlayerSliderColors.circularSliderColors(activeColor), modifier = modifier, squigglesSpec = SquigglySlider.SquigglesSpec(amplitude = if (isPlaying) 2.dp else 0.dp, strokeWidth = 6.dp))
        SliderStyle.Simple -> Slider(value = value, valueRange = valueRange, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, colors = PlayerSliderColors.simpleSliderColors(activeColor), thumb = { Spacer(Modifier.size(0.dp)) }, track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.simpleSliderColors(activeColor), trackHeight = 3.dp) }, modifier = modifier)
    }
}

@Composable
fun PlayerTimeLabel(sliderPosition: Long?, position: Long, duration: Long, textBackgroundColor: Color) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)) {
        Text(text = makeTimeString(sliderPosition ?: position), style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlayerPlaybackControls(playerDesignStyle: PlayerDesignStyle, playbackState: Int, isPlaying: Boolean, isLoading: Boolean, repeatMode: Int, canSkipPrevious: Boolean, canSkipNext: Boolean, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, icBackgroundColor: Color, playPauseRoundness: androidx.compose.ui.unit.Dp, playerConnection: PlayerConnection, currentSongLiked: Boolean) {
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { FilledTonalIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(width = 65.dp, height = 50.dp).clip(RoundedCornerShape(32.dp))) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }; Spacer(Modifier.width(16.dp)); FilledIconButton(onClick = { if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.player.togglePlayPause() } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(width = 100.dp, height = 60.dp).clip(RoundedCornerShape(32.dp))) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(42.dp), color = iconButtonColor, strokeWidth = 3.dp) else Icon(painter = painterResource(when { playbackState == STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }), contentDescription = null, modifier = Modifier.size(42.dp)) }; Spacer(Modifier.width(16.dp)); FilledTonalIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(width = 65.dp, height = 50.dp).clip(RoundedCornerShape(32.dp))) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) } } }
        PlayerDesignStyle.V3 -> { Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 1f else 0.4f), modifier = Modifier.size(22.dp)) }; Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(textBackgroundColor.copy(alpha = 0.08f)).clickable(enabled = canSkipPrevious) { playerConnection.seekToPrevious() }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f), modifier = Modifier.size(26.dp)) }; Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(50)).background(textBackgroundColor).clickable { if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.player.togglePlayPause() } }, contentAlignment = Alignment.Center) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(32.dp), color = icBackgroundColor, strokeWidth = 2.5.dp) else Icon(painter = painterResource(when { playbackState == STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }), contentDescription = null, tint = icBackgroundColor, modifier = Modifier.size(34.dp)) }; Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(textBackgroundColor.copy(alpha = 0.08f)).clickable(enabled = canSkipNext) { playerConnection.seekToNext() }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (canSkipNext) 0.9f else 0.4f), modifier = Modifier.size(26.dp)) }; Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable { playerConnection.player.toggleRepeatMode() }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f), modifier = Modifier.size(22.dp)) } } }
        PlayerDesignStyle.V4 -> { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Surface(onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled }, shape = RoundedCornerShape(16.dp), color = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 0.2f else 0.08f), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 1f else 0.6f), modifier = Modifier.size(22.dp)) } }; Spacer(Modifier.width(12.dp)); Surface(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious, shape = RoundedCornerShape(18.dp), color = textBackgroundColor.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f), modifier = Modifier.size(28.dp)) } } }; Surface(onClick = { if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.player.togglePlayPause() } }, shape = RoundedCornerShape(28.dp), color = textButtonColor, modifier = Modifier.padding(horizontal = 20.dp).size(88.dp)) { Box(contentAlignment = Alignment.Center) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(40.dp), color = icBackgroundColor, strokeWidth = 3.dp) else Icon(painter = painterResource(when { playbackState == STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }), contentDescription = null, tint = icBackgroundColor, modifier = Modifier.size(44.dp)) } }; Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) { Surface(onClick = playerConnection::seekToNext, enabled = canSkipNext, shape = RoundedCornerShape(18.dp), color = textBackgroundColor.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (canSkipNext) 1f else 0.4f), modifier = Modifier.size(28.dp)) } }; Spacer(Modifier.width(12.dp)); Surface(onClick = { playerConnection.player.toggleRepeatMode() }, shape = RoundedCornerShape(16.dp), color = textBackgroundColor.copy(alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.2f else 0.08f), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), contentDescription = null, tint = textBackgroundColor.copy(alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.6f else 1f), modifier = Modifier.size(22.dp)) } } } } }
        PlayerDesignStyle.V1, PlayerDesignStyle.V5 -> { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) { Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }, color = textBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center).alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f), onClick = { playerConnection.player.toggleRepeatMode() }) }; Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_previous, enabled = canSkipPrevious, color = textBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection::seekToPrevious) }; Spacer(Modifier.width(8.dp)); Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor).clickable { if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.player.togglePlayPause() } }) { if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(36.dp), color = iconButtonColor, strokeWidth = 3.dp) else Image(painter = painterResource(if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp)) }; Spacer(Modifier.width(8.dp)); Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_next, enabled = canSkipNext, color = textBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection::seekToNext) }; Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border, color = if (currentSongLiked) MaterialTheme.colorScheme.error else textBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center), onClick = playerConnection.toggleLike) } } }
        PlayerDesignStyle.V6 -> { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) { Surface(shape = RoundedCornerShape(28.dp), color = textBackgroundColor.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Surface(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious, shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 8.dp, bottomEnd = 8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f).height(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (canSkipPrevious) 1f else 0.4f), modifier = Modifier.size(28.dp)) } }; Spacer(Modifier.width(6.dp)); Surface(onClick = { if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.player.togglePlayPause() } }, shape = RoundedCornerShape(28.dp), color = textButtonColor, modifier = Modifier.size(width = 88.dp, height = 80.dp)) { Box(contentAlignment = Alignment.Center) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(40.dp), color = iconButtonColor, strokeWidth = 3.dp) else Icon(painter = painterResource(when { playbackState == STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }), contentDescription = null, tint = iconButtonColor, modifier = Modifier.size(44.dp)) } }; Spacer(Modifier.width(6.dp)); Surface(onClick = playerConnection::seekToNext, enabled = canSkipNext, shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f).height(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (canSkipNext) 1f else 0.4f), modifier = Modifier.size(28.dp)) } } } }; Spacer(modifier = Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Surface(onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled }, shape = RoundedCornerShape(50), color = if (shuffleModeEnabled) MaterialTheme.colorScheme.tertiaryContainer else textBackgroundColor.copy(alpha = 0.08f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.onTertiaryContainer else textBackgroundColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) } }; Spacer(Modifier.width(16.dp)); Surface(onClick = { playerConnection.player.toggleRepeatMode() }, shape = RoundedCornerShape(50), color = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.tertiaryContainer else textBackgroundColor.copy(alpha = 0.08f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), contentDescription = null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onTertiaryContainer else textBackgroundColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) } } } } }
        else -> {}
    }
}

@Composable
fun PlayerControlsContent(mediaMetadata: MediaMetadata, playerDesignStyle: PlayerDesignStyle, sliderStyle: SliderStyle, playbackState: Int, isPlaying: Boolean, isLoading: Boolean, repeatMode: Int, canSkipPrevious: Boolean, canSkipNext: Boolean, textButtonColor: Color, iconButtonColor: Color, textBackgroundColor: Color, icBackgroundColor: Color, sliderPosition: Long?, position: Long, duration: Long, playerConnection: PlayerConnection, navController: NavController, state: BottomSheetState, menuState: MenuState, bottomSheetPageState: BottomSheetPageState, clipboardManager: ClipboardManager, context: Context, onSliderValueChange: (Long) -> Unit, onSliderValueChangeFinished: () -> Unit) {
    if (playerDesignStyle == PlayerDesignStyle.APPLE) {
        ApplePlayerStyle(mediaMetadata = mediaMetadata, isPlaying = isPlaying, isLoading = isLoading, position = position, duration = duration, onPlayPause = { playerConnection.player.togglePlayPause() }, onNext = playerConnection::seekToNext, onPrev = playerConnection::seekToPrevious, onSeek = onSliderValueChange)
        return
    }
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val playPauseRoundness by animateDpAsState(targetValue = if (isPlaying) 24.dp else 36.dp, animationSpec = tween(durationMillis = 90, easing = LinearEasing), label = "playPauseRoundness")
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
        Column(modifier = Modifier.weight(1f)) { PlayerTitleSection(mediaMetadata = mediaMetadata, textBackgroundColor = textBackgroundColor, navController = navController, state = state, clipboardManager = clipboardManager, context = context) }
        Spacer(Modifier.width(12.dp)); PlayerTopActions(mediaMetadata = mediaMetadata, playerDesignStyle = playerDesignStyle, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, textBackgroundColor = textBackgroundColor, playerConnection = playerConnection, navController = navController, menuState = menuState, state = state, bottomSheetPageState = bottomSheetPageState, context = context, currentSongLiked = currentSongLiked)
    }
    Spacer(Modifier.height(12.dp)); PlayerSlider(sliderStyle = sliderStyle, sliderPosition = sliderPosition, position = position, duration = duration, isPlaying = isPlaying, textButtonColor = textButtonColor, onValueChange = onSliderValueChange, onValueChangeFinished = onSliderValueChangeFinished)
    Spacer(Modifier.height(4.dp)); PlayerTimeLabel(sliderPosition = sliderPosition, position = position, duration = duration, textBackgroundColor = textBackgroundColor)
    Spacer(Modifier.height(12.dp)); PlayerPlaybackControls(playerDesignStyle = playerDesignStyle, playbackState = playbackState, isPlaying = isPlaying, isLoading = isLoading, repeatMode = repeatMode, canSkipPrevious = canSkipPrevious, canSkipNext = canSkipNext, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, textBackgroundColor = textBackgroundColor, icBackgroundColor = icBackgroundColor, playPauseRoundness = playPauseRoundness, playerConnection = playerConnection, currentSongLiked = currentSongLiked)
}

@Composable
fun PlayerBackground(playerBackground: PlayerBackgroundStyle, mediaMetadata: MediaMetadata?, gradientColors: List<Color>, disableBlur: Boolean, playerCustomImageUri: String, playerCustomBlur: Float, playerCustomContrast: Float, playerCustomBrightness: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> { AnimatedContent(targetState = mediaMetadata?.thumbnailUrl, transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }, label = "") { thumbnailUrl -> if (thumbnailUrl != null) { Box(Modifier.fillMaxSize()) { AsyncImage(model = thumbnailUrl, contentDescription = "Blurred background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().let { if (disableBlur) it else it.blur(radius = 60.dp) }); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = PlayerBackgroundColorUtils.buildBlurOverlayStops(gradientColors)))); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f))) } } } }
            PlayerBackgroundStyle.GRADIENT -> { AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }, label = "") { colors -> if (colors.isNotEmpty()) { Box(Modifier.fillMaxSize()) { val gradientColorStops = if (colors.size >= 3) arrayOf(0.0f to colors[0].copy(alpha = 0.92f), 0.5f to colors[1].copy(alpha = 0.75f), 1.0f to colors[2].copy(alpha = 0.65f)) else arrayOf(0.0f to colors[0].copy(alpha = 0.9f), 0.6f to colors[0].copy(alpha = 0.55f), 1.0f to Color.Black.copy(alpha = 0.7f)); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops))); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f))) } } } }
            PlayerBackgroundStyle.COLORING -> { AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }, label = "") { colors -> if (colors.isNotEmpty()) { val baseColor = PlayerBackgroundColorUtils.ensureComfortableColor(colors.first()); Box(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().background(baseColor)); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = PlayerBackgroundColorUtils.buildColoringStops(baseColor)))); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) } } } }
            PlayerBackgroundStyle.BLUR_GRADIENT -> { AnimatedContent(targetState = mediaMetadata?.thumbnailUrl, transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }, label = "") { thumbnailUrl -> if (thumbnailUrl != null) { Box(Modifier.fillMaxSize()) { AsyncImage(model = thumbnailUrl, contentDescription = "Blurred background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().let { if (disableBlur) it else it.blur(radius = 65.dp) }); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = PlayerBackgroundColorUtils.buildBlurGradientStops(gradientColors)))); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f))) } } } }
            PlayerBackgroundStyle.CUSTOM -> { AnimatedContent(targetState = playerCustomImageUri, transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) }, label = "") { uri -> if (uri.isNotBlank()) { Box(Modifier.fillMaxSize()) { val matrix = floatArrayOf(playerCustomContrast, 0f, 0f, 0f, (1f - playerCustomContrast) * 128f + (playerCustomBrightness - 1f) * 255f, 0f, playerCustomContrast, 0f, 0f, (1f - playerCustomContrast) * 128f + (playerCustomBrightness - 1f) * 255f, 0f, 0f, playerCustomContrast, 0f, (1f - playerCustomContrast) * 128f + (playerCustomBrightness - 1f) * 255f, 0f, 0f, 0f, 1f, 0f); AsyncImage(model = Uri.parse(uri), contentDescription = "Custom background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().let { if (disableBlur) it else it.blur(radius = playerCustomBlur.dp) }, colorFilter = ColorFilter.colorMatrix(ColorMatrix(matrix))); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) } } } }
            PlayerBackgroundStyle.GLOW, PlayerBackgroundStyle.GLOW_ANIMATED -> { AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(1200)) togetherWith fadeOut(tween(1200)) }, label = "") { colors -> if (colors.isNotEmpty()) { Box(Modifier.fillMaxSize().drawWithCache { onDrawBehind { drawRect(Color(0xFF050505)); colors.forEachIndexed { i, c -> drawRect(Brush.radialGradient(listOf(c.copy(0.8f), Color.Transparent), center = Offset(size.width * (i % 2), size.height * (i / 2)), radius = size.width)) } } }) } } }
            else -> {}
        }
    }
}
