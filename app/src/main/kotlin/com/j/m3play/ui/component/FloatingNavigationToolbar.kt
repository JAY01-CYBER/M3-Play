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
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val mainContainerColor = if (pureBlack) {
        Color(0xFF141414)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    // SLIM & ELEGANT DIMENSIONS
    val barHeight = 56.dp
    val innerItemHeight = 42.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val home = items.firstOrNull { it.route == Screens.Home.route }
        val library = items.firstOrNull { it.route == Screens.Library.route }
        val search = items.firstOrNull { it.route == Screens.Search.route }

        // 1. Main Navigation Pill (Home & Library)
        Surface(
            color = mainContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            shadowElevation = 12.dp, // High elevation for better floating look
        ) {
            Row(
                modifier = Modifier
                    .height(barHeight) // Strict height for perfect symmetry
                    .padding(horizontal = 8.dp), // Space from left and right edges
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Breathing space between tabs
                verticalAlignment = Alignment.CenterVertically,
            ) {
                home?.let {
                    GoogleFloatingNavItem(it, isSelected(it), accentColor, innerItemHeight) { onItemClick(it, isSelected(it)) }
                }
                library?.let {
                    GoogleFloatingNavItem(it, isSelected(it), accentColor, innerItemHeight) { onItemClick(it, isSelected(it)) }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // 2. Search Floating Action Button (FAB)
        search?.let {
            Surface(
                color = mainContainerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                shadowElevation = 12.dp,
                modifier = Modifier.size(barHeight) // Matches Pill height exactly
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = accentColor),
                            onClick = { onItemClick(it, isSelected(it)) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isSelected(it)) it.iconIdActive else it.iconIdInactive),
                        contentDescription = stringResource(it.titleId),
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected(it)) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleFloatingNavItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    itemHeight: Dp,
    onClick: () -> Unit,
) {
    val animationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val colorAnimationSpec = tween<Color>(durationMillis = 300, easing = LinearOutSlowInEasing)

    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = colorAnimationSpec,
        label = "bg_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = colorAnimationSpec,
        label = "content_color"
    )

    Row(
        modifier = Modifier
            .height(itemHeight) // Controlled inner height to prevent "sausage" effect
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = if (selected) 20.dp else 16.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = animationSpec) + expandHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start
            ),
            exit = fadeOut(animationSpec = animationSpec) + shrinkHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start
            )
        ) {
            Text(
                text = stringResource(screen.titleId),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
