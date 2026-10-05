package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import com.mocharealm.accompanist.lyrics.ui.internal.effects.lyricsVisualLayer

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp

@Composable
fun LyricsLineItem(
    isFocused: Boolean,
    isRightAligned: Boolean,
    onLineClicked: () -> Unit,
    onLinePressed: () -> Unit,
    blurRadius: () -> Float,
    modifier: Modifier = Modifier,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.4f,
    blendMode: BlendMode = BlendMode.SrcOver,
    isInteractive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scaleState by
        animateFloatAsState(
            targetValue = if (isFocused) 1f else 0.98f,
            animationSpec =
                if (isFocused) {
                    tween(durationMillis = 600, easing = LinearOutSlowInEasing)
                } else {
                    tween(durationMillis = 300, easing = EaseInOut)
                },
            label = "scale",
        )

    val alphaState by
        animateFloatAsState(
            targetValue = if (isFocused) activeAlpha else inactiveAlpha,
            label = "alpha",
        )

    Box(
        modifier =
            modifier.fillMaxWidth().lyricsVisualLayer {
                scaleX = scaleState
                scaleY = scaleState
                alpha = alphaState
                transformOrigin = TransformOrigin(if (isRightAligned) 1f else 0f, 1f)
                this.blendMode = blendMode
                compositingStrategy = CompositingStrategy.Auto

                val radius = blurRadius()
                renderEffect =
                    if (radius > 0f) {
                        BlurEffect(
                            radiusX = radius,
                            radiusY = radius,
                            edgeTreatment = TileMode.Clamp,
                        )
                    } else null
            }
    ) {
        // Clip the ripple, never the lyrics that can draw beyond their animated layout height.
        if (isInteractive)
            Box(
                Modifier.matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = onLineClicked, onLongClick = onLinePressed)
            )
        content()
    }
}
