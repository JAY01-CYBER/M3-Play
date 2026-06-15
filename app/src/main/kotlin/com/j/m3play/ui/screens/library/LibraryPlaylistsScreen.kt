/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
import com.j.m3play.constants.PlaylistTagsFilterKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.constants.ShowSpotifyPlaylistKey
import com.j.m3play.constants.SpotifyConnectedKey
import com.j.m3play.constants.SpotifyTokenKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.ui.component.CreatePlaylistDialog
import com.j.m3play.ui.component.LibraryPlaylistListItem
import com.j.m3play.ui.component.LocalMenuState
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
    contentPadding: PaddingValues,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)

    val database = LocalDatabase.current
    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) { selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet() }
    val filteredPlaylistIds by database.playlistIdsByTags(if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()).collectAsState(initial = emptyList())

    val playlists by viewModel.allPlaylists.collectAsState()
    
    val visiblePlaylists = playlists.filter { playlist ->
        val name = playlist.playlist.name ?: ""
        val matchesName = !name.contains("episode", ignoreCase = true)
        val matchesTags = selectedTagIds.isEmpty() || playlist.id in filteredPlaylistIds
        matchesName && matchesTags
    }

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
    
    val listHeaderItems = 1 
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
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

    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() } }
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) { lazyListState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, initialTextFieldValue = initialTextFieldValue, allowSyncing = allowSyncing)

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50), 
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    SortHeader(
                        sortType = sortType, 
                        sortDescending = sortDescending, 
                        onSortTypeChange = onSortTypeChange, 
                        onSortDescendingChange = onSortDescendingChange, 
                        sortTypeText = { t -> when (t) { PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSortType.NAME -> R.string.sort_by_name; PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count; PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated; PlaylistSortType.CUSTOM -> R.string.sort_by_custom } }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEnterReorderMode) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp)) {
                        IconButton(onClick = { reorderEnabled = !reorderEnabled }) { Icon(painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock), null) }
                    }
                }
                FloatingActionButton(
                    onClick = { showCreatePlaylistDialog = true }, 
                    modifier = Modifier.size(40.dp), 
                    containerColor = MaterialTheme.colorScheme.primaryContainer, 
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(painterResource(R.drawable.add), null)
                }
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = { if (ytmSync) viewModel.sync() })) {
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        ) {
            item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

            if (canReorderPlaylists) {
                itemsIndexed(items = mutableVisiblePlaylists, key = { _, item -> item.id }, contentType = { _, _ -> CONTENT_TYPE_PLAYLIST }) { _, playlist ->
                    ReorderableItem(state = reorderableState, key = playlist.id) {
                        LibraryPlaylistListItem(
                            navController = navController, 
                            menuState = menuState, 
                            coroutineScope = coroutineScope, 
                            playlist = playlist, 
                            showDragHandle = true, 
                            dragHandleModifier = Modifier.draggableHandle(), 
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                }
            } else {
                items(items = visiblePlaylists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { playlist ->
                    LibraryPlaylistListItem(
                        navController = navController, 
                        menuState = menuState, 
                        coroutineScope = coroutineScope, 
                        playlist = playlist, 
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }

            if (spotifyPlaylists.isNotEmpty()) {
                item(key = "spotify_header", contentType = CONTENT_TYPE_HEADER) {
                    Text("Spotify Playlist", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp))
                }
                items(spotifyPlaylists, key = { "sp_${it.id}" }) { sp ->
                    ListItem(
                        headlineContent = { Text(sp.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${sp.tracks?.total ?: 0} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { AsyncImage(model = sp.images.firstOrNull()?.url, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) },
                        trailingContent = { Icon(painterResource(R.drawable.library_music), contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { navController.navigate("spotify_playlist/${sp.id}") }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing, 
            state = pullRefreshState, 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = contentPadding.calculateTopPadding())
        )
    }
}
