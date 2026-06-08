package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// 1. Google Play Style Liquid Morphing Loader (Oval -> Star -> Pentagon) for Pull-to-Refresh
@Composable
fun BouncyLoadingIndicator(modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(0) }

    // Control karta hai ki ek shape kitni der screen par rukegi aur kab morph hogi
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200) // Pause time before changing shape
            step += 1
        }
    }

    // Smooth transition from one shape to the next
    val animatedProgress by animateFloatAsState(
        targetValue = step.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "shape_morph"
    )

    // Lagataar ghoomne (rotation) ke liye
    val infiniteTransition = rememberInfiniteTransition(label = "rotation_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(48.dp)) {
        val center = this.center
        // Base radius thoda chota rakha hai taaki points/lobes canvas se bahar na jayen
        val baseRadius = size.minDimension / 2 * 0.75f

        // Safe interpolation values nikal rahe hain
        val progressVal = animatedProgress.coerceAtLeast(0f)
        val shapeA = floor(progressVal).toInt() % 5
        val shapeB = (floor(progressVal).toInt() + 1) % 5
        val fraction = progressVal - floor(progressVal)

        val path = Path()
        val points = 150 // High resolution for perfectly smooth liquid curves

        for (i in 0..points) {
            val angle = (i.toFloat() / points) * (2 * Math.PI).toFloat()

            // Dono shapes (Current aur Next) ka radius calculate karte hain
            val rA = getShapeRadius(shapeA, angle, baseRadius)
            val rB = getShapeRadius(shapeB, angle, baseRadius)
            
            // Un dono radius ko fraction ke hisaab se mix (interpolate) karte hain
            val currentRadius = rA + (rB - rA) * fraction

            val x = center.x + currentRadius * cos(angle)
            val y = center.y + currentRadius * sin(angle)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Pura path draw hone ke baad usko continuously rotate karna
        rotate(rotation) {
            drawPath(path = path, color = color)
        }
    }
}

// Ye function decide karta hai ki shape kaisa dikhega based on Math equations
private fun getShapeRadius(shapeIndex: Int, angle: Float, baseRadius: Float): Float {
    return when (shapeIndex) {
        0 -> baseRadius * (1f + 0.25f * sin(angle * 2))  // Oval (2 lobes)
        1 -> baseRadius * (1f + 0.15f * sin(angle * 10)) // 10-point Star
        2 -> baseRadius * (1f + 0.12f * cos(angle * 5))  // Pentagon
        3 -> baseRadius * (1f + 0.20f * cos(angle * 4))  // 4-point Clover
        4 -> baseRadius * (1f + 0.10f * sin(angle * 8))  // 8-point Wavy Blob
        else -> baseRadius
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

// Preview
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
