package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Keeps contextual shaping and lifts original syllables as a whole. */
object IndicProfile : WordLevelLyricsProfile() {
    override fun matches(syllable: KaraokeSyllable): Boolean =
        syllable.content.any {
            it.code in 0x0900..0x0DFF || it.code in 0x1CD0..0x1CFF || it.code in 0xA8E0..0xA8FF
        }
}
