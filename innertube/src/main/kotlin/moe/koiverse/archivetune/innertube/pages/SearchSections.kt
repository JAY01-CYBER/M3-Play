/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.innertube.pages

import com.j.m3play.innertube.models.SectionListRenderer
import com.j.m3play.innertube.models.response.SearchResponse

/** Search shelves can be nested in item sections, or follow an empty navigation tab. */
internal fun SearchResponse.searchSections(): List<SectionListRenderer.Content> {
    val roots = contents?.tabbedSearchResultsRenderer?.tabs.orEmpty()
        .flatMap { it.tabRenderer.content?.sectionListRenderer?.contents.orEmpty() } +
        contents?.sectionListRenderer?.contents.orEmpty()
    return roots.flatMap { section ->
        listOf(section) + section.itemSectionRenderer?.contents.orEmpty().map { child ->
            SectionListRenderer.Content(
                musicCarouselShelfRenderer = null,
                musicShelfRenderer = child.musicShelfRenderer,
                musicCardShelfRenderer = null,
                musicPlaylistShelfRenderer = null,
                musicDescriptionShelfRenderer = null,
                musicResponsiveHeaderRenderer = null,
                musicEditablePlaylistDetailHeaderRenderer = null,
                gridRenderer = child.gridRenderer,
                itemSectionRenderer = null,
            )
        }
    }
}
