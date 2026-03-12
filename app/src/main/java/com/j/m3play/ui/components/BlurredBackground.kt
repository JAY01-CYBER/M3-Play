package com.j.m3play.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun BlurredBackground(imageUrl: String, playbackProgress: Float) {
    val transition = rememberInfiniteTransition(label = "bg")
    val shimmer = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Reverse),
        label = "shimmer",
    )
    val drift = transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "drift",
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = drift.value
                    translationY = drift.value * 0.35f
                }
                .blur(50.dp)
                .alpha(0.45f),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xAA35215D),
                            Color(0x884A86A8),
                            Color(0xAA10283A),
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 1200f + 300f * playbackProgress),
                    ),
                ),
        )

        Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
            val grainOffset = shimmer.value * size.minDimension
            for (x in 0..20) {
                val px = (x * size.width / 20f + grainOffset) % size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(px, 0f),
                    end = Offset((px + 120f) % size.width, size.height),
                    blendMode = BlendMode.Overlay,
                )
            }
        }
    }
}
