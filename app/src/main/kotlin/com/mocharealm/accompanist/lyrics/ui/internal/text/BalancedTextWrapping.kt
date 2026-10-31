package com.mocharealm.accompanist.lyrics.ui.internal.text

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

internal const val EmergencyBreakPenalty = 1_000_000_000.0

/** Insert explicit, balanced breaks at Unicode line opportunities before Compose lays out text. */
internal fun wrapTextWithBalancedLineBreaks(
    text: String,
    style: TextStyle,
    maxWidth: Float,
    measurer: TextMeasurer,
): String {
    if (text.isEmpty() || maxWidth <= 0f) return text
    val localeTag = style.localeList?.firstOrNull()?.toLanguageTag()
    val result = StringBuilder(text.length)
    var paragraphStart = 0
    var index = 0
    while (index < text.length) {
        if (text[index] != '\n' && text[index] != '\r') {
            index++
            continue
        }
        appendWrappedParagraph(result, text.substring(paragraphStart, index), style, maxWidth, localeTag, measurer)
        val newlineEnd = if (text[index] == '\r' && text.getOrNull(index + 1) == '\n') index + 2 else index + 1
        result.append(text, index, newlineEnd)
        index = newlineEnd
        paragraphStart = newlineEnd
    }
    appendWrappedParagraph(result, text.substring(paragraphStart), style, maxWidth, localeTag, measurer)
    return result.toString()
}

private fun appendWrappedParagraph(
    output: StringBuilder,
    text: String,
    style: TextStyle,
    maxWidth: Float,
    localeTag: String?,
    measurer: TextMeasurer,
) {
    if (text.isEmpty()) return
    val breaks = balancedTextLineBreaks(text, style, maxWidth, localeTag, measurer)
    if (breaks == null) {
        output.append(text)
        return
    }
    var start = 0
    for (end in breaks) {
        output.append(text, start, end)
        if (end < text.length) output.append('\n')
        start = end
    }
}

/** Returns balanced line ends for one paragraph, using legal breaks or grapheme fallbacks. */
internal fun balancedTextLineBreaks(
    text: String,
    style: TextStyle,
    maxWidth: Float,
    localeTag: String?,
    measurer: TextMeasurer,
): IntArray? {
    if (text.isEmpty()) return intArrayOf(0)
    val legal = lineBreakBoundaries(text, localeTag).toSet()
    val graphemes = graphemeBoundaries(text)
    val layout = measurer.measure(text, style, softWrap = false)
    val widthPrefix = FloatArray(text.length + 1)
    var clusterStart = 0
    while (clusterStart < text.length) {
        var clusterEnd = clusterStart + 1
        while (clusterEnd < text.length && !graphemes[clusterEnd]) clusterEnd++
        var left = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        for (offset in clusterStart until clusterEnd) {
            val bounds = layout.getBoundingBox(offset)
            left = minOf(left, bounds.left)
            right = maxOf(right, bounds.right)
        }
        val advance = (right - left).takeIf { it.isFinite() && it > 0f } ?: 0f
        widthPrefix[clusterStart + 1] = widthPrefix[clusterStart] + advance
        for (offset in clusterStart + 1 until clusterEnd) {
            widthPrefix[offset + 1] = widthPrefix[clusterStart + 1]
        }
        clusterStart = clusterEnd
    }
    val candidates = buildList {
        for (offset in 1 until text.length) {
            if (!graphemes[offset]) continue
            add(LineBreakCandidate(offset, if (offset in legal) 0.0 else EmergencyBreakPenalty))
        }
    }
    return balancedLineBreaks(text.length, candidates, maxWidth) { start, end ->
            var visibleEnd = end
            while (visibleEnd > start && text[visibleEnd - 1].isWhitespace()) visibleEnd--
            if (start == 0 && visibleEnd == text.length) layout.size.width.toFloat()
            else widthPrefix[visibleEnd] - widthPrefix[start]
        }
}
