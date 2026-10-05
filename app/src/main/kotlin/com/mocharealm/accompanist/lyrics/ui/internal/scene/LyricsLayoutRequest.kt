package com.mocharealm.accompanist.lyrics.ui.internal.scene

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.prepareLyrics
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile

/** Layout invalidation inputs. Paint color and playback never invalidate text geometry. */
internal data class LyricsLayoutRequest(
    val lyrics: SyncedLyrics,
    val profiles: List<LyricsProfile>,
    val measurer: TextMeasurer,
    val normalStyle: TextStyle,
    val accompanimentStyle: TextStyle,
    val phoneticStyle: TextStyle,
    val width: Float,
    val density: Density,
    val direction: LayoutDirection,
) {
    fun prepare() = prepareLyrics(
        lyrics, profiles, measurer, normalStyle, accompanimentStyle, phoneticStyle,
        width, density.density, true, fontScale = density.fontScale,
    )
}
