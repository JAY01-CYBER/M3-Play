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
    val mainContainerColor = if (pureBlack) {
        Color(0xFF141414) // Slightly lifted black for better separation
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding() // FIX: Ye system home indicator ke upar overlap hone se rokega
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
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
            shadowElevation = 8.dp, // Thoda aur premium floating shadow
            tonalElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(56.dp) // FIX: Fixed standard height for perfect symmetry
                    .padding(horizontal = 6.dp), // Outer padding for pill
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                shadowElevation = 8.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.size(56.dp) // FIX: Exactly matching the pill's height (56dp)
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
    onClick: () -> Unit,
) {
    val animationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val colorAnimationSpec = tween<Color>(durationMillis = 300, easing = LinearOutSlowInEasing)

    // FIX: Alpha 0.2f kar diya hai taaki light background par accent color properly dikhe
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
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
            .height(44.dp) // FIX: 56dp pill ke andar 44dp ka item = perfect 6dp vertical padding top/bottom
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
                fontWeight = FontWeight.Bold, // Changed back to Bold so it stands out
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Visible, // Ensures no "..." truncation glitch
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
