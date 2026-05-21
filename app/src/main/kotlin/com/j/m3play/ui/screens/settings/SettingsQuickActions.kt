/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsQuickActionsSection(
    actions: List<SettingsQuickAction>,
    columns: Int = SettingsDimensions.CompactColumns,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val rows = actions.chunked(columns)
        rows.forEach { rowActions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    action: SettingsQuickAction,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.TilePressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "tileScale",
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.IconPressRotation else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconRotation",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .aspectRatio(SettingsDimensions.QuickActionTileAspectRatio),
        shape = RoundedCornerShape(SettingsDimensions.QuickActionCardCornerRadius),
        // Premium Expensive Color
        color = MaterialTheme.colorScheme.surfaceContainerHigh, 
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            action.accentColor.copy(alpha = 0.15f), // Richer Gradient Start
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(16.dp), // Slightly more breathing room
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(SettingsDimensions.QuickActionIconSize)
                        .clip(RoundedCornerShape(16.dp)) // Squircle Premium Shape
                        .background(action.accentColor.copy(alpha = 0.20f)), // Richer Inner Color
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = action.icon,
                        contentDescription = action.label,
                        tint = action.accentColor,
                        modifier = Modifier
                            .size(SettingsDimensions.QuickActionIconInnerSize)
                            .graphicsLayer { rotationZ = iconRotation },
                    )
                }

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleSmall, // Better Hierarchy
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
