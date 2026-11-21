/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieParsingTest {

    @Test
    fun parseCookieString_handlesWebViewCookieWithoutSpaces() {
        val input = "SID=1;HSID=2;SAPISID=abc123;SSID=4"

        val cookies = parseCookieString(input)

        assertEquals(4, cookies.size)
        assertTrue(cookies.containsKey("SAPISID"))
        assertEquals("abc123", cookies["SAPISID"])
    }

    @Test
    fun parseCookieString_trimsPartsAndIgnoresMalformed() {
        val input = "  SID=1 ;  ; invalid ; SAPISID = token  ; =bad ; SSID=4  "

        val cookies = parseCookieString(input)

        assertEquals("1", cookies["SID"])
        assertEquals("token", cookies["SAPISID"])
        assertEquals("4", cookies["SSID"])
        assertEquals(3, cookies.size)
    }
}
