/*
 * ╭────────────────────────────────────────────╮
 * │      M3Play Playlist Design System         │
 * │--------------------------------------------│
 * │  Material Design 3 Unified Playlist Base   │
 * │  Smooth animations, expensive gradients    │
 * │  Touch-optimized scrolling behavior        │
 * │                                            │
 * │  Signature: M3PLAY::PLAYLIST::BASE::V1      │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState

/**
 * Material Design 3 Gradient Mesh Background for Playlists
 * Expensive but smooth animation for premium feel
 * Uses derived state to optimize recomposition
 */
@Composable
fun PlaylistGradientBackground(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    lazyListState: LazyListState,
    enabled: Boolean = true,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Calculate gradient opacity based on scroll position - optimized with derivedStateOf
    val gradientAlpha by remember {
        derivedStateOf {
            if (!enabled) return@derivedStateOf 0f
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    if (!enabled || gradientColors.isEmpty() || gradientAlpha <= 0f) return

    Box(
        modifier = modifier
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
                    val c3 = gradientColors.getOrElse(3) { c0 }
                    val c4 = gradientColors.getOrElse(4) { c1 }

                    // Primary color blob - top center
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c0.copy(alpha = gradientAlpha * 0.75f),
                                c0.copy(alpha = gradientAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.15f),
                            radius = width * 0.8f
                        )
                    )

                    // Secondary color blob - left side
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c1.copy(alpha = gradientAlpha * 0.55f),
                                c1.copy(alpha = gradientAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.1f, height * 0.4f),
                            radius = width * 0.6f
                        )
                    )

                    // Third color blob - right side
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c2.copy(alpha = gradientAlpha * 0.5f),
                                c2.copy(alpha = gradientAlpha * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.9f, height * 0.35f),
                            radius = width * 0.55f
                        )
                    )

                    // Fourth color blob - bottom left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c3.copy(alpha = gradientAlpha * 0.35f),
                                c3.copy(alpha = gradientAlpha * 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.25f, height * 0.65f),
                            radius = width * 0.75f
                        )
                    )

                    // Fifth color blob - bottom center
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c4.copy(alpha = gradientAlpha * 0.3f),
                                c4.copy(alpha = gradientAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.55f, height * 0.85f),
                            radius = width * 0.9f
                        )
                    )
                } else if (gradientColors.isNotEmpty()) {
                    // Fallback for single color
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                gradientColors[0].copy(alpha = gradientAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.25f),
                            radius = width * 0.85f
                        )
                    )
                }

                // Vertical gradient fade to surface
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                            surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                            surfaceColor
                        ),
                        startY = height * 0.4f,
                        endY = height
                    )
                )
            }
    )
}

/**
 * M3 Playlist Hero Header Components
 * Large thumbnail with shadow and metadata display
 */
@Composable
fun PlaylistHeroThumbnail(
    modifier: Modifier = Modifier,
    thumbnailUrls: List<String>,
    gradientColor: Color,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnailUrls.isEmpty() -> {
                // Placeholder
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.material.icons.filled.placeholder as? androidx.compose.ui.graphics.painter.Painter
                                ?: androidx.compose.material.icons.filled.MusicNote as? androidx.compose.ui.graphics.painter.Painter
                                ?: androidx.compose.material.icons.filled.AudioTrack as? androidx.compose.ui.graphics.painter.Painter
                                ?: androidx.compose.material.icons.filled.Album as? androidx.compose.ui.graphics.painter.Painter
                                ?: throw IllegalStateException("No valid icon found"),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            thumbnailUrls.size == 1 -> {
                // Single thumbnail
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = gradientColor.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    coil3.compose.AsyncImage(
                        model = thumbnailUrls[0],
                        contentDescription = null,
                        contentScale = androidx.compose.foundation.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                // Grid of thumbnails
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = gradientColor.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        listOf(
                            Alignment.TopStart,
                            Alignment.TopEnd,
                            Alignment.BottomStart,
                            Alignment.BottomEnd,
                        ).forEachIndexed { index, alignment ->
                            coil3.compose.AsyncImage(
                                model = thumbnailUrls.getOrNull(index),
                                contentDescription = null,
                                contentScale = androidx.compose.foundation.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .align(alignment)
                                    .size(120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smooth spring animation configuration for playlist interactions
 * Optimized for 60fps devices with spring physics
 */
val PlaylistSpringAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// Helper for shadow effect
private fun Modifier.shadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
    spotColor: Color = Color.Black
): Modifier = this.then(
    androidx.compose.material3.CardDefaults.outlinedCardElevation(defaultElevation = elevation).run {
        this as? Modifier ?: Modifier
    }
)
