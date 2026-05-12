package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    val toolbarShape = RoundedCornerShape(32.dp)
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val borderColor = if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)

    val searchScreen = items.firstOrNull { it.route == Screens.Search.route }
    val primaryScreens = items.filter { it.route != Screens.Search.route }.take(if (slim) 2 else 3)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = toolbarShape,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp,
            modifier = Modifier.weight(1f, fill = false).widthIn(max = if (slim) 330.dp else 430.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryScreens.forEach { screen ->
                    CherryNavItem(
                        screen = screen,
                        selected = isSelected(screen),
                        onClick = { onItemClick(screen, isSelected(screen)) },
                        modifier = Modifier.weight(1f, fill = true),
                    )
                }
            }
        }

        if (searchScreen != null) {
            DetachedCircleButton(
                iconRes = searchScreen.iconIdActive,
                contentDescription = stringResource(searchScreen.titleId),
                selected = isSelected(searchScreen),
                onClick = { onItemClick(searchScreen, isSelected(searchScreen)) },
                pureBlack = pureBlack,
            )
        }

        if (onMusicRecognitionClick != null) {
            DetachedCircleButton(
                iconRes = R.drawable.mic,
                contentDescription = musicRecognitionContentDescription,
                selected = false,
                onClick = onMusicRecognitionClick,
                pureBlack = pureBlack,
            )
        } else if (onShuffleClick != null && shuffleIconRes != null) {
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                selected = false,
                onClick = onShuffleClick,
                pureBlack = pureBlack,
                roundedSquare = true,
            )
        }
    }
}

@Composable
private fun CherryNavItem(
    screen: Screens,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Applied Spring Animation for Colors
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_item_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_item_content",
    )
    
    // Applied Spring Animation for the Sliding/Expanding Padding Effect
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_item_padding"
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                    contentDescription = stringResource(screen.titleId),
                    modifier = Modifier.size(22.dp),
                )
                if (selected) {
                    Text(
                        text = stringResource(screen.titleId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
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
    roundedSquare: Boolean = false,
) {
    val shape = if (roundedSquare) RoundedCornerShape(24.dp) else CircleShape
    val containerColor by animateColorAsState(
        targetValue = when {
            pureBlack -> Color.White.copy(alpha = 0.08f)
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        },
        label = "detached_button_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "detached_button_content",
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f),
        ),
        shadowElevation = 10.dp,
        modifier = Modifier
            .size(if (roundedSquare) 72.dp else 64.dp)
            .clip(shape)
            .shadow(10.dp, shape, clip = false),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
