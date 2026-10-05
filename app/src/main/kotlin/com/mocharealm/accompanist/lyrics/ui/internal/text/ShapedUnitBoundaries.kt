package com.mocharealm.accompanist.lyrics.ui.internal.text

import com.mocharealm.accompanist.lyrics.ui.profile.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.floor

internal expect fun graphemeBoundaries(text: String): BooleanArray

/**
 * Advance boxes are not ink bounds. Before allowing independent transforms, keep a grapheme
 * together and require an empty raster column at the proposed cut. This deliberately merges
 * ambiguous boundaries instead of reshaping isolated characters and losing joining/kerning. It uses
 * the same raster scale as the text atlas and runs only during preparation.
 */
internal fun protectShapedUnits(units: List<ProfileTextUnit>): List<ProfileTextUnit> {
    if (units.size < 2) return units
    val result = ArrayList<ProfileTextUnit>(units.size)
    var begin = 0
    while (begin < units.size) {
        val layout = units[begin].layout
        var end = begin + 1
        while (end < units.size && units[end].layout === layout) end++
        if (end == begin + 1) {
            result.add(units[begin])
        } else {
            val boundaries = graphemeBoundaries(layout.layoutInput.text.text)
            val cuts =
                FloatArray(end - begin - 1) { index ->
                    val a = units[begin + index]
                    val b = units[begin + index + 1]
                    when {
                        a.sourceRange == null || b.sourceRange == null -> Float.NaN
                        a.sourceRange.max != b.sourceRange.min || !boundaries[a.sourceRange.max] ->
                            Float.NaN
                        a.width <= 0f || b.width <= 0f -> Float.NaN
                        a.right <= b.left -> a.right
                        b.right <= a.left -> a.left
                        else -> Float.NaN
                    }
                }
            val clear = clearInkCuts(layout, cuts)
            var first = begin
            for (index in begin until end - 1) {
                val a = units[index].sourceRange
                val b = units[index + 1].sourceRange
                val safeSource = a != null && b != null && a.max == b.min && boundaries[a.max]
                if (safeSource && clear[index - begin]) {
                    result.add(mergeShapedUnits(units, first, index + 1))
                    first = index + 1
                }
            }
            result.add(mergeShapedUnits(units, first, end))
        }
        begin = end
    }
    return result
}

private fun mergeShapedUnits(units: List<ProfileTextUnit>, begin: Int, end: Int): ProfileTextUnit {
    if (end == begin + 1) return units[begin]
    val first = units[begin]
    var left = first.left
    var right = first.right
    var start = first.start
    var finish = first.end
    var animationStart = first.animation.start
    var animationEnd = first.animation.end
    for (index in begin + 1 until end) {
        val unit = units[index]
        left = minOf(left, unit.left)
        right = maxOf(right, unit.right)
        start = minOf(start, unit.start)
        finish = maxOf(finish, unit.end)
        animationStart = minOf(animationStart, unit.animation.start)
        animationEnd = maxOf(animationEnd, unit.animation.end)
    }
    val timing = ArrayList<ProfileTiming>()
    val phonetics = ArrayList<String>()
    for (index in begin until end) {
        val unit = units[index]
        for (part in unit.timing) timing.add(
            part.copy(left = part.left + unit.left - left, right = part.right + unit.left - left)
        )
        unit.phonetic?.let(phonetics::add)
    }
    return first.copy(
        left = left,
        right = right,
        start = start,
        end = finish,
        timing = timing,
        phonetic = phonetics.joinToString(" ").ifEmpty { null },
        animation = ProfileAnimationUnit(animationStart, animationEnd),
        sourceRange =
            first.sourceRange?.let { range ->
                units[end - 1].sourceRange?.let { TextRange(range.min, it.max) }
            },
    )
}

private fun clearInkCuts(layout: TextLayoutResult, cuts: FloatArray): BooleanArray {
    val clear = BooleanArray(cuts.size) { cuts[it].isFinite() }
    if (clear.none { it }) return clear
    // Bound scratch memory even for an exceptionally long, unwrapped source syllable.
    val tileWidth = minOf(2048, layout.size.width.coerceAtLeast(1))
    val padding = layout.size.height.coerceAtLeast(1)
    val height = layout.size.height + padding * 2
    val pixels = IntArray(tileWidth * height)
    val tiles = mutableMapOf<Int, MutableList<Int>>()
    for (index in cuts.indices) {
        if (!clear[index]) continue
        val cut = cuts[index]
        if (cut <= 0f || cut >= layout.size.width) {
            clear[index] = false
            continue
        }
        tiles.getOrPut(cut.toInt() / tileWidth) { mutableListOf() }.add(index)
    }
    for ((tile, indices) in tiles) {
        val origin = tile * tileWidth
        val bitmap = ImageBitmap(tileWidth, height)
        CanvasDrawScope().draw(
            layout.layoutInput.density,
            LayoutDirection.Ltr,
            Canvas(bitmap),
            Size(tileWidth.toFloat(), height.toFloat()),
        ) {
            drawText(layout, Color.White, topLeft = Offset(-origin.toFloat(), padding.toFloat()))
        }
        bitmap.readPixels(pixels)
        for (index in indices) {
            val cut = cuts[index]
            val column = floor(cut).toInt() - origin
            // Include both pixels touched by a fractional clip, plus one pixel of guard.
            for (y in 0 until height) {
                for (x in maxOf(0, column - 1)..minOf(tileWidth - 1, column + 1)) {
                    if (pixels[y * tileWidth + x] ushr 24 != 0) clear[index] = false
                }
                if (!clear[index]) break
            }
        }
    }
    return clear
}
