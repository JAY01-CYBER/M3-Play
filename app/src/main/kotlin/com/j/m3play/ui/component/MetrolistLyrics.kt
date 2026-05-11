/**
 * ╭──────────────────────────────────────────────────────────╮
 * │ Credits: Adapted from the Metrolist / Glossy Project     │
 * │ Original Concept & Logic: Metrolist Contributors         │
 * │ Exact Canvas Wipe & Velocity Bouncy Scroll Engine        │
 * ╰──────────────────────────────────────────────────────────╯
 */

package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.lyrics.LyricsEntry
import com.j.m3play.lyrics.MetrolistLyricsUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Handles • • • Indicators & Lines seamlessly
sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(val index: Int, val gap: Long, val startTime: Long, val endTime: Long) : LyricsListItem()
}

@Composable
fun MetrolistLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 1. Parsing with Real Metrolist Math
    val parsedLyrics = remember(currentLyrics) {
        val lyricsText = currentLyrics?.toString() ?: ""
        val rawLines = MetrolistLyricsUtils.parseLyrics(lyricsText)
        
        val mergedList = mutableListOf<LyricsListItem>()
        if (rawLines.isNotEmpty()) {
            rawLines.forEachIndexed { i, entry ->
                if (entry.text.isNotBlank()) {
                    mergedList.add(LyricsListItem.Line(i, entry))
                }
                if (i < rawLines.size - 1) {
                    val nextStart = rawLines[i + 1].time
                    val currentEnd = if (!entry.words.isNullOrEmpty()) {
                        (entry.words.last().endTime * 1000).toLong()
                    } else if (entry.text.isBlank()) {
                        entry.time
                    } else null

                    if (currentEnd != null && currentEnd < nextStart) {
                        val gap = nextStart - currentEnd
                        // Metrolist Original Apple Music Dots Math
                        if (gap > 4000L) {
                            mergedList.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart))
                        }
                    }
                }
            }
        }
        mergedList
    }

    // 2. Exact 60FPS Time Tracker
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(parsedLyrics, playerConnection.playbackState) {
        var lastPlayerPos = player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()
        
        while (isActive) {
            delay(16)
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

    // 3. Active Line Index
    val activeIndex by remember(currentPositionMs, parsedLyrics) {
        derivedStateOf {
            val idx = parsedLyrics.indexOfLast { item ->
                when (item) {
                    is LyricsListItem.Line -> item.entry.time <= currentPositionMs + 200L
                    is LyricsListItem.Indicator -> item.startTime <= currentPositionMs
                }
            }
            if (idx != -1) idx else 0
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No synced lyrics available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    // 4. Metrolist Original Velocity Physics (Bouncy Scroll)
    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(isAutoScrollEnabled, activeIndex) {
        if (isAutoScrollEnabled && abs(userManualOffset) > 1f) {
            val start = userManualOffset
            val anim = Animatable(start)
            var lastValue = start
            anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), FastOutSlowInEasing)) {
                userManualOffset += (value - lastValue)
                lastValue = value
            }
            userManualOffset = 0f
        }
    }

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        flingJob?.cancel()
                        velocityTracker.resetTracking()
                        isAutoScrollEnabled = false
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        
                        verticalDrag(down.id) { change ->
                            userManualOffset += change.positionChange().y
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        }
                        
                        val velocity = velocityTracker.calculateVelocity().y
                        flingJob = scope.launch {
                            AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                userManualOffset = value
                            }
                        }
                    }
                }
            }
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * 0.35f 
        val lineHeightPx = with(density) { 52.dp.toPx() } // Dynamic gap
        
        parsedLyrics.forEachIndexed { listIndex, item ->
            val distance = abs(listIndex - activeIndex)
            val targetOffset = anchorY + ((listIndex - activeIndex) * (lineHeightPx + 60f))
            
            val animatedOffset by animateFloatAsState(
                targetValue = targetOffset, 
                animationSpec = tween(750, (distance * 20).coerceAtMost(200), FastOutSlowInEasing),
                label = "offset_$listIndex"
            )

            val isActive = listIndex == activeIndex
            val isPassed = listIndex < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "scale"
            )

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPassed) 0.3f else 0.45f,
                animationSpec = tween(400),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, 
                        indication = null 
                    ) {
                        val seekTime = when (item) {
                            is LyricsListItem.Line -> item.entry.time
                            is LyricsListItem.Indicator -> item.startTime
                        }
                        playerConnection.player.seekTo(seekTime)
                        isAutoScrollEnabled = true
                        userManualOffset = 0f
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                when (item) {
                    is LyricsListItem.Indicator -> {
                        // 5. Apple Music 3 Dots Logic
                        val progress = ((currentPositionMs - item.startTime).toFloat() / item.gap.toFloat()).coerceIn(0f, 1f)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            repeat(3) { dotIndex ->
                                val dotActive = isActive && progress > (dotIndex * 0.33f)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .graphicsLayer { this.alpha = alpha }
                                        .background(
                                            color = Color.White.copy(alpha = if (dotActive) 1f else 0.25f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                    
                    is LyricsListItem.Line -> {
                        // 6. EXACT CANVAS TEXT MEASURE & WIPE
                        val entry = item.entry
                        val baseStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 44.sp,
                            shadow = if (isActive) Shadow(
                                color = Color.White.copy(alpha = 0.5f),
                                offset = Offset.Zero,
                                blurRadius = 30f 
                            ) else null
                        )

                        val textLayoutResult = remember(entry.text) {
                            textMeasurer.measure(text = entry.text, style = baseStyle)
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { textLayoutResult.size.height.toDp() })
                                .padding(horizontal = 24.dp)
                                .graphicsLayer { this.alpha = alpha }
                        ) {
                            // Draw base dim text
                            drawText(
                                textLayoutResult = textLayoutResult,
                                color = Color.White.copy(alpha = if (isActive) 0.25f else 1f)
                            )

                            // Apply Karaoke Wipe on active text
                            if (isActive) {
                                if (!entry.words.isNullOrEmpty()) {
                                    // Math for exact word-by-word bounds calculation
                                    var charGlobalIndex = 0
                                    entry.words.forEach { word ->
                                        // Space trailing mapping is already done in MetrolistLyricsUtils
                                        val wordText = word.text 
                                        val wordStartMs = (word.startTime * 1000).toLong()
                                        val wordEndMs = (word.endTime * 1000).toLong()
                                        val wordDuration = wordEndMs - wordStartMs
                                        val charDuration = if (wordText.isNotEmpty()) wordDuration / wordText.length else 1L
                                        
                                        for (i in wordText.indices) {
                                            if (charGlobalIndex >= entry.text.length) break
                                            val charStartMs = wordStartMs + (i * charDuration)
                                            val charProgress = ((currentPositionMs - charStartMs).toFloat() / charDuration.toFloat()).coerceIn(0f, 1f)
                                            
                                            val bounds = textLayoutResult.getBoundingBox(charGlobalIndex)
                                            
                                            if (charProgress > 0f) {
                                                val fillWidth = bounds.left + (bounds.width * charProgress)
                                                clipRect(left = bounds.left, top = bounds.top, right = fillWidth, bottom = bounds.bottom) {
                                                    drawText(textLayoutResult, color = Color.White)
                                                }
                                            }
                                            charGlobalIndex++
                                        }
                                    }
                                } else {
                                    // Fallback if song has no WordTimestamps
                                    val fallbackDuration = 4000f
                                    val lineProgress = ((currentPositionMs - entry.time).toFloat() / fallbackDuration).coerceIn(0f, 1f)
                                    val fillWidth = size.width * lineProgress
                                    clipRect(right = fillWidth) {
                                        drawText(textLayoutResult, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
