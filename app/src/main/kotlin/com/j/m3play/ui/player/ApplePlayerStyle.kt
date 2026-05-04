package com.j.m3play.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.j.m3play.R

@Composable
fun ApplePlayerStyle(
    title: String,
    artist: String,
    artworkUri: Any?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLyricsClick: () -> Unit,
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. FULL SCREEN ALBUM ART (Sharp & Immersive)
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. GRADIENT OVERLAY (Bottom dark shadow for readability)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black
                        ),
                        startY = 500f
                    )
                )
        )

        // 3. UI CONTENT LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            
            // Song Info & Top Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Screenshot style Star & Menu icons
                Row {
                    IconButton(onClick = { /* Fav logic */ }) {
                        Icon(painterResource(R.drawable.apple_timer), null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { /* Menu logic */ }) {
                        Icon(painterResource(R.drawable.apple_queue), null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // APPLE STYLE PROGRESS SLIDER
            val progress = if (duration > 0) position.toFloat() / duration else 0f
            Slider(
                value = progress,
                onValueChange = { onSeek((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent, // Hidden thumb like Apple
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // MAIN PLAYBACK CONTROLS (Play, Pause, Skip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        painter = painterResource(R.drawable.apple_prev),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.apple_pause else R.drawable.apple_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.apple_next),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            // BOTTOM UTILITY BAR (Lyrics, Sleep Timer, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLyricsClick) {
                    Icon(
                        painter = painterResource(R.drawable.apple_lyrics),
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onTimerClick) {
                    Icon(
                        painter = painterResource(R.drawable.apple_timer),
                        contentDescription = "Sleep Timer",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        painter = painterResource(R.drawable.apple_queue),
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
