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
    val baseSurface = if (pureBlack) Color(0xFF101010) else MaterialTheme.colorScheme.surface
    val softenedAccent = rememberSoftAccent(accentColor, baseSurface)

    val mainContainerColor = lerp(baseSurface, softenedAccent, 0.12f)
    val detachedContainerColor = lerp(MaterialTheme.colorScheme.surfaceVariant, accentColor, 0.08f)

    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }
    val mood = items.firstOrNull { it.route == Screens.MoodAndGenres.route }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onMusicRecognitionClick != null) {
            DetachedCircleButton(
                iconRes = R.drawable.mic, 
                contentDescription = musicRecognitionContentDescription,
                onClick = onMusicRecognitionClick,
                containerColor = detachedContainerColor
            )
        }

        Surface(
            color = mainContainerColor,
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 3.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.widthIn(max = if (slim) 260.dp else 300.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                home?.let {
                    BarItem(it, isSelected(it), accentColor, softenedAccent) {
                        onItemClick(it, isSelected(it))
                    }
                }
                library?.let {
                    BarItem(it, isSelected(it), accentColor, softenedAccent) {
                        onItemClick(it, isSelected(it))
                    }
                }
                search?.let {
                    BarItem(it, isSelected(it), accentColor, softenedAccent) {
                        onItemClick(it, isSelected(it))
                    }
                }
            }
        }

        if (onShuffleClick != null && shuffleIconRes != null) {
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                onClick = onShuffleClick,
                containerColor = detachedContainerColor
            )
        }

        mood?.let {
            DetachedCircleButton(
                iconRes = it.iconIdActive,
                contentDescription = stringResource(it.titleId),
                onClick = { onItemClick(it, isSelected(it)) },
                containerColor = detachedContainerColor
            )
        }
    }
}

@Composable
private fun RowScope.BarItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    pillColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (selected) pillColor.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "indicator"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        label = "content"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, 
                onClick = onClick
            )
    ) {
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
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
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
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
