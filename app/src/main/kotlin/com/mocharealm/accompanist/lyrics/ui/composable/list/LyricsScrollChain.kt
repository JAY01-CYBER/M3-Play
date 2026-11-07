package com.mocharealm.accompanist.lyrics.ui.composable.list

import androidx.compose.runtime.Immutable

/**
 * The leading item's scroll keeps its configured duration/easing; following items spring behind it.
 */
@Immutable
data class LyricsScrollChain(
    val stiffness: Float = 100f,
    val damping: Float = 12f,
    val coupling: Float = 0.65f,
    val distanceFalloff: Float = 0.25f,
    val minResponse: Float = 0.35f,
) {
    init {
        require(stiffness.isFinite() && stiffness in 1f..1000f)
        require(damping.isFinite() && damping in 1f..100f)
        require(coupling.isFinite() && coupling in 0f..0.95f)
        require(distanceFalloff.isFinite() && distanceFalloff in 0f..1f)
        require(minResponse.isFinite() && minResponse in 0.05f..1f)
    }
}
