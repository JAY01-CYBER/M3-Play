/*
 * ♪ M3Play Signature Component
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 *
 * Signature: M3PLAY::SIGNATURE::UI::V1
 */

package com.j.m3play.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun TimeGreetingCard(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val subtitle = when (hour) {
        in 5..11 -> "Start your day with music"
        in 12..16 -> "Enjoy your day with music"
        in 17..20 -> "Relax with evening tunes"
        else -> "Slow down with night vibes"
    }

    val weatherIcon = when (hour) {
        in 5..11 -> "🌤️"
        in 12..16 -> "☀️"
        in 17..20 -> "🌙"
        else -> "🌌"
    }

    val sparkleText = when (hour) {
        in 17..23, in 0..4 -> "✦ ✦ ✦"
        else -> "✨ ✨"
    }

    val isMorning = hour in 5..11
    val isAfternoon = hour in 12..16
    val isNightLike = hour in 17..23 || hour in 0..4

    val transition = rememberInfiniteTransition(label = "time_card_v3")

    val emojiOffsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isMorning || isAfternoon) -4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiOffsetY"
    )

    val emojiScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isNightLike) 1.04f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiScale"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = if (isNightLike) 0.14f else 0.06f,
        targetValue = if (isNightLike) 0.30f else 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val sparkleAlpha by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    val searchPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searchPulse"
    )

    val streakOffsetX by transition.animateFloat(
        initialValue = -120f,
        targetValue = 320f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600),
            repeatMode = RepeatMode.Restart
        ),
        label = "streakOffsetX"
    )

    val starTwinkle by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    val gradientShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    var searchPressed by remember { mutableStateOf(false) }
    val searchScale by animateFloatAsState(
        targetValue = if (searchPressed) 0.92f else 1f,
        animationSpec = spring(),
        label = "searchPressScale"
    )

    val startColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f)
    val midColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f)
    val endColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)

    val animatedBrush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to startColor,
            (0.45f + (gradientShift * 0.15f)) to midColor,
            1f to endColor
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(animatedBrush)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (isMorning || isAfternoon) {
            Box(
                modifier = Modifier
                    .offset(x = streakOffsetX.dp, y = (-8).dp)
                    .size(width = 90.dp, height = 80.dp)
                    .alpha(0.10f)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(40.dp)
                    )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 2.dp)
                .size(50.dp)
                .alpha(glowAlpha)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    shape = CircleShape
                )
        )

        Text(
            text = sparkleText,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isNightLike) starTwinkle else sparkleAlpha),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp, top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isNightLike) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .alpha(glowAlpha)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                        )
                    }

                    Text(
                        text = weatherIcon,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .offset(y = emojiOffsetY.dp)
                            .scale(emojiScale)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "$subtitle ${if (isMorning || isAfternoon) "☀️" else if (isNightLike) "🌙" else ""}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .scale(searchPulse * searchScale)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        searchPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchClick()
                        searchPressed = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
