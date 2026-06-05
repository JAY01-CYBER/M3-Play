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

@OptIn(ExperimentalMaterial3Api::class)
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
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit
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
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        ),
                        startY = 400f
                    )
                )
        )

        // 3. UI CONTENT LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding() // FIX: Prevents overlap with system navigation bar
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
                
                Row {
                    IconButton(onClick = onTimerClick) {
                        Icon(painterResource(R.drawable.bedtime), null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = onQueueClick) {
                        Icon(painterResource(R.drawable.queue_music), null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // APPLE STYLE PROGRESS SLIDER (Fixed Seek Logic)
            val safeDuration = if (duration > 0) duration.toFloat() else 1f
            val safePosition = position.toFloat().coerceIn(0f, safeDuration)

            Slider(
                value = safePosition,
                valueRange = 0f..safeDuration,
                onValueChange = { onSeekChange(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent, 
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // MAIN PLAYBACK CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTTOM UTILITY BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLyricsClick) {
                    // Changed to standard chat/notes icon in case apple_lyrics doesn't exist
                    Icon(
                        painter = painterResource(R.drawable.chat), 
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onTimerClick) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = "Sleep Timer",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
