package com.j.m3play.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.j.m3play.R
import com.j.m3play.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplePlayerStyle(
    title: String,
    artist: String,
    artworkUri: Any?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMenuClick: () -> Unit,
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
    onSeekFinished: () -> Unit,
    thumbnailContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. DYNAMIC BLURRED BACKGROUND (Like Apple Music)
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 80.dp) // Heavy blur for background gradient effect
        )

        // Dark overlay to ensure white text is always readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // 2. MAIN CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.weight(1f))

            // 3. CENTER ALBUM ART / CANVAS (Square, Padded, Shadowed)
            val imageScale by animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "AppleArtScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f) // Makes it a perfect square
                    .scale(imageScale)
                    .shadow(elevation = if (isPlaying) 24.dp else 8.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Renders the Canvas or default Thumbnail here
                thumbnailContent()
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. SONG INFO ROW (Title, Artist & Action Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Favorite Icon in Circle
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp).clickable { onFavoriteClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), 
                                contentDescription = "Favorite", 
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.9f), 
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Menu Icon in Circle
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp).clickable { onMenuClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert), 
                                contentDescription = "Options", 
                                tint = Color.White.copy(alpha = 0.9f), 
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. SEEKBAR
            val safeDuration = if (duration > 0) duration.toFloat() else 1f
            val safePosition = position.toFloat().coerceIn(0f, safeDuration)

            Slider(
                value = safePosition,
                valueRange = 0f..safeDuration,
                onValueChange = { onSeekChange(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )

            // Current Time & Remaining Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = makeTimeString(position), 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "-${makeTimeString(duration - position)}", 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 6. MAIN PLAYBACK CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 7. BOTTOM UTILITY BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics
                IconButton(onClick = onLyricsClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz), 
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Sleep Timer replacing Cast as requested
                IconButton(onClick = onTimerClick) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = "Sleep Timer",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Queue
                IconButton(onClick = onQueueClick) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
