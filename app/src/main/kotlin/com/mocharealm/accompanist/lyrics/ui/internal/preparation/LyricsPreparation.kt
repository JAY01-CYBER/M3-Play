package com.mocharealm.accompanist.lyrics.ui.internal.preparation

import com.mocharealm.accompanist.lyrics.ui.internal.text.*
import com.mocharealm.accompanist.lyrics.ui.preparation.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile
import com.mocharealm.accompanist.lyrics.ui.preparation.MeasuredLyricsLine
import com.mocharealm.accompanist.lyrics.ui.internal.text.resolveProfiles
import com.mocharealm.accompanist.lyrics.ui.internal.text.isRtl

private data class PreparedAtom(
    val profile: LyricsProfile,
    val group: PreparedGroup,
    val sourceText: String,
    val phoneticGapBefore: Float = 0f,
)

/**
 * Prepare the entire scene before publishing it. The cache also shares nested/top-level
 * accompaniment.
 */
internal fun prepareLyricsInternal(
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
    translationStyle: TextStyle,
): PreparedLyrics {
    val cache = mutableMapOf<KaraokeLine, PreparedLine>()
    val sweepWidths = mutableMapOf<TextStyle, Float>()
    val sides =
        lyrics.lines.map { line ->
            val rtl =
                when (line) {
                    is KaraokeLine -> line.syllables.joinToString("") { it.content }.isRtl()
                    is SyncedLine -> line.content.isRtl()
                    else -> false
                }
            if (line is KaraokeLine && line.alignment == KaraokeAlignment.End) !rtl else rtl
        }
    val layoutWidth = if (sides.any { it } && sides.any { !it }) width * 0.8f else width
    return PreparedLyrics(
        lyrics.lines.map { line ->
            when (line) {
                is KaraokeLine ->
                    prepareLine(
                        line,
                        profiles,
                        measurer,
                        normalStyle,
                        accompanimentStyle,
                        phoneticStyle,
                        translationStyle,
                        width,
                        density,
                        showPhonetic,
                        true,
                        cache,
                        sweepWidths,
                        layoutWidth,
                        fontScale,
                    )
                is SyncedLine ->
                    prepareLine(
                        KaraokeLine.MainKaraokeLine(
                            listOf(KaraokeSyllable(line.content, line.start, line.end, languageTag = line.languageTag)),
                            line.translation,
                            KaraokeAlignment.Start,
                            line.start,
                            line.end,
                            phonetic = line.phonetic,
                            languageTag = line.languageTag,
                        ),
                        profiles,
                        measurer,
                        normalStyle,
                        accompanimentStyle,
                        phoneticStyle,
                        translationStyle,
                        width,
                        density,
                        showPhonetic,
                        false,
                        mutableMapOf(),
                        sweepWidths,
                        layoutWidth,
                        fontScale,
                    )
                else -> null
            }
        }
    )
}

/** All shaping, grouping, wrapping and geometry is completed before this value is published. */
internal fun prepareLyricsLineInternal(
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
    translationStyle: TextStyle,
): PreparedLine =
    prepareLine(
        line,
        profiles,
        measurer,
        normalStyle,
        accompanimentStyle,
        phoneticStyle,
        translationStyle,
        width,
        density,
        showPhonetic,
        animate,
        mutableMapOf(),
        mutableMapOf(),
        fontScale = fontScale,
    )

private fun prepareLine(
    line: KaraokeLine,
    profiles: List<LyricsProfile>,
    measurer: TextMeasurer,
    normalStyle: TextStyle,
    accompanimentStyle: TextStyle,
    phoneticStyle: TextStyle,
    translationStyle: TextStyle,
    width: Float,
    density: Float,
    showPhonetic: Boolean,
    animate: Boolean,
    cache: MutableMap<KaraokeLine, PreparedLine>,
    sweepWidths: MutableMap<TextStyle, Float>,
    layoutWidth: Float = width,
    fontScale: Float = 1f,
): PreparedLine {
    cache[line]?.let {
        return it
    }
    require(width.isFinite() && width > 0f) { "Prepared lyrics require a finite, positive width" }
    val accompaniment = line is KaraokeLine.AccompanimentKaraokeLine
    val style =
        (if (accompaniment) accompanimentStyle else normalStyle).copy(
            textMotion = TextMotion.Animated,
            lineBreak = LineBreak.Paragraph,
        )
    val animatedPhoneticStyle = phoneticStyle.copy(textMotion = TextMotion.Animated)
    val animatedTranslationStyle =
        translationStyle.copy(
            textMotion = TextMotion.Animated,
            lineBreak = LineBreak.Paragraph,
        )
    val sweepFadeWidth =
        sweepWidths.getOrPut(style) {
            measurer
                .measure("M", style, softWrap = false)
                .size
                .width
                .toFloat()
                .takeIf { it.isFinite() && it > 0f }
                ?: 0.001f
        }
    val rtl = line.syllables.joinToString("") { it.content }.isRtl()
    val rightAligned = if (line.alignment == KaraokeAlignment.End) !rtl else rtl
    val measuredLine =
        MeasuredLyricsLine(
            measurer.measure(
                line.syllables.joinToString("") { it.content },
                style,
                softWrap = false,
            ),
            line.syllables,
        )
    val runs =
        resolveProfiles(line.syllables, profiles, leadingWhitespace = rightAligned).map { run ->
            PreparedProfileRun(
                run.profile,
                buildList {
                    for (source in run.groups) {
                        for (fragment in
                            run.profile.wrap(
                                run.profile.prepare(measuredLine, source, measurer, style),
                                measurer,
                                layoutWidth,
                            )) {
                            val units =
                                fragment.map { text ->
                                    PreparedTextUnit(
                                        text,
                                        if (showPhonetic)
                                            text.phonetic
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let {
                                                    measurer.measure(
                                                        it,
                                                        animatedPhoneticStyle,
                                                        softWrap = true,
                                                        constraints =
                                                            Constraints(
                                                                maxWidth =
                                                                    layoutWidth
                                                                        .toInt()
                                                                        .coerceAtLeast(1)
                                                            ),
                                                    )
                                                }
                                        else null,
                                    )
                                }
                            if (units.isNotEmpty())
                                add(PreparedGroup(units, accompaniment || !animate, run.profile))
                        }
                    }
                },
            )
        }
    val phoneticSpaceWidth by lazy(LazyThreadSafetyMode.NONE) {
        measurer.measure(" ", animatedPhoneticStyle, softWrap = false).size.width.toFloat()
    }
    val atoms = buildList {
        for (run in runs) for (group in run.groups) {
            val atom = PreparedAtom(run.profile, group, group.preparedSourceText())
            val previous = lastOrNull()
            add(
                atom.copy(
                    phoneticGapBefore =
                        if (previous != null)
                            phoneticWordGap(previous, atom, rtl) { phoneticSpaceWidth }
                        else 0f,
                )
            )
        }
    }
    val rows = mutableListOf<PreparedRow>()
    var rowRuns = mutableListOf<PreparedProfileRun>()
    var rowGroups = mutableListOf<PreparedGroup>()
    val rowGaps = mutableListOf<Float>()
    var rowProfile: LyricsProfile? = null
    var rowWidth = 0f
    var top = 0f
    val wrappedRowSpacing = 8f * density
    var previousPhoneticSpacing = 0f
    fun flushRun() {
        if (rowGroups.isNotEmpty()) rowRuns.add(PreparedProfileRun(rowProfile!!, rowGroups))
        rowGroups = mutableListOf()
    }
    fun flushRow() {
        flushRun()
        if (rowRuns.isEmpty()) {
            rowProfile = null
            return
        }
        var baseline = 0f
        var descent = 0f
        var phoneticTextHeight = 0f
        var hasPhonetics = false
        for (run in rowRuns) for (group in run.groups) for (unit in group.units) {
            baseline = maxOf(baseline, unit.text.baseline)
            descent = maxOf(descent, unit.text.height - unit.text.baseline)
            unit.phonetic?.let {
                hasPhonetics = true
                phoneticTextHeight = maxOf(phoneticTextHeight, it.size.height.toFloat())
            }
        }
        val phoneticGap = if (hasPhonetics) 4f * density else 0f
        val phoneticHeight = phoneticTextHeight + phoneticGap
        val left = if (rightAligned) width - rowWidth else 0f
        var x = if (rtl) left + rowWidth else left
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        val lefts = mutableListOf<Float>()
        val rights = mutableListOf<Float>()
        var start = Int.MAX_VALUE
        var end = Int.MIN_VALUE
        var sweepEnd = Int.MIN_VALUE
        val windows = mutableListOf<RenderWindow>()
        var groupIndex = 0
        for (run in rowRuns) for (group in run.groups) {
            val gap = rowGaps[groupIndex++]
            x += if (rtl) -gap else gap
            val groupLeft = if (rtl) x - group.width else x
            val groupRtl = group.preparedSourceText().isRtl(fallback = rtl)
            val textLeft = groupLeft + if (groupRtl) group.width - group.textWidth else 0f
            val unitEffects =
                group.effects.scale || (group.effects.glow && !group.effects.glowAsGroup)
            val groupGlowEnd =
                if (group.effects.glow && group.effects.glowAsGroup)
                    group.start + group.animationDuration
                else Float.NEGATIVE_INFINITY
            group.pivot =
                Offset(textLeft + group.textWidth / 2f, top + baseline + descent)
            var localX = textLeft + if (groupRtl) group.textWidth else 0f
            for ((index, unit) in group.units.withIndex()) {
                val unitX =
                    if (group.sharedLayout)
                        textLeft + unit.text.left - group.shapingLeft
                    else if (groupRtl) localX - unit.width else localX
                unit.position = Offset(unitX, top + baseline - unit.text.baseline)
                unit.phoneticPosition =
                    Offset(
                        groupLeft +
                            (if (groupRtl) group.width - (unit.phonetic?.size?.width ?: 0) else 0f) -
                            unitX,
                        unit.text.baseline + descent + phoneticGap,
                    )
                unit.animationStart =
                    if (unitEffects && group.units.size > 1)
                        group.start +
                            (group.duration - group.animationDuration) * index /
                                (group.units.size - 1)
                    else unit.text.sourceStart.toFloat()
                for (timing in unit.text.timing) {
                    starts.add(timing.start)
                    ends.add(timing.end)
                    if (timing.end > timing.start)
                        windows.add(RenderWindow(timing.start, timing.end))
                    lefts.add(unitX + timing.left)
                    rights.add(unitX + timing.right)
                }
                if (group.effects.lift)
                    windows.add(
                        RenderWindow(
                            unit.text.animation.start,
                            (unit.text.animation.start.toLong() + 700)
                                .coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt(),
                        )
                    )
                if (unitEffects)
                    windows.add(
                        RenderWindow(
                            unit.animationStart.toInt(),
                            (unit.animationStart + group.animationDuration).toInt(),
                        )
                    )
                start =
                    minOf(
                        start,
                        unit.text.start,
                        if (group.effects.lift) unit.text.animation.start else unit.text.start,
                    )
                sweepEnd = maxOf(sweepEnd, unit.text.end)
                end =
                    maxOf(
                        end,
                        unit.text.end,
                        if (unitEffects) (unit.animationStart + group.animationDuration).toInt()
                        else Int.MIN_VALUE,
                        if (group.effects.lift)
                            (unit.text.animation.start.toLong() + 700)
                                .coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        else Int.MIN_VALUE,
                    )
                localX += if (groupRtl) -unit.width else unit.width
            }
            if (groupGlowEnd.isFinite()) {
                windows.add(RenderWindow(group.start, groupGlowEnd.toInt()))
                end = maxOf(end, groupGlowEnd.toInt())
            }
            group.staticPosition =
                Offset(
                    textLeft,
                    group.units.first().position.y,
                )
            group.effectsEnd =
                maxOf(
                    group.start.toFloat(),
                    groupGlowEnd,
                    group.units.maxOf {
                        maxOf(
                            if (unitEffects) it.animationStart + group.animationDuration
                            else Float.NEGATIVE_INFINITY,
                            if (group.effects.lift) it.text.animation.start + 700f
                            else Float.NEGATIVE_INFINITY,
                        )
                    },
                )
            x += if (rtl) -group.width else group.width
        }
        // Timing arrays are an index into physical geometry; hierarchy remains intact for drawing.
        val order = starts.indices.sortedBy { starts[it] }
        val mergedWindows = mutableListOf<RenderWindow>()
        for (window in windows.sortedBy { it.start }) {
            val previous = mergedWindows.lastOrNull()
            if (previous != null && window.start <= previous.end)
                mergedWindows[mergedWindows.lastIndex] =
                    RenderWindow(previous.start, maxOf(previous.end, window.end))
            else mergedWindows.add(window)
        }
        val padding = maxOf(32f * density, rowWidth * 0.15f)
        rows.add(
            PreparedRow(
                rowRuns,
                Rect(
                    left - padding,
                    top - padding,
                    left + rowWidth + padding,
                    top + phoneticHeight + baseline + descent + padding,
                ),
                rtl,
                start,
                end,
                sweepEnd,
                animate,
                mergedWindows,
                IntArray(order.size) { starts[order[it]] },
                IntArray(order.size) { ends[order[it]] },
                FloatArray(order.size) { lefts[order[it]] },
                FloatArray(order.size) { rights[order[it]] },
                top,
                phoneticHeight + baseline + descent,
                phoneticHeight,
                sweepFadeWidth,
                previousPhoneticSpacing,
                width = rowWidth,
            )
        )
        previousPhoneticSpacing = if (hasPhonetics) wrappedRowSpacing else 0f
        top += phoneticHeight + baseline + descent + previousPhoneticSpacing
        rowRuns = mutableListOf()
        rowGaps.clear()
        rowWidth = 0f
        rowProfile = null
    }
    fun appendRows(start: Int, end: Int) {
        var current = start
        while (current < end) {
            val profile = atoms[current].profile
            if (rowProfile !== profile) {
                flushRun()
                rowProfile = profile
            }
            val group = atoms[current].group
            val gap = if (current > start) atoms[current].phoneticGapBefore else 0f
            rowGroups.add(group)
            rowGaps.add(gap)
            rowWidth += gap + group.width
            current++
        }
        flushRow()
    }
    var segmentStart = 0
    for (index in atoms.indices) {
        if (index > segmentStart && atoms[index].group.units.first().text.breakBefore) {
            appendBalancedRows(
                atoms,
                segmentStart,
                index,
                layoutWidth,
                style.localeList?.firstOrNull()?.toLanguageTag(),
                ::appendRows,
            )
            segmentStart = index
        }
    }
    appendBalancedRows(
        atoms,
        segmentStart,
        atoms.size,
        layoutWidth,
        style.localeList?.firstOrNull()?.toLanguageTag(),
        ::appendRows,
    )
    val nested =
        (line as? KaraokeLine.MainKaraokeLine)?.accompanimentLines.orEmpty().map {
            prepareLine(
                it,
                profiles,
                measurer,
                normalStyle,
                accompanimentStyle,
                phoneticStyle,
                translationStyle,
                width,
                density,
                showPhonetic,
                animate,
                cache,
                sweepWidths,
                layoutWidth,
                fontScale,
            )
        }
    val mainTextStart = line.syllables.minOfOrNull { it.start } ?: line.start
    fun textStart(nestedLine: PreparedLine) =
        nestedLine.source.syllables.minOfOrNull { it.start } ?: nestedLine.source.start
    val orderedNested = nested.sortedBy { textStart(it) }
    val translationText = line.translation?.takeIf { it.isNotBlank() }
    val wrappedTranslation =
        translationText?.let {
            wrapTextWithBalancedLineBreaks(
                it,
                animatedTranslationStyle,
                layoutWidth,
                measurer,
            )
        }
    val linePhoneticText = line.phonetic?.takeIf { showPhonetic && it.isNotBlank() }
    val wrappedPhonetic =
        linePhoneticText?.let { phonetic ->
            val translationRows =
                wrappedTranslation?.split(Regex("\\r\\n|\\r|\\n")).orEmpty()
            val aligned = alignPhoneticLineBreaks(phonetic, translationRows)
            wrapTextWithBalancedLineBreaks(
                aligned,
                animatedPhoneticStyle,
                layoutWidth,
                measurer,
            )
        }
    return PreparedLine(
            line,
            runs,
            rows,
            width,
            (top - previousPhoneticSpacing).coerceAtLeast(0f),
            rightAligned,
            orderedNested.filter { textStart(it) < mainTextStart },
            orderedNested.filter { textStart(it) >= mainTextStart },
            wrappedTranslation?.let {
                measurer.measure(
                    it,
                    animatedTranslationStyle.copy(
                        textAlign = if (rightAligned) TextAlign.Right else TextAlign.Left
                    ),
                    constraints = Constraints(maxWidth = layoutWidth.toInt().coerceAtLeast(1)),
                )
            },
            wrappedPhonetic?.let {
                measurer.measure(
                    it,
                    animatedPhoneticStyle.copy(
                        textAlign = if (rightAligned) TextAlign.Right else TextAlign.Left
                    ),
                    constraints = Constraints(maxWidth = layoutWidth.toInt().coerceAtLeast(1)),
                )
            },
        )
        .also { cache[line] = it }
}

/** Add only the missing caption separation when either word is wider in pronunciation. */
private fun phoneticWordGap(
    previous: PreparedAtom,
    next: PreparedAtom,
    rtl: Boolean,
    spaceWidth: () -> Float,
): Float {
    val before = previous.group
    val after = next.group
    if (before.phoneticWidth <= 0f || after.phoneticWidth <= 0f) return 0f
    if (before.phoneticWidth <= before.textWidth && after.phoneticWidth <= after.textWidth)
        return 0f
    // Captions align to their own script's reading edge, including within a mixed-direction row.
    val trailingSlack =
        if (previous.sourceText.isRtl(fallback = rtl) == rtl) before.width - before.phoneticWidth
        else 0f
    val leadingSlack =
        if (next.sourceText.isRtl(fallback = rtl) != rtl) after.width - after.phoneticWidth
        else 0f
    return (spaceWidth() - trailingSlack - leadingSlack).coerceAtLeast(0f)
}

/** Keep line-level phonetic captions on the same rows as their translation. */
private fun alignPhoneticLineBreaks(phonetic: String, translationRows: List<String>): String {
    if (translationRows.size < 2 || phonetic.any { it == '\n' || it == '\r' }) return phonetic
    val rowCount = translationRows.size
    val weights = translationRows.map { it.count { char -> !char.isWhitespace() }.coerceAtLeast(1) }
    val totalWeight = weights.sum().toFloat()
    val words = phonetic.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return phonetic

    if (words.size >= rowCount) {
        val result = ArrayList<String>(rowCount)
        var wordStart = 0
        var accumulatedWeight = 0
        for (row in 0 until rowCount - 1) {
            accumulatedWeight += weights[row]
            val remainingRows = rowCount - row - 1
            val target = (words.size * accumulatedWeight / totalWeight).toInt()
            val wordEnd = target.coerceIn(wordStart + 1, words.size - remainingRows)
            result.add(words.subList(wordStart, wordEnd).joinToString(" "))
            wordStart = wordEnd
        }
        result.add(words.subList(wordStart, words.size).joinToString(" "))
        return result.joinToString("\n")
    }

    // Kana romanization may not contain spaces. Split it at grapheme boundaries in proportion
    // to the translation row lengths rather than letting it ignore authored translation rows.
    val boundaries = graphemeBoundaries(phonetic)
    val candidates = (1 until phonetic.length).filter { boundaries[it] }
    if (candidates.size < rowCount - 1) return phonetic
    val result = ArrayList<String>(rowCount)
    var start = 0
    var accumulatedWeight = 0
    for (row in 0 until rowCount - 1) {
        accumulatedWeight += weights[row]
        val remainingRows = rowCount - row - 1
        val target = (phonetic.length * accumulatedWeight / totalWeight).toInt()
        val firstCandidate = candidates.indexOfFirst { it > start }
        val lastCandidate = candidates.size - remainingRows
        if (firstCandidate < 0 || firstCandidate > lastCandidate) return phonetic
        val end =
            candidates
                .subList(firstCandidate, lastCandidate + 1)
                .filter { phonetic.substring(start, it).isNotBlank() }
                .minByOrNull { kotlin.math.abs(it - target) }
                ?: return phonetic
        result.add(phonetic.substring(start, end).trim())
        start = end
    }
    result.add(phonetic.substring(start).trim())
    return result.joinToString("\n")
}

private fun appendBalancedRows(
    atoms: List<PreparedAtom>,
    start: Int,
    end: Int,
    maxWidth: Float,
    localeTag: String?,
    appendRows: (Int, Int) -> Unit,
) {
    if (start >= end) return
    val text = buildString { for (index in start until end) append(atoms[index].sourceText) }
    val legalOffsets = lineBreakBoundaries(text, localeTag).toSet()
    val atomOffsets = IntArray(end - start + 1)
    for (index in start until end) {
        atomOffsets[index - start + 1] =
            atomOffsets[index - start] + atoms[index].sourceText.length
    }
    val candidates = mutableListOf<LineBreakCandidate>()
    for (offset in 1 until end - start) {
        val textOffset = atomOffsets[offset]
        val nextText = atoms[start + offset].sourceText
        var whitespaceEnd = textOffset
        while (whitespaceEnd < text.length && text[whitespaceEnd].isWhitespace()) whitespaceEnd++
        val legal =
            textOffset in legalOffsets ||
                (nextText.firstOrNull()?.isWhitespace() == true &&
                    legalOffsets.any { it in textOffset..whitespaceEnd })
        candidates.add(
            LineBreakCandidate(
                offset,
                if (legal) 0.0 else EmergencyBreakPenalty,
            )
        )
    }
    val widths = FloatArray(end - start + 1)
    for (index in start until end) {
        widths[index - start + 1] =
            widths[index - start] + atoms[index].phoneticGapBefore + atoms[index].group.width
    }
    val selected =
        balancedLineBreaks(end - start, candidates, maxWidth) { from, to ->
            // A caption gap belongs between words, never before the first word of a new row.
            widths[to] - widths[from] - atoms[start + from].phoneticGapBefore
        }
    if (selected != null) {
        var rowStart = start
        for (rowEnd in selected) {
            appendRows(rowStart, start + rowEnd)
            rowStart = start + rowEnd
        }
        return
    }

    // A single indivisible drawable can exceed the viewport. Preserve it as an overflow row,
    // while keeping ordinary rows within the available width.
    var rowStart = start
    var rowWidth = 0f
    for (index in start until end) {
        val group = atoms[index].group
        var gap = if (index > rowStart) atoms[index].phoneticGapBefore else 0f
        if (index > rowStart && rowWidth + gap + group.width > maxWidth) {
            appendRows(rowStart, index)
            rowStart = index
            rowWidth = 0f
            gap = 0f
        }
        rowWidth += gap + group.width
    }
    appendRows(rowStart, end)
}

private fun PreparedGroup.preparedSourceText(): String =
    units.joinToString("") { unit ->
        val text = unit.text.layout.layoutInput.text.text
        val range = unit.text.sourceRange
        if (range != null && range.min >= 0 && range.max <= text.length) {
            text.substring(range.min, range.max)
        } else {
            var first = -1
            var last = -1
            for (index in text.indices) {
                val bounds = unit.text.layout.getBoundingBox(index)
                if (bounds.right > unit.text.left && bounds.left < unit.text.right) {
                    if (first < 0) first = index
                    last = index
                }
            }
            if (first >= 0) text.substring(first, last + 1) else text
        }
    }
