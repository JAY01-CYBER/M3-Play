package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
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
    screens: List<Screens>,
    currentScreen: String?,
    onScreenSelected: (String) -> Unit,
    onSearchClick: (() -> Unit)? = null,
    onMenuButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    useSlimCollapsedLayout: Boolean = false,
) {
    val toolbarShape = RoundedCornerShape(32.dp)
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val borderColor = if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)

    val primaryScreens = screens.filter { it.route != Screens.Search.route }.take(3)

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
            modifier = Modifier.weight(1f, fill = false).widthIn(max = if (useSlimCollapsedLayout) 340.dp else 420.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryScreens.forEach { screen ->
                    CherryNavItem(
                        screen = screen,
                        selected = currentScreen == screen.route,
                        onClick = { onScreenSelected(screen.route) },
                        modifier = Modifier.weight(1f, fill = true),
                    )
                }
            }
        }

        if (onSearchClick != null) {
            DetachedCircleButton(
                iconRes = R.drawable.search,
                selected = currentScreen == Screens.Search.route,
                onClick = onSearchClick,
                pureBlack = pureBlack,
            )
        }

        if (onMenuButtonClick != null) {
            DetachedCircleButton(
                iconRes = R.drawable.more_horiz,
                selected = false,
                onClick = onMenuButtonClick,
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
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        label = "nav_item_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_item_content",
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
