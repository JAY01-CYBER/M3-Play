package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.ui.preparation.MeasuredLyricsLine

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

object LatinProfile : DefaultLyricsProfile() {
    override fun prepare(
        line: MeasuredLyricsLine,
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> =
        bindSyllableAnimations(line, super.prepare(line, group, measurer, style))

    override fun effects(
        units: List<ProfileTextUnit>,
        accompaniment: Boolean,
    ): ProfileGroupEffects {
        val duration =
            (units.maxOfOrNull { it.end } ?: 0).toLong() - (units.minOfOrNull { it.start } ?: 0)
        val timingCount = units.sumOf { it.timing.size }
        val enabled =
            !accompaniment && timingCount > 1 && duration >= 1000 && duration / timingCount > 200
        return ProfileGroupEffects(scale = enabled, glow = enabled)
    }

    override fun matches(syllable: KaraokeSyllable) =
        syllable.content.any {
            it in 'A'..'Z' ||
                it in 'a'..'z' ||
                it.code in 0x00C0..0x024F ||
                it.code in 0x1E00..0x1EFF
        }

    override fun prepareWithLayout(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
        initialLayout: TextLayoutResult?,
    ): List<ProfileTextUnit> {
        val content = group.joinToString("") { it.content }
        // These are candidate timing ranges. Shared shaping boundary protection below the
        // profile joins ranges that cannot be transformed independently, for every font/script.
        val layout = initialLayout ?: measurer.measure(content, style, softWrap = false)
        val result = mutableListOf<ProfileTextUnit>()
        var offset = 0
        for (syllable in group) {
            val animation = ProfileAnimationUnit(syllable.start, syllable.end)
            for (index in syllable.content.indices) {
                val bounds = layout.getBoundingBox(offset + index)
                val start =
                    syllable.start +
                        ((syllable.end.toLong() - syllable.start) * index / syllable.content.length)
                            .toInt()
                val end =
                    syllable.start +
                        ((syllable.end.toLong() - syllable.start) * (index + 1) /
                                syllable.content.length)
                            .toInt()
                val range = TextRange(offset + index, offset + index + 1)
                result.add(
                    ProfileTextUnit(
                        layout,
                        bounds.left,
                        bounds.right,
                        start,
                        end,
                        if (index == 0) syllable.phonetic else null,
                        timing = listOf(ProfileTiming(start, end, 0f, bounds.width, range)),
                        sourceStart = syllable.start,
                        animation = animation,
                        sourceRange = range,
                    )
                )
            }
            offset += syllable.content.length
        }
        return result
    }
}
