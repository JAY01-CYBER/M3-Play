package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val detachedContainerColor = lerp(baseSurface, softenedAccent, 0.16f)

    val home = items.firstOrNull { it.route == Screens.Home.route }
    val library = items.firstOrNull { it.route == Screens.Library.route }
    val search = items.firstOrNull { it.route == Screens.Search.route }
    val mood = items.firstOrNull { it.route == Screens.MoodAndGenres.route }

    Column(modifier = modifier) {

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            
            if (onMusicRecognitionClick != null) {
                DetachedCircleButton(
                    iconRes = R.drawable.mic,
                    contentDescription = musicRecognitionContentDescription,
                    onClick = onMusicRecognitionClick,
                    containerColor = detachedContainerColor,
                    modifier = Modifier.offset(x = 4.dp)
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    home?.let {
                        AppleNavItem(it, isSelected(it), softenedAccent) {
                            onItemClick(it, isSelected(it))
                        }
                    }
                    library?.let {
                        AppleNavItem(it, isSelected(it), softenedAccent) {
                            onItemClick(it, isSelected(it))
                        }
                    }
                    search?.let {
                        AppleNavItem(it, isSelected(it), softenedAccent) {
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
}

@Composable
private fun AppleNavItem(
    screen: Screens,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (selected) lerp(MaterialTheme.colorScheme.surface, accentColor, 0.7f)
        else Color.Transparent,
        spring(stiffness = Spring.StiffnessMedium)
    )

    val color by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        spring(stiffness = Spring.StiffnessMedium)
    )

    val pad by animateDpAsState(
        if (selected) 15.dp else 12.dp,
        spring(stiffness = Spring.StiffnessMedium)
    )

    Surface(
        onClick = onClick,
        color = bg,
        contentColor = color,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.defaultMinSize(minHeight = 50.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = pad, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (selected) screen.iconIdActive else screen.iconIdInactive
                ),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(18.dp),
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(screen.titleId),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
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
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        modifier = modifier.size(54.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
