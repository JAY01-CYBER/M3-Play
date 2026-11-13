/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.j.m3play.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.screens.Screens
import com.j.m3play.ui.theme.LocalBlurEnabled
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean = false, 
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    accentColor: Color = MaterialTheme.colorScheme.primary, 
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onIdentifyClick: () -> Unit,
    onMoodClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack)
    val liquidEffectsEnabled = LocalBlurEnabled.current
    val fabGlassSurfaceColor = Color.White.copy(alpha = 0.10f)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = if (LocalBlurEnabled.current) Color.Transparent else toolbarContainerColor,
    )
    
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

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
                        modifier = Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                if (liquidEffectsEnabled) {
                                    blur(16.dp.toPx())
                                    lens(10.dp.toPx(), 14.dp.toPx(), chromaticAberration = false)
                                }
                            },
                            highlight = {
                                if (liquidEffectsEnabled) Highlight.Default.copy(alpha = 0.48f)
                                else Highlight.Default.copy(alpha = 0f)
                            },
                            shadow = { Shadow(alpha = if (liquidEffectsEnabled) 0.14f else 0f) },
                            onDrawSurface = {
                                drawRect(fabGlassSurfaceColor)
                            },
                        ),
                        containerColor = if (liquidEffectsEnabled) Color.Transparent else floatingToolbarFabContainerColor(pureBlack = pureBlack),
                        contentColor = if (liquidEffectsEnabled) Color.White else floatingToolbarFabContentColor(pureBlack = pureBlack),
                    ) {
                        // Rotation + Icon Morphing Animation add kiya
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

                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false },
                        shape = RoundedCornerShape(16.dp), 
                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp, 
                    ) {
                        DropdownMenuItem(
                            text = { Text("Identify Music") },
                            onClick = { 
                                fabMenuExpanded = false 
                                onIdentifyClick() 
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                            },
                            colors = getMenuColors(pureBlack)
                        )

                        DropdownMenuItem(
                            text = { Text("Mood & Genres") },
                            onClick = { 
                                fabMenuExpanded = false 
                                onMoodClick() 
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.List, contentDescription = null)
                            },
                            colors = getMenuColors(pureBlack)
                        )

                        DropdownMenuItem(
                            text = { Text("Shuffle") },
                            onClick = { 
                                fabMenuExpanded = false 
                                onShuffleClick() 
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                            },
                            colors = getMenuColors(pureBlack)
                        )
                    }
                }
            },
            modifier = Modifier
                .widthIn(max = 480.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32.dp) },
                    effects = {
                        if (liquidEffectsEnabled) {
                            blur(18.dp.toPx())
                            lens(12.dp.toPx(), 16.dp.toPx(), chromaticAberration = false)
                        }
                    },
                    highlight = {
                        if (liquidEffectsEnabled) Highlight.Default.copy(alpha = 0.46f)
                        else Highlight.Default.copy(alpha = 0f)
                    },
                    shadow = {
                        if (liquidEffectsEnabled) Shadow(alpha = 0.16f)
                        else Shadow(alpha = 0f)
                    },
                    innerShadow = {
                        if (liquidEffectsEnabled) InnerShadow(radius = 6.dp, alpha = 0.12f)
                        else InnerShadow(radius = 0.dp, alpha = 0f)
                    },
                    onDrawSurface = {
                        // Neutral charcoal glass avoids inheriting strong wallpaper/theme hues.
                        drawRect(Color.Black.copy(alpha = 0.28f))
                        drawRect(Color.White.copy(alpha = 0.045f))
                    },
                )
                .clip(RoundedCornerShape(32.dp)),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    pureBlack = pureBlack,
                    slim = slim, 
                    backdrop = backdrop,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    }
}

@Composable
private fun getMenuColors(pureBlack: Boolean): MenuItemColors {
    return MenuDefaults.itemColors(
        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    pureBlack: Boolean,
    slim: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val blurEnabled = LocalBlurEnabled.current
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> floatingToolbarSelectedItemContainerColor(pureBlack = pureBlack)
            else -> Color.Transparent
        },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> if (blurEnabled) Color.White else floatingToolbarSelectedItemContentColor(pureBlack = pureBlack)
            else -> if (blurEnabled) Color.White.copy(alpha = 0.84f) else floatingToolbarItemContentColor(pureBlack = pureBlack)
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
    // Read CompositionLocals here; Backdrop draw callbacks are not composable.
    // Keep the selected lens surface neutral instead of inheriting accent/theme hues.
    val selectedSurfaceColor = Color.White.copy(alpha = 0.14f)

    Row(
        modifier = Modifier
            .scale(scale)
            .animateContentSize()
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            if (blurEnabled) {
                                blur(12.dp.toPx())
                                lens(8.dp.toPx(), 12.dp.toPx(), chromaticAberration = false)
                            }
                        },
                        highlight = {
                            if (blurEnabled) Highlight.Default.copy(alpha = 0.55f)
                            else Highlight.Default.copy(alpha = 0f)
                        },
                        shadow = { Shadow(alpha = if (blurEnabled) 0.12f else 0f) },
                        innerShadow = {
                            if (blurEnabled) InnerShadow(radius = 5.dp, alpha = 0.13f)
                            else InnerShadow(radius = 0.dp, alpha = 0f)
                        },
                        layerBlock = {
                            val press = if (isPressed) 1f else 0f
                            val stretch = 1f + 0.035f * press
                            scaleX = stretch
                            scaleY = 1f - 0.015f * press
                        },
                        onDrawSurface = {
                            drawRect(selectedSurfaceColor)
                            drawRect(Color.White.copy(alpha = if (blurEnabled) 0.035f else 0f))
                        },
                    )
                } else {
                    Modifier.background(color = containerColor, shape = shape)
                }
            )
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
