package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val borderColor = if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)

    val homeScreen = items.firstOrNull { it.route == Screens.Home.route }
    val libraryScreen = items.firstOrNull { it.route == Screens.Library.route }
    val searchScreen = items.firstOrNull { it.route == Screens.Search.route }
    val moodScreen = items.firstOrNull { it.route == Screens.MoodAndGenres.route }

    val primaryScreens = listOfNotNull(homeScreen, libraryScreen, searchScreen)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp,
            modifier = Modifier.widthIn(max = if (slim) 248.dp else 270.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryScreens.forEach { screen ->
                    CompactNavItem(
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
            )
        }

        if (onShuffleClick != null && shuffleIconRes != null) {
            DetachedCircleButton(
                iconRes = shuffleIconRes,
                contentDescription = shuffleContentDescription,
                selected = false,
                onClick = onShuffleClick,
                pureBlack = pureBlack,
            )
        }

        if (moodScreen != null) {
            DetachedCircleButton(
                iconRes = moodScreen.iconIdActive,
                contentDescription = stringResource(moodScreen.titleId),
                selected = isSelected(moodScreen),
                onClick = { onItemClick(moodScreen, isSelected(moodScreen)) },
                pureBlack = pureBlack,
            )
        }
    }
}

@Composable
private fun CompactNavItem(
    screen: Screens,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
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
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.defaultMinSize(minHeight = 50.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
) {
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
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f),
        ),
        shadowElevation = 10.dp,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .shadow(10.dp, CircleShape, clip = false),
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
