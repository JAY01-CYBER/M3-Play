package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Position of the focused item's top edge within the lyrics viewport. */
@Immutable
sealed interface LyricsAnchor {
    /** Distance from the top of the lyrics viewport, clamped to its height. */
    data class Fixed(val offset: Dp) : LyricsAnchor {
        init {
            require(offset.value.isFinite() && offset >= 0.dp)
        }
    }

    /** Fraction of the lyrics viewport height; 0.5 places the anchor at its center. */
    data class Fraction(val fraction: Float) : LyricsAnchor {
        init {
            require(fraction.isFinite() && fraction in 0f..1f)
        }
    }
}
