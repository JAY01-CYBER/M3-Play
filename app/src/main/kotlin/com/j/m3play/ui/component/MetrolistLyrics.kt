package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.LocalPlayerConnection
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class MetroLyricLine(val text: String, val startTimeMs: Long)

@Composable
fun MetrolistLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // LRC Parsing
    val parsedLyrics = remember(currentLyrics) {
        val lyricsText = currentLyrics?.toString() ?: ""
        if (lyricsText.isBlank()) emptyList()
        else {
            val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
            lyricsText.lines().mapNotNull { line ->
                regex.find(line)?.let { match ->
                    val (min, sec, mil, text) = match.destructured
                    val timeMs = (min.toLong() * 60 * 1000) + (sec.toLong() * 1000) + (if (mil.length == 2) mil.toLong() * 10 else mil.toLong())
                    if (text.trim().isNotEmpty()) MetroLyricLine(text.trim(), timeMs) else null
                }
            }
        }
    }

    val position = sliderPositionProvider() ?: playerConnection.player.currentPosition
    
    // Active Index Calculation
    val activeIndex by remember(position, parsedLyrics) {
        derivedStateOf {
            val idx = parsedLyrics.indexOfLast { it.startTimeMs <= position + 250L }
            if (idx != -1) idx else 0
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No synced lyrics available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    // MetroList Custom Manual Drag & Velocity Scroll Logic
    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Auto-scroll back to center
    LaunchedEffect(isAutoScrollEnabled, activeIndex) {
        if (isAutoScrollEnabled && abs(userManualOffset) > 1f) {
            val start = userManualOffset
            val anim = Animatable(start)
            var lastValue = start
            anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                userManualOffset += (value - lastValue)
                lastValue = value
            }
            userManualOffset = 0f
        }
    }

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
        val lineHeightPx = with(density) { 68.dp.toPx() } 
        val gapPx = with(density) { 16.dp.toPx() }
        
        parsedLyrics.forEachIndexed { listIndex, line ->
            val distance = abs(listIndex - activeIndex)
            val targetOffset = anchorY + ((listIndex - activeIndex) * (lineHeightPx + gapPx))
            
            // EXACT MetroList Staggered delay scroll animation
            val animatedOffset by animateFloatAsState(
                targetValue = if (isAutoScrollEnabled) targetOffset else targetOffset, 
                animationSpec = tween(750, (distance * 20).coerceAtMost(200), FastOutSlowInEasing),
                label = "offset_$listIndex"
            )

            val isActive = listIndex == activeIndex
            val isPassed = listIndex < activeIndex
            
            // MetroList Alpha Fade
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPassed) 0.3f else 0.5f,
                animationSpec = tween(400, easing = LinearOutSlowInEasing),
                label = "alpha_$listIndex"
            )

            // EXACT MetroList Bouncy Scale
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "scale_$listIndex"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        playerConnection.player.seekTo(line.startTimeMs)
                        isAutoScrollEnabled = true
                        userManualOffset = 0f
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = line.text,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
