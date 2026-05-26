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
    // Google Material 3 Surface Colors
    val mainContainerColor = if (pureBlack) {
        Color(0xFF111111) // Pure AMOLED black ke liye halka sa grey tone for separation
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp), // Thoda upar float karega Google jaisa
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Main Navigation Pill (Home & Library)
        Surface(
            color = mainContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            shadowElevation = 6.dp, // Google standard floating elevation
        ) {
            Row(
                modifier = Modifier.padding(8.dp), // Inner padding for pill
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                home?.let {
                    GoogleFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
                }
                library?.let {
                    GoogleFloatingNavItem(it, isSelected(it), accentColor) { onItemClick(it, isSelected(it)) }
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
                shadowElevation = 6.dp,
                modifier = Modifier.size(56.dp) // Standard Google FAB Size
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = accentColor), // Google M3 Ripple Effect
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
    onClick: () -> Unit,
) {
    // Standard M3 easing & duration (bouncy spring ki jagah professional smooth tween)
    val animationSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
    val colorAnimationSpec = tween<Color>(durationMillis = 250, easing = LinearOutSlowInEasing)

    // Background Container Color (Active tab ke liye Google style)
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = colorAnimationSpec,
        label = "bg_color"
    )

    // Icon & Text Color
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = colorAnimationSpec,
        label = "content_color"
    )

    Row(
        modifier = Modifier
            .height(48.dp) // Standard Google touch target height
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor), // M3 Touch Ripple
                onClick = onClick
            )
            .padding(horizontal = if (selected) 20.dp else 16.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )

        // Text animation with clean fade
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = animationSpec) + expandHorizontally(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start
            ),
            exit = fadeOut(animationSpec = animationSpec) + shrinkHorizontally(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start
            )
        ) {
            Text(
                text = stringResource(screen.titleId),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, // M3 uses SemiBold mostly for nav labels
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip, // Clip ensures it doesn't try to show "..." while animating
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
