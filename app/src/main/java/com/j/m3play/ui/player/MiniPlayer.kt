package com.j.m3play.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.utils.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val offsetXAnimatable = remember { Animatable(0f) }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1f,
        animationSpec = tween(400),
        label = ""
    )

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val artworkRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .shadow(14.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
        ) {

            Box(Modifier.fillMaxSize()) {
                if (duration > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((position.toFloat() / duration).coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                            .align(Alignment.TopStart)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {

                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {

                    if (duration > 0) {
                        CircularProgressIndicator(
                            progress = { (position.toFloat() / duration) },
                            modifier = Modifier.size(52.dp),
                            strokeWidth = 3.dp
                        )
                    }

                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                                rotationZ = if (isPlaying) artworkRotation * 0.05f else 0f
                            }
                            .clickable {

                                if (playbackState == Player.STATE_ENDED) {

                                    playerConnection.player.seekTo(0)
                                    playerConnection.player.play()

                                } else {

                                    playerConnection.player.togglePlayPause()

                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    mediaMetadata?.let {

                        Text(
                            text = it.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )

                        Text(
                            text = it.artists.joinToString { a -> a.name },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    enabled = canSkipNext,
                    onClick = { playerConnection.player.seekToNext() }
                ) {
                    Icon(
                        painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
