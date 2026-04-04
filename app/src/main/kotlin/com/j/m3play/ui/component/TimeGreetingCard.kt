package com.j.m3play.ui.component

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun TimeGreetingCard(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        in 12..16 -> "Enjoy your day with music ☀️"
        in 17..20 -> "Relax with evening tunes 🌙"
        else -> "Slow down with night vibes 🌌"
    }

    val emoji = when (hour) {
        in 5..11 -> "🌤️"
        in 12..16 -> "☀️"
        in 17..20 -> "🌙"
        else -> "🌌"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "✨✨",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.TopEnd)
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
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
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

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSearchClick()
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
