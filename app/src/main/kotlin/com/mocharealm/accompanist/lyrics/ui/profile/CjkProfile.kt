package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

object CjkProfile : DefaultLyricsProfile() {
    override fun effects(
        units: List<ProfileTextUnit>,
        accompaniment: Boolean,
    ): ProfileGroupEffects {
        val duration =
            (units.maxOfOrNull { it.end } ?: 0).toLong() - (units.minOfOrNull { it.start } ?: 0)
        return ProfileGroupEffects(glow = !accompaniment && duration >= 1000)
    }

    override fun matches(syllable: KaraokeSyllable): Boolean {
        val text = syllable.content
        for (index in text.indices) {
            val value = text[index]
            if (value.isCjk() || value.isJapanese() || value.isKorean()) return true
            if (
                value.isHighSurrogate() &&
                    index + 1 < text.length &&
                    text[index + 1].isLowSurrogate()
            ) {
                val codePoint =
                    0x10000 + ((value.code - 0xD800) shl 10) + text[index + 1].code - 0xDC00
                if (codePoint in 0x20000..0x323AF) return true
            }
        }
        return false
    }

    override fun groups(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
        val result = mutableListOf<MutableList<KaraokeSyllable>>()
        val boundaries = graphemeBoundaries(syllables.joinToString("") { it.content })
        var sourceOffset = 0
        for (syllable in syllables) {
            var start = 0
            var index = 0
            val ranges = mutableListOf<IntRange>()
            while (index < syllable.content.length) {
                start = index++
                while (index < syllable.content.length && !boundaries[sourceOffset + index]) index++
                while (
                    index < syllable.content.length &&
                        (syllable.content[index].category in
                            listOf(
                                CharCategory.NON_SPACING_MARK,
                                CharCategory.COMBINING_SPACING_MARK,
                                CharCategory.ENCLOSING_MARK,
                            ) ||
                            syllable.content[index].isWhitespace() ||
                            syllable.content[index].toString().isPunctuation())
                ) index++
                ranges.add(start until index)
            }
            ranges.forEachIndexed { i, range ->
                val target =
                    if (!boundaries[sourceOffset + range.first] && result.isNotEmpty())
                        result.last()
                    else mutableListOf<KaraokeSyllable>().also(result::add)
                target.add(
                    syllable.copy(
                        content = syllable.content.substring(range),
                        start =
                            syllable.start +
                                ((syllable.end.toLong() - syllable.start) * i / ranges.size)
                                    .toInt(),
                        end =
                            syllable.start +
                                ((syllable.end.toLong() - syllable.start) * (i + 1) / ranges.size)
                                    .toInt(),
                        phonetic = if (i == 0) syllable.phonetic else null,
                    )
                )
            }
            sourceOffset += syllable.content.length
        }
        return result
    }
}
