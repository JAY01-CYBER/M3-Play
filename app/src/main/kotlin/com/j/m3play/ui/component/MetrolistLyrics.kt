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

// ViewModel Models: Line aur Indicator (Instrumental Break) ke liye
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
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    
    // LyricsViewModel ka pura Parse aur Merging Logic yahan hai
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
                
                // Line duration calculation for Karaoke Fill
                val actualDuration = minOf(nextTime - current.startTimeMs, 4500L)
                mergedList.add(current.copy(durationMs = actualDuration))
                
                // LyricsViewModel: Instrumental Gap Logic (• • •)
                val gap = nextTime - (current.startTimeMs + actualDuration)
                if (gap > 3500L) { // Agar agle bol aane me 3.5 sec se zyada gap hai toh dots dikhao
                    mergedList.add(MetroLyricItem.Indicator(current.startTimeMs + actualDuration, gap))
                }
            }
            mergedList
        }
    }

    val position = sliderPositionProvider() ?: playerConnection.player.currentPosition
    
    val activeIndex by remember(position, parsedLyrics) {
        derivedStateOf {
            val idx = parsedLyrics.indexOfLast { it.startTimeMs <= position + 200L }
            if (idx != -1) idx else 0
        }
    }

    val listState = rememberLazyListState()

    // Smooth Scroll Offset to keep active line centered
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
            
            // EXACT Metrolist Alpha Animation
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPassed) 0.3f else 0.45f,
                animationSpec = tween(400, easing = LinearOutSlowInEasing),
                label = "alpha"
            )

            // EXACT Metrolist Bouncy Scale Animation
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy, 
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )

            // Progress for Karaoke Fill & Indicator Dots
            val itemProgress = if (isActive) {
                ((position - item.startTimeMs).toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            } else if (isPassed) 1f else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(0f, 0.5f) // Zoom from Left
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
                    // Normal Lyrics Line
                    is MetroLyricItem.Line -> {
                        Text(
                            text = item.text,
                            color = Color.White.copy(alpha = if (isActive) 0.25f else 0.8f),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Start,
                            style = TextStyle(
                                // EXACT Metrolist Glow
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
                                        // Simulated `LyricsLine.kt` wipe effect (Karaoke Fill)
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
                    
                    // Instrumental Break Indicator (Apple Music Style Dots)
                    is MetroLyricItem.Indicator -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            repeat(3) { dotIndex ->
                                // Har ek dot gaane ki timing ke hisaab se dheere dheere glow karega
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
