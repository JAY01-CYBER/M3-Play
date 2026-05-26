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
    // Pure black user preference ya standard Material 3 surfaceContainer fallback
    val mainContainerColor = if (pureBlack) {
        Color(0xFF000000)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Home aur Library ka Main Navigation Pill
        Surface(
            color = mainContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            shadowElevation = 12.dp,
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .animateContentSize(animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        Spacer(Modifier.width(16.dp))

        // 2. Search ka Circular Floating Action Button
        search?.let {
            Surface(
                color = mainContainerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                shadowElevation = 12.dp,
                tonalElevation = 6.dp,
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

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, 
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val bg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bg_color"
    )

    val color by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
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
            ),
        shape = CircleShape,
        color = bg,
        contentColor = color,
    ) {
        Row(
            modifier = Modifier
                .then(if (!isDetached) Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) else Modifier)
                .padding(
                    horizontal = if (selected && !isDetached) 20.dp else 16.dp,
                    vertical = 16.dp
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(26.dp)
            )

            if (selected && !isDetached) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
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
}
