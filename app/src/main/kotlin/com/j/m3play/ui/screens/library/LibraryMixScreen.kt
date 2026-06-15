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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.CONTENT_TYPE_HEADER
import com.j.m3play.constants.CONTENT_TYPE_PLAYLIST
import com.j.m3play.constants.MixSortDescendingKey
import com.j.m3play.constants.MixSortType
import com.j.m3play.constants.MixSortTypeKey
import com.j.m3play.constants.PlaylistSortType
import com.j.m3play.constants.PlaylistSortTypeKey
import com.j.m3play.constants.PlaylistTagsFilterKey
import com.j.m3play.constants.ShowLikedPlaylistKey
import com.j.m3play.constants.ShowDownloadedPlaylistKey
import com.j.m3play.constants.ShowTopPlaylistKey
import com.j.m3play.constants.ShowCachedPlaylistKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.Playlist
import com.j.m3play.extensions.move
import com.j.m3play.ui.component.AlbumListItem
import com.j.m3play.ui.component.ArtistListItem
import com.j.m3play.ui.component.LibraryPlaylistListItem
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.AlbumMenu
import com.j.m3play.ui.menu.ArtistMenu
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    contentPadding: PaddingValues,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val (playlistSortType) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) { selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet() }
    val database = LocalDatabase.current
    val filteredPlaylistIds by database.playlistIdsByTags(if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()).collectAsState(initial = emptyList())

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val albums by viewModel.albums.collectAsState()
    val artist by viewModel.artists.collectAsState()
    val playlist by viewModel.playlists.collectAsState()

    val collator = remember { Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY } }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val visiblePlaylists = remember(playlist, selectedTagIds, filteredPlaylistIds) { if (selectedTagIds.isEmpty()) playlist else playlist.filter { it.id in filteredPlaylistIds } }
    val otherItems = remember(albums, artist) { albums + artist }
    val sortedOtherItems = remember(otherItems, sortType, sortDescending) {
        when (sortType) {
            MixSortType.CREATE_DATE -> otherItems.sortedBy { when (it) { is Album -> it.album.bookmarkedAt; is Artist -> it.artist.bookmarkedAt; else -> null } }
            MixSortType.NAME -> otherItems.sortedWith(compareBy(collator) { when (it) { is Album -> it.album.title; is Artist -> it.artist.name; else -> "" } })
            MixSortType.LAST_UPDATED -> otherItems.sortedBy { when (it) { is Album -> it.album.lastUpdateTime; is Artist -> it.artist.lastUpdateTime; else -> null } }
        }.let { if (sortDescending) it.asReversed() else it }
    }

    val customPlaylistMode = playlistSortType == PlaylistSortType.CUSTOM
    val canEnterReorderMode = customPlaylistMode && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val listHeaderItems = 2
            
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
        if (!canReorderPlaylists || (!reorderableState.isAnyItemDragging && dragInfo == null)) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging, canReorderPlaylists) {
        if (!canReorderPlaylists || reorderableState.isAnyItemDragging) return@LaunchedEffect
        dragInfo ?: return@LaunchedEffect
        val playlistsToReorder = mutableVisiblePlaylists.toList()
        database.transaction { playlistsToReorder.forEachIndexed { index, p -> setPlaylistCustomOrder(p.id, index) } }
        dragInfo = null
    }

    LaunchedEffect(canEnterReorderMode) { if (!canEnterReorderMode) reorderEnabled = false }

    val allItems = remember(customPlaylistMode, visiblePlaylists, sortedOtherItems, albums, artist, sortType, sortDescending) {
        if (customPlaylistMode) (visiblePlaylists + sortedOtherItems).distinctBy { it.id } else {
            val combined = (albums + artist + visiblePlaylists).distinctBy { it.id }
            when (sortType) {
                MixSortType.CREATE_DATE -> combined.sortedBy { when (it) { is Album -> it.album.bookmarkedAt; is Artist -> it.artist.bookmarkedAt; is Playlist -> it.playlist.createdAt; else -> null } }
                MixSortType.NAME -> combined.sortedWith(compareBy(collator) { when (it) { is Album -> it.album.title; is Artist -> it.artist.name; is Playlist -> it.playlist.name; else -> "" } })
                MixSortType.LAST_UPDATED -> combined.sortedBy { when (it) { is Album -> it.album.lastUpdateTime; is Artist -> it.artist.lastUpdateTime; is Playlist -> it.playlist.lastUpdateTime; else -> null } }
            }.let { if (sortDescending) it.asReversed() else it }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { lazyListState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { viewModel.syncAllLibrary() } }

    val headerContent = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Recently Played", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.heightIn(min = 36.dp)) { 
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { type -> when (type) { MixSortType.CREATE_DATE -> R.string.sort_by_create_date; MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated; MixSortType.NAME -> R.string.sort_by_name } })
                }
            }
            if (canEnterReorderMode) {
                 Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 8.dp).size(36.dp)) {
                    IconButton(onClick = { reorderEnabled = !reorderEnabled }) { Icon(painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock), null, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        ) {
            item(key = "auto_playlists", contentType = CONTENT_TYPE_HEADER) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // EXACT VIDEO REPLICA: Favorite Songs Wide Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        onClick = { navController.navigate("auto_playlist/liked") }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("MOST PLAYED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Favorite Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Auto playlist", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, onClick = { /* Play action */ }) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play all", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, onClick = { /* Shuffle action */ }) {
                                    Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // EXACT VIDEO REPLICA: 2x2 Grid Small Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Liked songs
                        Surface(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = { navController.navigate("auto_playlist/liked") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFFE57373).copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.padding(6.dp))
                                }
                                Column {
                                    Text("Liked songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Auto playlist", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        // Offline
                        Surface(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = { navController.navigate("auto_playlist/downloaded") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF64B5F6).copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.padding(6.dp))
                                }
                                Column {
                                    Text("Offline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Downloaded", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cached
                        Surface(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = { navController.navigate("cache_playlist/cached") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFFBA68C8).copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.padding(6.dp))
                                }
                                Column {
                                    Text("Cached", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Instant playback", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        // Local Files
                        Surface(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = { navController.navigate("top_playlist/$topSize") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF81C784).copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.padding(6.dp))
                                }
                                Column {
                                    Text("Local Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("On device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

            if (customPlaylistMode) {
                if (canReorderPlaylists) {
                    itemsIndexed(items = mutableVisiblePlaylists, key = { _, item -> item.id }, contentType = { _, _ -> CONTENT_TYPE_PLAYLIST }) { _, item ->
                        ReorderableItem(state = reorderableState, key = item.id) {
                            LibraryPlaylistListItem(
                                navController = navController, 
                                menuState = menuState, 
                                coroutineScope = coroutineScope, 
                                playlist = item, 
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
                    items(items = visiblePlaylists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                        LibraryPlaylistListItem(
                            navController = navController, 
                            menuState = menuState, 
                            coroutineScope = coroutineScope, 
                            playlist = item, 
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                }

                items(items = sortedOtherItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                    when (item) {
                        is Artist -> { 
                            ArtistListItem(
                                artist = item, 
                                trailingContent = { IconButton(onClick = { menuState.show { ArtistMenu(item, coroutineScope, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, 
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .combinedClickable(onClick = { navController.navigate("artist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(item, coroutineScope, menuState::dismiss) } })
                            ) 
                        }
                        is Album -> { 
                            AlbumListItem(
                                album = item, 
                                isActive = item.id == mediaMetadata?.album?.id, 
                                isPlaying = isPlaying, 
                                trailingContent = { IconButton(onClick = { menuState.show { AlbumMenu(item, navController, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, 
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .combinedClickable(onClick = { navController.navigate("album/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(item, navController, menuState::dismiss) } })
                            ) 
                        }
                        else -> {}
                    }
                }
            } else {
                items(items = allItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                    when (item) {
                        is Playlist -> { 
                            LibraryPlaylistListItem(
                                navController = navController, 
                                menuState = menuState, 
                                coroutineScope = coroutineScope, 
                                playlist = item, 
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) 
                        }
                        is Artist -> { 
                            ArtistListItem(
                                artist = item, 
                                trailingContent = { IconButton(onClick = { menuState.show { ArtistMenu(item, coroutineScope, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, 
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .combinedClickable(onClick = { navController.navigate("artist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(item, coroutineScope, menuState::dismiss) } })
                            ) 
                        }
                        is Album -> { 
                            AlbumListItem(
                                album = item, 
                                isActive = item.id == mediaMetadata?.album?.id, 
                                isPlaying = isPlaying, 
                                trailingContent = { IconButton(onClick = { menuState.show { AlbumMenu(item, navController, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, 
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .combinedClickable(onClick = { navController.navigate("album/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(item, navController, menuState::dismiss) } })
                            ) 
                        }
                        else -> {}
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
