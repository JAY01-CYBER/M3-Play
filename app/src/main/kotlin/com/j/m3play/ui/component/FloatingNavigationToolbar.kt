package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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
    val softenedAccent = rememberSoftAccent(accentColor = accentColor, surface = baseSurface)

    
    val mainContainerColor = lerp(baseSurface, softenedAccent, 0.12f)
    val detachedContainerColor = lerp(baseSurface, softenedAccent, 0.16f)

    val homeScreen = items.firstOrNull { it.route == Screens.Home.route }
    val libraryScreen = items.firstOrNull { it.route == Screens.Library.route }
    val searchScreen = items.firstOrNull { it.route == Screens.Search.route }
    val moodScreen = items.firstOrNull { it.route == Screens.MoodAndGenres.route }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        
        Surface(
            color = mainContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
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

                
                homeScreen?.let {
                    AppleNavItem(
                        screen = it,
                        selected = isSelected(it),
                        accentColor = softenedAccent,
                        onClick = { onItemClick(it, isSelected(it)) },
                    )
                }

                
                if (onMusicRecognitionClick != null) {
                    Surface(
                        onClick = onMusicRecognitionClick,
                        shape = CircleShape,
                        color = detachedContainerColor,
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = musicRecognitionContentDescription,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            
                libraryScreen?.let {
                    AppleNavItem(
                        screen = it,
                        selected = isSelected(it),
                        accentColor = softenedAccent,
                        onClick = { onItemClick(it, isSelected(it)) },
                    )
                }

            
                searchScreen?.let {
                    AppleNavItem(
                        screen = it,
                        selected = isSelected(it),
                        accentColor = softenedAccent,
                        onClick = { onItemClick(it, isSelected(it)) },
                    )
                }
            }
        }

        
        if (onShuffleClick != null && shuffleIconRes != null) {
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                selected = false,
                onClick = onShuffleClick,
                pureBlack = pureBlack,
                accentColor = softenedAccent,
                containerColor = detachedContainerColor,
            )
        }

    
        moodScreen?.let {
            DetachedCircleButton(
                iconRes = it.iconIdActive,
                contentDescription = stringResource(it.titleId),
                selected = isSelected(it),
                onClick = { onItemClick(it, isSelected(it)) },
                pureBlack = pureBlack,
                accentColor = softenedAccent,
                containerColor = detachedContainerColor,
            )
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
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            lerp(MaterialTheme.colorScheme.surface, accentColor, 0.7f)
        } else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), 
        label = "",
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "",
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 15.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "",
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.defaultMinSize(minHeight = 50.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (selected) screen.iconIdActive else screen.iconIdInactive
                ),
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(18.dp),
            )
            if (selected) {
                Text(
                    text = stringResource(screen.titleId),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun rememberSoftAccent(
    accentColor: Color,
    surface: Color,
): Color {
    val safeAccent = if (accentColor.alpha == 0f) MaterialTheme.colorScheme.primary else accentColor
    return lerp(surface, safeAccent, 0.7f)
}

@Composable
private fun DetachedCircleButton(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    pureBlack: Boolean,
    accentColor: Color,
    containerColor: Color,
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        shape = CircleShape,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.size(54.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
