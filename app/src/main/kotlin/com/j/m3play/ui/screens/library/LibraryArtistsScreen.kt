package com.j.m3play.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.ArtistFilter
import com.j.m3play.constants.ArtistFilterKey
import com.j.m3play.constants.ArtistSortDescendingKey
import com.j.m3play.constants.ArtistSortType
import com.j.m3play.constants.ArtistSortTypeKey
import com.j.m3play.constants.ArtistViewTypeKey
import com.j.m3play.constants.CONTENT_TYPE_ARTIST
import com.j.m3play.constants.CONTENT_TYPE_HEADER
import com.j.m3play.constants.GridItemSize
import com.j.m3play.constants.GridItemsSizeKey
import com.j.m3play.constants.GridThumbnailHeight
import com.j.m3play.constants.LibraryViewType
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.LibraryArtistGridItem
import com.j.m3play.ui.component.LibraryArtistListItem
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LibraryArtistsViewModel
import com.j.m3play.extensions.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.LIST)

    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSortTypeKey, ArtistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() }
    }

    val artists by viewModel.allArtists.collectAsState()
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

    val optimizedArtists = remember(artists) { artists?.distinctBy { it.id } ?: emptyList() }

    val artistHeaderCards = @Composable {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                modifier = Modifier.weight(1f).height(120.dp).bounceClick {},
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), modifier = Modifier.size(36.dp)) {
                            Icon(painterResource(R.drawable.person), null, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("TOP ARTIST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("No Artist Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), onClick = {}) {
                            Text("Play all", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = {}) { Icon(painterResource(R.drawable.more_vert), null) }
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Artists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${optimizedArtists.size}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
                        sortTypeText = { t -> when (t) { ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date; ArtistSortType.NAME -> R.string.sort_by_name; ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count; ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time } }
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
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                    item(key = "large_title", contentType = CONTENT_TYPE_HEADER) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                            Text("Artists", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).dp))
                            Spacer(Modifier.height(4.dp))
                            Text("All your artists, in one place", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "artist_cards", contentType = CONTENT_TYPE_HEADER) { artistHeaderCards() }
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { actionRow() }
                    if (optimizedArtists.isEmpty()) item { EmptyPlaceholder(icon = R.drawable.artist, text = stringResource(R.string.library_artist_empty), modifier = Modifier.animateItem()) }
                    items(items = optimizedArtists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { artist ->
                        LibraryArtistListItem(
                            navController = navController, 
                            menuState = menuState, 
                            coroutineScope = coroutineScope, 
                            artist = artist,
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(state = lazyGridState, modifier = Modifier.fillMaxSize(), columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                    item(key = "large_title", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                            Text("Artists", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).dp))
                            Spacer(Modifier.height(4.dp))
                            Text("All your artists, in one place", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "artist_cards", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { artistHeaderCards() }
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { actionRow() }
                    if (optimizedArtists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyPlaceholder(icon = R.drawable.artist, text = stringResource(R.string.library_artist_empty)) }
                    items(items = optimizedArtists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { artist ->
                        LibraryArtistGridItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, modifier = Modifier, artist = artist)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(100.dp)) }
                }
        }
        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
    }
}
