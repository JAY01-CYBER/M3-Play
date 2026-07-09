/*
 * ♪ M3Play Signature Component
 * File: TimeGreetingCard.kt
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 * 
 * Signature: M3PLAY::SIGNATURE::TIME_GREETING::V5 (Canvas Art Fixed)
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

    // Vibrant & Fixed Colors for 6 States
    val greetingState = when (hour) {
        in 4..6 -> TimeGreetingData(
            "Good Morning 🌅", "Start your day with music", "Morning Mix",
            listOf(Color(0xFF6A4C93), Color(0xFFB5838D)), 
            Color(0xFFFFB4A2), Color(0xFF352F44), Color(0xFF5C5470), false, Color.White
        )
        in 7..11 -> TimeGreetingData(
            "Good Morning ☀️", "Start your day with music", "Morning Mix",
            listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)), 
            Color(0xFFFFEA70), Color(0xFF68B984), Color(0xFF3D8361), false, Color(0xFF1E1E1E)
        )
        in 12..15 -> TimeGreetingData(
            "Good Afternoon ☀️", "Keep the energy going", "Afternoon Vibes",
            listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)), // Clear mid-day blue
            Color(0xFFF9D423), // Bright sun
            Color(0xFF38ef7d), Color(0xFF11998e), // Fresh green hills
            false, Color.White
        )
        in 16..18 -> TimeGreetingData(
            "Good Evening 🌇", "Unwind with mellow tunes", "Evening Vibes",
            listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), 
            Color(0xFFFFD56B), Color(0xFF8B4513), Color(0xFF5C2E0E), false, Color.White
        )
        in 19..22 -> TimeGreetingData(
            "Good Night 🌙", "Relax and listen", "Night Mix",
            listOf(Color(0xFF141E30), Color(0xFF243B55)), 
            Color(0xFFE0E0E0), Color(0xFF0B1320), Color(0xFF040914), true, Color.White
        )
        else -> TimeGreetingData( // 23 to 3 (Late Night)
            "Still Awake? 🌚", "Let the music keep you company", "Late Night Mix",
            listOf(Color(0xFF0F2027), Color(0xFF203A43)), 
            Color(0xFFB0BEC5), Color(0xFF050B14), Color(0xFF02050A), true, Color.White
        )
    }

    val isNightLike = greetingState.isNight
    val sparkleText = if (isNightLike) "✦  ✦" else "✨ ✨"

    val transition = rememberInfiniteTransition(label = "time_card_v5")

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
        
        // 1. PERFECTED DYNAMIC CANVAS BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = greetingState.skyColors,
                    startY = 0f,
                    endY = h
                ),
                size = size
            )

            // Stars for Night Time
            if (greetingState.isNight) {
                drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.006f, center = Offset(w * 0.15f, h * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.008f, center = Offset(w * 0.45f, h * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.4f), radius = w * 0.005f, center = Offset(w * 0.75f, h * 0.15f))
                drawCircle(Color.White.copy(alpha = 0.7f), radius = w * 0.007f, center = Offset(w * 0.85f, h * 0.3f))
            }

            // Sun or Moon (Scaled perfectly with height now)
            drawCircle(
                color = greetingState.sunMoonColor,
                radius = h * 0.25f, 
                center = Offset(w * 0.75f, h * 0.35f)
            )

            // Back Hill (Math fixed: Center pushed down so it only covers the bottom part)
            drawCircle(
                color = greetingState.hillBack,
                radius = w * 0.8f,
                center = Offset(w * 0.1f, h + (w * 0.55f)) 
            )

            // Front Hill
            drawCircle(
                color = greetingState.hillFront,
                radius = w * 0.7f,
                center = Offset(w * 0.9f, h + (w * 0.5f))
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
