package com.j.m3play.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface

    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp,
        modifier = modifier
            .widthIn(max = if (slim) 260.dp else 300.dp)
            
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) 
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            home?.let {
                ExpressiveFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
            }
            library?.let {
                ExpressiveFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
            }
            search?.let {
                ExpressiveFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
            }
        }
    }
}

@Composable
private fun ExpressiveFloatingNavItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            selected -> 1f
            else -> 0.95f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_nav_item_scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "content_color"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, 
                onClick = onClick
            )
            // Yahan bhi animateContentSize lagaya hai taaki individual pill smoothly stretch ho
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected) 16.dp else 12.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                    contentDescription = stringResource(screen.titleId),
                    modifier = Modifier.size(20.dp)
                )
            }

            
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    expandFrom = Alignment.Start, 
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start, 
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(screen.titleId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
