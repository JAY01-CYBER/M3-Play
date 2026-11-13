/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 * See LICENSE for terms; existing attributions are preserved.
 */
package com.music.youlyplus

import com.music.youlyplus.models.LyricsItem
import com.music.youlyplus.models.Syllable
import org.junit.Assert.assertEquals
import org.junit.Test

class TimingTest {
    @Test fun preservesSyllableWhitespaceAndFinalEndTime() {
        val lines = listOf(LyricsItem(time = 1000, text = "Hello world", syllabus = listOf(
            Syllable("Hel", 1000, 500), Syllable("lo ", 1500, 500), Syllable("world", 2000, 1000),
        )))
        val result = with(YouLyPlus) { lines.convertToLrc() }
        assertEquals("[00:01.000]<00:01.000>Hel<00:01.500>lo <00:02.000>world<00:03.000>", result)
    }
}
