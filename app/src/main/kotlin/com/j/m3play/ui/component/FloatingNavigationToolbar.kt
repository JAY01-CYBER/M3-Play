package com.j.m3play.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean, // Compat parameter for older calls
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    // DIM COLOR FIX & BLACK BACKGROUND SEPARATION
    // Surface Container is best for modern M3 elevation. Fallback for pureBlack.
    val containerColor = if (pureBlack) {
        Color(0xFF1E1E1E) // Lighter black to create depth on pure black bg
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    // PREMIUM COMPACT DIMENSIONS
    val barHeight = 64.dp // Standard Material 3 Navigation Bar Height (Floating)
    val totalItemsWidth = 240.dp // To ensure perfectly symmetrical float

    Box(
        modifier = modifier
            .navigationBarsPadding() // Crucial for bottom indicator spacing
            .padding(bottom = 24.dp) // Premium distance from bottom
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // MAIN SINGLE SYMMETRICAL PILL
        Surface(
            modifier = Modifier
                .width(totalItemsWidth)
                .height(barHeight),
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape, // Perfectly spherical ends
            shadowElevation = 12.dp, // Expressive high float
            tonalElevation = 6.dp, // Modern M3 color blending
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center, // Centers items in the pill
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    
                    // Manually created M3 Pill Item for dynamic floating context
                    ModernM3PillItem(
                        screen = screen,
                        selected = selected,
                        accentColor = accentColor,
                        onClick = { onItemClick(screen, selected) },
                        modifier = Modifier.weight(1f) // Ensures items are equal and symmetrical
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernM3PillItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val colorAnimationSpec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)

    // Standard M3 Colors
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = colorAnimationSpec,
        label = "icon_color"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = colorAnimationSpec,
        label = "label_color"
    )
    
    // Premium standard ripple effect
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default indication for custom pill indication below
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ICON CONTAINER (Pill highlight behind the icon)
        Box(
            modifier = Modifier
                .height(32.dp) // Standard M3 Indicator Height
                .width(64.dp) // Standard M3 Indicator Width
                .clip(CircleShape) // Standard M3 Pill Shape
                .then(
                    if (selected) Modifier.background(accentColor.copy(alpha = 0.2f)) // The M3 "pop"
                    else Modifier // Non-selected transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(24.dp), // Standard M3 Icon Size
                tint = iconColor
            )
        }

        Spacer(Modifier.height(4.dp))

        // LABEL TEXT (Dynamic animation, proper weight)
        Text(
            text = stringResource(screen.titleId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
