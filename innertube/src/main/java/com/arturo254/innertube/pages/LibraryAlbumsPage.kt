package com.arturo254.innertube.pages

import com.arturo254.innertube.models.Album
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.Artist
import com.arturo254.innertube.models.ArtistItem
import com.arturo254.innertube.models.MusicResponsiveListItemRenderer
import com.arturo254.innertube.models.MusicTwoRowItemRenderer
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.YTItem
import com.arturo254.innertube.models.oddElements
import com.arturo254.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
