package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.ui.graphics.*
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow

/** Mutable draw cursor and reusable paints belong to one rendering host. */
internal class RowRenderState(private val row: PreparedRow) {
    internal var sweepCursor = 0
    internal var previousTime = Int.MIN_VALUE
    internal val layerPaint = Paint()
    internal val mask: Brush =
        Brush.horizontalGradient(
            if (row.rtl) listOf(Color.White.copy(alpha = 0.2f), Color.White)
            else listOf(Color.White, Color.White.copy(alpha = 0.2f)),
            startX = -row.sweepFadeWidth / 2f,
            endX = row.sweepFadeWidth / 2f,
        )

    internal fun sweepCenter(now: Int): Float = with(row) {
        if (sweepStarts.isEmpty()) return bounds.left
        if (now < previousTime || now.toLong() - previousTime > 1000L) {
            var low = 0
            var high = sweepStarts.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (sweepStarts[middle] <= now) low = middle + 1 else high = middle
            }
            sweepCursor = (low - 1).coerceAtLeast(0)
        }
        while (
            sweepCursor < sweepStarts.lastIndex && now >= sweepStarts[sweepCursor + 1]
        ) sweepCursor++
        previousTime = now
        val i = sweepCursor
        val p =
            if (sweepEnds[i] <= sweepStarts[i]) {
                if (now >= sweepStarts[i]) 1f else 0f
            } else
                ((now.toLong() - sweepStarts[i]).toFloat() /
                        (sweepEnds[i].toLong() - sweepStarts[i]).toFloat())
                    .coerceIn(0f, 1f)
        val halfFade = sweepFadeWidth / 2f
        // The first/last timing units own the extra half-fade travel, within their
        // original durations. A single unit owns both extensions. RTL mirrors geometry,
        // not time: its center moves from the right side towards the left.
        val direction = if (rtl) -1f else 1f
        val from = (if (rtl) sweepRight[i] else sweepLeft[i]) -
            if (i == 0) direction * halfFade else 0f
        val to = (if (rtl) sweepLeft[i] else sweepRight[i]) +
            if (i == sweepStarts.lastIndex) direction * halfFade else 0f
        return from + (to - from) * p
    }
}
