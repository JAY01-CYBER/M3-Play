/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 * See LICENSE for terms; existing attributions are preserved.
 */
package com.j.m3play.simpmusic

import com.j.m3play.simpmusic.models.SimpMusicApiResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun acceptsDocumentedResponseAndFieldNames() {
        val response = json.decodeFromString<SimpMusicApiResponse>("""{"success":true,"data":[{"videoId":"abcdefghijk","songTitle":"Example","artistName":"Test","durationSeconds":180,"plainLyric":"Example text","syncedLyrics":"[00:01.00]Example text"}]}""")
        assertTrue(response.isSuccessful)
        assertEquals(180, response.data.single().duration)
        assertEquals("[00:01.00]Example text", response.data.single().syncedLyrics)
    }

    @Test fun retainsLegacyCompatibilityAndHonorsExplicitFailure() {
        assertTrue(json.decodeFromString<SimpMusicApiResponse>("""{"type":"success","data":[]}""").isSuccessful)
        assertFalse(json.decodeFromString<SimpMusicApiResponse>("""{"success":false,"type":"success","data":[]}""").isSuccessful)
        assertFalse(json.decodeFromString<SimpMusicApiResponse>("""{"data":[]}""").isSuccessful)
    }
}
