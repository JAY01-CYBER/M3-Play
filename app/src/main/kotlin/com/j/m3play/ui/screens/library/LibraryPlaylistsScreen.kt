/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.j.m3play.ui.theme.PlayerColorExtractor
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.CONTENT_TYPE_HEADER
import com.j.m3play.constants.CONTENT_TYPE_PLAYLIST
import com.j.m3play.constants.PlaylistSortDescendingKey
import com.j.m3play.constants.PlaylistSortType
import com.j.m3play.constants.PlaylistSortTypeKey
import com.j.m3play.constants.ShowLikedPlaylistKey
import com.j.m3play.constants.ShowDownloadedPlaylistKey
import com.j.m3play.constants.ShowTopPlaylistKey
import com.j.m3play.constants.ShowCachedPlaylistKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.PlaylistTagsFilterKey
import com.j.m3play.constants.ShowSpotifyPlaylistKey
import com.j.m3play.constants.SpotifyConnectedKey
import com.j.m3play.constants.SpotifyTokenKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.ui.component.CreatePlaylistDialog
import com.j.m3play.ui.component.LibraryPlaylistListItem
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.M3AutoPlaylistCard
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LibraryPlaylistsViewModel
import com.j.m3play.LocalDatabase
import com.j.m3play.extensions.move
import com.j.m3play.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)

    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) { selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet() }
    val database = LocalDatabase.current
    val filteredPlaylistIds by database.playlistIdsByTags(if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()).collectAsState(initial = emptyList())

    val playlists by viewModel.allPlaylists.collectAsState()
    val visiblePlaylists = playlists.filter { playlist ->
        val name = playlist.playlist.name ?: ""
        val matchesName = !name.contains("episode", ignoreCase = true)
        val matchesTags = selectedTagIds.isEmpty() || playlist.id in filteredPlaylistIds
        matchesName && matchesTags
    }

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)

    val isSpotifyConnected by context.dataStore.data.map { it[SpotifyConnectedKey] ?: false }.collectAsState(initial = false)
    val showSpotifyPlaylist by context.dataStore.data.map { it[ShowSpotifyPlaylistKey] ?: true }.collectAsState(initial = true)
    val spotifyToken by context.dataStore.data.map { it[SpotifyTokenKey] ?: "" }.collectAsState(initial = "")
 
    var spotifyPlaylists by remember { mutableStateOf<List<com.j.m3play.spotify.models.SpotifyPlaylist>>(emptyList()) }

    LaunchedEffect(isSpotifyConnected, showSpotifyPlaylist, spotifyToken) {
        if (isSpotifyConnected && showSpotifyPlaylist && spotifyToken.isNotEmpty()) {
            com.j.m3play.spotify.Spotify.accessToken = spotifyToken
            com.j.m3play.spotify.Spotify.myPlaylists(limit = 50).onSuccess { paging -> spotifyPlaylists = paging.items }
        } else {
            spotifyPlaylists = emptyList()
        }
    }

    val lazyListState = rememberLazyListState()
    val canEnterReorderMode = sortType == PlaylistSortType.CUSTOM && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    
    val listHeaderItems = 4 
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) { from, to ->
        if (!canReorderPlaylists) return@rememberReorderableLazyListState
        if (from.index < listHeaderItems || to.index < listHeaderItems) return@rememberReorderableLazyListState
        val fromIndex = from.index - listHeaderItems
        val toIndex = to.index - listHeaderItems
        if (fromIndex !in mutableVisiblePlaylists.indices || toIndex !in mutableVisiblePlaylists.indices) return@rememberReorderableLazyListState

        dragInfo = (dragInfo?.first ?: fromIndex) to toIndex
        mutableVisiblePlaylists.move(fromIndex, toIndex)
    }

    LaunchedEffect(visiblePlaylists, canReorderPlaylists, reorderableState.isAnyItemDragging, dragInfo) {
        if (!canReorderPlaylists) { mutableVisiblePlaylists.clear(); mutableVisiblePlaylists.addAll(visiblePlaylists); return@LaunchedEffect }
        if (!reorderableState.isAnyItemDragging && dragInfo == null) { mutableVisiblePlaylists.clear(); mutableVisiblePlaylists.addAll(visiblePlaylists) }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging, canReorderPlaylists) {
        if (!canReorderPlaylists || reorderableState.isAnyItemDragging) return@LaunchedEffect
        dragInfo ?: return@LaunchedEffect
        val playlistsToReorder = mutableVisiblePlaylists.toList()
        database.transaction { playlistsToReorder.forEachIndexed { index, playlist -> setPlaylistCustomOrder(playlist.id, index) } }
        dragInfo = null
    }
    
    LaunchedEffect(canEnterReorderMode) { if (!canEnterReorderMode) reorderEnabled = false }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() } }
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) { lazyListState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) }
    }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    LaunchedEffect(playlists) {
        val thumbnailUrl = playlists.firstOrNull { it.songThumbnails.isNotEmpty() }?.songThumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { withContext(Dispatchers.IO) { context.imageLoader.execute(request) } }.getOrNull()
            if (result?.image != null) {
                val palette = withContext(Dispatchers.Default) { Palette.from(result.image!!.toBitmap()).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
            }
        } else gradientColors = emptyList()
    }
    
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 900f)).coerceIn(0f, 1f) else 0f } }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, initialTextFieldValue = initialTextFieldValue, allowSyncing = allowSyncing)

    val headerContent = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.height(40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { t -> when (t) { PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSortType.NAME -> R.string.sort_by_name; PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count; PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated; PlaylistSortType.CUSTOM -> R.string.sort_by_custom } })
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEnterReorderMode) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.height(40.dp)) {
                        IconButton(onClick = { reorderEnabled = !reorderEnabled }) { Icon(painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock), null) }
                    }
                }
                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }, modifier = Modifier.size(40.dp), containerColor = MaterialTheme.colorScheme.primaryContainer, elevation = FloatingActionButtonDefaults.elevation(0.dp)) {
                    Icon(painterResource(R.drawable.add), null)
                }
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(surfaceColor).pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = { if (ytmSync) viewModel.sync() })) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxSize(0.7f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                val w = size.width; val h = size.height
                if (gradientColors.size >= 3) {
                    val c0 = gradientColors[0]; val c1 = gradientColors[1]; val c2 = gradientColors[2]; val c3 = gradientColors.getOrElse(3) { c0 }; val c4 = gradientColors.getOrElse(4) { c1 }
                    drawRect(Brush.radialGradient(listOf(c0.copy(alpha = gradientAlpha * 0.34f), c0.copy(alpha = gradientAlpha * 0.2f), c0.copy(alpha = gradientAlpha * 0.11f), Color.Transparent), Offset(w * 0.15f, h * 0.1f), w * 0.55f))
                    drawRect(Brush.radialGradient(listOf(c1.copy(alpha = gradientAlpha * 0.32f), c1.copy(alpha = gradientAlpha * 0.19f), c1.copy(alpha = gradientAlpha * 0.1f), Color.Transparent), Offset(w * 0.85f, h * 0.2f), w * 0.65f))
                    drawRect(Brush.radialGradient(listOf(c2.copy(alpha = gradientAlpha * 0.28f), c2.copy(alpha = gradientAlpha * 0.16f), c2.copy(alpha = gradientAlpha * 0.085f), Color.Transparent), Offset(w * 0.3f, h * 0.45f), w * 0.6f))
                    drawRect(Brush.radialGradient(listOf(c3.copy(alpha = gradientAlpha * 0.24f), c3.copy(alpha = gradientAlpha * 0.13f), Color.Transparent), Offset(w * 0.7f, h * 0.5f), w * 0.7f))
                    drawRect(Brush.radialGradient(listOf(c4.copy(alpha = gradientAlpha * 0.2f), c4.copy(alpha = gradientAlpha * 0.11f), Color.Transparent), Offset(w * 0.5f, h * 0.75f), w * 0.8f))
                } else {
                    drawRect(Brush.radialGradient(listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.34f), Color.Transparent), Offset(w * 0.5f, h * 0.3f), w * 0.7f))
                }
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = gradientAlpha * 0.22f), surfaceColor), startY = h * 0.4f, endY = h))
            }) {}
        }
        
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            item(key = "large_title", contentType = CONTENT_TYPE_HEADER) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Playlists", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                    Text("All your playlists, organized for you", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
            item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

            item(key = "auto_playlists", contentType = CONTENT_TYPE_HEADER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val pills = mutableListOf<@Composable () -> Unit>()
                    if (showLiked) pills.add { M3AutoPlaylistCard(stringResource(R.string.liked), "Auto playlist", R.drawable.favorite, { navController.navigate("auto_playlist/liked") }, Modifier.fillMaxWidth()) }
                    if (showDownloaded) pills.add { M3AutoPlaylistCard(stringResource(R.string.offline), "Auto playlist", R.drawable.download, { navController.navigate("auto_playlist/downloaded") }, Modifier.fillMaxWidth()) }
                    if (showTop) pills.add { M3AutoPlaylistCard(stringResource(R.string.my_top) + " $topSize", "Auto playlist", R.drawable.trending_up, { navController.navigate("top_playlist/$topSize") }, Modifier.fillMaxWidth()) }
                    if (showCached) pills.add { M3AutoPlaylistCard(stringResource(R.string.cached_playlist), "Shuffle all", R.drawable.cached, { navController.navigate("cache_playlist/cached") }, Modifier.fillMaxWidth()) }
                    pills.chunked(2).forEach { rowPills ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowPills.forEach { pill -> Box(modifier = Modifier.weight(1f)) { pill() } }
                            if (rowPills.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (canReorderPlaylists) {
                itemsIndexed(items = mutableVisiblePlaylists, key = { _, item -> item.id }, contentType = { _, _ -> CONTENT_TYPE_PLAYLIST }) { _, playlist ->
                    ReorderableItem(state = reorderableState, key = playlist.id) {
                        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                            LibraryPlaylistListItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, playlist = playlist, showDragHandle = true, dragHandleModifier = Modifier.draggableHandle(), modifier = Modifier.animateItem())
                        }
                    }
                }
            } else {
                items(items = visiblePlaylists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { playlist ->
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                        LibraryPlaylistListItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, playlist = playlist, modifier = Modifier.animateItem())
                    }
                }
            }

            if (spotifyPlaylists.isNotEmpty()) {
                item(key = "spotify_header", contentType = CONTENT_TYPE_HEADER) {
                    Text("Spotify Playlist", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp))
                }
                items(spotifyPlaylists, key = { "sp_${it.id}" }) { sp ->
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { navController.navigate("spotify_playlist/${sp.id}") }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                        ListItem(
                            headlineContent = { Text(sp.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${sp.tracks?.total ?: 0} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { AsyncImage(model = sp.images.firstOrNull()?.url, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) },
                            trailingContent = { Icon(painterResource(R.drawable.library_music), contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
    }
}
