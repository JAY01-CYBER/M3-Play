/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.screens.playlist

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.j.m3play.R
import dev.chrisbanes.haze.HazeState
import com.j.m3play.ui.component.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePlaylistScreen(
    title: String,
    lazyListState: LazyListState,
    gradientColors: List<Color>,
    surfaceColor: Color,
    isSearching: Boolean,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchToggle: () -> Unit,
    isSelectionMode: Boolean,
    selectionCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
    disableBlur: Boolean,
    headerContent: @Composable () -> Unit,
    listContent: LazyListScope.() -> Unit
) {
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val transparentAppBar by remember { derivedStateOf { !disableBlur && !isSelectionMode && !showTopBarTitle } }

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.55f)
                    .align(Alignment.TopCenter)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(c0.copy(alpha = gradientAlpha * 0.75f), Color.Transparent),
                                    center = Offset(width * 0.5f, height * 0.15f), radius = width * 0.8f
                                )
                            )
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(c1.copy(alpha = gradientAlpha * 0.55f), Color.Transparent),
                                    center = Offset(width * 0.1f, height * 0.4f), radius = width * 0.6f
                                )
                            )
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(c2.copy(alpha = gradientAlpha * 0.5f), Color.Transparent),
                                    center = Offset(width * 0.9f, height * 0.35f), radius = width * 0.55f
                                )
                            )
                        }
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, surfaceColor),
                                startY = height * 0.4f, endY = height
                            )
                        )
                    }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues()
        ) {
            item { headerContent() }
            listContent()
        }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                Crossfade(targetState = Triple(isSelectionMode, isSearching, showTopBarTitle), label = "AppBarTitle") { (selection, searching, showTitle) ->
                    when {
                        selection -> Text("$selectionCount Selected", style = MaterialTheme.typography.titleLarge)
                        searching -> GlassmorphicSearch(searchQuery, onSearchQueryChange)
                        showTitle -> Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) onSearchToggle()
                    else if (isSelectionMode) onClearSelection()
                    else onBack()
                }) {
                    Icon(
                        painter = painterResource(if (isSelectionMode) R.drawable.close else R.drawable.arrow_back),
                        contentDescription = "Back or Close"
                    )
                }
            },
            actions = {
                if (isSelectionMode) {
                    IconButton(onClick = onSelectAll) {
                        Icon(painterResource(R.drawable.select_all), contentDescription = "Select All")
                    }
                } else if (!isSearching) {
                    IconButton(onClick = onSearchToggle) {
                        Icon(painterResource(R.drawable.search), contentDescription = "Search")
                    }
                }
            }
        )
    }
}

@Composable
fun GlassmorphicSearch(query: TextFieldValue, onQueryChange: (TextFieldValue) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search...", style = MaterialTheme.typography.titleMedium) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp)
            .height(48.dp)
    )
}

fun Modifier.bounceClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) = this.composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable { onClick() }
}


/**
 * Shared action strip used by every glossy collection detail screen.
 * Keeps the Shuffle / Play / More geometry identical while each screen
 * supplies its own playback and overflow actions.
 */
data class GlossyCollectionMenuAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun GlossyCollectionActionRow(
    hazeState: HazeState,
    disableBlur: Boolean,
    enabled: Boolean = true,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    moreActions: List<GlossyCollectionMenuAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val glassModifier = if (disableBlur) Modifier else Modifier.liquidGlass(hazeState, CircleShape)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(glassModifier)
                .clickable(enabled = enabled, onClick = onShuffle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 120.dp)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                .clickable(enabled = enabled, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(glassModifier)
                    .clickable(enabled = moreActions.isNotEmpty()) { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                moreActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            expanded = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}
