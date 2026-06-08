package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// 1. The Bouncy Morphing Loading Indicator
@Composable
fun BouncyLoadingIndicator(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isExpanded = !isExpanded
            delay(600)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.2f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BouncyScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 12.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MorphShape"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(cornerRadius)
            )
    )
}

// 2. The Crinkled (Wavy) Progress Ring
@Composable
fun CrinkledProgressRing(
    modifier: Modifier = Modifier,
    waveCount: Int = 8,
    waveAmplitude: Float = 10f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CrinkledRing")
    
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseOffset"
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(64.dp)) {
        val center = this.center
        val baseRadius = size.minDimension / 2 - waveAmplitude - 4.dp.toPx()
        val path = Path()

        val steps = 120
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2 * Math.PI).toFloat()
            val radiusVariation = sin(angle * waveCount + phaseOffset) * waveAmplitude
            val currentRadius = baseRadius + radiusVariation

            val x = center.x + currentRadius * cos(angle)
            val y = center.y + currentRadius * sin(angle)

            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// 3. Preview Section
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
