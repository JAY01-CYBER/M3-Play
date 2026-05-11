/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  YT Music Premium Home Components          │
 * │  Signature: M3PLAY::UI::YTM_PREMIUM_V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.j.m3play.LocalDatabase
import com.j.m3play.R
import com.j.m3play.constants.GridThumbnailHeight
import com.j.m3play.constants.ListItemHeight
import com.j.m3play.constants.ListThumbnailSize
import com.j.m3play.constants.ThumbnailCornerRadius
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.LocalItem
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.EpisodeItem
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.PodcastItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.pages.HomePage
import com.j.m3play.models.MediaMetadata
import com.j.m3play.models.SimilarRecommendation
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.AlbumGridItem
import com.j.m3play.ui.component.ArtistGridItem
import com.j.m3play.ui.component.MenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.SongGridItem
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.GridItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.AlbumMenu
import com.j.m3play.ui.menu.ArtistMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.menu.YouTubeArtistMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.viewmodels.CommunityPlaylistItem
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

// ==========================================
// YT MUSIC PREMIUM CAROUSEL (For Discover/Mixes)
// ==========================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YTPremiumDiscoverCard(
    item: YTItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w544-h544"))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.9f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    
                    val subtitle = when(item) {
                        is SongItem -> item.artists.joinToString(", ") { it.name }
                        is PlaylistItem -> item.author?.name ?: "Auto Playlist"
                        else -> "YouTube Music"
                    }

                    Text(
                        text = "Because you liked $subtitle",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ==========================================
// GLOSSY / METROLIST COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    quickPicks: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    horizontalLazyGridItemWidth: Dp,
    lazyGridState: LazyGridState,
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val distinctQuickPicks = remember(quickPicks) { quickPicks.distinctBy { it.id } }

    LazyHorizontalGrid(
        state = lazyGridState,
        rows = GridCells.Fixed(4),
        flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight * 4)
    ) {
        items(
            items = distinctQuickPicks,
            key = { "home_quickpick_${it.id}" }
        ) { song ->
            SongListItem(
                song = song,
                showInLibraryIcon = true,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                isSwipeable = false,
                trailingContent = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                },
                modifier = Modifier
                    .width(horizontalLazyGridItemWidth)
                    .combinedClickable(
                        onClick = {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                            }
                        }
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialSection(
    speedDialSongs: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distinctSpeedDial = remember(speedDialSongs) { speedDialSongs.distinctBy { it.id }.take(24) }
    val speedDialIndexById = remember(distinctSpeedDial) { distinctSpeedDial.mapIndexed { index, song -> song.id to index }.toMap() }
    val tileSize = 130.dp
    val spacing = 10.dp
    val state = rememberLazyGridState()
    val rowCount = min(3, distinctSpeedDial.size + 1)
    val gridHeight = (tileSize * rowCount) + (spacing * (rowCount - 1))

    fun playSpeedDialQueue(startIndex: Int) {
        if (distinctSpeedDial.isEmpty()) return
        playerConnection.playQueue(
            ListQueue(
                title = context.getString(R.string.speed_dial),
                items = distinctSpeedDial.map { it.toMediaItem() },
                startIndex = startIndex,
            )
        )
    }

    val dotState by remember(state, distinctSpeedDial.size) {
        derivedStateOf {
            val songsPerDot = 8
            val totalSongs = distinctSpeedDial.size
            if (totalSongs <= 0) {
                Triple(0, 0, 0)
            } else {
                val pages = ceil(totalSongs / songsPerDot.toFloat()).toInt().coerceAtLeast(1)
                val visibleSongIndex = state.firstVisibleItemIndex.coerceIn(0, (totalSongs - 1).coerceAtLeast(0))
                val currentPage = (visibleSongIndex / songsPerDot).coerceIn(0, pages - 1)
                val dots = min(3, pages)
                val selectedDot = if (pages <= 3) currentPage else ((currentPage.toFloat() / (pages - 1).coerceAtLeast(1)) * (dots - 1)).toInt().coerceIn(0, dots - 1)
                Triple(dots, selectedDot, pages)
            }
        }
    }

    val (dotsCount, selectedDotIndex) = dotState.let { (dots, selected, _) -> dots to selected }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyHorizontalGrid(
            state = state,
            rows = GridCells.Fixed(rowCount),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            modifier = Modifier.fillMaxWidth().height(gridHeight),
        ) {
            items(items = distinctSpeedDial, key = { it.id }, contentType = { "speed_dial_song" }) { song ->
                val songIndex = speedDialIndexById[song.id] ?: 0
                val isActive = song.id == mediaMetadata?.id

                Box(
                    modifier = Modifier
                        .width(tileSize)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = { if (isActive) playerConnection.player.togglePlayPause() else playSpeedDialQueue(songIndex) },
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }
                        )
                ) {
                    AsyncImage(model = song.song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                    Text(text = song.song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp))
                    if (isActive && isPlaying) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                            Icon(painter = painterResource(R.drawable.volume_up), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp).size(16.dp))
                        }
                    }
                }
            }

            item(key = "speed_dial_random", contentType = "speed_dial_random") {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(tileSize).aspectRatio(1f).combinedClickable(
                            onClick = { if (distinctSpeedDial.isNotEmpty()) playSpeedDialQueue(Random.nextInt(distinctSpeedDial.size)) },
                            onLongClick = {}
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(painter = painterResource(R.drawable.casino), contentDescription = stringResource(R.string.speed_dial_random), tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        if (dotsCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                repeat(dotsCount) { index ->
                    val isSelected = index == selectedDotIndex
                    Surface(color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), shape = CircleShape, modifier = Modifier.size(if (isSelected) 8.dp else 6.dp)) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepListeningSection(
    keepListening: List<LocalItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val rows = if (keepListening.size > 6) 2 else 1
    val gridHeight = (GridThumbnailHeight + with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 + MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
    }) * rows

    LazyHorizontalGrid(
        state = rememberLazyGridState(),
        rows = GridCells.Fixed(rows),
        modifier = modifier.fillMaxWidth().height(gridHeight)
    ) {
        items(
            items = keepListening,
            key = { item -> 
                when (item) {
                    is Song -> "song_${item.id}"
                    is Album -> "album_${item.id}"
                    is Artist -> "artist_${item.id}"
                    is Playlist -> "playlist_${item.id}"
                }
            }
        ) { item ->
            LocalGridItem(item = item, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForgottenFavoritesSection(
    forgottenFavorites: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    horizontalLazyGridItemWidth: Dp,
    lazyGridState: LazyGridState,
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val rows = min(4, forgottenFavorites.size)
    val distinctForgottenFavorites = remember(forgottenFavorites) { forgottenFavorites.distinctBy { it.id } }
    
    LazyHorizontalGrid(
        state = lazyGridState,
        rows = GridCells.Fixed(rows),
        flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
        modifier = modifier.fillMaxWidth().height(ListItemHeight * rows)
    ) {
        items(items = distinctForgottenFavorites, key = { it.id }) { song ->
            SongListItem(
                song = song, showInLibraryIcon = true, isActive = song.id == mediaMetadata?.id, isPlaying = isPlaying, isSwipeable = false,
                trailingContent = {
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                },
                modifier = Modifier.width(horizontalLazyGridItemWidth).combinedClickable(
                        onClick = { if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) },
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountPlaylistsSection(
    accountPlaylists: List<PlaylistItem>,
    accountName: String,
    accountImageUrl: String?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val distinctPlaylists = remember(accountPlaylists) { accountPlaylists.distinctBy { it.id } }
    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = modifier) {
        items(items = distinctPlaylists, key = { it.id }) { item ->
            YouTubeGridItemWrapper(item = item, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimilarRecommendationsSection(
    recommendation: SimilarRecommendation,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = modifier) {
        items(items = recommendation.items, key = { it.id }) { item ->
            YouTubeGridItemWrapper(item = item, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
        }
    }
}

@Composable
fun HomeLoadingShimmer(modifier: Modifier = Modifier) {
    ShimmerHost(modifier = modifier) {
        TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp))
        LazyRow { items(4) { GridItemPlaceHolder() } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YouTubeGridItemWrapper(
    item: YTItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    com.j.m3play.ui.component.YouTubeGridItem(
        item = item,
        isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
        isPlaying = isPlaying,
        coroutineScope = scope,
        thumbnailRatio = 1f,
        modifier = modifier.combinedClickable(
            onClick = {
                when (item) {
                    is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                    is AlbumItem -> navController.navigate("album/${item.id}")
                    is ArtistItem -> navController.navigate("artist/${item.id}")
                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                    is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                    is EpisodeItem -> playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem())))
                }
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menuState.show {
                    when (item) {
                        is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                        is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                        is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                        is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                        is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = menuState::dismiss)
                        is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                    }
                }
            }
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalGridItem(
    item: LocalItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    when (item) {
        is Song -> SongGridItem(
            song = item,
            modifier = modifier.fillMaxWidth().combinedClickable(
                    onClick = { if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())) },
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = item, navController = navController, onDismiss = menuState::dismiss) } }
                ),
            isActive = item.id == mediaMetadata?.id, isPlaying = isPlaying
        )
        is Album -> AlbumGridItem(
            album = item, isActive = item.id == mediaMetadata?.album?.id, isPlaying = isPlaying, coroutineScope = scope,
            modifier = modifier.fillMaxWidth().combinedClickable(
                    onClick = { navController.navigate("album/${item.id}") },
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) } }
                )
        )
        is Artist -> ArtistGridItem(
            artist = item,
            modifier = modifier.fillMaxWidth().combinedClickable(
                    onClick = { navController.navigate("artist/${item.id}") },
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(originalArtist = item, coroutineScope = scope, onDismiss = menuState::dismiss) } }
                )
        )
        is Playlist -> { /* Not displayed */ }
    }
}

@Composable
fun AccountPlaylistsTitle(accountName: String, accountImageUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NavigationTitle(
        label = stringResource(R.string.your_youtube_playlists), title = accountName,
        thumbnail = {
            if (accountImageUrl != null) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(accountImageUrl).diskCachePolicy(CachePolicy.ENABLED).diskCacheKey(accountImageUrl).crossfade(true).build(), placeholder = painterResource(id = R.drawable.person), error = painterResource(id = R.drawable.person), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(ListThumbnailSize).clip(CircleShape))
            } else { Icon(painter = painterResource(id = R.drawable.person), contentDescription = null, modifier = Modifier.size(ListThumbnailSize)) }
        },
        onClick = onClick, modifier = modifier
    )
}

@Composable
fun SimilarRecommendationsTitle(recommendation: SimilarRecommendation, navController: NavController, modifier: Modifier = Modifier) {
    NavigationTitle(
        label = stringResource(R.string.similar_to), title = recommendation.title.title,
        thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl -> {
                val shape = if (recommendation.title is Artist) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
                AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(shape))
            }
        },
        onClick = {
            when (recommendation.title) {
                is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                is Album -> navController.navigate("album/${recommendation.title.id}")
                is Artist -> navController.navigate("artist/${recommendation.title.id}")
                is Playlist -> {}
            }
        }, modifier = modifier
    )
}

@Composable
fun HomePageSectionTitle(section: HomePage.Section, navController: NavController, modifier: Modifier = Modifier) {
    NavigationTitle(
        title = section.title, label = section.label,
        thumbnail = section.thumbnail?.let { thumbnailUrl -> {
                val shape = if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
                AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(shape))
            }
        },
        onClick = section.endpoint?.browseId?.let { browseId -> { if (browseId == "FEmusic_moods_and_genres") navController.navigate("mood_and_genres") else navController.navigate("browse/$browseId") } },
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.AccountPlaylistsContainer(viewModel: HomeViewModel, accountName: String?, accountImageUrl: String?, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, scope: CoroutineScope) {
    item {
        val accountPlaylists by viewModel.accountPlaylists.collectAsState()
        val currentPlaylists = accountPlaylists
        if (!currentPlaylists.isNullOrEmpty()) {
            Column {
                AccountPlaylistsTitle(accountName = accountName ?: "", accountImageUrl = accountImageUrl, onClick = { navController.navigate("account") }, modifier = Modifier)
                AccountPlaylistsSection(accountPlaylists = currentPlaylists, accountName = accountName ?: "", accountImageUrl = accountImageUrl, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.SimilarRecommendationsContainer(viewModel: HomeViewModel, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, scope: CoroutineScope) {
     item {
        val similarRecommendations by viewModel.similarRecommendations.collectAsState()
        Column {
            similarRecommendations?.forEach { recommendation ->
                SimilarRecommendationsTitle(recommendation = recommendation, navController = navController, modifier = Modifier)
                SimilarRecommendationsSection(recommendation = recommendation, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
            }
        }
    }
}

@Composable
fun CommunityPlaylistsSection(playlists: List<CommunityPlaylistItem>, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(playlists, key = { it.playlist.id }) { item ->
            CommunityPlaylistCard(
                item = item, onClick = { navController.navigate("online_playlist/${item.playlist.id}") },
                onSongClick = { song -> playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())) }, modifier = Modifier
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroSpeedDialSection(items: List<YTItem>, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val viewModel: HomeViewModel = hiltViewModel()
    val isRandomizing by viewModel.isRandomizing.collectAsState()
    val pinnedEntries by database.speedDialDao.getAll().collectAsState(initial = emptyList())
    val distinctItems = remember(items) { items.distinctBy { it.id }.take(26) }
    val columns = 3; val rows = 3; val slotsPerPage = columns * rows; val firstPageContentSlots = slotsPerPage - 1
    val pagerCount = remember(distinctItems.size) { when { distinctItems.isEmpty() -> 1; distinctItems.size <= firstPageContentSlots -> 1; else -> 1 + ceil((distinctItems.size - firstPageContentSlots) / slotsPerPage.toFloat()).toInt() } }
    val pagerState = rememberPagerState(pageCount = { pagerCount })
    val coroutineScope = rememberCoroutineScope()

    fun openItem(item: YTItem) {
        when (item) {
            is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
            is AlbumItem -> navController.navigate("album/${item.id}")
            is ArtistItem -> navController.navigate("artist/${item.id}")
            is PlaylistItem -> { val rawType = pinnedEntries.find { it.id == item.id }?.type; if (rawType == "LOCAL_PLAYLIST") navController.navigate("local_playlist/${item.id}") else navController.navigate("online_playlist/${item.id}") }
            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
            is EpisodeItem -> playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem())))
        }
    }

    fun showItemMenu(item: YTItem) {
        menuState.show {
            when (item) {
                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
            }
        }
    }

    @Composable
    fun SpeedDialPoster(item: YTItem) {
        val isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
        val isPinned by database.speedDialDao.isPinned(item.id).collectAsState(initial = false)

        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = { if (item is SongItem && isActive) playerConnection.player.togglePlayPause() else openItem(item) }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showItemMenu(item) })) {
            AsyncImage(model = item.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.92f)))))
            Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (item !is SongItem) Icon(painter = painterResource(R.drawable.navigate_next), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            if (isPinned) { Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.28f), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(painter = painterResource(R.drawable.bookmark_filled), contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp).size(12.dp)) } } 
            else if (isActive && isPlaying && item is SongItem) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(painter = painterResource(R.drawable.volume_up), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp).size(12.dp)) } }
        }
    }

    @Composable
    fun RandomTile() {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxSize().combinedClickable(onClick = { if (!isRandomizing) { coroutineScope.launch { viewModel.getRandomItem()?.let(::openItem) } } }, onLongClick = {})) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val dotColor = MaterialTheme.colorScheme.onSecondaryContainer
                listOf(Alignment.TopStart to 20.dp, Alignment.TopEnd to 20.dp, Alignment.Center to 0.dp, Alignment.BottomStart to 20.dp, Alignment.BottomEnd to 20.dp).forEach { (align, offset) -> Box(modifier = Modifier.align(align).padding(offset).size(12.dp).clip(CircleShape).background(dotColor.copy(alpha = if (isRandomizing) 0.35f else 1f))) }
                if (isRandomizing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(414.dp)) { page ->
            val pageItems = when (page) { 0 -> distinctItems.take(firstPageContentSlots); else -> distinctItems.drop(firstPageContentSlots + (page - 1) * slotsPerPage).take(slotsPerPage) }
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until columns) {
                            val slotIndex = row * columns + col
                            Box(modifier = Modifier.weight(1f).padding(vertical = 5.dp)) {
                                when { page == 0 && slotIndex == slotsPerPage - 1 -> RandomTile(); slotIndex < pageItems.size -> SpeedDialPoster(pageItems[slotIndex]); else -> Spacer(modifier = Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
            }
        }
        if (pagerState.pageCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.height(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(pagerState.pageCount) { index ->
                    val color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageSectionContent(section: HomePage.Section, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, scope: CoroutineScope, modifier: Modifier = Modifier) {
    val sectionTitle = section.title.lowercase()
    val sectionSongs = section.items.filterIsInstance<SongItem>()
    val isSongsOnlySection = section.items.isNotEmpty() && section.items.all { it is SongItem }
    
    when {
        sectionTitle.contains("discover") || sectionTitle.contains("mix") || sectionTitle.contains("listen again") -> {
            LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxWidth()) {
                items(items = section.items.distinctBy { it.id }, key = { "premium_card_${it.id}" }) { item ->
                    YTPremiumDiscoverCard(item = item, onClick = { when (item) { is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata())); is AlbumItem -> navController.navigate("album/${item.id}"); is ArtistItem -> navController.navigate("artist/${item.id}"); is PlaylistItem -> navController.navigate("online_playlist/${item.id}") } }, navController = navController, modifier = Modifier.width(180.dp).aspectRatio(1f))
                }
            }
        }
        isSongsOnlySection -> {
            BoxWithConstraints {
                val horizontalLazyGridItemWidth = maxWidth * if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                LazyHorizontalGrid(state = rememberLazyGridState(), rows = GridCells.Fixed(4), contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = modifier.fillMaxWidth().height(ListItemHeight * 4)) {
                    items(items = sectionSongs.distinctBy { it.id }, key = { "list_song_${it.id}" }) { song ->
                        YouTubeListItem(
                            item = song, isActive = song.id == mediaMetadata?.id, isPlaying = isPlaying, isSwipeable = false,
                            trailingContent = { IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) } },
                            modifier = Modifier.width(horizontalLazyGridItemWidth).combinedClickable(onClick = { playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())) }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } })
                        )
                    }
                }
            }
        }
        else -> {
            LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = modifier) {
                items(items = section.items.distinctBy { it.id }, key = { "grid_item_${it.id}" }) { item ->
                    YouTubeGridItemWrapper(item = item, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)
                }
            }
        }
    }
}
