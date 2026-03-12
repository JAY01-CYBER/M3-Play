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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.j.m3play.model.Song
import com.j.m3play.ui.components.BlurredBackground

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BlurredBackground(imageUrl = song.thumbnailUrl)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxSize().weight(1f), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Minimize", tint = Color.White)
                }
            }

            Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.size(280.dp)) {
                AsyncImage(model = song.thumbnailUrl, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = song.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(text = song.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f))

            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.size(24.dp))
                FloatingActionButton(onClick = onPlayPause, shape = CircleShape, containerColor = Color.White) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "PlayPause",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.size(24.dp))
                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
