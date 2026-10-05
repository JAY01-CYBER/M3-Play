package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Profile-owned policy; easing and effect implementation remain in the renderer. */
data class ProfileGroupEffects(
    val scale: Boolean = false,
    val glow: Boolean = false,
    val lift: Boolean = true,
)

/** Shared identity means these drawables move together, independent of text segmentation. */
data class ProfileAnimationUnit(val start: Int, val end: Int)
