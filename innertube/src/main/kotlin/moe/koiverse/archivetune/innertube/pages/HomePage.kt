/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V2 (Fixed)
 */

package com.j.m3play.innertube.pages

import com.j.m3play.innertube.models.Album
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.Artist
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.BrowseEndpoint
import com.j.m3play.innertube.models.MusicCarouselShelfRenderer
import com.j.m3play.innertube.models.MusicResponsiveListItemRenderer 
import com.j.m3play.innertube.models.MusicTwoRowItemRenderer
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SectionListRenderer
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.models.oddElements
import com.j.m3play.innertube.models.filterExplicit
import java.util.UUID

// Helper function for high res images
private fun String?.getHighResThumbnailUrl(): String? {
    if (this == null) return null
    return this.replace(Regex("=w\\d+-h\\d+([a-zA-Z0-9\\-]+)?"), "=w544-h544-l90-rj")
}

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title = renderer.chipCloudChipRenderer.text?.runs?.firstOrNull()?.text ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                )
            }
        }
    }

    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                return Section(
                    title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return null,
                    label = renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl(),
                    endpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    
                    items = renderer.contents.mapNotNull { content ->
                        content.musicTwoRowItemRenderer?.let { fromMusicTwoRowItemRenderer(it) }
                            ?: content.musicResponsiveListItemRenderer?.let { fromMusicResponsiveListItemRenderer(it) }
                    }.ifEmpty {
                        return null
                    }
                )
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
                val videoId = renderer.playlistItemData?.videoId
                    ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                    ?: return null
                    
                val title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null

                return SongItem(
                    id = videoId,
                    title = title,
                    artists = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.oddElements()?.mapNotNull {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    } ?: emptyList(),
                    album = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId ?: ""
                        )
                    },
                    duration = null, // 🔧 FIXED: Reverted to null mapping to match Int? expectation
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl() ?: "",
                    explicit = renderer.badges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true,
                    endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                    syncSessionId = "${videoId}_${UUID.randomUUID().toString().take(8)}" 
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs ?: emptyList()
                        val (artistRuns, albumRuns) = subtitleRuns.partition { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true
                        }
                        
                        val artists = artistRuns.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }
                        
                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            album = albumRuns.firstOrNull { run ->
                                run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                            }?.let { run ->
                                Album(
                                    name = run.text,
                                    id = run.navigationEndpoint?.browseEndpoint?.browseId ?: ""
                                )
                            },
                            duration = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl() ?: "",
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            syncSessionId = "${renderer.navigationEndpoint.watchEndpoint.videoId}_${UUID.randomUUID().toString().take(8)}"
                        )
                    }
                    renderer.isAlbum -> {
                        // 🔧 FIXED: Safely parsing string to Int? to avoid breaking other files
                        val yearInt = renderer.subtitle?.runs?.lastOrNull()?.text?.takeIf { it.matches(Regex("\\d{4}")) }?.toIntOrNull()

                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.mapNotNull {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            } ?: emptyList(),
                            year = yearInt, 
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl() ?: "",
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        val songCount = renderer.subtitle?.runs?.firstOrNull { it.text.contains("song", ignoreCase = true) }?.text

                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.firstOrNull()?.text ?: "Unknown",
                                id = null
                            ),
                            songCountText = songCount,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl() ?: "",
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()?.getHighResThumbnailUrl() ?: "",
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        )
                    }

                    else -> null
                }
            }
        }
    }

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterExplicit())
            })
        } else this
}
