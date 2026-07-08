/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package moe.koiverse.archivetune.innertube

import moe.koiverse.archivetune.innertube.models.AccountInfo
import moe.koiverse.archivetune.innertube.models.YTItem
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.Artist
import moe.koiverse.archivetune.innertube.models.ArtistItem
import moe.koiverse.archivetune.innertube.models.BrowseEndpoint
import moe.koiverse.archivetune.innertube.models.GridRenderer
import moe.koiverse.archivetune.innertube.models.MediaInfo
import moe.koiverse.archivetune.innertube.models.MusicResponsiveListItemRenderer
import moe.koiverse.archivetune.innertube.models.MusicTwoRowItemRenderer
import moe.koiverse.archivetune.innertube.models.MusicCarouselShelfRenderer
import moe.koiverse.archivetune.innertube.models.MusicShelfRenderer
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SearchSuggestions
import moe.koiverse.archivetune.innertube.models.Run
import moe.koiverse.archivetune.innertube.models.Runs
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB_REMIX
import moe.koiverse.archivetune.innertube.models.YouTubeLocale
import moe.koiverse.archivetune.innertube.models.getContinuation
import moe.koiverse.archivetune.innertube.models.getItems
import moe.koiverse.archivetune.innertube.models.oddElements
import moe.koiverse.archivetune.innertube.models.response.AccountMenuResponse
import moe.koiverse.archivetune.innertube.models.response.BrowseResponse
import moe.koiverse.archivetune.innertube.models.response.CreatePlaylistResponse
import moe.koiverse.archivetune.innertube.models.response.GetQueueResponse
import moe.koiverse.archivetune.innertube.models.response.GetSearchSuggestionsResponse
import moe.koiverse.archivetune.innertube.models.response.GetTranscriptResponse
import moe.koiverse.archivetune.innertube.models.response.NextResponse
import moe.koiverse.archivetune.innertube.models.response.PlayerResponse
import moe.koiverse.archivetune.innertube.models.response.SearchResponse
import moe.koiverse.archivetune.innertube.pages.AlbumPage
import moe.koiverse.archivetune.innertube.pages.ArtistItemsContinuationPage
import moe.koiverse.archivetune.innertube.pages.ArtistItemsPage
import moe.koiverse.archivetune.innertube.pages.ArtistPage
import moe.koiverse.archivetune.innertube.pages.ChartsPage
import moe.koiverse.archivetune.innertube.pages.BrowseResult
import moe.koiverse.archivetune.innertube.pages.ExplorePage
import moe.koiverse.archivetune.innertube.pages.HistoryPage
import moe.koiverse.archivetune.innertube.pages.HomePage
import moe.koiverse.archivetune.innertube.pages.LibraryContinuationPage
import moe.koiverse.archivetune.innertube.pages.LibraryPage
import moe.koiverse.archivetune.innertube.pages.MoodAndGenres
import moe.koiverse.archivetune.innertube.pages.NewReleaseAlbumPage
import moe.koiverse.archivetune.innertube.pages.NextPage
import moe.koiverse.archivetune.innertube.pages.NextResult
import moe.koiverse.archivetune.innertube.pages.PlaylistContinuationPage
import moe.koiverse.archivetune.innertube.pages.PlaylistPage
import moe.koiverse.archivetune.innertube.pages.RelatedPage
import moe.koiverse.archivetune.innertube.pages.SearchPage
import moe.koiverse.archivetune.innertube.pages.SearchResult
import moe.koiverse.archivetune.innertube.pages.SearchSuggestionPage
import moe.koiverse.archivetune.innertube.pages.SearchSummary
import moe.koiverse.archivetune.innertube.pages.SearchSummaryPage
import moe.koiverse.archivetune.innertube.utils.PoTokenGenerator
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.Proxy
import java.util.Locale
import kotlin.random.Random

object YouTube {
    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }
    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }
    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }
    var poToken: String?
        get() = innerTube.poToken
        set(value) {
            innerTube.poToken = value
        }
    var webClientPoTokenEnabled: Boolean = false
    var poTokenGvs: String? = null
    var poTokenPlayer: String? = null
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }
    var streamBypassProxy: Boolean = false
    val streamProxy: Proxy?
        get() = if (streamBypassProxy) null else proxy
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    private fun needsServiceIntegrity(client: YouTubeClient): Boolean {
        val name = client.clientName.uppercase(Locale.US)
        return name == "WEB" ||
            name == "WEB_REMIX" ||
            name == "WEB_CREATOR" ||
            name == "MWEB" ||
            name == "WEB_EMBEDDED_PLAYER" ||
            name == "TVHTML5" ||
            name == "TVHTML5_SIMPLY_EMBEDDED_PLAYER" ||
            name == "TVHTML5_SIMPLY"
    }

    private fun resolvePlayerPoToken(client: YouTubeClient, videoId: String, explicitPoToken: String?): String? {
        val explicit = explicitPoToken?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        if (!webClientPoTokenEnabled) return null
        if (!needsServiceIntegrity(client)) return null

        val userExtracted = poTokenPlayer?.takeIf { it.isNotBlank() }
        if (userExtracted != null) return userExtracted

        val webFallback = poToken?.takeIf { it.isNotBlank() }
        if (webFallback != null) return webFallback

        return null
    }

    internal fun resolveGvsPoToken(): String? {
        if (!webClientPoTokenEnabled) return null
        return poTokenGvs?.takeIf { it.isNotBlank() }
            ?: poToken?.takeIf { it.isNotBlank() }
    }

    internal fun appendGvsPoToken(url: String, client: YouTubeClient? = null): String {
        val token = resolveGvsPoToken() ?: return url
        if (url.contains("pot=")) return url

        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}pot=$token"
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
        SearchSuggestions(
            queries = response.contents?.getOrNull(0)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull { content ->
                content.searchSuggestionRenderer?.suggestion?.runs?.joinToString(separator = "") { it.text }
            }.orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull {
                it.musicResponsiveListItemRenderer?.let { renderer ->
                    SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                }
            }.orEmpty()
        )
    }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query).body<SearchResponse>()
        val parsedSummaries = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
            
            if (content.musicCardShelfRenderer != null) {
                SearchSummary(
                    title = content.musicCardShelfRenderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.firstOrNull()?.text ?: "Top result",
                    items = listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(content.musicCardShelfRenderer))
                        .plus(
                            content.musicCardShelfRenderer.contents
                                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                ?.mapNotNull { SearchSummaryPage.fromMusicResponsiveListItemRenderer(it) }
                                .orEmpty()
                        )
                        .distinctBy { it.id }
                        .ifEmpty { return@mapNotNull null }
                )
            } else {
                // 🔥 THE MAGIC FIX: Extracting shelf from inside itemSectionRenderer if nested!
                val shelf = content.musicShelfRenderer 
                    ?: content.itemSectionRenderer?.contents?.firstOrNull()?.musicShelfRenderer
                    
                if (shelf != null) {
                    SearchSummary(
                        title = shelf.title?.runs?.firstOrNull()?.text ?: "Other",
                        items = shelf.contents?.getItems()
                            ?.mapNotNull { SearchSummaryPage.fromMusicResponsiveListItemRenderer(it) }
                            ?.distinctBy { it.id }
                            ?.ifEmpty { return@mapNotNull null } ?: return@mapNotNull null
                    )
                } else null
            }
        } ?: emptyList()
        
        SearchSummaryPage(summaries = parsedSummaries)
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        
        // 🔥 THE MAGIC FIX: Safe extraction for filtered searches too!
        val contents = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
            
        val shelf = contents?.musicShelfRenderer 
            ?: contents?.itemSectionRenderer?.contents?.firstOrNull()?.musicShelfRenderer

        SearchResult(
            items = shelf?.contents?.getItems()?.mapNotNull {
                SearchPage.toYTItem(it)
            }.orEmpty(),
            continuation = shelf?.continuations?.getContinuation()
        )
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        val items = response.continuationContents?.musicShelfContinuation?.contents
            ?.mapNotNull {
                SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
            } ?: emptyList()
        SearchResult(
            items = items,
            continuation = if (items.isEmpty()) null else response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()
        )
    }

    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        val contents = response.contents ?: throw IllegalStateException("Missing browse contents for $browseId")
        val twoColumn = contents.twoColumnBrowseResultsRenderer
            ?: throw IllegalStateException("Missing twoColumnBrowseResultsRenderer for $browseId")
        val tabs = twoColumn.tabs ?: throw IllegalStateException("Missing tabs for $browseId")
        val header = tabs.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicResponsiveHeaderRenderer
            ?: throw IllegalStateException("Missing album header for $browseId")
        val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
        val albumTitle = header.title.runs?.firstOrNull()?.text
            ?: throw IllegalStateException("Missing album title for $browseId")
        val albumArtists = (header.straplineTextOne ?: throw IllegalStateException("Missing album artists for $browseId"))
            .runs
            ?.oddElements()
            ?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            }
            ?: throw IllegalStateException("Missing album artists runs for $browseId")
        val albumYear = header.subtitle.runs?.lastOrNull()?.text?.toIntOrNull()
        val albumThumbnail = (header.thumbnail ?: throw IllegalStateException("Missing album thumbnail for $browseId"))
            .musicThumbnailRenderer
            ?.getThumbnailUrl()
            ?: throw IllegalStateException("Missing album thumbnail url for $browseId")
        AlbumPage(
            album = AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = albumTitle,
                artists = albumArtists,
                year = albumYear,
                thumbnail = albumThumbnail,
                explicit = false,
            ),
            songs = if (withSongs) albumSongs(playlistId, AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = albumTitle,
                artists = albumArtists,
                year = albumYear,
                thumbnail = albumThumbnail,
                explicit = false
            )).getOrThrow() else emptyList(),
            otherVersions = twoColumn.secondaryContents?.sectionListRenderer?.contents?.getOrNull(1)?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty()
        )
    }

    suspend fun albumSongs(playlistId: String, album: AlbumItem? = null): Result<List<SongItem>> = runCatching {
        var response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
        val songs = response.contents?.twoColumnBrowseResultsRenderer
            ?.secondaryContents?.sectionListRenderer
            ?.contents?.firstOrNull()
            ?.musicPlaylistShelfRenderer?.contents?.getItems()
            ?.mapNotNull {
                AlbumPage.getSong(it, album)
            }!!
            .toMutableList()
        var continuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
            .contents.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getContinuation()
        val seenContinuations = mutableSetOf<String>()
        var requestCount = 0
        val maxRequests = 50
        
        var consecutiveEmptyResponses = 0
        while (continuation != null && requestCount < maxRequests) {
            if (continuation in seenContinuations) {
                break
            }
            seenContinuations.add(continuation)
            requestCount++
            
            response = innerTube.browse(
                client = WEB_REMIX,
                continuation = continuation,
            ).body<BrowseResponse>()
            
            val newSongs = response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getItems()?.mapNotNull {
                AlbumPage.getSong(it, album)
            }.orEmpty()
            
            if (newSongs.isEmpty()) {
                consecutiveEmptyResponses++
                if (consecutiveEmptyResponses >= 2) break
            } else {
                consecutiveEmptyResponses = 0
                songs += newSongs
            }
            
            continuation = response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()
        }
        songs
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()

        ArtistPage(
            artist = ArtistItem(
                id = browseId,
                title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                channelId = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId,
                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer
                    ?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
                    ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                        ?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
            ),
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text
        )
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        val gridRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.gridRenderer
        if (gridRenderer != null) {
            ArtistItemsPage(
                title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                items = gridRenderer.items.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    }
                },
                continuation = gridRenderer.continuations?.getContinuation()
            )
        } else {
            val musicPlaylistShelfRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer
            ArtistItemsPage(
                title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                items = musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                    } ?: emptyList(),
                continuation = musicPlaylistShelfRenderer?.contents?.getContinuation()
            )
        }
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()

        when {
            response.continuationContents?.gridContinuation != null -> {
                val gridContinuation = response.continuationContents.gridContinuation
                val items = gridContinuation.items.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    }
                }
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else gridContinuation.continuations?.getContinuation()
                )
            }

            response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                val musicPlaylistShelfContinuation = response.continuationContents.musicPlaylistShelfContinuation
                val items = musicPlaylistShelfContinuation.contents.getItems().mapNotNull {
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                }
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else musicPlaylistShelfContinuation.continuations?.getContinuation()
                )
            }

            else -> {
                val continuationItems = response.onResponseReceivedActions?.firstOrNull()
                    ?.appendContinuationItemsAction?.continuationItems
                val items = continuationItems?.getItems()?.mapNotNull {
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                } ?: emptyList()
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else continuationItems?.getContinuation()
                )
            }
        }
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "VL$playlistId",
            setLogin = true
        ).body<BrowseResponse>()
        val base = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        val header = base?.musicResponsiveHeaderRenderer ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        if (header == null) throw IllegalStateException("PLAYLIST_PRIVATE")

        val title = header.title.runs?.firstOrNull()?.text ?: throw IllegalStateException("PLAYLIST_PRIVATE")
        val thumbnail = header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
            ?: throw IllegalStateException("PLAYLIST_PRIVATE")

        val editable = base?.musicEditablePlaylistDetailHeaderRenderer != null

        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = title,
                author = header.straplineTextOne?.runs?.firstOrNull()?.let {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                songCountText = header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = thumbnail,
                playEndpoint = null,
                shuffleEndpoint = header.buttons.lastOrNull()?.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                radioEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                isEditable = editable
            ),
            songs = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                    PlaylistPage.fromMusicResponsiveListItemRenderer(it)
                } ?: emptyList(),
            songsContinuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getContinuation(),
            continuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.continuations?.getContinuation()
        )
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            browseId = "",
            setLogin = true
        ).body<BrowseResponse>()

        val mainContents: List<MusicShelfRenderer.Content> = buildList {
            response.continuationContents?.sectionListContinuation?.contents
                .orEmpty()
                .forEach { sectionContent ->
                    addAll(sectionContent.musicPlaylistShelfRenderer?.contents.orEmpty())
                }
        }

        val appendedContents = response.onResponseReceivedActions
            ?.firstOrNull()
            ?.appendContinuationItemsAction
            ?.continuationItems
            .orEmpty()

        val allContents = mainContents + appendedContents

        val songs = allContents
            .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            .mapNotNull(PlaylistPage::fromMusicResponsiveListItemRenderer)

        val nextContinuation = if (songs.isEmpty()) null else {
            response.continuationContents
                ?.sectionListContinuation
                ?.continuations
                ?.getContinuation()
                ?: response.continuationContents
                    ?.musicPlaylistShelfContinuation
                    ?.continuations
                    ?.getContinuation()
                ?: response.continuationContents
                    ?.musicShelfContinuation
                    ?.continuations
                    ?.getContinuation()
                ?: response.onResponseReceivedActions
                    ?.firstOrNull()
                    ?.appendContinuationItemsAction
                    ?.continuationItems
                    ?.getContinuation()
        }

        PlaylistContinuationPage(
            songs = songs,
            continuation = nextContinuation
        )
    }

    suspend fun home(continuation: String? = null, params: String? = null): Result<HomePage> = runCatching {
        if (continuation != null) {
            return@runCatching homeContinuation(continuation).getOrThrow()
        }

        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_home", params = params, setLogin = true).body<BrowseResponse>()
        val continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()
        val sectionListRender = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer
        val sections = sectionListRender?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()
        val chips = sectionListRender.header?.chipCloudRenderer?.chips?.mapNotNull { HomePage.Chip.fromChipCloudChipRenderer(it) }
        HomePage(chips, sections, continuation)
    }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        val sections = response.continuationContents?.sectionListContinuation?.contents
            ?.mapNotNull { it.musicCarouselShelfRenderer }
            ?.mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.orEmpty()
        val nextContinuation = if (sections.isEmpty()) null else {
            response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
        }
        HomePage(
            chips = null,
            sections = sections,
            continuation = nextContinuation
        )
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore").body<BrowseResponse>()
        ExplorePage(
            newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer).orEmpty(),
            moodAndGenres = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicNavigationButtonRenderer }
                ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                .orEmpty()
        )
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_new_releases_albums").body<BrowseResponse>()
        val contents =
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                .orEmpty()

        contents
            .asSequence()
            .flatMap { content ->
                when {
                    content.gridRenderer?.items != null -> {
                        content.gridRenderer.items
                            .asSequence()
                            .mapNotNull { it.musicTwoRowItemRenderer }
                            .mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                    }

                    content.musicCarouselShelfRenderer?.contents != null -> {
                        content.musicCarouselShelfRenderer.contents
                            .asSequence()
                            .mapNotNull { it.musicTwoRowItemRenderer }
                            .mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                    }

                    else -> emptySequence()
                }
            }
            .toList()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
    }

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                when {
                    content.gridRenderer != null -> {
                        BrowseResult.Item(
                            title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.gridRenderer.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    content.musicCarouselShelfRenderer != null -> {
                        BrowseResult.Item(
                            title = content.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.musicCarouselShelfRenderer.contents
                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    else -> null
                }
            }.orEmpty()
        )
    }

    suspend fun library(browseId: String, tabIndex: Int = 0) = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = browseId,
            setLogin = true
        ).body<BrowseResponse>()

        val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs

        val contents = if (tabs != null && tabIndex >= 0 && tabIndex < tabs.size) {
            tabs[tabIndex].tabRenderer.content?.sectionListRenderer?.contents?.firstOrNull()
        } else {
            null
        }

        when {
            contents?.gridRenderer != null -> {
                LibraryPage(
                    items = contents.gridRenderer.items.orEmpty()
                        .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                    continuation = contents.gridRenderer.continuations?.getContinuation()
                )
            }

            contents?.musicShelfRenderer?.contents != null -> {
                LibraryPage(
                    items = contents.musicShelfRenderer.contents
                        .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                    continuation = contents.musicShelfRenderer.continuations?.getContinuation()
                )
            }

            else -> {
                LibraryPage(
                    items = emptyList(),
                    continuation = null
                )
            }
        }
    }

    suspend fun libraryContinuation(continuation: String) = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        val contents = response.continuationContents

        when {
            contents?.gridContinuation != null -> {
                val items = contents.gridContinuation.items
                    .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                    .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                LibraryContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else contents.gridContinuation.continuations?.getContinuation()
                )
            }

            contents?.musicShelfContinuation?.contents != null -> {
                val items = contents.musicShelfContinuation.contents
                    .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                    .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
                LibraryContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else contents.musicShelfContinuation.continuations?.getContinuation()
                )
            }

            else -> {
                LibraryContinuationPage(
                    items = emptyList(),
                    continuation = null
                )
            }
        }
    }

    suspend fun libraryRecentActivity(): Result<LibraryPage> = runCatching {
        val continuation = LibraryFilter.FILTER_RECENT_ACTIVITY.value

        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        val gridItems = response.continuationContents?.sectionListContinuation?.contents?.firstOrNull()
            ?.gridRenderer?.items
        
        if (gridItems == null) {
            return@runCatching LibraryPage(
                items = emptyList(),
                continuation = null
            )
        }
        
        val items = gridItems.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                LibraryPage.fromMusicTwoRowItemRenderer(renderer)
            }
        }.toMutableList()

        items.forEachIndexed { index, item ->
            if (item is ArtistItem) {
                artist(item.id).getOrNull()?.artist?.let { fetchedArtist ->
                    items[index] = fetchedArtist.copy(thumbnail = item.thumbnail)
                }
            }
        }

        LibraryPage(
            items = items,
            continuation = null
        )
    }

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_charts",
            params = "ggMGCgQIgAQ%3D",
            continuation = continuation
        ).body<BrowseResponse>()

        val sections = mutableListOf<ChartsPage.ChartSection>()
    
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.forEach { content ->
            
                content.musicCarouselShelfRenderer?.let { renderer ->
                    val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: return@forEach
                
                    val items = renderer.contents.mapNotNull { item ->
                        when {
                            item.musicResponsiveListItemRenderer != null -> 
                                convertToChartItem(item.musicResponsiveListItemRenderer)
                            item.musicTwoRowItemRenderer != null -> 
                                convertMusicTwoRowItem(item.musicTwoRowItemRenderer)
                            else -> null
                        }
                    }.filterNotNull()
                
                    if (items.isNotEmpty()) {
                        sections.add(
                            ChartsPage.ChartSection(
                                title = title,
                                items = items,
                                chartType = determineChartType(title)
                            )
                        )
                    }
                }
            
                content.gridRenderer?.let { renderer ->
                    val title = renderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: return@let
                
                    val items = renderer.items.mapNotNull { item ->
                        item.musicTwoRowItemRenderer?.let { renderer ->
                            convertMusicTwoRowItem(renderer)
                        }
                    }.filterNotNull()
                
                    if (items.isNotEmpty()) {
                        sections.add(
                            ChartsPage.ChartSection(
                                title = title,
                                items = items,
                                chartType = ChartsPage.ChartType.NEW_RELEASES
                            )
                        )
                    }
                }
            }

        ChartsPage(
            sections = sections,
            continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
        )
    }

    private fun determineChartType(title: String): ChartsPage.ChartType {
        return when {
            title.contains("Trending", ignoreCase = true) -> ChartsPage.ChartType.TRENDING
            title.contains("Top", ignoreCase = true) -> ChartsPage.ChartType.TOP
            else -> ChartsPage.ChartType.GENRE
        }
    }

    private fun convertToChartItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        return try {
            val chartVideoId = renderer.playlistItemData?.videoId 
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId

            when {
                renderer.flexColumns.size >= 3 && chartVideoId != null -> {
                    val firstColumn = renderer.flexColumns.getOrNull(0)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text ?: return null
                
                    val secondColumn = renderer.flexColumns.getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text ?: return null

                    val titleRun = firstColumn.runs?.firstOrNull() ?: return null
                    val title = titleRun.text.takeIf { it.isNotBlank() } ?: return null

                    val artists = secondColumn.runs?.mapNotNull { run ->
                        run.text.takeIf { it.isNotBlank() }?.let { name ->
                            Artist(
                                name = name,
                                id = run.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }
                    } ?: emptyList()

                    val thirdColumn = renderer.flexColumns.getOrNull(2)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text

                    SongItem(
                        id = chartVideoId,
                        title = title,
                        artists = artists,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.badges?.any { 
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" 
                        } == true,
                        chartPosition = thirdColumn?.runs?.firstOrNull()?.text?.toIntOrNull(),
                        chartChange = thirdColumn?.runs?.getOrNull(1)?.text
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            println("Error converting chart item: ${e.message}\n${Json.encodeToString(renderer)}")
            null
        }
    }

    private fun convertMusicTwoRowItem(renderer: MusicTwoRowItemRenderer): YTItem? {
        return try {
            when {
                renderer.isSong -> {
                    val subtitle = renderer.subtitle?.runs ?: return null
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle.mapNotNull {
                            it.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                Artist(name = it.text, id = id)
                            }
                        },
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.any {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } == true
                    )
                }
                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.mapNotNull {
                            it.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                Artist(name = it.text, id = id)
                            }
                        },
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.any {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } == true
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            println("Error converting two row item: ${e.message}\n${Json.encodeToString(renderer)}")
            null
        }
    }

    suspend fun musicHistory() = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_history",
            setLogin = true
        ).body<BrowseResponse>()

        HistoryPage(
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull {
                    it.musicShelfRenderer?.let { musicShelfRenderer ->
                        HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                    }
                }
        )
    }

    suspend fun likeVideo(videoId: String, like: Boolean) = runCatching {
        if (like)
            innerTube.likeVideo(WEB_REMIX, videoId)
        else
            innerTube.unlikeVideo(WEB_REMIX, videoId)
    }

    suspend fun likePlaylist(playlistId: String, like: Boolean) = runCatching {
        if (like)
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        else
            innerTube.unlikePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean) = runCatching {
        if (subscribe)
            innerTube.subscribeChannel(WEB_REMIX, channelId)
        else
            innerNormally I can help with things like this, but I don't seem to have access to that content. You can try again or ask me for something else.
