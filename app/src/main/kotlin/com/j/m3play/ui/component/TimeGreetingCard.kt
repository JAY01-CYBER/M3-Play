/*
 * ♪ M3Play Signature Component
 * File: TimeGreetingCard.kt
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 * 
 * Signature: M3PLAY::SIGNATURE::TIME_GREETING::V2 (Gradient & 6-States)
 */

package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// Data class to hold 6 different states
data class TimeGreetingData(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val gradientColors: List<Color>,
    val textColor: Color
)

@Composable
fun TimeGreetingCard(
    onMixClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    // 6-State Time Logic matching your design requirements
    val greetingState = when (hour) {
        in 4..6 -> TimeGreetingData(
            "Good Morning 🌅", "Start your day with music", "Morning Mix",
            listOf(Color(0xFF8360C3), Color(0xFF2EBF91)), Color.White
        )
        in 7..11 -> TimeGreetingData(
            "Good Morning ☀️", "Start your day with music", "Morning Mix",
            listOf(Color(0xFFD4FC79), Color(0xFF96E6A1)), Color(0xFF1E1E1E)
        )
        in 12..15 -> TimeGreetingData(
            "Good Afternoon ☀️", "Keep the energy going", "Afternoon Vibes",
            listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)), Color(0xFF1E1E1E)
        )
        in 16..18 -> TimeGreetingData(
            "Good Evening 🌇", "Unwind with mellow tunes", "Evening Vibes",
            listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), Color.White
        )
        in 19..22 -> TimeGreetingData(
            "Good Night 🌙", "Relax and listen", "Night Mix",
            listOf(Color(0xFF141E30), Color(0xFF243B55)), Color.White
        )
        else -> TimeGreetingData( // 23 to 3 (Late Night)
            "Still Awake? 🌚", "Let the music keep you company", "Late Night Mix",
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), Color.White
        )
    }

    val isNightLike = hour in 17..23 || hour in 0..4
    val sparkleText = if (isNightLike) "✦  ✦" else "✨ ✨"

    // Preserving your custom animations
    val transition = rememberInfiniteTransition(label = "time_card_v4")

    val glowAlpha by transition.animateFloat(
        initialValue = if (isNightLike) 0.15f else 0.08f,
        targetValue = if (isNightLike) 0.35f else 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val streakOffsetX by transition.animateFloat(
        initialValue = -120f,
        targetValue = 350f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Restart
        ),
        label = "streakOffsetX"
    )

    val starTwinkle by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
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
            animation = tween(5000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    // Animated Background Brush
    val animatedBrush = Brush.linearGradient(
        colors = greetingState.gradientColors,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, gradientShift * 500f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(160.dp) // Fixed height for that premium big card look
            .clip(RoundedCornerShape(24.dp))
            .background(animatedBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f), // Subtle glass border
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Animation Layer: Streaks for daytime
        if (!isNightLike) {
            Box(
                modifier = Modifier
                    .offset(x = streakOffsetX.dp, y = (-10).dp)
                    .size(width = 100.dp, height = 180.dp)
                    .alpha(0.12f)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Animation Layer: Corner Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 12.dp)
                .size(60.dp)
                .alpha(glowAlpha)
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                )
        )

        // Animation Layer: Sparkles/Stars
        Text(
            text = sparkleText,
            color = greetingState.textColor.copy(alpha = if (isNightLike) starTwinkle else starTwinkle * 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
        )

        // Content Layer (Without Search Button)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greetingState.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = greetingState.textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greetingState.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = greetingState.textColor.copy(alpha = 0.85f)
                )
            }

            // The New "Mix" Button
            Button(
                onClick = onMixClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.25f) // Glassy semi-transparent dark button
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Mix",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = greetingState.buttonText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
