package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Keeps contextual shaping and lifts original syllables as a whole. */
object SoutheastAsianProfile : WordLevelLyricsProfile() {
    override fun matches(syllable: KaraokeSyllable): Boolean =
        syllable.content.any {
            it.code in 0x0E00..0x0EFF ||
                it.code in 0x1000..0x109F ||
                it.code in 0x1780..0x17FF ||
                it.code in 0x19E0..0x19FF ||
                it.code in 0xA9E0..0xA9FF ||
                it.code in 0xAA60..0xAA7F
        }
}
