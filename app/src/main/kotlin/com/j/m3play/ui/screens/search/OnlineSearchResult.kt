package com.j.m3play.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.j.m3play.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.SearchFilterHeight
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.menu.YouTubeArtistMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    pureBlack: Boolean,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf { searchFilter?.value?.let { viewModel.viewStateMap[it] } }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" } }
            .collect { shouldLoadMore -> if (shouldLoadMore) viewModel.loadMore() }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
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
            isActive = when (item) { is SongItem -> mediaMetadata?.id == item.id; is AlbumItem -> mediaMetadata?.album?.id == item.id; else -> false },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(onClick = longClick) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White) }
            },
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
                )
                .animateItem(),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(if (pureBlack) Color.Black else CustomBgColor)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + AppBarHeight + 60.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        ) {
            if (searchFilter == null) {
                searchSummary?.summaries?.forEachIndexed { index, summary ->
                    // Section Title
                    item {
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(start = 16.dp, top = if (index == 0) 16.dp else 24.dp, bottom = 12.dp)
                        )
                    }

                    // Premium Top Result Card Logic
                    if (summary.title.equals("Top result", ignoreCase = true) && summary.items.isNotEmpty()) {
                        val topItem = summary.items.first()
                        item(key = "top_result_card_${topItem.id}") {
                            PremiumTopResultCard(
                                item = topItem,
                                onPlayClick = {
                                    if (topItem is SongItem) {
                                        if (topItem.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = topItem.id), topItem.toMediaMetadata()))
                                    }
                                },
                                onMenuClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (topItem) {
                                            is SongItem -> YouTubeSongMenu(song = topItem, navController = navController, onDismiss = menuState::dismiss)
                                            is AlbumItem -> YouTubeAlbumMenu(albumItem = topItem, navController = navController, onDismiss = menuState::dismiss)
                                            is ArtistItem -> YouTubeArtistMenu(artist = topItem, onDismiss = menuState::dismiss)
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = topItem, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                        }
                                    }
                                }
                            )
                        }

                        if (summary.items.size > 1) {
                            item {
                                Text(
                                    text = "More from YouTube",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                                )
                            }
                            items(items = summary.items.drop(1), key = { "more_from_yt_${it.id}" }, itemContent = ytItemContent)
                            
                            // "See all results >" Button (As in screenshot)
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(CustomSurfaceColor)
                                        .clickable { /* Navigate to full results */ }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "See all results  >", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                }
                            }
                        }
                    } else {
                        items(items = summary.items, key = { "${summary.title}/${it.id}/${summary.items.indexOf(it)}" }, itemContent = ytItemContent)
                    }
                }

                if (searchSummary?.summaries?.isEmpty() == true) {
                    item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
                }
            } else {
                // Filtered Items View
                items(items = itemsPage?.items.orEmpty().distinctBy { it.id }, key = { "filtered_${it.id}" }, itemContent = ytItemContent)
                if (itemsPage?.continuation != null) { item(key = "loading") { ShimmerHost { repeat(3) { ListItemPlaceHolder() } } } }
                if (itemsPage?.items?.isEmpty() == true) { item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) } }
            }

            if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                item { ShimmerHost { repeat(8) { ListItemPlaceHolder() } } }
            }
        }

        // Custom Chips Row Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = AppBarHeight)
                .fillMaxWidth()
                .background(if (pureBlack) Color.Black else CustomBgColor)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "All",
                    FILTER_SONG to "Songs",
                    FILTER_VIDEO to "Videos",
                    FILTER_ALBUM to "Albums"
                )
                
                items(filters.size) { index ->
                    val filter = filters[index]
                    val isSelected = searchFilter == filter.first
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) CustomAccentColor else CustomSurfaceColor)
                            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .clickable {
                                if (viewModel.filter.value != filter.first) viewModel.filter.value = filter.first
                                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Optional Icons can be added here like in the screenshot
                        Text(
                            text = filter.second,
                            color = if (isSelected) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Search Bar Top Layout
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(if (pureBlack) Color.Black else CustomBgColor)
                .statusBarsPadding()
                .height(AppBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(CustomSurfaceColor)
                    .clickable { navController.popBackStack() }, 
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = viewModel.query.value, color = Color.White, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                    Icon(painter = painterResource(R.drawable.close), contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// --- EXACT Screenshot Match Premium Top Result Card ---
@Composable
fun PremiumTopResultCard(
    item: YTItem,
    onPlayClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val subtitleText = when (item) {
        is SongItem -> "Song • ${item.artists.joinToString { it.name }}\nAlbum • ${item.album?.name ?: "Unknown"}"
        is ArtistItem -> "Artist"
        is AlbumItem -> "Album • ${item.artists?.joinToString { it.name } ?: ""}"
        is PlaylistItem -> "Playlist • ${item.author?.name ?: ""}"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CustomSurfaceColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(if (item is ArtistItem) CircleShape else RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitleText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                
                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp).padding(top = 4.dp)) {
                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = "Menu", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons matching the screenshot exactly
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                
                // Main PLAY Button (Peach color)
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CustomAccentColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // SAVE Button (Transparent)
                Row(
                    modifier = Modifier.weight(1f).clickable { /* Save Logic */ }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save", fontWeight = FontWeight.Normal, color = Color.White, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // DOWNLOAD Button (Circle outline)
                Box(
                    modifier = Modifier.size(40.dp).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable { /* Download Logic */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.download), contentDescription = "Download", tint = Color.White, modifier = Modifier.size(20.dp)) // Ensure you have a download icon
                }
            }
        }
    }
}
