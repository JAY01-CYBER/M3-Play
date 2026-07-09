/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.pages

import com.j.m3play.innertube.models.Album
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.Artist
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.MusicCardShelfRenderer
import com.j.m3play.innertube.models.MusicResponsiveListItemRenderer
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.models.clean
import com.j.m3play.innertube.models.filterExplicit
import com.j.m3play.innertube.models.filterVideo
import com.j.m3play.innertube.models.oddElements
import com.j.m3play.innertube.models.splitBySeparator
import com.j.m3play.innertube.utils.parseTime

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    fun filterExplicit(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items =
                            s.items.filterExplicit().ifEmpty {
                                return@mapNotNull null
                            },
                    )
                },
            )
        } else {
            this
        }

    fun filterVideo(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items = s.items.filterVideo().ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else {
            this
        }

    companion object {
        fun fromMusicCardShelfRenderer(renderer: MusicCardShelfRenderer): YTItem? {
            val subtitle = renderer.subtitle.runs?.splitBySeparator()
            return when {
                renderer.onTap.watchEndpoint != null -> {
                    SongItem(
                        id = renderer.onTap.watchEndpoint.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(1)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        album = subtitle.getOrNull(2)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId!!)
                        },
                        duration = subtitle.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                    )
                }

                renderer.onTap.browseEndpoint?.isArtistEndpoint == true -> {
                    ArtistItem(
                        id = renderer.onTap.browseEndpoint.browseId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MIX" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.onTap.browseEndpoint?.isAlbumEndpoint == true -> {
                    AlbumItem(
                        browseId = renderer.onTap.browseEndpoint.browseId,
                        playlistId = renderer.buttons.firstOrNull()?.buttonRenderer?.command?.anyWatchEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(1)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        year = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                    )
                }

                renderer.onTap.browseEndpoint?.isPlaylistEndpoint == true -> {
                    PlaylistItem(
                        id = renderer.onTap.browseEndpoint.browseId.removePrefix("VL"),
                        title = renderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.joinToString(separator = "") { it.text } ?: return null,
                        author = Artist(id = null, name = renderer.subtitle.runs?.joinToString { it.text } ?: return null),
                        songCountText = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "PLAY_ARROW" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = null,
                    )
                }
                else -> null
            }
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
            val secondaryLine = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator() ?: emptyList()
            val thirdLine = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator() ?: emptyList()
            val listRun = (secondaryLine + thirdLine).clean()

            val videoId = renderer.playlistItemData?.videoId 
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId 
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
            
            val title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null
            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null
            val isExplicit = renderer.badges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true

            return when {
                renderer.isSong || videoId != null -> {
                    SongItem(
                        id = videoId ?: return null,
                        title = title,
                        artists = listRun.getOrNull(0)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: listOf(Artist(name = "Unknown", id = null)),
                        album = listRun.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId!!)
                        },
                        duration = secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime() ?: thirdLine.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = thumbnail,
                        explicit = isExplicit,
                    )
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title = title,
                        thumbnail = thumbnail,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = title,
                        artists = listRun.getOrNull(0)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: listOf(Artist(name = "Unknown", id = null)),
                        year = listRun.getOrNull(1)?.firstOrNull()?.text?.toIntOrNull(),
                        thumbnail = thumbnail,
                        explicit = isExplicit,
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                        title = title,
                        author = listRun.getOrNull(0)?.firstOrNull()?.let {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: Artist(name = "Unknown", id = null),
                        songCountText = listRun.getOrNull(1)?.lastOrNull()?.text,
                        thumbnail = thumbnail,
                        playEndpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    )
                }
                else -> null
            }
        }
    }
}
