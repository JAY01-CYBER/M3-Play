/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

val LocalBackdropState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun Modifier.appBackdropSource(zIndex: Float = 0f): Modifier {
    val state = LocalBackdropState.current
    return if (LocalBlurEnabled.current && state != null) hazeSource(state, zIndex = zIndex) else this
}

/** Captures the background only; labels and icons retain their original sharpness. */
@Composable
fun Modifier.frostedControl(tint: Color = MaterialTheme.colorScheme.surfaceContainer): Modifier {
    val state = LocalBackdropState.current
    if (!LocalBlurEnabled.current || state == null) return this
    val style = remember(tint) {
        HazeBlurStyle {
            blurRadius(18.dp)
            noiseFactor(0f)
            backgroundColor(tint)
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = 0.7f))))
        }
    }
    return hazeBlur(HazeInput.Sources(state), style, performanceMode = HazePerformanceMode.Performance)
}
