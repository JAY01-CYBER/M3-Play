package com.j.m3play.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R
import com.j.m3play.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplePlayerStyle(
    title: String,
    artist: String,
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
    formatLabel: String?,
    backgroundColor: Color,
    thumbnailContent: @Composable () -> Unit
) {
    val darkBg = Color(0xFF1E1E1E) 
    val blendColor = backgroundColor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .background(blendColor)
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.TopCenter)
        ) {
            thumbnailContent()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            0.8f to darkBg.copy(alpha = 0.8f),
                            1.0f to darkBg
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.8f to Color.Transparent,
                            1.0f to blendColor
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
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
                    
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
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

            Spacer(modifier = Modifier.height(24.dp))

            val safeDuration = if (duration > 0) duration.toFloat() else 1f
            val safePosition = position.toFloat().coerceIn(0f, safeDuration)

            Slider(
                value = safePosition,
                valueRange = 0f..safeDuration,
                onValueChange = { onSeekChange(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )

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

                if (formatLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq), 
                                contentDescription = null, 
                                tint = Color.White.copy(alpha = 0.8f), 
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatLabel, 
                                color = Color.White.copy(alpha = 0.8f), 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Text(
                    text = "-${makeTimeString(duration - position)}", 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        modifier = Modifier.size(48.dp)
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
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLyricsClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onTimerClick) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = "Sleep Timer",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
