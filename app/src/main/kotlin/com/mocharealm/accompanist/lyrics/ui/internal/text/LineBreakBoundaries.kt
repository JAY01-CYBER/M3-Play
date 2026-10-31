package com.mocharealm.accompanist.lyrics.ui.internal.text

/** Locale-aware legal line breaks supplied by the current platform's Unicode engine. */

internal fun lineBreakBoundaries(text: String, localeTag: String?): IntArray {
    if (text.isEmpty()) return intArrayOf(0)
    return (platformLineBreakBoundaries(text, localeTag).asSequence() + sequenceOf(0, text.length))
        .filter { boundary ->
            boundary in 0..text.length &&
                !(
                    boundary > 0 && boundary < text.length &&
                        text[boundary - 1].isHighSurrogate() && text[boundary].isLowSurrogate()
                )
        }
        .distinct()
        .sorted()
        .toList()
        .toIntArray()
}

internal data class LineBreakCandidate(val offset: Int, val penalty: Double = 0.0)

/** Selects visually balanced rows from platform-provided legal boundaries. */
internal fun balancedLineBreaks(
    textLength: Int,
    candidates: List<LineBreakCandidate>,
    maxWidth: Float,
    width: (start: Int, end: Int) -> Float,
): IntArray? {
    if (textLength <= 0) return intArrayOf(0)
    val byOffset = mutableMapOf<Int, Double>()
    byOffset[0] = 0.0
    byOffset[textLength] = 0.0
    for (candidate in candidates) {
        if (candidate.offset in 1 until textLength) {
            byOffset[candidate.offset] =
                minOf(byOffset[candidate.offset] ?: Double.POSITIVE_INFINITY, candidate.penalty)
        }
    }
    val offsets = byOffset.keys.sorted()
    val costs = DoubleArray(offsets.size) { Double.POSITIVE_INFINITY }
    val previous = IntArray(offsets.size) { -1 }
    costs[0] = 0.0
    for (endIndex in 1 until offsets.size) {
        val end = offsets[endIndex]
        for (startIndex in endIndex - 1 downTo 0) {
            if (!costs[startIndex].isFinite()) continue
            val start = offsets[startIndex]
            val measuredWidth = width(start, end)
            if (measuredWidth > maxWidth && startIndex != endIndex - 1) break
            if (measuredWidth > maxWidth) continue
            val slack = (maxWidth - measuredWidth).coerceAtLeast(0f).toDouble()
            val cost = costs[startIndex] + slack * slack + (byOffset[end] ?: 0.0)
            if (cost < costs[endIndex]) {
                costs[endIndex] = cost
                previous[endIndex] = startIndex
            }
        }
    }
    if (!costs.last().isFinite()) return null
    val result = mutableListOf<Int>()
    var index = offsets.lastIndex
    while (index > 0) {
        result.add(offsets[index])
        index = previous[index]
        if (index < 0) return null
    }
    result.reverse()
    return result.toIntArray()
}
