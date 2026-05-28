/*
 * ♪ M3Play Signature Component
 * File: TimeGreetingCard.kt
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 *
 * Signature: M3PLAY::SIGNATURE::TIME_GREETING::PREMIUM_FINAL
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun TimeGreetingCard(
    userName: String? = null,
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

    val displayName = if (!userName.isNullOrBlank()) userName else "User"

    val transition = rememberInfiniteTransition(label = "time_card_premium")

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
        initialValue = if (isNightLike) 0.14f else 0.08f,
        targetValue = if (isNightLike) 0.35f else 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val searchPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searchPulse"
    )

    val gradientShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    var searchPressed by remember { mutableStateOf(false) }
    val searchScale by animateFloatAsState(
        targetValue = if (searchPressed) 0.90f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
        label = "searchPressScale"
    )

    // Premium Color Palette
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow

    val cardAnimatedBrush = Brush.linearGradient(
        colors = listOf(
            surfaceHigh.copy(alpha = 0.95f),
            surfaceLow.copy(alpha = 0.85f - (gradientShift * 0.1f)),
            primaryColor.copy(alpha = 0.08f + (gradientShift * 0.05f))
        )
    )

    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.4f),
            Color.Transparent,
            primaryColor.copy(alpha = 0.3f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            // Premium Soft Shadow
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = primaryColor.copy(alpha = 0.15f),
                ambientColor = primaryColor.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(cardAnimatedBrush)
            .border(
                width = 1.dp,
                brush = glassBorderBrush,
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp) 
    ) {
        // Glowing Ambient Orb (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-8).dp)
                .size(70.dp)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Text(
            text = sparkleText,
            color = primaryColor.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 2.dp, top = 0.dp)
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
                // Weather Icon Box with subtle inner shadow/glow
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .alpha(glowAlpha)
                            .background(
                                color = primaryColor.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    )

                    Text(
                        text = weatherIcon,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .offset(y = emojiOffsetY.dp)
                            .scale(emojiScale)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting, $displayName",
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Premium Floating Search Button
            Box(
                modifier = Modifier
                    .scale(searchPulse * searchScale)
                    .size(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = primaryColor.copy(alpha = 0.2f),
                        ambientColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        searchPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSearchClick()
                        searchPressed = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp) 
                )
            }
        }
    }
}
