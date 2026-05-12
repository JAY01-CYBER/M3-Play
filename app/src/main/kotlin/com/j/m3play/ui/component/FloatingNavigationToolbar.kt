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

    // Screens ko filter kar rahe hain
    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Home aur Library ka Pill
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            shadowElevation = 12.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                home?.let {
                    ExpressiveFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
                }
                library?.let {
                    ExpressiveFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 2. Search ka Detached Circular Button
        search?.let {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = CircleShape,
                shadowElevation = 12.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ExpressiveFloatingNavItem(
                        screen = it,
                        selected = isSelected(it),
                        accentColor = accentColor,
                        isDetached = true
                    ) { onItemClick(it, isSelected(it)) }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFloatingNavItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    isDetached: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy scale animation
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            selected -> 1f
            else -> 0.95f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val bg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val color by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .then(if (!isDetached) Modifier.animateContentSize() else Modifier),
        shape = CircleShape,
        color = bg,
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected && !isDetached) 16.dp else 12.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(22.dp)
            )

            if (selected && !isDetached) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                ) {
                    Row {
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
}
