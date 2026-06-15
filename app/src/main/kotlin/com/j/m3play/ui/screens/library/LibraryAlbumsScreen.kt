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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AlbumFilter
import com.j.m3play.constants.AlbumFilterKey
import com.j.m3play.constants.AlbumSortDescendingKey
import com.j.m3play.constants.AlbumSortType
import com.j.m3play.constants.AlbumSortTypeKey
import com.j.m3play.constants.AlbumViewTypeKey
import com.j.m3play.constants.CONTENT_TYPE_ALBUM
import com.j.m3play.constants.CONTENT_TYPE_HEADER
import com.j.m3play.constants.GridItemSize
import com.j.m3play.constants.GridItemsSizeKey
import com.j.m3play.constants.GridThumbnailHeight
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.LibraryViewType
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.LibraryAlbumGridItem
import com.j.m3play.ui.component.LibraryAlbumListItem
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LibraryAlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.LIST)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    LaunchedEffect(Unit) {
        if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() }
    }

    val albums by viewModel.allAlbums.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val pullRefreshState = rememberPullToRefreshState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val optimizedAlbums = remember(albums, hideExplicit) {
        val list = if (hideExplicit) albums.filter { !it.album.explicit } else albums
        list.distinctBy { it.id }
    }

    val actionRow = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.wrapContentHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    SortHeader(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { t -> when (t) { AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date; AlbumSortType.NAME -> R.string.sort_by_name; AlbumSortType.ARTIST -> R.string.sort_by_artist; AlbumSortType.YEAR -> R.string.sort_by_year; AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count; AlbumSortType.LENGTH -> R.string.sort_by_length; AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { viewType = viewType.toggle() }, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(when (viewType) { LibraryViewType.LIST -> R.drawable.list; LibraryViewType.GRID -> R.drawable.grid_view }), null, modifier = Modifier.padding(10.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = { if (ytmSync) viewModel.refresh(filter) })) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState, 
                    modifier = Modifier.fillMaxSize(), 
                    contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
                ) {
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { actionRow() }
                    if (optimizedAlbums.isEmpty()) item { EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.library_album_empty), modifier = Modifier.animateItem()) }
                    items(items = optimizedAlbums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { album ->
                        LibraryAlbumListItem(
                            navController = navController, 
                            menuState = menuState, 
                            album = album, 
                            isActive = album.id == mediaMetadata?.album?.id, 
                            isPlaying = isPlaying, 
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState, 
                    modifier = Modifier.fillMaxSize(), 
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), 
                    contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
                ) {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { actionRow() }
                    if (optimizedAlbums.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.library_album_empty)) }
                    items(items = optimizedAlbums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { album ->
                        LibraryAlbumGridItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, album = album, isActive = album.id == mediaMetadata?.album?.id, isPlaying = isPlaying, modifier = Modifier)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(100.dp)) }
                }
        }
        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
    }
}
