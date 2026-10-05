/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.j.m3play.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.theme.LocalBlurEnabled
import com.j.m3play.ui.theme.appBlur
import kotlin.math.abs

private val ArchiveLyricsEase = androidx.compose.animation.core.CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** ArchiveTune-style line hierarchy: distance controls alpha and blur, active stays crisp. */
@Composable
internal fun Modifier.lyricsLineFocus(
    isSynced: Boolean,
    isActive: Boolean,
    distance: Int,
    isBrowsing: Boolean,
    textAlign: TextAlign,
): Modifier {
    val blurEnabled = LocalBlurEnabled.current
    val lineAlpha = when {
        !isSynced -> 0.92f
        isActive -> 1f
        isBrowsing -> when {
            distance == 1 -> 0.72f
            distance == 2 -> 0.56f
            distance == 3 -> 0.40f
            else -> 0.28f
        }
        distance == 1 -> 0.52f
        distance == 2 -> 0.30f
        distance == 3 -> 0.18f
        else -> 0.10f
    }
    val targetBlur = when {
        !isSynced || isActive || isBrowsing || !blurEnabled -> 0f
        distance == 1 -> 2f
        distance == 2 -> 5f
        else -> 12f
    }
    val alpha by animateFloatAsState(
        targetValue = lineAlpha,
        animationSpec = tween(if (isActive) 330 else 500, easing = FastOutSlowInEasing),
        label = "archiveLyricsLineAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.95f,
        animationSpec = tween(166, easing = FastOutSlowInEasing),
        label = "archiveLyricsLineScale",
    )
    val blur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "archiveLyricsLineBlur",
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pivot = when (textAlign) {
        TextAlign.Right -> 1f
        TextAlign.Left -> 0f
        TextAlign.End -> if (isRtl) 0f else 1f
        TextAlign.Center -> 0.5f
        else -> if (isRtl) 1f else 0f
    }
    return this
        .graphicsLayer {
            alpha = alpha
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(pivot, 0.5f)
        }
        .appBlur(blur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
}

internal suspend fun LazyListState.scrollToLyric(index: Int) {
    if (index !in 0 until layoutInfo.totalItemsCount) return
    val anchor = layoutInfo.viewportStartOffset + (layoutInfo.viewportSize.height * 0.35f).toInt()
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item != null) {
        val delta = item.offset - anchor
        if (abs(delta) > 1) animateScrollBy(delta.toFloat(), tween(650, easing = ArchiveLyricsEase))
    } else if (abs(index - firstVisibleItemIndex) > 15) {
        scrollToItem(index, -anchor)
    } else {
        animateScrollToItem(index, -anchor)
    }
}
