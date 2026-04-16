package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val mainContainerColor = if (pureBlack) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    }

    val detachedContainerColor = if (pureBlack) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }

    val homeScreen = items.firstOrNull { it.route == Screens.Home.route }
    val libraryScreen = items.firstOrNull { it.route == Screens.Library.route }
    val searchScreen = items.firstOrNull { it.route == Screens.Search.route }
    val moodScreen = items.firstOrNull { it.route == Screens.MoodAndGenres.route }

    val primaryScreens = listOfNotNull(homeScreen, libraryScreen, searchScreen)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = mainContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.widthIn(max = if (slim) 252.dp else 282.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryScreens.forEach { screen ->
                    AppleNavItem(
                        screen = screen,
                        selected = isSelected(screen),
                        onClick = { onItemClick(screen, isSelected(screen)) },
                    )
                }
            }
        }

        if (onMusicRecognitionClick != null) {
            DetachedCircleButton(
                iconRes = R.drawable.mic,
                contentDescription = musicRecognitionContentDescription,
                selected = false,
                onClick = onMusicRecognitionClick,
                pureBlack = pureBlack,
                containerColor = detachedContainerColor,
            )
        }

        if (onShuffleClick != null && shuffleIconRes != null) {
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                selected = false,
                onClick = onShuffleClick,
                pureBlack = pureBlack,
                containerColor = detachedContainerColor,
            )
        }

        if (moodScreen != null) {
            DetachedCircleButton(
                iconRes = moodScreen.iconIdActive,
                contentDescription = stringResource(moodScreen.titleId),
                selected = isSelected(moodScreen),
                onClick = { onItemClick(moodScreen, isSelected(moodScreen)) },
                pureBlack = pureBlack,
                containerColor = detachedContainerColor,
            )
        }
    }
}

@Composable
private fun AppleNavItem(
    screen: Screens,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color.Black.copy(alpha = 0.07f)
            else Color.White.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        label = "apple_nav_item_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        label = "apple_nav_item_content",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 15.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "apple_nav_item_padding",
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (selected) 1.dp else 0.dp,
        modifier = Modifier.defaultMinSize(minHeight = 50.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
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
private fun DetachedCircleButton(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    pureBlack: Boolean,
    containerColor: Color,
) {
    val resolvedContainerColor by animateColorAsState(
        targetValue = if (selected) {
            if (pureBlack) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
        } else {
            containerColor
        },
        label = "detached_button_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        label = "detached_button_content",
    )

    Surface(
        onClick = onClick,
        color = resolvedContainerColor,
        contentColor = contentColor,
        shape = CircleShape,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
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

