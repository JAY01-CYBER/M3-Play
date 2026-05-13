/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
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
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.menu.*
import com.j.m3play.utils.listItemShape
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
    val focusRequester = remember { FocusRequester() }

    var isSearchFocused by remember { mutableStateOf(false) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    BackHandler(enabled = isSearchFocused) {
        isSearchFocused = false
        focusManager.clearFocus()
    }

    val encodedQuery = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
    val decodedQuery = remember(encodedQuery) { try { URLDecoder.decode(encodedQuery, "UTF-8") } catch (e: Exception) { encodedQuery } }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(decodedQuery, TextRange(decodedQuery.length))) }

    LaunchedEffect(decodedQuery) { query = TextFieldValue(decodedQuery, TextRange(decodedQuery.length)) }

    val onSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                isSearchFocused = false
                focusManager.clearFocus()
                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}") {
                    popUpTo("search/${URLEncoder.encode(decodedQuery, "UTF-8")}") { inclusive = true }
                }
                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) { database.query { insert(SearchHistory(query = searchQuery)) } }
                }
            }
        }
    }

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) { derivedStateOf { searchFilter?.value?.let { viewModel.viewStateMap[it] } } }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" } }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

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
            shape = listItemShape(index, size),
            trailingContent = { IconButton(onClick = longClick) { Icon(painterResource(R.drawable.more_vert), null) } },
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem -> {
                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                            else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        }
                        is AlbumItem -> navController.navigate("album/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                    }
                },
                onLongClick = longClick,
            ).animateItem(),
        )
    }

    // MAGICAL FIX: Bulletproof Top Padding for Status Bar
    val topPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .padding(top = topPadding) 
    ) {
        // 1. Search Bar Header (with safe padding)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_yt_music), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(painterResource(R.drawable.arrow_back), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
            trailingIcon = { if (query.text.isNotEmpty()) IconButton(onClick = { query = TextFieldValue("") }) { Icon(painterResource(R.drawable.close), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (pureBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = if (pureBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) isSearchFocused = true }
        )

        // 2. Main Content Area
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ChipsRow(
                    chips = listOf(
                        null to stringResource(R.string.filter_all),
                        FILTER_SONG to stringResource(R.string.filter_songs),
                        FILTER_VIDEO to stringResource(R.string.filter_videos),
                        FILTER_ALBUM to stringResource(R.string.filter_albums),
                        FILTER_ARTIST to stringResource(R.string.filter_artists),
                        FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                        FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                    ),
                    currentValue = searchFilter,
                    onValueUpdate = {
                        if (viewModel.filter.value != it) viewModel.filter.value = it
                        coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 120.dp), // Leaves safe space for Mini Player
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (searchFilter == null) {
                        searchSummary?.summaries?.forEach { summary ->
                            item { NavigationTitle(summary.title) }
                            itemsIndexed(summary.items, key = { index, item -> "${summary.title}/${item.id}/$index" }) { index, item ->
                                ytItemContent(item, index, summary.items.size)
                            }
                        }
                        if (searchSummary?.summaries?.isEmpty() == true) item { EmptyPlaceholder(R.drawable.search, stringResource(R.string.no_results_found)) }
                    } else {
                        itemsIndexed(itemsPage?.items.orEmpty().distinctBy { it.id }, key = { _, it -> "filtered_${it.id}" }) { index, item ->
                            ytItemContent(item, index, itemsPage?.items.orEmpty().distinctBy { it.id }.size)
                        }
                        if (itemsPage?.continuation != null) item(key = "loading") { ShimmerHost { repeat(3) { ListItemPlaceHolder() } } }
                        if (itemsPage?.items?.isEmpty() == true) item { EmptyPlaceholder(R.drawable.search, stringResource(R.string.no_results_found)) }
                    }
                    if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) item { ShimmerHost { repeat(8) { ListItemPlaceHolder() } } }
                }
            }

            // 3. Search Suggestions Layer
            if (isSearchFocused) {
                OnlineSearchScreen(
                    query = query.text,
                    onQueryChange = { query = it },
                    navController = navController,
                    onSearch = onSearch,
                    onDismiss = { isSearchFocused = false; focusManager.clearFocus() },
                    pureBlack = pureBlack
                )
            }
        }
    }
}
