/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 * See LICENSE for terms; existing attributions are preserved.
 */
package com.j.m3play.lrclib

import com.j.m3play.lrclib.models.Track
import com.j.m3play.lrclib.models.bestMatchingFor
import org.junit.Assert.*
import org.junit.Test

class TrackMatchingTest {
    private val track = Track(1, "Example", "Test Artist", 180.0, "Example text", null)

    @Test fun handlesUnknownDurationAndPlainLyrics() {
        assertEquals(track, listOf(track).bestMatchingFor(0, "Example", "Test Artist"))
        assertEquals(track, listOf(track).bestMatchingFor(-1, "Example", "Test Artist"))
    }

    @Test fun rejectsOtherRecordingLengths() {
        assertEquals(track, listOf(track).bestMatchingFor(181))
        assertNull(listOf(track).bestMatchingFor(200))
    }
}
