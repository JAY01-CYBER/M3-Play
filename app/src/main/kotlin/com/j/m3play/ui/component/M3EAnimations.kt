package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// 1. Google Play Style Blob Morphing Loader (For Pull-to-Refresh)
@Composable
fun BouncyLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "blob_transition")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Morphs between a perfect circle (0f) and a 4-petal squishy shape (1f)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morphing"
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(48.dp)) {
        val radius = size.minDimension / 2
        val center = this.center

        // Determines how deep the "squish" goes
        val squishDepth = radius * 0.35f * morphProgress

        val path = Path()
        val points = 80 // High point count for perfectly smooth curves
        
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * (2 * Math.PI)
            
            // Creates the 4-sided "star" or "flower" wobble
            val variation = sin(angle * 4) * squishDepth
            val currentRadius = radius - variation

            val x = center.x + (currentRadius * cos(angle)).toFloat()
            val y = center.y + (currentRadius * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        rotate(rotation) {
            drawPath(path = path, color = color)
        }
    }
}

// 2. Google Play Style Indeterminate Wavy Ring (For Lists & Bottom Loading)
@Composable
fun CrinkledProgressRing(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_transition")

    // Spins the entire ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Animates the wave flowing through the ring
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "wave_offset"
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(48.dp)) {
        // Subtract stroke width so it doesn't clip outside the canvas
        val strokeW = 4.dp.toPx()
        val radius = size.minDimension / 2 - strokeW
        val center = this.center
        
        val path = Path()
        val points = 100
        
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * (2 * Math.PI)
            
            // 3 distinct waves that ripple around the circle
            val variation = sin(angle * 3 + waveOffset) * (radius * 0.12f)
            val currentRadius = radius + variation

            val x = center.x + (currentRadius * cos(angle)).toFloat()
            val y = center.y + (currentRadius * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        rotate(rotation) {
            drawPath(
                path = path, 
                color = color, 
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun M3EAnimationsPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                BouncyLoadingIndicator()
                CrinkledProgressRing()
            }
        }
    }
}
