/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 * See LICENSE for terms; existing attributions are preserved.
 */
package com.j.m3play.betterlyrics

import org.junit.Assert.*
import org.junit.Test

class TTMLTimingTest {
    @Test fun preservesSeparateSyllablesAndSpacesInsideSpans() {
        val lines = TTMLParser.parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01.000" end="00:04.000"><span begin="00:01.000" end="00:01.500">Hel</span><span begin="00:01.500" end="00:02.000">lo </span><span begin="00:02.000" end="00:04.000">world</span></p></div></body></tt>""")
        assertEquals(1, lines.size)
        assertEquals(listOf("Hel", "lo ", "world"), lines.single().words.map { it.text })
        assertEquals(listOf(1.0, 1.5, 2.0), lines.single().words.map { it.startTime })
        assertEquals("Hello world", lines.single().text)
    }

    @Test fun preservesWhitespaceBetweenTimedSpans() {
        val lines = TTMLParser.parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01.000" end="00:04.000"><span begin="00:01.000" end="00:02.000">First</span> <span begin="00:02.000" end="00:04.000">second</span></p></div></body></tt>""")
        assertEquals("First second", lines.single().words.joinToString("") { it.text })
    }

    @Test fun rejectsEntityDeclarationsBeforeParsing() {
        val document = """<!DOCTYPE tt [<!ENTITY external SYSTEM "file:///does-not-exist">]><tt><body><p begin="1s" end="2s">&external;</p></body></tt>"""
        assertTrue(TTMLParser.parseTTML(document).isEmpty())
    }
}
