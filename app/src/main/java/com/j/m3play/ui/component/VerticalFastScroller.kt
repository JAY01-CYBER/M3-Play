package com.j.m3play.ui.component

import android.view.ViewConfiguration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun VerticalFastScroller(
    listState: LazyListState,
    thumbColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    topContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.maxByOrNull { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.maxByOrNull { it.width }?.width ?: 0

        val scrollerPlaceable = subcompose("scroller") {
            val layoutInfo = listState.layoutInfo
            val showScroller = layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
            if (!showScroller) return@subcompose

            val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
            var thumbOffsetY by remember(thumbTopPadding) { mutableStateOf(thumbTopPadding) }

            val dragInteractionSource = remember { MutableInteractionSource() }
            val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
            val scrolled = remember {
                MutableSharedFlow<Unit>(
                    extraBufferCapacity = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
                )
            }

            val heightPx =
                contentHeight.toFloat() - thumbTopPadding - listState.layoutInfo.afterContentPadding
            val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
            val trackHeightPx = heightPx - thumbHeightPx

            // Haptic feedback
            val hapticFeedback = LocalHapticFeedback.current

            // Animaciones mejoradas para Material Design 3
            val thumbScale = remember { Animatable(1f) }
            val thumbElevation = remember { Animatable(3f) } // Elevación base

            // Colores animados para transiciones suaves
            val thumbColorAnimated by animateColorAsState(
                targetValue = if (isThumbDragged) {
                    MaterialTheme.colorScheme.primary
                } else {
                    thumbColor
                },
                animationSpec = tween(300),
                label = "thumb_color"
            )

            // Animación de grosor del track
            val trackAlphaAnimated by animateFloatAsState(
                targetValue = if (isThumbDragged) 0.5f else 0.3f,
                animationSpec = tween(300),
                label = "track_alpha"
            )

            // Efectos de interacción mejorados con más dinamismo
            LaunchedEffect(isThumbDragged) {
                if (isThumbDragged) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    thumbScale.animateTo(
                        targetValue = 1.3f, // Más escala para mejor feedback
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                    thumbElevation.animateTo(
                        targetValue = 12f, // Mayor elevación
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    )
                } else {
                    thumbScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    thumbElevation.animateTo(
                        targetValue = 3f, // Elevación base más pronunciada
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                }
            }

            // When thumb dragged - Mejorado para mejor precisión
            LaunchedEffect(thumbOffsetY) {
                if (layoutInfo.totalItemsCount == 0 || !isThumbDragged) return@LaunchedEffect
                val scrollRatio = (thumbOffsetY - thumbTopPadding) / trackHeightPx
                val scrollItem = layoutInfo.totalItemsCount * scrollRatio
                val scrollItemRounded = scrollItem.roundToInt().coerceIn(0, layoutInfo.totalItemsCount - 1)

                // Mejora: Cálculo más preciso del offset dentro del item
                val scrollItemSize = layoutInfo.visibleItemsInfo
                    .find { it.index == scrollItemRounded }?.size
                    ?: run {
                        // Fallback: usar tamaño promedio si el item no está visible
                        val averageItemSize = layoutInfo.visibleItemsInfo
                            .takeIf { it.isNotEmpty() }
                            ?.map { it.size }
                            ?.average()?.toInt()
                            ?: 200 // Valor por defecto razonable
                        averageItemSize
                    }

                val scrollItemOffset = (scrollItemSize * (scrollItem - scrollItemRounded))
                    .coerceIn(0f, scrollItemSize.toFloat())

                try {
                    listState.scrollToItem(
                        index = scrollItemRounded,
                        scrollOffset = scrollItemOffset.roundToInt()
                    )
                    scrolled.tryEmit(Unit)
                } catch (e: Exception) {
                    // Fallback silencioso si hay error de scroll
                }
            }

            // When list scrolled - Optimizado para máxima fluidez
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                if (listState.layoutInfo.totalItemsCount == 0 || isThumbDragged) return@LaunchedEffect

                // Usar snapshotFlow para mejor rendimiento
                snapshotFlow {
                    Triple(
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset,
                        listState.layoutInfo.totalItemsCount
                    )
                }.collect { (_, _, totalItems) ->
                    if (totalItems == 0) return@collect

                    val scrollOffset = computeScrollOffset(state = listState)
                    val scrollRange = computeScrollRange(state = listState)

                    if (scrollRange > heightPx) {
                        val proportion = scrollOffset.toFloat() / (scrollRange.toFloat() - heightPx)
                        val newThumbOffset = (trackHeightPx * proportion.coerceIn(0f, 1f) + thumbTopPadding)

                        // Interpolación suave para reducir jank
                        val smoothFactor = 0.8f
                        thumbOffsetY = thumbOffsetY * (1f - smoothFactor) + newThumbOffset * smoothFactor
                        scrolled.tryEmit(Unit)
                    }
                }
            }

            // Thumb alpha con transición más suave y inteligente
            val alpha = remember { Animatable(0f) }
            val isThumbVisible = alpha.value > 0f

            // Detección de velocidad de scroll para mejor UX
            var lastScrollTime by remember { mutableStateOf(0L) }
            var scrollVelocity by remember { mutableStateOf(0f) }

            LaunchedEffect(scrolled, alpha) {
                scrolled.collectLatest {
                    val currentTime = System.currentTimeMillis()
                    scrollVelocity = if (lastScrollTime > 0) {
                        1000f / (currentTime - lastScrollTime).coerceAtLeast(1)
                    } else 0f
                    lastScrollTime = currentTime

                    alpha.snapTo(1f)
                    // Tiempo de fade adaptativo según velocidad
                    val fadeDelay = if (scrollVelocity > 10f) 3000 else 2000
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
                            delayMillis = fadeDelay
                        )
                    )
                }
            }

            // Indicador de track mejorado con animaciones
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbTopPadding.roundToInt()) }
                    .height(with(LocalDensity.current) { trackHeightPx.toDp() })
                    .padding(horizontal = 8.dp)
                    .padding(end = endContentPadding)
                    .width(TrackThickness)
                    .alpha(alpha.value * trackAlphaAnimated)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(TrackThickness / 2)
                    )
            )

            // Thumb principal mejorado con más características
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                    .height(ThumbLength)
                    .then(
                        // Exclude thumb from gesture area only when needed
                        if (isThumbVisible && !isThumbDragged && !listState.isScrollInProgress) {
                            Modifier.systemGestureExclusion()
                        } else Modifier,
                    )
                    .padding(horizontal = 6.dp) // Menos padding para thumb más ancho
                    .padding(end = endContentPadding)
                    .width(ThumbThickness)
                    .scale(thumbScale.value)
                    .alpha(alpha.value)
                    .shadow(
                        elevation = thumbElevation.value.dp,
                        shape = ThumbShape,
                        clip = false,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(
                        color = thumbColorAnimated,
                        shape = ThumbShape
                    )
                    .then(
                        // Recompose opts
                        if (!listState.isScrollInProgress) {
                            Modifier.draggable(
                                interactionSource = dragInteractionSource,
                                orientation = Orientation.Vertical,
                                enabled = isThumbVisible,
                                state = rememberDraggableState { delta ->
                                    val sensitivity = 1.2f // Factor de sensibilidad ajustable
                                    val adjustedDelta = delta * sensitivity

                                    val newOffsetY = thumbOffsetY + adjustedDelta
                                    thumbOffsetY = newOffsetY.coerceIn(
                                        thumbTopPadding,
                                        thumbTopPadding + trackHeightPx
                                    )

                                    // Haptic feedback más inteligente
                                    if (abs(adjustedDelta) > 3f) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                            )
                        } else Modifier,
                    ),
            )
        }.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val scrollerWidth = scrollerPlaceable.maxByOrNull { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.forEach {
                it.placeRelative(0, 0)
            }
            scrollerPlaceable.forEach {
                it.placeRelative(contentWidth - scrollerWidth, 0)
            }
        }
    }
}

private fun computeScrollOffset(state: LazyListState): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0

    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val minPosition = min(startChild.index, endChild.index)
    val maxPosition = max(startChild.index, endChild.index)
    val itemsBefore = minPosition.coerceAtLeast(0)
    val startDecoratedTop = startChild.top
    val laidOutArea = abs(endChild.bottom - startDecoratedTop)
    val itemRange = abs(minPosition - maxPosition) + 1

    // Mejora: Manejo más robusto de divisiones por cero
    val avgSizePerRow = if (itemRange > 0 && laidOutArea > 0) {
        laidOutArea.toFloat() / itemRange
    } else {
        200f // Valor por defecto
    }

    return (itemsBefore * avgSizePerRow + (0 - startDecoratedTop)).roundToInt().coerceAtLeast(0)
}

private fun computeScrollRange(state: LazyListState): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0

    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = endChild.bottom - startChild.top
    val laidOutRange = abs(startChild.index - endChild.index) + 1

    // Mejora: Cálculo más preciso del rango total
    val averageItemSize = if (laidOutRange > 0 && laidOutArea > 0) {
        laidOutArea.toFloat() / laidOutRange
    } else {
        200f // Valor por defecto
    }

    val totalRange = (averageItemSize * state.layoutInfo.totalItemsCount).roundToInt()

    // Añadir padding si existe
    return totalRange + state.layoutInfo.beforeContentPadding + state.layoutInfo.afterContentPadding
}

// Valores mejorados siguiendo Material Design 3
private val ThumbLength = 48.dp
private val ThumbThickness = 12.dp // Más ancho para mejor agarre
private val TrackThickness = 6.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)
private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = 2000,
)

private val LazyListItemInfo.top: Int
    get() = offset

private val LazyListItemInfo.bottom: Int
    get() = offset + size