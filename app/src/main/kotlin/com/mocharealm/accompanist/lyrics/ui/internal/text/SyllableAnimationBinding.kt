package com.mocharealm.accompanist.lyrics.ui.internal.text

import com.mocharealm.accompanist.lyrics.ui.preparation.MeasuredLyricsLine
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileTextUnit

internal fun bindSyllableAnimations(line: MeasuredLyricsLine, units: List<ProfileTextUnit>) =
    units.map { unit ->
        val source = line.sourceFor(unit.start, unit.end)
        if (source == null) unit
        else unit.copy(animation = source.animation, sourceStart = source.source.start)
    }
