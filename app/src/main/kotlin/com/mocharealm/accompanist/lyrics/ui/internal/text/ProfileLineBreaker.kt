package com.mocharealm.accompanist.lyrics.ui.internal.text

import com.mocharealm.accompanist.lyrics.ui.profile.*

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/**
 * The paragraph engine only selects safe breaks. Every resulting fragment is shaped as a standalone
 * drawable, exactly like an ordinary unit; paragraph offsets never enter row layout.
 */
internal fun breakShapedUnit(
    unit: ProfileTextUnit,
    measurer: TextMeasurer,
    maxWidth: Float,
): List<ProfileTextUnit> {
    val input = unit.layout.layoutInput
    val wholeLayout = unit.left == 0f && unit.right == unit.layout.size.width.toFloat()
    var sourceOffset = 0
    var sourceEnd = input.text.length
    if (unit.sourceRange != null) {
        sourceOffset = unit.sourceRange.min
        sourceEnd = unit.sourceRange.max
    } else if (!wholeLayout) {
        // Compatibility for arbitrary profile slices. Keep only endpoints, not a boxed index list.
        var first = -1
        var last = -1
        for (index in input.text.text.indices) {
            val bounds = unit.layout.getBoundingBox(index)
            if (bounds.right > unit.left && bounds.left < unit.right) {
                if (first < 0) first = index
                last = index
            }
        }
        if (first < 0) return listOf(unit)
        sourceOffset = first
        sourceEnd = last + 1
    }
    if (sourceEnd == 0) return listOf(unit)
    if (
        sourceEnd < input.text.length &&
            input.text.text[sourceEnd - 1].isHighSurrogate() &&
            input.text.text[sourceEnd].isLowSurrogate()
    )
        sourceEnd++
    val text = input.text.subSequence(sourceOffset, sourceEnd)
    val breaks =
        measurer.measure(
            text,
            input.style,
            softWrap = true,
            constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
        )
    val result = mutableListOf<ProfileTextUnit>()
    val indexedTiming =
        unit.timing.isNotEmpty() &&
            unit.timing.all { it.sourceRange != null } &&
            unit.timing.zipWithNext().all { (a, b) -> a.sourceRange!!.max <= b.sourceRange!!.min }
    fun timingAt(offset: Int, left: Float, right: Float): ProfileTiming? {
        if (indexedTiming) {
            var low = 0
            var high = unit.timing.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (unit.timing[middle].sourceRange!!.max <= offset) low = middle + 1
                else high = middle
            }
            return unit.timing.getOrNull(low)?.takeIf { offset >= it.sourceRange!!.min }
        }
        return unit.timing.firstOrNull {
            it.sourceRange?.let { range -> offset in range.min until range.max }
                ?: (right > unit.left + it.left && left < unit.left + it.right)
        }
    }
    fun append(begin: Int, end: Int) {
        var drawableEnd = end
        while (drawableEnd > begin && text.text[drawableEnd - 1] in "\r\n") drawableEnd--
        val fragment = text.subSequence(begin, drawableEnd)
        val layout = measurer.measure(fragment, input.style, softWrap = false)
        // Joining forms can change width when a paragraph break becomes a standalone run.
        // Ask the same platform breaker for a smaller safe boundary; never split UTF-16 manually.
        if (layout.size.width > maxWidth && end - begin > 1) {
            var limit = (maxWidth - (layout.size.width - maxWidth) - 1f).toInt().coerceAtLeast(1)
            while (true) {
                val retry =
                    measurer.measure(
                        fragment,
                        input.style,
                        softWrap = true,
                        constraints = Constraints(maxWidth = limit),
                    )
                val split = retry.getLineEnd(0)
                if (split in 1 until fragment.length) {
                    append(begin, begin + split)
                    append(begin + split, end)
                    return
                }
                if (limit == 1) break // A single indivisible cluster cannot be wrapped further.
                limit = (limit / 2).coerceAtLeast(1)
            }
        }
        val left = 0f
        val right = layout.size.width.toFloat()
        val timings = mutableListOf<ProfileTiming>()
        for (i in begin until drawableEnd) {
            val old = unit.layout.getBoundingBox(sourceOffset + i)
            val bounds = layout.getBoundingBox(i - begin)
            val sourceTiming = timingAt(sourceOffset + i, old.left, old.right) ?: continue
            val span = (sourceTiming.right - sourceTiming.left).coerceAtLeast(0.001f)
            val rtl =
                unit.layout.getBidiRunDirection(sourceOffset + i) ==
                    androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
            val from =
                if (rtl) unit.left + sourceTiming.right - old.right
                else old.left - unit.left - sourceTiming.left
            val to =
                if (rtl) unit.left + sourceTiming.right - old.left
                else old.right - unit.left - sourceTiming.left
            val duration = sourceTiming.end.toLong() - sourceTiming.start
            val range = sourceTiming.sourceRange
            val timingStart =
                if (range != null && sourceOffset + i == range.min) sourceTiming.start
                else sourceTiming.start + (duration * (from / span).coerceIn(0f, 1f)).toInt()
            val timingEnd =
                if (range != null && sourceOffset + i + 1 == range.max) sourceTiming.end
                else sourceTiming.start + (duration * (to / span).coerceIn(0f, 1f)).toInt()
            timings.add(
                ProfileTiming(
                    timingStart,
                    timingEnd,
                    bounds.left - left,
                    bounds.right - left,
                    androidx.compose.ui.text.TextRange(i - begin, i - begin + 1),
                )
            )
        }
        result.add(
            unit.copy(
                layout = layout,
                left = left,
                right = right,
                start = timings.minOfOrNull { it.start } ?: unit.start,
                end = timings.maxOfOrNull { it.end } ?: unit.end,
                timing = timings,
                phonetic = if (result.isEmpty()) unit.phonetic else null,
                breakBefore = true,
                sourceRange = androidx.compose.ui.text.TextRange(0, fragment.length),
            )
        )
    }
    for (line in 0 until breaks.lineCount) append(
        breaks.getLineStart(line),
        breaks.getLineEnd(line),
    )
    return result
}
