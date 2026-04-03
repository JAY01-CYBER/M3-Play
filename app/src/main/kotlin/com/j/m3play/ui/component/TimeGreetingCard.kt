package com.j.m3play.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun TimeGreetingCard(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val subtitle = when (hour) {
        in 5..11 -> "Start your day with music ☀️"
        in 12..16 -> "Boost your mood with beats 🔥"
        in 17..20 -> "Relax with evening tunes 🌙"
        else -> "Slow down and enjoy the night 🌌"
    }

    val emoji = when (hour) {
        in 5..11 -> "🌤️"
        in 12..16 -> "☀️"
        in 17..20 -> "🌙"
        else -> "🌌"
    }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "greeting_card_anim")

    val emojiScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiScale"
    )

    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    var searchPressed by remember { mutableStateOf(false) }
    val searchScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (searchPressed) 0.92f else 1f,
        animationSpec = spring(),
        label = "searchScale"
    )
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(420)
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "✨ ✨",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .alpha(sparkleAlpha * 0.65f),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.scale(emojiScale)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(searchScale)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    searchPressed = true
                                    delay(90)
                                    searchPressed = false
                                    onSearchClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
