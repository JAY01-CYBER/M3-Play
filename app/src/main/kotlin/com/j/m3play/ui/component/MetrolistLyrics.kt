package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.LocalPlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

sealed class MetroLyricItem {
    abstract val startTimeMs: Long
    abstract val durationMs: Long

    data class Line(
        override val startTimeMs: Long,
        override val durationMs: Long,
        val text: String
    ) : MetroLyricItem()

    data class Indicator(
        override val startTimeMs: Long,
        override val durationMs: Long
    ) : MetroLyricItem()
}

@Composable
fun MetrolistLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    
    val parsedLyrics = remember(currentLyrics) {
        val lyricsText = currentLyrics?.toString() ?: ""
        if (lyricsText.isBlank()) emptyList()
        else {
            val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
            val rawLines = lyricsText.lines().mapNotNull { line ->
                regex.find(line)?.let { match ->
                    val (min, sec, mil, text) = match.destructured
                    val timeMs = (min.toLong() * 60 * 1000) + (sec.toLong() * 1000) + (if (mil.length == 2) mil.toLong() * 10 else mil.toLong())
                    if (text.trim().isNotEmpty()) MetroLyricItem.Line(timeMs, 0L, text.trim()) else null
                }
            }

            val mergedList = mutableListOf<MetroLyricItem>()
            for (i in rawLines.indices) {
                val current = rawLines[i]
                val nextTime = if (i < rawLines.size - 1) rawLines[i + 1].startTimeMs else current.startTimeMs + 5000L
                
                // Line duration calculation for Karaoke Wipe
                val actualDuration = minOf(nextTime - current.startTimeMs, 4000L)
                mergedList.add(current.copy(durationMs = actualDuration))
                
                // Apple Music Style: Instrumental Gap Logic (• • •)
                val gap = nextTime - (current.startTimeMs + actualDuration)
                if (gap > 3500L) {
                    mergedList.add(MetroLyricItem.Indicator(current.startTimeMs + actualDuration, gap))
                }
            }
            mergedList
        }
    }

    // 🚀 THE FIX: 60 FPS Smooth Time Tracker (No ViewModel needed!)
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(parsedLyrics, playerConnection.playbackState) {
        var lastPlayerPos = player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()
        
        while (isActive) {
            delay(16) // 16ms = ~60 FPS update
            val sliderPos = sliderPositionProvider()
            if (sliderPos != null) {
                currentPositionMs = sliderPos
                lastPlayerPos = sliderPos
                lastUpdateTime = System.currentTimeMillis()
            } else {
                val now = System.currentTimeMillis()
                val playerPos = player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                val elapsed = now - lastUpdateTime
                currentPositionMs = lastPlayerPos + (if (player.isPlaying) elapsed else 0)
            }
        }
    }
    
    val activeIndex by remember(currentPositionMs, parsedLyrics) {
        derivedStateOf {
            val idx = parsedLyrics.indexOfLast { it.startTimeMs <= currentPositionMs + 200L }
            if (idx != -1) idx else 0
        }
    }

    val listState = rememberLazyListState()

    // Smooth Scroll Auto-Centering
    LaunchedEffect(activeIndex) {
        if (parsedLyrics.isNotEmpty() && activeIndex >= 0) {
            listState.animateScrollToItem(index = maxOf(0, activeIndex), scrollOffset = -450)
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No synced lyrics available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 350.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        itemsIndexed(parsedLyrics) { index, item ->
            val isActive = index == activeIndex
            val isPassed = index < activeIndex
            
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPassed) 0.3f else 0.45f,
                animationSpec = tween(400, easing = LinearOutSlowInEasing),
                label = "alpha"
            )

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy, 
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )

            val itemProgress = if (isActive) {
                ((currentPositionMs - item.startTimeMs).toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            } else if (isPassed) 1f else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, 
                        indication = null 
                    ) {
                        playerConnection.player.seekTo(item.startTimeMs)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                when (item) {
                    is MetroLyricItem.Line -> {
                        Text(
                            text = item.text,
                            color = Color.White.copy(alpha = if (isActive) 0.25f else 0.8f),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Start,
                            style = TextStyle(
                                shadow = if (isActive) Shadow(
                                    color = Color.White.copy(alpha = 0.5f),
                                    offset = Offset.Zero,
                                    blurRadius = 30f 
                                ) else null
                            ),
                            modifier = if (isActive) {
                                Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        // THE APPLE MUSIC WIPE / KARAOKE FILL EFFECT
                                        val currentX = size.width * itemProgress
                                        val edgeWidth = 36.dp.toPx()
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.White, Color.Transparent),
                                                startX = currentX - edgeWidth,
                                                endX = currentX + edgeWidth
                                            ),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                            } else Modifier
                        )
                    }
                    
                    is MetroLyricItem.Indicator -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            repeat(3) { dotIndex ->
                                val dotActive = isActive && itemProgress > (dotIndex * 0.33f)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = Color.White.copy(alpha = if (dotActive) 1f else 0.25f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
