package com.j.m3play.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.PauseSearchHistoryKey
import com.j.m3play.constants.SearchFilterHeight
import com.j.m3play.db.entities.SearchHistory
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.j.m3play.innertube.models.*
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.menu.*
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    pureBlack: Boolean = false
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) { derivedStateOf { searchFilter?.value?.let { viewModel.viewStateMap[it] } } }

    val ytItemContent: @Composable LazyItemScope.(YTItem, Int, Int) -> Unit = { item: YTItem, index: Int, size: Int ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                }
            }
        }
        
        YouTubeListItem(
            item = item,
            isActive = when (item) {
                is SongItem -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                else -> false
            },
            isPlaying = isPlaying,
            trailingContent = { IconButton(onClick = longClick) { Icon(painterResource(R.drawable.more_vert), null) } },
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                            }
                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = longClick,
                ),
            index = index,
            totalSize = size
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        
        val topMargin = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + AppBarHeight
        
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = topMargin + SearchFilterHeight + 8.dp, bottom = 140.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            if (searchFilter == null) {
                searchSummary?.summaries?.forEachIndexed { index, summary ->
                    if (index > 0) item(key = "divider_$index") { HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
                    item { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) { Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(10.dp)); Text(text = summary.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) } }
                    itemsIndexed(items = summary.items, key = { _, it -> "${summary.title}/${it.id}/${summary.items.indexOf(it)}" }) { idx, item -> ytItemContent(item, idx, summary.items.size) }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (searchSummary?.summaries?.isEmpty() == true) item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
            } else {
                val filteredItems = itemsPage?.items.orEmpty().distinctBy { it.id }
                itemsIndexed(items = filteredItems, key = { _, it -> "filtered_${it.id}" }) { idx, item -> ytItemContent(item, idx, filteredItems.size) }
                if (itemsPage?.continuation != null) item(key = "loading") { ShimmerHost { repeat(3) { ListItemPlaceHolder() } } }
                if (itemsPage?.items?.isEmpty() == true) item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
            }
            if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) item { ShimmerHost { repeat(8) { ListItemPlaceHolder() } } }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topMargin) 
        ) {
            ChipsRow(
                chips = listOf(null to stringResource(R.string.filter_all), FILTER_SONG to stringResource(R.string.filter_songs), FILTER_VIDEO to stringResource(R.string.filter_videos), FILTER_ALBUM to stringResource(R.string.filter_albums), FILTER_ARTIST to stringResource(R.string.filter_artists), FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists), FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists)),
                currentValue = searchFilter,
                onValueUpdate = { if (viewModel.filter.value != it) viewModel.filter.value = it; coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
            )
        }
    }
}
