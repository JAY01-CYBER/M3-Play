package com.j.m3play.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.j.m3play.data.Song
import com.j.m3play.ui.components.BlurredBackground
import com.j.m3play.ui.designsystem.FrostedLayer
import com.j.m3play.ui.designsystem.MotionSpecs
import com.j.m3play.ui.designsystem.ShapeTokens

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val pressScale = animateFloatAsState(if (isPlaying) 1f else 0.95f, MotionSpecs.fluidSpring, label = "press")
    Box(modifier = Modifier.fillMaxSize()) {
        BlurredBackground(imageUrl = song.thumbnailUrl, playbackProgress = progress)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = onMinimize, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
            }

            AnimatedContent(
                targetState = song,
                transitionSpec = { (fadeIn(animationSpec = MotionSpecs.mediumTween) togetherWith fadeOut(animationSpec = MotionSpecs.mediumTween)) },
                label = "artwork",
            ) { targetSong ->
                FrostedLayer(modifier = Modifier.size(320.dp).animateContentSize(), shape = ShapeTokens.artwork) {
                    AsyncImage(
                        model = targetSong.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(ShapeTokens.artwork),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            AnimatedContent(targetState = song.title, label = "title") {
                Text(it, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(song.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            FrostedLayer(modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.control) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Icon(Icons.Default.SkipPrevious, "Prev", modifier = Modifier.size(36.dp).clickable { })
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play",
                        modifier = Modifier.size(44.dp).scale(pressScale.value).clickable { onPlayPause() },
                    )
                    Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(36.dp).clickable { })
                }
            }
        }
    }
}
