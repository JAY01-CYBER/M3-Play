package com.j.m3play.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object ShapeTokens {
    val card = RoundedCornerShape(24.dp)
    val control = RoundedCornerShape(20.dp)
    val artwork = RoundedCornerShape(28.dp)
}

object ElevationOverlays {
    @Composable
    fun surface(level: Float): Color {
        val base = MaterialTheme.colorScheme.surface
        val tint = MaterialTheme.colorScheme.primary
        return base.copy(alpha = 0.68f).harmonize(tint, level.coerceIn(0f, 1f) * 0.45f)
    }
}

object GlassSurfaces {
    @Composable
    fun containerBrush(): Brush {
        val top = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        val bottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        return Brush.verticalGradient(listOf(top, bottom))
    }

    @Composable
    fun borderColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
}

@Composable
fun FrostedLayer(
    modifier: Modifier = Modifier,
    shape: Shape = ShapeTokens.card,
    blurRadius: Int = 20,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .blur(blurRadius.dp)
            .background(GlassSurfaces.containerBrush())
            .border(1.dp, GlassSurfaces.borderColor(), shape),
        content = content,
    )
}

fun Color.harmonize(target: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * t,
        green = green + (target.green - green) * t,
        blue = blue + (target.blue - blue) * t,
        alpha = alpha,
    )
}

object MotionSpecs {
    val fluidSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    val mediumTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = 450,
        easing = EasingEmphasized,
    )

    val listEnterTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = 380,
        easing = EasingStandard,
    )

    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EasingStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}
