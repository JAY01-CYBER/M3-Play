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
                        items = s.items.filterExplicit().ifEmpty { return@mapNotNull null },
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
            // SAFE PARSER: No strict rules, just pure ID extraction
            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.flexColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.navigationEndpoint?.watchEndpoint?.videoId

            val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId
                ?: renderer.menu?.menuRenderer?.items?.firstNotNullOfOrNull { it.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId }

            val title = renderer.flexColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: "Unknown"

            val secondaryRuns = renderer.flexColumns?.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            val rawSubtitle = secondaryRuns?.joinToString("") { it.text } ?: ""
            
            val artists = secondaryRuns?.filter { it.navigationEndpoint?.browseEndpoint != null }?.map {
                Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
            }?.ifEmpty { null } ?: listOf(Artist(name = rawSubtitle.ifBlank { "Unknown" }, id = null))

            val durationStr = renderer.flexColumns?.lastOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
            val duration = try { durationStr?.parseTime() } catch (e: Exception) { null }

            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() 
                ?: renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url ?: ""
            val explicit = renderer.badges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true

            if (videoId != null) {
                return SongItem(
                    id = videoId,
                    title = title,
                    artists = artists,
                    album = null,
                    duration = duration,
                    thumbnail = thumbnail,
                    explicit = explicit
                )
            }

            if (browseId != null) {
                if (browseId.startsWith("UC")) {
                    return ArtistItem(id = browseId, title = title, thumbnail = thumbnail, shuffleEndpoint = null, radioEndpoint = null)
                }
                if (browseId.startsWith("MPRE") || browseId.startsWith("FEmusic")) {
                    val pId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: ""
                    return AlbumItem(browseId = browseId, playlistId = pId, title = title, artists = artists, year = null, thumbnail = thumbnail, explicit = explicit)
                }
                return PlaylistItem(id = browseId.removePrefix("VL"), title = title, author = artists.firstOrNull(), songCountText = null, thumbnail = thumbnail, playEndpoint = null, shuffleEndpoint = null, radioEndpoint = null, isEditable = false)
            }

            return null
        }
    }
}
