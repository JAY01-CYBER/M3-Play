package com.mocharealm.accompanist.lyrics.ui.internal.effects

import androidx.compose.animation.core.spring

internal val LyricsRevealSpring =
    spring<Float>(dampingRatio = 1f, stiffness = 180f, visibilityThreshold = 0.001f)

internal fun revealScale(progress: Float) = 0.8f + 0.2f * progress.coerceIn(0f, 1f)

internal fun revealAlpha(progress: Float) = (progress / 0.65f).coerceIn(0f, 1f)

internal fun revealBlurIndex(progress: Float) = ((1f - progress.coerceIn(0f, 1f)) * 64f).toInt()

