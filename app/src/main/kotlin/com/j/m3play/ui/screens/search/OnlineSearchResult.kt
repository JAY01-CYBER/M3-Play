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
import androidx.compose.ui.graphics.StrokeCap
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
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
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
            isActive = when (item) {
                is SongItem -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                else -> false
            },
            isPlaying = isPlaying,
            trailingContent = { IconButton(onClick = longClick) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) } },
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
        modifier = Modifier.fillMaxSize().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + AppBarHeight + SearchFilterHeight + 8.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        ) {
            if (searchFilter == null) {
                searchSummary?.summaries?.forEachIndexed { index, summary ->
                    if (index > 0) {
                        item(key = "divider_$index") {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                            Spacer(Modifier.width(10.dp))
                            Text(text = summary.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (summary.title.equals("Top result", ignoreCase = true) && summary.items.isNotEmpty()) {
                        val topItem = summary.items.first()
                        item(key = "top_result_card_${topItem.id}") {
                            PremiumTopResultCard(
                                item = topItem,
                                pureBlack = pureBlack,
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
                            item { Text(text = "MORE FROM YOUTUBE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)) }
                            items(items = summary.items.drop(1), key = { "more_from_yt_${it.id}" }, itemContent = ytItemContent)
                        }
                    } else {
                        items(items = summary.items, key = { "${summary.title}/${it.id}/${summary.items.indexOf(it)}" }, itemContent = ytItemContent)
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (searchSummary?.summaries?.isEmpty() == true) {
                    item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
                }
            } else {
                items(items = itemsPage?.items.orEmpty().distinctBy { it.id }, key = { "filtered_${it.id}" }, itemContent = ytItemContent)
                
                if (itemsPage?.continuation != null) {
                    // NEW: Wavy Circular Progress Loading instead of Shimmer
                    item(key = "loading") { 
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeCap = StrokeCap.Round) 
                        }
                    }
                }
                if (itemsPage?.items?.isEmpty() == true) {
                    item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
                }
            }

            if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeCap = StrokeCap.Round) 
                    } 
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = AppBarHeight).fillMaxWidth()
        ) {
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
                icons = mapOf(
                    null to R.drawable.search,
                    FILTER_SONG to R.drawable.music_note,
                    FILTER_VIDEO to R.drawable.slow_motion_video,
                    FILTER_ALBUM to R.drawable.album,
                    FILTER_ARTIST to R.drawable.person,
                    FILTER_COMMUNITY_PLAYLIST to R.drawable.queue_music,
                    FILTER_FEATURED_PLAYLIST to R.drawable.playlist_play,
                ),
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface).statusBarsPadding().height(AppBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Box(
                modifier = Modifier.weight(1f).padding(end = 16.dp, top = 8.dp, bottom = 8.dp).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (pureBlack) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant).clickable { navController.popBackStack() }, 
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = stringResource(R.string.search_yt_music), modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// --- Premium Top Result Card (Theme Dependent Colors) ---
@Composable
fun PremiumTopResultCard(
    item: YTItem,
    pureBlack: Boolean,
    onPlayClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val subtitleText = when (item) {
        is SongItem -> "Song • ${item.artists.joinToString { it.name }}"
        is ArtistItem -> "Artist"
        is AlbumItem -> "Album • ${item.artists?.joinToString { it.name } ?: ""}"
        is PlaylistItem -> "Playlist • ${item.author?.name ?: ""}"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pureBlack) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(if (item is ArtistItem) CircleShape else RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitleText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                
                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp).padding(top = 4.dp)) {
                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Row(
                    modifier = Modifier.weight(1f).clickable { /* Save Logic */ }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save", fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
