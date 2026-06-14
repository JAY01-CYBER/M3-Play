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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.j.m3play.constants.CONTENT_TYPE_ALBUM
import com.j.m3play.constants.CONTENT_TYPE_HEADER
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.EmptyPlaceholder
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
    onDeselect: () -> Unit,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val filterContent = @Composable {
        Row {
            Spacer(Modifier.width(12.dp))
            FilterChip(
                label = { Text(stringResource(R.string.albums)) },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = onDeselect,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(painterResource(R.drawable.close), null) },
            )
            ChipsRow(
                chips = listOf(AlbumFilter.LIKED to stringResource(R.string.filter_liked), AlbumFilter.LIBRARY to stringResource(R.string.filter_library), AlbumFilter.DOWNLOADED to stringResource(R.string.filter_downloaded), AlbumFilter.DOWNLOADED_FULL to stringResource(R.string.filter_downloaded_full)),
                currentValue = filter,
                onValueUpdate = { filter = it },
                modifier = Modifier.weight(1f),
            )
        }
    }

    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() } }

    val albums by viewModel.allAlbums.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { lazyListState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }

    val optimizedAlbums = remember(albums, hideExplicit) { (if (hideExplicit) albums.filter { !it.album.explicit } else albums).distinctBy { it.id } }

    val headerContent = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.height(40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { t -> when (t) { AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date; AlbumSortType.NAME -> R.string.sort_by_name; AlbumSortType.ARTIST -> R.string.sort_by_artist; AlbumSortType.YEAR -> R.string.sort_by_year; AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count; AlbumSortType.LENGTH -> R.string.sort_by_length; AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time } })
                }
            }
            Spacer(Modifier.weight(1f))
            Text(text = pluralStringResource(R.plurals.n_album, optimizedAlbums.size, optimizedAlbums.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }

    Box(modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = { if (ytmSync) viewModel.refresh(filter) })) {
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            item(key = "large_title", contentType = CONTENT_TYPE_HEADER) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Albums", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                    Text("All your albums", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
            item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

            if (optimizedAlbums.isEmpty()) {
                item { EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.library_album_empty), modifier = Modifier.animateItem()) }
            }

            items(items = optimizedAlbums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { album ->
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                    LibraryAlbumListItem(navController = navController, menuState = menuState, album = album, isActive = album.id == mediaMetadata?.album?.id, isPlaying = isPlaying, modifier = Modifier.animateItem())
                }
            }
        }
        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
    }
}
