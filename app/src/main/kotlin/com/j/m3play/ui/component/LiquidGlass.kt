/**
 * Glossy Project (C) 2026
 * Liquid Glass Physics officially matching Simp Music configuration
 */
package com.j.m3play.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost

import com.kyant.backdrop.*
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.tanh

import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

@Composable
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun rememberBackdrop(color: Color = Color.Unspecified): Backdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { 
        LayerBackdrop(graphicsLayer = graphicsLayer, onDraw = {}) 
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun Modifier.layerBackdrop(backdrop: Backdrop): Modifier {
    return if (backdrop is LayerBackdrop) {
        this.then(Modifier.kyantLayerBackdrop(backdrop))
    } else {
        this
    }
}

@Composable
fun rememberGlassInteraction(): InteractiveHighlight {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope) { InteractiveHighlight(animationScope) }
}

fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: Backdrop,
    layer: GraphicsLayer?,
    luminanceAnimation: Float,
    shape: Shape,
    interaction: InteractiveHighlight?
): Modifier = composed {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            val press = interaction?.pressProgress ?: 0f
            
            // EXACT SIMP MUSIC FORMULA
            vibrancy()
            colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
            
            val blurRadius = if (l > 0f) {
                androidx.compose.ui.util.lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
            } else {
                androidx.compose.ui.util.lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
            }
            blur(blurRadius + 2f.dp.toPx() * press)
            
            lens(size.minDimension / 4f + 2f.dp.toPx() * press, size.minDimension / 2f, false)
        },
        layerBlock = {
            if (interaction != null) {
                val width = size.width
                val height = size.height
                val progress = interaction.pressProgress
                val scale = androidx.compose.ui.util.lerp(1f, 1.12f, progress) // 1.12 press scale
                
                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                val offset = interaction.offset
                
                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                
                val maxDragScale = 4f.dp.toPx() / size.height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
                scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
            }
        },
        onDrawSurface = {
            // Darken more as the background brightens so the glass never washes out
            val darken = androidx.compose.ui.util.lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            val baseColor = if (isDark) Color.Black else Color.White
            drawRect(baseColor.copy(alpha = darken))
            
            val press = interaction?.pressProgress ?: 0f
            if (press > 0f) {
                // Simp Music Radial Glow Effect
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f * press),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 1.2f
                    ),
                    blendMode = BlendMode.Plus
                )
            }
        }
    )
    .border(0.5.dp, Color.White.copy(alpha = 0.20f), shape)
    .then(interaction?.modifier ?: Modifier)
    .then(interaction?.gestureModifier ?: Modifier)
}

fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    isInteractive: Boolean = true
): Modifier = composed {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    
    this.drawInteractiveGlass(
        isDark = true, // Force dark mode aesthetic for music players
        backdrop = backdrop,
        layer = null,
        luminanceAnimation = 0.5f, // Neutral static luminance
        shape = shape,
        interaction = if (isInteractive) interactiveHighlight else null
    )
}


/**
 * Haze-backed Liquid Glass surface used by playlist controls.
 *
 * The playlist LazyColumn is itself a Haze source, so controls inside that source
 * explicitly select All; the default Behind selection would skip their ancestor source.
 */
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: Shape,
    isInteractive: Boolean = true
): Modifier = composed {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    val hazeStyle = remember {
        HazeBlurStyle {
            blurRadius(26.dp)
            noiseFactor(0.10f)
            backgroundColor(Color.Black.copy(alpha = 0.16f))
            colorEffects(
                listOf(
                    HazeColorEffect.tint(Color.White.copy(alpha = 0.14f))
                )
            )
        }
    }

    this
        .clip(shape)
        .hazeBlur(
            input = HazeInput.Sources(
                state = hazeState,
                selection = HazeSourceSelection.Behind
            ),
            style = hazeStyle
        )
        .border(0.7.dp, Color.White.copy(alpha = 0.34f), shape)
        .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
        .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
}

@Composable
fun LiquidGlassIconButton(
    hazeState: HazeState,
    painter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(hazeState, shape, isInteractive = true)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop,
    painter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(backdrop, shape, isInteractive = true)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, 
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter, contentDescription = null, tint = Color.White)
    }
}
