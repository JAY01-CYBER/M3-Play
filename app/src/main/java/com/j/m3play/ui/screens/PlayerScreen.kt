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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.j.m3play.data.Song
import com.j.m3play.ui.components.BlurredBackground

@Composable
fun FullPlayerScreen(
    song: Song,
    onMinimize: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Blur
        BlurredBackground(imageUrl = song.thumbnailUrl)

        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Minimize",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big Album Art
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .size(320.dp)
                    .shadow(20.dp)
            ) {
                AsyncImage(model = song.thumbnailUrl, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Song Info
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Playback Controls (Rhythm Style)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Prev",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(24.dp))
                FloatingActionButton(
                    onClick = { /* Play/Pause */ },
                    shape = CircleShape,
                    containerColor = Color.White
                ) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
