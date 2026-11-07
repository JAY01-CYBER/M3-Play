/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.j.m3play.lyrics

import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsQualityEvaluatorTest {
    @Test
    fun realWordTimingRanksAbovePlainText() {
        val wordTimed = """
            [00:01.000]<00:01.000>Hello <00:01.500>there
            [00:03.000]<00:03.000>How <00:03.400>are <00:03.700>you
            [00:05.000]<00:05.000>Doing <00:05.500>today
        """.trimIndent()
        val plain = """
            Hello there
            How are you
            Doing today
            I am singing
            And the song goes on
        """.trimIndent()

        val timedScore = LyricsQualityEvaluator.assess(wordTimed, 180)
        val plainScore = LyricsQualityEvaluator.assess(plain, 180)

        assertTrue(timedScore.isWordSynced)
        assertTrue(timedScore.score > plainScore.score)
    }

    @Test
    fun rejectsMalformedTtmlInsteadOfTreatingMarkupAsLyrics() {
        val result = LyricsQualityEvaluator.assess("<tt><broken", 180)
        assertTrue(result.score < 0)
    }

    @Test
    fun penalizesTimingThatExtendsFarPastTrackDuration() {
        val lyrics = """
            [03:00.000]Last line
            [04:30.000]Wrong recording tail
            [05:00.000]Way too late
        """.trimIndent()

        val result = LyricsQualityEvaluator.assess(lyrics, 180)
        assertTrue(result.score < 50)
    }
}
