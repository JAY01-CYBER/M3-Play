/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.betterlyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UnisonTest {
    @Test fun missingVideoFallsBackToEncodedMetadata() = runBlocking {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            if (calls == 1) {
                assertEquals("abcdefghijk", request.url.parameters["v"])
                respond("", HttpStatusCode.NotFound)
            } else {
                assertEquals("Test & song", request.url.parameters["song"])
                assertEquals("An artist", request.url.parameters["artist"])
                assertEquals("120", request.url.parameters["duration"])
                respond("""{"success":true,"data":{"lyrics":"[00:01.00]Test line","format":"lrc","score":5}}""")
            }
        }
        HttpClient(engine).use { http ->
            assertEquals("[00:01.00]Test line", UnisonClient(http).getLyrics("abcdefghijk", "Test & song", "An artist", null, 120))
        }
        assertEquals(2, calls)
    }

    @Test fun rateLimitDoesNotTriggerMetadataRetry() = runBlocking {
        var calls = 0
        HttpClient(MockEngine { calls++; respond("", HttpStatusCode.TooManyRequests) }).use { http ->
            val result = runCatching { UnisonClient(http).getLyrics("abcdefghijk", "Test", "Artist", null, 0) }
            assertTrue(result.isFailure)
        }
        assertEquals(1, calls)
    }

    @Test fun unsuccessfulOrUnsupportedEnvelopesAreRejected() {
        HttpClient(MockEngine { respond("") }).use { http ->
            val client = UnisonClient(http)
            assertTrue(runCatching { client.decode("""{"success":false,"data":null}""") }.isFailure)
            assertTrue(runCatching { client.decode("""{"success":true,"data":{"lyrics":"x","format":"html"}}""") }.isFailure)
        }
    }
}
