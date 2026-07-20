@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.j.m3play.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean = false, 
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary, 
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onIdentifyClick: () -> Unit,
    onMoodClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = toolbarContainerColor,
    )
    
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    
    // Animation state ko manage karne ke liye naya MutableTransitionState
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = fabMenuExpanded

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                Box {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        shape = CircleShape, 
                        containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
                        contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
                    ) {
                        // Rotation animation taaki X smootly ghum ke aaye
                        val rotation by animateFloatAsState(
                            targetValue = if (fabMenuExpanded) 90f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy, 
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "fab_rotation"
                        )
                        Icon(
                            imageVector = if (fabMenuExpanded) Icons.Rounded.Close else Icons.Rounded.MoreVert, 
                            contentDescription = "More Options",
                            modifier = Modifier.rotate(rotation)
                        )
                    }

                    // Popup ab tab tak visible rahega jab tak animation chal raha hai
                    if (transitionState.currentState || transitionState.targetState) {
                        Popup(
                            alignment = Alignment.BottomEnd, 
                            onDismissRequest = { fabMenuExpanded = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            // Google Keep jesa mast Slide aur Bounce Animation
                            AnimatedVisibility(
                                visibleState = transitionState,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy, 
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn() + scaleIn(initialScale = 0.8f),
                                exit = slideOutVertically(
                                    targetOffsetY = { it / 2 },
                                    animationSpec = tween(durationMillis = 150)
                                ) + fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(16.dp), 
                                    modifier = Modifier.padding(bottom = 80.dp) 
                                ) {
                                    SpeedDialItem(
                                        text = "Identify Music",
                                        icon = Icons.Rounded.Search,
                                        pureBlack = pureBlack,
                                        onClick = {
                                            fabMenuExpanded = false
                                            onIdentifyClick()
                                        }
                                    )
                                    
                                    SpeedDialItem(
                                        text = "Mood & Genres",
                                        icon = Icons.Rounded.List,
                                        pureBlack = pureBlack,
                                        onClick = {
                                            fabMenuExpanded = false
                                            onMoodClick()
                                        }
                                    )
                                    
                                    SpeedDialItem(
                                        text = "Shuffle",
                                        icon = Icons.Rounded.PlayArrow,
                                        pureBlack = pureBlack,
                                        onClick = {
                                            fabMenuExpanded = false
                                            onShuffleClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.widthIn(max = 480.dp),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    pureBlack = pureBlack,
                    slim = slim, 
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    text: String,
    icon: ImageVector,
    pureBlack: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(percent = 50), 
        color = if (pureBlack) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = text, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = text, 
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    pureBlack: Boolean,
    slim: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> floatingToolbarSelectedItemContainerColor(pureBlack = pureBlack)
            else -> Color.Transparent
        },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> floatingToolbarSelectedItemContentColor(pureBlack = pureBlack)
            else -> floatingToolbarItemContentColor(pureBlack = pureBlack)
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
    
    val showLabel = selected && !slim 

    Row(
        modifier = Modifier
            .scale(scale)
            .animateContentSize()
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .widthIn(min = 48.dp)
            .padding(
                horizontal = if (showLabel) 16.dp else 12.dp,
                vertical = 12.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean): Color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean): Color = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer
@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean): Color = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
