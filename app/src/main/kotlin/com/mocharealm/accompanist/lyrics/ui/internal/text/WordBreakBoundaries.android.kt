package com.mocharealm.accompanist.lyrics.ui.internal.text

import android.os.Build
import java.text.BreakIterator as JavaBreakIterator
import java.util.Locale

internal fun platformWordBreakBoundaries(text: String, localeTag: String?): IntArray {
    val locale = localeTag?.let(Locale::forLanguageTag) ?: Locale.ROOT
    val boundaries = ArrayList<Int>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val iterator = android.icu.text.BreakIterator.getWordInstance(locale)
        iterator.setText(text)
        var boundary = iterator.first()
        while (boundary != android.icu.text.BreakIterator.DONE) {
            boundaries.add(boundary)
            boundary = iterator.next()
        }
    } else {
        val iterator = JavaBreakIterator.getWordInstance(locale)
        iterator.setText(text)
        var boundary = iterator.first()
        while (boundary != JavaBreakIterator.DONE) {
            boundaries.add(boundary)
            boundary = iterator.next()
        }
    }
    return boundaries.toIntArray()
}
