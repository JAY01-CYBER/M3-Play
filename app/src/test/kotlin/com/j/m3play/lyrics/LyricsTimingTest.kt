/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTimingTest {
    private val lines = listOf(LyricsEntry(0, ""), LyricsEntry(1000, "First"), LyricsEntry(2000, "Second"))
    @Test fun seekingToExactTimestampSelectsNewLine() {
        assertEquals(0, LyricsUtils.findCurrentLineIndex(lines, 999, 0))
        assertEquals(1, LyricsUtils.findCurrentLineIndex(lines, 1000, 0))
        assertEquals(2, LyricsUtils.findCurrentLineIndex(lines, 2000, 0))
    }
    @Test fun appliesLeadOnceAndClampsSongBoundaries() {
        assertEquals(1, LyricsUtils.findCurrentLineIndex(lines, 700, 300))
        assertEquals(2, LyricsUtils.findCurrentLineIndex(lines, 100_000, 0))
        assertEquals(-1, LyricsUtils.findCurrentLineIndex(emptyList(), 0, 0))
    }
    @Test fun enhancedLrcPreservesSecondsWhitespaceAndExplicitEndMarker() {
        val line = LyricsUtils.parseLyrics(
            "[00:01.000]<00:01.000>Hel<00:01.500>lo <00:02.000>world<00:03.000>"
        ).single()
        val words = requireNotNull(line.words)
        assertEquals(1000L, line.time)
        assertEquals("Hello world", line.text)
        assertEquals(listOf("Hel", "lo ", "world"), words.map { it.text })
        assertEquals(listOf(1.0, 1.5, 2.0), words.map { it.startTime })
        assertEquals(listOf(1.5, 2.0, 3.0), words.map { it.endTime })
    }
}
