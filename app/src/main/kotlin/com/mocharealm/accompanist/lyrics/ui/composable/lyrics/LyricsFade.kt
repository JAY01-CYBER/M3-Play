package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fade length measured inward from a viewport edge. Zero disables that edge's fade. */
@Immutable
sealed interface LyricsFade {
    data class Fixed(val length: Dp) : LyricsFade {
        init { require(length.value.isFinite() && length >= 0.dp) }
    }

    /** Fraction of the lyrics viewport height, e.g. 0.2 fades the bottom 20%. */
    data class Fraction(val fraction: Float) : LyricsFade {
        init { require(fraction.isFinite() && fraction in 0f..1f) }
    }

    /** Length equal to the anchor's distance from the top minus [inset], clamped to zero. */
    data class ToAnchor(val inset: Dp = 0.dp) : LyricsFade {
        init { require(inset.value.isFinite() && inset >= 0.dp) }
    }
}
