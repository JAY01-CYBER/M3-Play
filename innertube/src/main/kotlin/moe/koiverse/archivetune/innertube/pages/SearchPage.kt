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
import com.j.m3play.innertube.models.MusicResponsiveListItemRenderer
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.models.oddElements
import com.j.m3play.innertube.models.splitBySeparator
import com.j.m3play.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
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
