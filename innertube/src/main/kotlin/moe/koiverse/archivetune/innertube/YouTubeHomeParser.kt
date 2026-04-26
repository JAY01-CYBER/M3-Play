package com.j.m3play.innertube

import com.j.m3play.innertube.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun YouTube.getMusicHome(): Result<List<HomeShelf>> = withContext(Dispatchers.IO) {
    runCatching {
        // FEmusic_home is the official browseId for YouTube Music Home
        val response = browse("FEmusic_home")
        val homeShelves = mutableListOf<HomeShelf>()

        val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
        val sectionListContents = tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents

        sectionListContents?.forEach { section ->
            val carouselRenderer = section.musicCarouselShelfRenderer
            
            if (carouselRenderer != null) {
                val title = carouselRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: "For You"
                val items = mutableListOf<YTItem>()

                carouselRenderer.contents?.forEach { itemRenderer ->
                    val twoRowItem = itemRenderer.musicTwoRowItemRenderer
                    
                    if (twoRowItem != null) {
                        val browseId = twoRowItem.navigationEndpoint?.browseEndpoint?.browseId
                        val videoId = twoRowItem.navigationEndpoint?.watchEndpoint?.videoId
                        val itemTitle = twoRowItem.title?.runs?.firstOrNull()?.text ?: ""
                        val subtitleText = twoRowItem.subtitle?.runs?.joinToString("") { it.text } ?: ""
                        val thumbnail = twoRowItem.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url ?: ""

                        when {
                            videoId != null -> {
                                items.add(
                                    SongItem(
                                        id = videoId,
                                        title = itemTitle,
                                        artists = listOf(Artist(name = subtitleText, id = null)),
                                        thumbnail = thumbnail,
                                        explicit = subtitleText.contains("Explicit", ignoreCase = true)
                                    )
                                )
                            }
                            browseId?.startsWith("MPREb") == true -> {
                                items.add(
                                    AlbumItem(
                                        browseId = browseId,
                                        playlistId = twoRowItem.navigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: "",
                                        title = itemTitle,
                                        artists = listOf(Artist(name = subtitleText, id = null)),
                                        thumbnail = thumbnail
                                    )
                                )
                            }
                            browseId?.startsWith("VL") == true || browseId?.startsWith("PL") == true -> {
                                items.add(
                                    PlaylistItem(
                                        id = browseId.removePrefix("VL"),
                                        title = itemTitle,
                                        author = Artist(name = subtitleText, id = null),
                                        songCountText = null,
                                        thumbnail = thumbnail,
                                        playEndpoint = null,
                                        shuffleEndpoint = null,
                                        radioEndpoint = null
                                    )
                                )
                            }
                            browseId?.startsWith("UC") == true -> {
                                items.add(
                                    ArtistItem(
                                        id = browseId,
                                        title = itemTitle,
                                        thumbnail = thumbnail,
                                        shuffleEndpoint = null,
                                        radioEndpoint = null
                                    )
                                )
                            }
                        }
                    }
                }

                if (items.isNotEmpty()) {
                    homeShelves.add(HomeShelf(title = title, items = items))
                }
            }
        }
        
        homeShelves
    }
}
