/*
 * M3Play Component Module
 *
 * Reusable UI building block
 * Signature: M3PLAY::COMPONENT::V1
 */

package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R
import com.j.m3play.ui.screens.Screens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val toolbarShape = RoundedCornerShape(34.dp)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    if (onShuffleClick != null && shuffleIconRes != null) {
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                Box {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription =
                                shuffleContentDescription.ifEmpty {
                                    stringResource(R.string.more)
                                },
                        )
                    }

                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.music_recognition)) },
                            onClick = {
                                fabMenuExpanded = false
                                onMusicRecognitionClick?.invoke()
                            },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color =
                                        if (pureBlack) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.mic),
                                            contentDescription =
                                                musicRecognitionContentDescription.ifEmpty {
                                                    stringResource(R.string.music_recognition)
                                                },
                                        )
                                    }
                                }
                            },
                            enabled = onMusicRecognitionClick != null,
                            colors =
                                MenuDefaults.itemColors(
                                    textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledLeadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                ),
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shuffle)) },
                            onClick = {
                                fabMenuExpanded = false
                                onShuffleClick()
                            },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color =
                                        if (pureBlack) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(shuffleIconRes),
                                            contentDescription =
                                                shuffleContentDescription.ifEmpty {
                                                    stringResource(R.string.shuffle)
                                                },
                                        )
                                    }
                                }
                            },
                            colors =
                                MenuDefaults.itemColors(
                                    textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
                }
            },
            modifier = modifier
                .widthIn(max = 500.dp)
                .shadow(18.dp, toolbarShape, clip = false)
                .clip(toolbarShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = toolbarShape,
                ),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    slim = slim,
                    pureBlack = pureBlack,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    } else {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier
                .widthIn(max = 440.dp)
                .shadow(18.dp, toolbarShape, clip = false)
                .clip(toolbarShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = toolbarShape,
                ),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    slim = slim,
                    pureBlack = pureBlack,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    slim: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val containerColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White.copy(alpha = 0.12f)
                selected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                else -> Color.Transparent
            },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White
                selected -> MaterialTheme.colorScheme.onSecondaryContainer
                pureBlack -> Color.White.copy(alpha = 0.82f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
            },
        label = "",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )
    val showLabel = selected && !slim && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .scale(scale)
                .animateContentSize()
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f) else Color.Transparent,
                    shape = shape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .widthIn(min = if (showLabel) 104.dp else 52.dp)
                .padding(
                    horizontal = if (showLabel) 18.dp else 13.dp,
                    vertical = 13.dp,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
            modifier = Modifier.size(if (showLabel) 22.dp else 21.dp),
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.1.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
