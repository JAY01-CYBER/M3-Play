package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.j.m3play.LocalPlayerAwareWindowInsets
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ==========================================
// 1. M3E LIQUID MORPHING INDICATOR (Video Wala)
// ==========================================
@Composable
fun BouncyLoadingIndicator(modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200) 
            step += 1
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = step.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "shape_morph"
    )

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
        val baseRadius = size.minDimension / 2 * 0.75f

        val progressVal = animatedProgress.coerceAtLeast(0f)
        val shapeA = floor(progressVal).toInt() % 5
        val shapeB = (floor(progressVal).toInt() + 1) % 5
        val fraction = progressVal - floor(progressVal)

        val path = Path()
        val points = 150 

        for (i in 0..points) {
            val angle = (i.toFloat() / points) * (2 * Math.PI).toFloat()

            val rA = getShapeRadius(shapeA, angle, baseRadius)
            val rB = getShapeRadius(shapeB, angle, baseRadius)
            
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

        rotate(rotation) {
            drawPath(path = path, color = color)
        }
    }
}

private fun getShapeRadius(shapeIndex: Int, angle: Float, baseRadius: Float): Float {
    return when (shapeIndex) {
        0 -> baseRadius * (1f + 0.25f * sin(angle * 2))  // Oval
        1 -> baseRadius * (1f + 0.15f * sin(angle * 10)) // 10-point Star
        2 -> baseRadius * (1f + 0.12f * cos(angle * 5))  // Pentagon
        3 -> baseRadius * (1f + 0.20f * cos(angle * 4))  // 4-point Clover
        4 -> baseRadius * (1f + 0.10f * sin(angle * 8))  // 8-point Wavy Blob
        else -> baseRadius
    }
}

// ==========================================
// 2. M3E WAVY PROGRESS RING
// ==========================================
@Composable
fun CrinkledProgressRing(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_transition")
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)), label = "rotation")
    val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)), label = "wave_offset")
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(48.dp)) {
        val strokeW = 4.dp.toPx()
        val radius = size.minDimension / 2 - strokeW
        val center = this.center
        val path = Path()
        val points = 100
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * (2 * Math.PI)
            val variation = sin(angle * 3 + waveOffset) * (radius * 0.12f)
            val currentRadius = radius + variation
            val x = center.x + (currentRadius * cos(angle)).toFloat()
            val y = center.y + (currentRadius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        rotate(rotation) {
            drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

// ==========================================
// 3. ARCHIVETUNE STYLE PULL TO REFRESH BOX
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val indicatorPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            val distanceFraction = state.distanceFraction

            if (isRefreshing || distanceFraction > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(indicatorPadding)
                        .padding(top = 16.dp)
                        .size(52.dp)
                        .graphicsLayer {
                            translationY = if (isRefreshing) 0f else (distanceFraction * 120f - 120f).coerceAtMost(0f)
                            alpha = if (isRefreshing) 1f else distanceFraction.coerceIn(0f, 1f)
                            scaleX = if (isRefreshing) 1f else distanceFraction.coerceIn(0.5f, 1f)
                            scaleY = if (isRefreshing) 1f else distanceFraction.coerceIn(0.5f, 1f)
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        BouncyLoadingIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        content = content,
    )
}
