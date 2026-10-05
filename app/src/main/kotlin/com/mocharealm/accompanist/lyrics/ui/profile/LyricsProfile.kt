package com.mocharealm.accompanist.lyrics.ui.profile

import com.mocharealm.accompanist.lyrics.ui.preparation.MeasuredLyricsLine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/**
 * Ordered, first-match writing-system policy. Profiles choose static effect policy; playback and
 * effect calculations belong to the renderer.
 */
interface LyricsProfile {
    fun matches(syllable: KaraokeSyllable): Boolean

    fun groups(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>>

    fun prepare(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit>

    fun prepare(
        line: MeasuredLyricsLine,
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> = prepare(group, measurer, style)

    fun effects(units: List<ProfileTextUnit>, accompaniment: Boolean): ProfileGroupEffects =
        ProfileGroupEffects()

    /**
     * Width-aware preparation. Each returned group is indivisible during row wrapping. Override to
     * choose script-specific breaks; oversized units use the platform line breaker. This runs only
     * during static preparation, never during playback.
     */
    fun wrap(
        units: List<ProfileTextUnit>,
        measurer: TextMeasurer,
        maxWidth: Float,
    ): List<List<ProfileTextUnit>> {
        val result = mutableListOf<List<ProfileTextUnit>>()
        var fragment = mutableListOf<ProfileTextUnit>()
        var width = 0f
        for (original in units) {
            if (original.width > maxWidth || original.layout.lineCount > 1) {
                if (fragment.isNotEmpty()) result.add(fragment)
                fragment = mutableListOf()
                width = 0f
                result.addAll(breakShapedUnit(original, measurer, maxWidth).map { listOf(it) })
                continue
            }
            val unit = original
            if (fragment.isNotEmpty() && width + unit.width > maxWidth) {
                result.add(fragment)
                fragment = mutableListOf()
                width = 0f
            }
            fragment.add(unit)
            width += unit.width
        }
        if (fragment.isNotEmpty()) result.add(fragment)
        return result
    }

    /** Optional single drawable for a group when its units share the same transform. */
    fun combine(units: List<ProfileTextUnit>): ProfileTextUnit? = null

    fun DrawScope.draw(unit: ProfileTextUnit, color: Color, shadow: Shadow)
}

/** Shared grouping, shaping, safe-boundary protection and drawing for custom profiles. */
open class DefaultLyricsProfile : LyricsProfile {
    override fun matches(syllable: KaraokeSyllable) = true

    override fun groups(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
        val result = mutableListOf<List<KaraokeSyllable>>()
        var group = mutableListOf<KaraokeSyllable>()
        for (source in syllables) {
            var begin = 0
            while (begin < source.content.length) {
                var end = begin
                while (end < source.content.length && !source.content[end].isWhitespace()) end++
                while (end < source.content.length && source.content[end].isWhitespace()) end++
                val syllable =
                    if (begin == 0 && end == source.content.length) source
                    else
                        source.copy(
                            content = source.content.substring(begin, end),
                            start =
                                source.start +
                                    ((source.end.toLong() - source.start) * begin /
                                            source.content.length)
                                        .toInt(),
                            end =
                                source.start +
                                    ((source.end.toLong() - source.start) * end /
                                            source.content.length)
                                        .toInt(),
                            phonetic = if (begin == 0) source.phonetic else null,
                        )
                group.add(syllable)
                if (syllable.content.lastOrNull()?.isWhitespace() == true) {
                    result.add(group)
                    group = mutableListOf()
                }
                begin = end
            }
        }
        if (group.isNotEmpty()) result.add(group)
        return result
    }

    override fun prepare(
        line: MeasuredLyricsLine,
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> =
        protectShapedUnits(
            prepareWithLayout(
                group,
                measurer,
                style,
                line.layout.takeIf {
                    it.layoutInput.text.text ==
                        group.joinToString("") { syllable -> syllable.content }
                },
            )
        )

    override fun prepare(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> {
        return protectShapedUnits(prepareWithLayout(group, measurer, style, null))
    }

    protected open fun prepareWithLayout(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
        initialLayout: TextLayoutResult?,
    ): List<ProfileTextUnit> {
        val layout =
            initialLayout
                ?: measurer.measure(group.joinToString("") { it.content }, style, softWrap = false)
        var offset = 0
        val timing =
            group.mapNotNull { syllable ->
                val begin = offset
                offset += syllable.content.length
                if (begin == offset) null
                else {
                    var left = Float.POSITIVE_INFINITY
                    var right = Float.NEGATIVE_INFINITY
                    for (index in begin until offset) {
                        val bounds = layout.getBoundingBox(index)
                        left = minOf(left, bounds.left)
                        right = maxOf(right, bounds.right)
                    }
                    ProfileTiming(
                        syllable.start,
                        syllable.end,
                        left,
                        right,
                        TextRange(begin, offset),
                    )
                }
            }
        return listOf(
            ProfileTextUnit(
                layout,
                0f,
                layout.size.width.toFloat(),
                group.minOf { it.start },
                group.maxOf { it.end },
                group.mapNotNull { it.phonetic }.joinToString(" ").ifBlank { null },
                timing,
                sourceRange = TextRange(0, offset),
            )
        )
    }

    override fun combine(units: List<ProfileTextUnit>): ProfileTextUnit? {
        if (units.size == 1) return units.single()
        if (units.any { it.layout.lineCount > 1 }) return null
        if (units.isEmpty() || units.any { it.layout !== units.first().layout }) return null
        return ProfileTextUnit(
            units.first().layout,
            units.minOf { it.left },
            units.maxOf { it.right },
            units.minOf { it.start },
            units.maxOf { it.end },
        )
    }

    override fun DrawScope.draw(unit: ProfileTextUnit, color: Color, shadow: Shadow) {
        // Preserve contextual shaping. Clipping is only needed for independently movable slices.
        if (unit.left == 0f && unit.right == unit.layout.size.width.toFloat()) {
            drawText(unit.layout, color, shadow = shadow)
        } else {
            clipRect(
                if (unit.left <= unit.layout.getLineLeft(0)) -unit.height else 0f,
                -unit.layout.size.height.toFloat(),
                if (unit.right >= unit.layout.getLineRight(0)) unit.width + unit.height
                else unit.width,
                unit.layout.size.height * 2f,
            ) {
                drawText(unit.layout, color, topLeft = Offset(-unit.left, 0f), shadow = shadow)
            }
        }
    }
}

/**
 * Preserves source timing units for grouping and motion while retaining shared shaping.
 * A word is the supplied KaraokeSyllable unit; this profile does not perform lexical segmentation.
 */
open class WordLevelLyricsProfile : DefaultLyricsProfile() {
    override fun prepare(
        line: MeasuredLyricsLine,
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> =
        bindSyllableAnimations(line, super.prepare(line, group, measurer, style))

    override fun prepareWithLayout(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
        initialLayout: TextLayoutResult?,
    ): List<ProfileTextUnit> {
        val whole = super.prepareWithLayout(group, measurer, style, initialLayout).single()
        var offset = 0
        return group.mapNotNull { syllable ->
            val begin = offset
            offset += syllable.content.length
            if (begin == offset) null
            else {
                var left = Float.POSITIVE_INFINITY
                var right = Float.NEGATIVE_INFINITY
                for (i in begin until offset) {
                    val bounds = whole.layout.getBoundingBox(i)
                    left = minOf(left, bounds.left)
                    right = maxOf(right, bounds.right)
                }
                ProfileTextUnit(
                    whole.layout,
                    left,
                    right,
                    syllable.start,
                    syllable.end,
                    syllable.phonetic,
                    timing =
                        listOf(
                            ProfileTiming(
                                syllable.start,
                                syllable.end,
                                0f,
                                right - left,
                                TextRange(begin, offset),
                            )
                        ),
                    sourceStart = syllable.start,
                    animation = ProfileAnimationUnit(syllable.start, syllable.end),
                    sourceRange = TextRange(begin, offset),
                )
            }
        }
    }

    // Never split inside a source syllable, even when it contains spaces. Adjacent
    // syllables without a separator stay shaped together to retain joining forms.
    override fun groups(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
        val result = mutableListOf<List<KaraokeSyllable>>()
        var group = mutableListOf<KaraokeSyllable>()
        for (syllable in syllables) {
            group.add(syllable)
            if (syllable.content.lastOrNull()?.isWhitespace() == true) {
                result.add(group)
                group = mutableListOf()
            }
        }
        if (group.isNotEmpty()) result.add(group)
        return result
    }
}

val DefaultLyricsProfiles: List<LyricsProfile> =
    listOf(
        ArabicProfile,
        HebrewProfile,
        IndicProfile,
        SoutheastAsianProfile,
        TibetanProfile,
        HangulJamoProfile,
        CyrillicProfile,
        GreekProfile,
        CjkProfile,
        LatinProfile,
        FallbackProfile,
    )