package com.j.m3play.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.j.m3play.player.PlayerUiState
import com.j.m3play.ui.components.BlurredBackground

@Composable
fun FullPlayerScreen(
    state: PlayerUiState,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val song = state.currentSong ?: return
    var sliderValue by remember(state.positionMs, state.durationMs) {
        mutableFloatStateOf(
            if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
        )
    }
    var scrubbing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredBackground(imageUrl = song.thumbnailUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Minimize", tint = Color.White)
                }
            }

            Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.size(320.dp)) {
                AsyncImage(model = song.thumbnailUrl, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = song.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = song.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f))

            Spacer(modifier = Modifier.height(24.dp))
            Slider(
                value = if (scrubbing) sliderValue else if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                onValueChange = {
                    scrubbing = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    val target = (state.durationMs * sliderValue).toLong()
                    onSeek(target)
                    scrubbing = false
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.width(24.dp))
                FloatingActionButton(onClick = onPlayPause, shape = CircleShape, containerColor = Color.White) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}
