/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Floating Liquid Glass navigation. The rendering pipeline follows Kyant0's
 * LiquidBottomTabs (Backdrop + combined tab backdrop + refractive selection),
 * with Glossy's damped drag/press interaction and Exterless-inspired rim glare.
 */
package com.j.m3play.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.screens.Screens
import com.j.m3play.ui.theme.LocalBlurEnabled
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LiquidBarShape = RoundedCornerShape(34.dp)
private val LiquidTabShape = RoundedCornerShape(30.dp)
private const val MORE_SEGMENT_COUNT = 1

@Composable
fun LiquidGlassNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onIdentifyClick: () -> Unit,
    onMoodClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    if (items.isEmpty()) return

    val blurEnabled = LocalBlurEnabled.current
    val dark = pureBlack || MaterialTheme.colorScheme.surface.luminanceCompat() < 0.5f
    val labelColor = if (dark) Color.White.copy(alpha = 0.92f) else Color(0xFF20242C)
    val accent = MaterialTheme.colorScheme.primary
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedIndex = items.indexOfFirst(isSelected).let { if (it < 0) 0 else it }
    val density = LocalDensity.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val glassLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val barInteraction = rememberGlassInteraction()
    val barWidth = 3
    val barHeight = 64.dp
    val barInset = 6.dp
    val capsule = RoundedCornerShape(percent = 50)
    val blobShape = RoundedCornerShape(percent = 50)
    val selection = remember(items.size) { Animatable(selectedIndex.toFloat()) }
    val press = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (!dragging && selectedIndex >= 0) {
            selection.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.72f, stiffness = 1000f))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f).height(barHeight)
                .clip(capsule)
                .drawInteractiveGlass(
                    isDark = dark,
                    backdrop = backdrop,
                    layer = glassLayer,
                    luminanceAnimation = 0.5f,
                    shape = capsule,
                    interaction = barInteraction,
                )
                .pointerInput(items.size) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            scope.launch { press.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 900f)) }
                        },
                        onDragEnd = {
                            val target = selection.value.roundToInt().coerceIn(0, items.lastIndex)
                            scope.launch {
                                selection.animateTo(target.toFloat(), spring(dampingRatio = 0.62f, stiffness = 1000f))
                                press.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 700f))
                            }
                            dragging = false
                            onItemClick(items[target], isSelected(items[target]))
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch {
                                selection.animateTo(selectedIndex.toFloat(), spring())
                                press.animateTo(0f, spring())
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            val segment = size.width.toFloat() / items.size
                            scope.launch { selection.snapTo((selection.value + amount.x / segment).coerceIn(0f, items.lastIndex.toFloat())) }
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val segmentWidth = (maxWidth - barInset * 2) / items.size
            val segmentWidthPx = with(density) { segmentWidth.toPx() }

            // Glossy's original tab-bar composition: capture the capsule as a layer,
            // then combine it with the live page backdrop for the moving glass lens.
            Row(
                modifier = Modifier.fillMaxSize().alpha(0f).layerBackdrop(tabsBackdrop)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { capsule },
                        effects = { vibrancy(); if (blurEnabled) blur(8.dp.toPx()) },
                    )
                    .padding(horizontal = barInset, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { screen ->
                    LiquidNavigationTabContent(screen, false, labelColor, Modifier.weight(1f))
                }
            }

            // Refractive selected island: same Kyant0 effect stack used by Glossy's LiquidGlassTabBar.
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = selection.value * segmentWidthPx + with(density) { barInset.toPx() }
                        scaleX = 1f + press.value * 0.18f
                        scaleY = 1f + press.value * 0.12f
                    }
                    .width(segmentWidth).height(56.dp)
                    .drawBackdrop(
                        backdrop = combinedBackdrop,
                        shape = { blobShape },
                        effects = {
                            vibrancy()
                            colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                            if (blurEnabled) {
                                blur(8.dp.toPx())
                                lens(10.dp.toPx() * press.value, 14.dp.toPx() * press.value, chromaticAberration = true)
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.6f) },
                        shadow = { Shadow(radius = 4.dp, alpha = 0.4f) },
                        innerShadow = { InnerShadow(radius = 8.dp * press.value, alpha = press.value) },
                        onDrawSurface = {
                            // Match Glossy's luminance-aware smoked glass rather than a fixed black overlay.
                            val darken = if (dark) 0.22f else 0.08f
                            drawRect(Color.Black.copy(alpha = darken))
                        },
                    ),
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = barInset, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, screen ->
                    val selected = isSelected(screen)
                    val color = if (selected) accent else labelColor
                    val interaction = remember(screen) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (pressed) 0.92f else if (selected) 1.035f else 1f,
                        animationSpec = spring(dampingRatio = 0.48f, stiffness = 560f),
                        label = "glossyM3TabPress$index",
                    )
                    Box(
                        modifier = Modifier.weight(1f).height(56.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interaction, indication = null, role = Role.Tab) {
                                scope.launch {
                                    selection.animateTo(index.toFloat(), spring(dampingRatio = 0.62f, stiffness = 1000f))
                                }
                                onItemClick(screen, selected)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        LiquidNavigationTabContent(screen, selected, color)
                    }
                }
            }
        }

        // Glossy's detached round glass action, wired to M3 Play's original More menu.
        val moreInteraction = remember { MutableInteractionSource() }
        val morePressed by moreInteraction.collectIsPressedAsState()
        val moreScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (morePressed) 0.93f else 1f,
            animationSpec = spring(dampingRatio = 0.48f, stiffness = 560f),
            label = "glossyM3MorePress",
        )
        Box(
            modifier = Modifier.size(64.dp)
                .graphicsLayer { scaleX = moreScale; scaleY = moreScale }
                .drawInteractiveGlass(
                    isDark = dark,
                    backdrop = backdrop,
                    layer = glassLayer,
                    luminanceAnimation = 0.5f,
                    shape = RoundedCornerShape(50),
                    interaction = rememberGlassInteraction(),
                )
                .clickable(interactionSource = moreInteraction, indication = null, role = Role.Button) {
                    menuExpanded = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More", modifier = Modifier.size(25.dp), tint = labelColor)
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(20.dp),
            ) {
                DropdownMenuItem(text = { Text("Identify Music") }, onClick = { menuExpanded = false; onIdentifyClick() })
                DropdownMenuItem(text = { Text("Mood & Genres") }, onClick = { menuExpanded = false; onMoodClick() })
                DropdownMenuItem(text = { Text("Shuffle") }, onClick = { menuExpanded = false; onShuffleClick() })
            }
        }
    }
}

@Composable
private fun LiquidNavigationTabContent(
    screen: Screens,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            modifier = Modifier.size(24.dp),
            tint = color,
        )
        Text(
            text = stringResource(screen.titleId),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
        )
    }
}

/** Exterless-inspired directional glare: bright upper lip, muted side and lower rim. */
private fun Modifier.exterlessGlassRim(
    dark: Boolean,
    radius: androidx.compose.ui.unit.Dp,
    strength: Float,
): Modifier = drawWithCache {
    val top = Color.White.copy(alpha = (if (dark) 0.52f else 0.78f) * strength)
    val mid = Color.White.copy(alpha = (if (dark) 0.08f else 0.18f) * strength)
    val bottom = (if (dark) Color.Black else Color(0xFF526070)).copy(alpha = (if (dark) 0.28f else 0.16f) * strength)
    val vertical = Brush.verticalGradient(listOf(top, mid, Color.Transparent, bottom), 0f, size.height)
    val horizontal = Brush.horizontalGradient(
        listOf(Color.White.copy(alpha = 0.20f * strength), Color.Transparent, Color.Black.copy(alpha = 0.09f * strength)),
        0f,
        size.width,
    )
    val corner = CornerRadius(radius.toPx(), radius.toPx())
    onDrawWithContent {
        drawContent()
        drawRoundRect(vertical, cornerRadius = corner, style = Stroke(width = 1.25.dp.toPx()))
        drawRoundRect(horizontal, cornerRadius = corner, style = Stroke(width = 0.7.dp.toPx()))
        drawRoundRect(
            color = Color.White.copy(alpha = (if (dark) 0.09f else 0.20f) * strength),
            topLeft = androidx.compose.ui.geometry.Offset(1.dp.toPx(), 1.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                (size.width - 2.dp.toPx()).coerceAtLeast(0f),
                (size.height - 2.dp.toPx()).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((radius - 1.dp).toPx().coerceAtLeast(0f)),
            style = Stroke(width = 0.55.dp.toPx()),
        )
    }
}

private fun Color.luminanceCompat(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
fun AdaptiveFloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    accentColor: Color,
    style: com.j.m3play.constants.NavigationBarStyle,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onIdentifyClick: () -> Unit,
    onMoodClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    when (style) {
        com.j.m3play.constants.NavigationBarStyle.CLASSIC -> FloatingNavigationToolbar(
            items = items,
            slim = slim,
            pureBlack = pureBlack,
            modifier = modifier,
            backdrop = backdrop,
            accentColor = accentColor,
            isSelected = isSelected,
            onItemClick = onItemClick,
            onIdentifyClick = onIdentifyClick,
            onMoodClick = onMoodClick,
            onShuffleClick = onShuffleClick,
        )
        com.j.m3play.constants.NavigationBarStyle.LIQUID_GLASS -> LiquidGlassNavigationToolbar(
            items = items,
            pureBlack = pureBlack,
            modifier = modifier,
            backdrop = backdrop,
            isSelected = isSelected,
            onItemClick = onItemClick,
            onIdentifyClick = onIdentifyClick,
            onMoodClick = onMoodClick,
            onShuffleClick = onShuffleClick,
        )
    }
}
