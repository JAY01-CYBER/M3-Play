package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Keeps contextual shaping and lifts original syllables as a whole. */
object ArabicProfile : WordLevelLyricsProfile() {
    override fun matches(syllable: KaraokeSyllable): Boolean =
        syllable.content.any {
            it.code in 0x0600..0x06FF ||
                it.code in 0x0750..0x077F ||
                it.code in 0x0870..0x089F ||
                it.code in 0x08A0..0x08FF ||
                it.code in 0xFB50..0xFDFF ||
                it.code in 0xFE70..0xFEFF
        }
}
