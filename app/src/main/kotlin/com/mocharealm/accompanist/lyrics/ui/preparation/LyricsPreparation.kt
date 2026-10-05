package com.mocharealm.accompanist.lyrics.ui.preparation

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.internal.preparation.prepareLyricsInternal
import com.mocharealm.accompanist.lyrics.ui.internal.preparation.prepareLyricsLineInternal
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile

/** Prepare a complete lyrics scene, including measured rows and nested accompaniment. */
fun prepareLyrics(
    lyrics: SyncedLyrics,
    profiles: List<LyricsProfile>,
    measurer: TextMeasurer,
    normalStyle: TextStyle,
    accompanimentStyle: TextStyle,
    phoneticStyle: TextStyle,
    width: Float,
    density: Float,
    showPhonetic: Boolean,
    fontScale: Float = 1f,
): PreparedLyrics =
    prepareLyricsInternal(
        lyrics,
        profiles,
        measurer,
        normalStyle,
        accompanimentStyle,
        phoneticStyle,
        width,
        density,
        showPhonetic,
        fontScale,
    )

/** Prepare one line for callers that own a line cache. */
fun prepareLyricsLine(
    line: KaraokeLine,
    profiles: List<LyricsProfile>,
    measurer: TextMeasurer,
    normalStyle: TextStyle,
    accompanimentStyle: TextStyle,
    phoneticStyle: TextStyle,
    width: Float,
    density: Float,
    showPhonetic: Boolean,
    animate: Boolean = true,
    fontScale: Float = 1f,
): PreparedLine =
    prepareLyricsLineInternal(
        line,
        profiles,
        measurer,
        normalStyle,
        accompanimentStyle,
        phoneticStyle,
        width,
        density,
        showPhonetic,
        animate,
        fontScale,
    )
