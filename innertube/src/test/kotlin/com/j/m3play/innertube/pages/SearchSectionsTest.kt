/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.innertube.pages

import com.j.m3play.innertube.models.response.SearchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SearchSectionsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun includesNestedShelvesAfterEmptyFirstTab() {
        val response = json.decodeFromString<SearchResponse>("""{
          "contents":{"tabbedSearchResultsRenderer":{"tabs":[
            {"tabRenderer":{"title":"Navigation"}},
            {"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
              {"itemSectionRenderer":{"contents":[{"musicShelfRenderer":{"title":{"runs":[{"text":"Albums"}]},"contents":[]}}]}},
              {"gridRenderer":{"items":[]}}
            ]}}}}
          ]}}
        }""")
        val sections = response.searchSections()
        assertEquals(1, sections.count { it.musicShelfRenderer != null })
        assertEquals(1, sections.count { it.gridRenderer != null })
    }

    @Test fun handlesDirectSectionsAndEmptyResponse() {
        val response = json.decodeFromString<SearchResponse>("""{"contents":{"sectionListRenderer":{"contents":[{"gridRenderer":{"items":[]}}]}}}""")
        assertEquals(1, response.searchSections().size)
        assertTrue(json.decodeFromString<SearchResponse>("{}").searchSections().isEmpty())
    }
}
