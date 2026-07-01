/*
 * ♪ M3Play Signature Component
 * File: TimeGreetingCard.kt
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 * 
 * Signature: M3PLAY::SIGNATURE::TIME_GREETING::V4 (Pure Canvas Art, No Images Needed)
 */

package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// Naya data class jo colors aur Canvas properties store karega
data class TimeGreetingData(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val skyColors: List<Color>,
    val sunMoonColor: Color,
    val hillBack: Color,
    val hillFront: Color,
    val isNight: Boolean,
    val textColor: Color
)

@Composable
fun TimeGreetingCard(
    onMixClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    // 6-State Time Logic with Custom Drawn Vector Art Colors
    val greetingState = when (hour) {
        in 4..6 -> TimeGreetingData(
            "Good Morning 🌅", "Start your day with music", "Morning Mix",
            listOf(Color(0xFF6A4C93), Color(0xFFB5838D)), // Purple to Pink Sky
            Color(0xFFFFB4A2), // Soft Sun
            Color(0xFF352F44), Color(0xFF5C5470), false, Color.White
        )
        in 7..11 -> TimeGreetingData(
            "Good Morning ☀️", "Start your day with music", "Morning Mix",
            listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)), // Bright Blue Sky
            Color(0xFFFFEA70), // Bright Yellow Sun
            Color(0xFF68B984), Color(0xFF3D8361), false, Color(0xFF1E1E1E)
        )
        in 12..15 -> TimeGreetingData(
            "Good Afternoon ☀️", "Keep the energy going", "Afternoon Vibes",
            listOf(Color(0xFF4CA1AF), Color(0xFF2C3E50)), // Deep Blue Afternoon
            Color(0xFFFFFFFF), // White Sun
            Color(0xFF0082C8), Color(0xFF005C97), false, Color.White
        )
        in 16..18 -> TimeGreetingData(
            "Good Evening 🌇", "Unwind with mellow tunes", "Evening Vibes",
            listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), // Orange Sunset Sky
            Color(0xFFFFD56B), // Golden Sun
            Color(0xFF8B4513), Color(0xFF5C2E0E), false, Color.White
        )
        in 19..22 -> TimeGreetingData(
            "Good Night 🌙", "Relax and listen", "Night Mix",
            listOf(Color(0xFF141E30), Color(0xFF243B55)), // Dark Starry Blue
            Color(0xFFE0E0E0), // Moon
            Color(0xFF0B1320), Color(0xFF040914), true, Color.White
        )
        else -> TimeGreetingData( // 23 to 3 (Late Night)
            "Still Awake? 🌚", "Let the music keep you company", "Late Night Mix",
            listOf(Color(0xFF000000), Color(0xFF1A1A24)), // Pitch Black
            Color(0xFFCCCCCC), // Dim Moon
            Color(0xFF0A0A10), Color(0xFF050508), true, Color.White
        )
    }

    val isNightLike = greetingState.isNight
    val sparkleText = if (isNightLike) "✦  ✦" else "✨ ✨"

    val transition = rememberInfiniteTransition(label = "time_card_v4")

    val glowAlpha by transition.animateFloat(
        initialValue = if (isNightLike) 0.15f else 0.08f,
        targetValue = if (isNightLike) 0.35f else 0.18f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val starTwinkle by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(1700), repeatMode = RepeatMode.Reverse),
        label = "starTwinkle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(160.dp) 
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        // 1. DYNAMIC CANVAS BACKGROUND (Ye aapke liye pahaad aur aasman draw karega)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky Gradient
            drawRect(Brush.verticalGradient(greetingState.skyColors))

            // Stars for Night Time
            if (isNightLike) {
                drawCircle(Color.White.copy(alpha = 0.4f), radius = 3f, center = Offset(w * 0.2f, h * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = 2f, center = Offset(w * 0.5f, h * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.8f), radius = 4f, center = Offset(w * 0.8f, h * 0.3f))
                drawCircle(Color.White.copy(alpha = 0.3f), radius = 2f, center = Offset(w * 0.3f, h * 0.4f))
            }

            // Sun or Moon
            val celestialX = if (isNightLike) w * 0.8f else w * 0.75f
            val celestialY = if (isNightLike) h * 0.3f else h * 0.4f
            drawCircle(
                color = greetingState.sunMoonColor,
                radius = if (isNightLike) 40f else 60f,
                center = Offset(celestialX, celestialY)
            )

            // Back Hill
            drawCircle(
                color = greetingState.hillBack,
                radius = w * 0.8f,
                center = Offset(w * 0.2f, h * 1.3f)
            )

            // Front Hill
            drawCircle(
                color = greetingState.hillFront,
                radius = w * 0.7f,
                center = Offset(w * 0.9f, h * 1.4f)
            )
        }

        // 2. CORNER GLOW & SPARKLES
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 12.dp)
                .size(60.dp)
                .alpha(glowAlpha)
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
        )
        Text(
            text = sparkleText,
            color = greetingState.textColor.copy(alpha = if (isNightLike) starTwinkle else starTwinkle * 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 16.dp)
        )

        // 3. TEXT & MIX BUTTON
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greetingState.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = greetingState.textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greetingState.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = greetingState.textColor.copy(alpha = 0.85f)
                )
            }

            // The Mix Button
            Button(
                onClick = onMixClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.25f)),
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
