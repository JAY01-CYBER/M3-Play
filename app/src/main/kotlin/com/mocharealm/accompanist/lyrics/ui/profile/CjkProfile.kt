package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

object CjkProfile : DefaultLyricsProfile() {
    override fun effects(
        units: List<ProfileTextUnit>,
        accompaniment: Boolean,
    ): ProfileGroupEffects {
        val duration =
            (units.maxOfOrNull { it.end } ?: 0).toLong() - (units.minOfOrNull { it.start } ?: 0)
        return ProfileGroupEffects(glow = !accompaniment && duration >= 1000, glowAsGroup = true)
    }

    override fun prepareWithLayout(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
        initialLayout: TextLayoutResult?,
    ): List<ProfileTextUnit> {
        val whole = super.prepareWithLayout(group, measurer, style, initialLayout).single()
        // groups() retains the source word for phonetic layout and glow. Its grapheme timings
        // become separate drawables so those words still lift one character at a time.
        return whole.timing.mapIndexed { index, timing ->
            val range = requireNotNull(timing.sourceRange)
            val from = whole.layout.getHorizontalPosition(range.min, usePrimaryDirection = true)
            val to = whole.layout.getHorizontalPosition(range.max, usePrimaryDirection = true)
            val left = minOf(from, to)
            val right = maxOf(from, to)
            ProfileTextUnit(
                layout = whole.layout,
                left = left,
                right = right,
                start = timing.start,
                end = timing.end,
                phonetic = if (index == 0) whole.phonetic else null,
                timing = listOf(timing.copy(left = 0f, right = right - left)),
                sourceRange = timing.sourceRange,
            )
        }
    }

    override fun protectUnits(units: List<ProfileTextUnit>): List<ProfileTextUnit> =
        // CJK graphemes move within separate advance cells. Requiring transparent guard columns
        // also merges normal adjacent characters; grapheme and overlapping-cell checks suffice.
        protectShapedUnits(units, requireClearInk = false)

    override fun wrap(
        units: List<ProfileTextUnit>,
        measurer: TextMeasurer,
        maxWidth: Float,
    ): List<List<ProfileTextUnit>> {
        if (units.isEmpty()) return emptyList()
        if (units.any { it.width > maxWidth || it.layout.lineCount > 1 })
            return super.wrap(units, measurer, maxWidth)
        val widths = FloatArray(units.size + 1)
        for (index in units.indices) widths[index + 1] = widths[index] + units[index].width
        if (widths.last() <= maxWidth) return listOf(units)

        // Preserve balanced Unicode wrapping after splitting a source word into drawables.
        val offsets = IntArray(units.size + 1)
        val text = buildString {
            for ((index, unit) in units.withIndex()) {
                val source = unit.layout.layoutInput.text.text
                val range = requireNotNull(unit.sourceRange)
                append(source, range.min, range.max)
                offsets[index + 1] = length
            }
        }
        val localeTag =
            units.first().layout.layoutInput.style.localeList?.firstOrNull()?.toLanguageTag()
        val legal = lineBreakBoundaries(text, localeTag).toSet()
        val candidates = (1 until units.size).map { index ->
            LineBreakCandidate(index, if (offsets[index] in legal) 0.0 else EmergencyBreakPenalty)
        }
        val breaks = balancedLineBreaks(units.size, candidates, maxWidth) { from, to ->
            widths[to] - widths[from]
        } ?: return super.wrap(units, measurer, maxWidth)
        var start = 0
        return breaks.map { end -> units.subList(start, end).also { start = end } }
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
        val text = syllables.joinToString("") { it.content }
        val boundaries = graphemeBoundaries(text)
        val words = wordBoundaries(syllables, text)
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
                val offset = sourceOffset + range.first
                // Word boundaries are independent of timing boundaries. Preserve a supplied
                // caption as one group when its source syllable spans several dictionary words.
                val startsWord = boundaries[offset] && words[offset] &&
                    (i == 0 || syllable.phonetic.isNullOrBlank())
                if (result.isEmpty() || startsWord) result.add(mutableListOf())
                result.last().add(
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

    private fun wordBoundaries(syllables: List<KaraokeSyllable>, text: String): BooleanArray {
        val result = BooleanArray(text.length + 1)
        var runStart = 0
        var offset = 0
        var localeTag: String? = null
        fun finishRun() {
            for (boundary in platformWordBreakBoundaries(text.substring(runStart, offset), localeTag)) {
                result[runStart + boundary] = true
            }
            result[runStart] = true
            result[offset] = true
        }
        for (syllable in syllables) {
            if (syllable.content.isEmpty()) continue
            if (syllable.languageTag != localeTag) {
                if (offset > runStart) finishRun()
                runStart = offset
                localeTag = syllable.languageTag
            }
            offset += syllable.content.length
        }
        if (offset > runStart) finishRun()
        result[0] = true
        result[text.length] = true
        return result
    }
}
