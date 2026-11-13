package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Keeps contextual shaping and lifts original syllables as a whole. */
object HangulJamoProfile : WordLevelLyricsProfile() {
    override fun matches(syllable: KaraokeSyllable): Boolean =
        syllable.content.any {
            it.code in 0x1100..0x11FF || it.code in 0xA960..0xA97F || it.code in 0xD7B0..0xD7FF
        }
}
