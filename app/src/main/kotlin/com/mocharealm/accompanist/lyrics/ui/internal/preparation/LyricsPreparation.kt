package com.mocharealm.accompanist.lyrics.ui.internal.preparation

import com.mocharealm.accompanist.lyrics.ui.internal.text.*
import com.mocharealm.accompanist.lyrics.ui.preparation.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
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
                            listOf(KaraokeSyllable(line.content, line.start, line.end)),
                            line.translation,
                            KaraokeAlignment.Start,
                            line.start,
                            line.end,
                        ),
                        profiles,
                        measurer,
                        normalStyle,
                        accompanimentStyle,
                        phoneticStyle,
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
): PreparedLine =
    prepareLine(
        line,
        profiles,
        measurer,
        normalStyle,
        accompanimentStyle,
        phoneticStyle,
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
            textMotion = TextMotion.Animated
        )
    val animatedPhoneticStyle = phoneticStyle.copy(textMotion = TextMotion.Animated)
    val sweepFadeWidth =
        sweepWidths.getOrPut(style) {
            val fontSize = style.fontSize
            val emPixels =
                when {
                    fontSize.isSp || fontSize.isEm ->
                        fontSize.value * density * fontScale
                    else ->
                        measurer
                            .measure("Hg", style, softWrap = false)
                            .size
                            .height
                            .toFloat()
                }
            (emPixels * 2f).takeIf { it.isFinite() && it > 0f } ?: 0.001f
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
    val rows = mutableListOf<PreparedRow>()
    var rowRuns = mutableListOf<PreparedProfileRun>()
    var rowGroups = mutableListOf<PreparedGroup>()
    var rowProfile: LyricsProfile? = null
    var rowWidth = 0f
    var top = 0f
    fun flushRun() {
        if (rowGroups.isNotEmpty()) rowRuns.add(PreparedProfileRun(rowProfile!!, rowGroups))
        rowGroups = mutableListOf()
    }
    fun flushRow() {
        flushRun()
        if (rowRuns.isEmpty()) return
        var baseline = 0f
        var descent = 0f
        var phoneticHeight = 0f
        for (run in rowRuns) for (group in run.groups) for (unit in group.units) {
            baseline = maxOf(baseline, unit.text.baseline)
            descent = maxOf(descent, unit.text.height - unit.text.baseline)
            phoneticHeight = maxOf(phoneticHeight, unit.phonetic?.size?.height?.toFloat() ?: 0f)
        }
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
        for (run in rowRuns) for (group in run.groups) {
            val groupLeft = if (rtl) x - group.width else x
            group.pivot =
                Offset(groupLeft + group.width / 2f, top + phoneticHeight + baseline + descent)
            var localX = groupLeft + (group.width - group.textWidth) / 2f
            for ((index, unit) in group.units.withIndex()) {
                val unitX =
                    if (group.sharedLayout)
                        groupLeft + (group.width - group.textWidth) / 2f + unit.text.left -
                            group.shapingLeft
                    else localX
                unit.position = Offset(unitX, top + phoneticHeight + baseline - unit.text.baseline)
                unit.phoneticPosition =
                    Offset(
                        (group.width - (unit.phonetic?.size?.width ?: 0)) / 2f + groupLeft - unitX,
                        -(unit.phonetic?.size?.height?.toFloat() ?: 0f),
                    )
                unit.animationStart =
                    if (group.awesome && group.units.size > 1)
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
                if (group.awesome)
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
                        (unit.animationStart + group.animationDuration).toInt(),
                        if (group.effects.lift)
                            (unit.text.animation.start.toLong() + 700)
                                .coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        else Int.MIN_VALUE,
                    )
                localX += unit.width
            }
            group.staticPosition =
                Offset(
                    groupLeft + (group.width - group.textWidth) / 2f,
                    group.units.first().position.y,
                )
            group.effectsEnd =
                group.units.maxOf {
                    maxOf(
                        it.animationStart + group.animationDuration,
                        if (group.effects.lift) it.text.animation.start + 700f else 0f,
                    )
                }
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
            )
        )
        top += phoneticHeight + baseline + descent
        rowRuns = mutableListOf()
        rowWidth = 0f
    }
    for (run in runs) {
        flushRun()
        rowProfile = run.profile
        for (group in run.groups) {
            if (
                rowWidth > 0f &&
                    (group.units.first().text.breakBefore || rowWidth + group.width > layoutWidth)
            )
                flushRow()
            rowGroups.add(group)
            rowWidth += group.width
        }
    }
    flushRow()
    val nested =
        (line as? KaraokeLine.MainKaraokeLine)?.accompanimentLines.orEmpty().map {
            prepareLine(
                it,
                profiles,
                measurer,
                normalStyle,
                accompanimentStyle,
                phoneticStyle,
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
    return PreparedLine(
            line,
            runs,
            rows,
            width,
            top,
            rightAligned,
            orderedNested.filter { textStart(it) < mainTextStart },
            orderedNested.filter { textStart(it) >= mainTextStart },
            line.translation
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    measurer.measure(
                        it,
                        animatedPhoneticStyle.copy(
                            textAlign = if (rightAligned) TextAlign.Right else TextAlign.Left
                        ),
                        constraints = Constraints(maxWidth = layoutWidth.toInt().coerceAtLeast(1)),
                    )
                },
            if (showPhonetic)
                line.phonetic
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        measurer.measure(
                            it,
                            animatedPhoneticStyle.copy(
                                textAlign = if (rightAligned) TextAlign.Right else TextAlign.Left
                            ),
                            constraints =
                                Constraints(maxWidth = layoutWidth.toInt().coerceAtLeast(1)),
                        )
                    }
            else null,
        )
        .also { cache[line] = it }
}
