package com.mocharealm.accompanist.lyrics.ui.profile

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

data class ProfileTiming(
    val start: Int,
    val end: Int,
    val left: Float,
    val right: Float,
    val sourceRange: TextRange? = null,
)

/** A drawable slice of a shaped layout, with source timing retained. */
data class ProfileTextUnit(
    val layout: TextLayoutResult,
    val left: Float,
    val right: Float,
    val start: Int,
    val end: Int,
    val phonetic: String? = null,
    val timing: List<ProfileTiming> = listOf(ProfileTiming(start, end, 0f, right - left)),
    val sourceStart: Int = start,
    val breakBefore: Boolean = false,
    val animation: ProfileAnimationUnit = ProfileAnimationUnit(sourceStart, end),
    /** UTF-16 range in [layout], used only during preparation; never a clipping rectangle. */
    val sourceRange: TextRange? = null,
) {
    val width = right - left
    val height = layout.size.height.toFloat()
    val baseline = layout.firstBaseline
}
