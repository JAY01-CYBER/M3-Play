package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val baseSurface = if (pureBlack) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh
    val toolbarColor = baseSurface.copy(alpha = 0.85f)
    val softAccent = rememberSoftAccent(accentColor, baseSurface)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        onMusicRecognitionClick?.let {
            DetachedCircleButton(
                iconRes = R.drawable.music_recognition, // Yeh icon name aapke purane code se liya hai
                contentDescription = musicRecognitionContentDescription,
                onClick = it,
                containerColor = toolbarColor
            )
            Spacer(Modifier.width(12.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp), // Thoda aur modern roundness (pill jaisa)
            color = toolbarColor,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    BarItem(
                        screen = screen,
                        selected = selected,
                        accentColor = accentColor,
                        pillColor = softAccent,
                        onClick = { onItemClick(screen, selected) }
                    )
                }
            }
        }

        if (onShuffleClick != null && shuffleIconRes != null) {
            Spacer(Modifier.width(12.dp))
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                onClick = onShuffleClick,
                containerColor = toolbarColor
            )
        }
    }
}

@Composable
private fun BarItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    pillColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 🌸 Smooth Bounce Animation on Press 🌸
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Pill Indicator Color Transition
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) pillColor.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "indicator"
    )
    
    // Icon & Text Color Transition
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        label = "content"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f) // YAHAN FIX HAI: Isse items ek dusre ki jagah nahi lenge aur apni fix jagah par rahenge
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom bounce instead of default ripple
                onClick = onClick
            )
    ) {
        // ✨ Material 3 Pill Highlight ✨
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .background(indicatorColor, CircleShape)
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Text Always Visible (No Layout Shift)
        Text(
            text = stringResource(screen.titleId),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberSoftAccent(accent: Color, surface: Color): Color {
    val safe = if (accent.alpha == 0f) MaterialTheme.colorScheme.primary else accent
    return lerp(surface, safe, 0.7f)
}

@Composable
private fun DetachedCircleButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
