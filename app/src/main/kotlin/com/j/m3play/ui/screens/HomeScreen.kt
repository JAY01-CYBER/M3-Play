package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.*
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.models.*
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.*
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: DailyDiscoverItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsState(initial = 0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val song = dailyDiscover.recommendation as? SongItem

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (song != null) {
                        menuState.show {
                            YouTubeSongMenu(song = song, navController = navController, onDismiss = { menuState.dismiss() })
                        }
                    }
                },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dailyDiscover.recommendation.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w544-h544"))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        ),
                    ),
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = dailyDiscover.recommendation.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        text = buildString {
                            append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                            if (playCount > 0) append(" • $playCount plays")
                        },
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = "Based on ${dailyDiscover.seed.title}",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        modifier = modifier.width(320.dp).height(420.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(model = item.songs.getOrNull(0)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                            AsyncImage(model = item.songs.getOrNull(1)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(model = item.songs.getOrNull(2)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                            AsyncImage(model = item.songs.getOrNull(3)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = item.playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = item.playlist.author?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                item.songs.take(3).forEach { song ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = song.thumbnail.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Column {
                            Text(text = song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                IconButton(onClick = { item.playlist.playEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) { Icon(painter = painterResource(R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                IconButton(onClick = { item.playlist.radioEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)) { Icon(painter = painterResource(R.drawable.radio), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val dailyDiscover by viewModel.dailyDiscover.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val metroSpeedDialItems by viewModel.metroSpeedDialItems.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState(initial = emptyList())
    val homePage by viewModel.homePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    val quickPicksLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!disableBlur) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxSize(0.7f).zIndex(-1f).drawWithCache {
                onDrawBehind {
                    drawRect(Brush.verticalGradient(listOf(Color.Blue.copy(0.2f), Color.Transparent)))
                }
            })
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)) {
            val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val itemWidth = maxWidth * widthFactor
            val favoritesSnap = remember(forgottenFavoritesLazyGridState) { SnapLayoutInfoProvider(forgottenFavoritesLazyGridState) { l, i -> (l * widthFactor / 2f - i / 2f) } }
            val quickSnap = remember(quickPicksLazyGridState) { SnapLayoutInfoProvider(quickPicksLazyGridState) { l, i -> (l * widthFactor / 2f - i / 2f) } }

            LazyColumn(state = lazylistState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                if (showHomeCategoryChips) {
                    item { ChipsRow(chips = homePage?.chips.orEmpty().map { it to it.title }, currentValue = selectedChip, onValueUpdate = { viewModel.toggleChip(it) }) }
                }

                item { TimeGreetingCard(onSearchClick = { navController.navigate("search/") }) }

                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionCard(title = "Liked", icon = R.drawable.favorite, onClick = { navController.navigate("auto_playlist/liked") }, modifier = Modifier.weight(1f))
                        ActionCard(title = "Downloads", icon = R.drawable.download, onClick = { navController.navigate("auto_playlist/downloaded") }, modifier = Modifier.weight(1f))
                    }
                }

                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { list ->
                    item { NavigationTitle(title = "Daily Discover") }
                    item {
                        val carouselState = rememberCarouselState { list.size }
                        HorizontalMultiBrowseCarousel(state = carouselState, preferredItemWidth = 320.dp, itemSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(320.dp)) { i ->
                            DailyDiscoverCard(dailyDiscover = list[i], onClick = { playerConnection.playQueue(YouTubeQueue(list[i].recommendation.id, list[i].recommendation.toMediaMetadata())) }, navController = navController)
                        }
                    }
                }

                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item { NavigationTitle(title = "From the community") }
                    item { CommunityPlaylistsSection(playlists = playlists, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    item { NavigationTitle(title = "Quick picks") }
                    item { QuickPicksSection(quickPicks = picks, mediaMetadata = mediaMetadata, isPlaying = isPlaying, horizontalLazyGridItemWidth = itemWidth, lazyGridState = quickPicksLazyGridState, snapLayoutInfoProvider = quickSnap, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                metroSpeedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = "Speed dial") }
                    item { MetroSpeedDialSection(items = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = "Keep listening") }
                    item { KeepListeningSection(keepListening = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                }

                AccountPlaylistsContainer(viewModel, accountName, accountImageUrl, mediaMetadata, isPlaying, navController, playerConnection, menuState, haptic, scope)

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                    item { NavigationTitle(title = "Forgotten favorites") }
                    item { ForgottenFavoritesSection(favorites, mediaMetadata, isPlaying, itemWidth, forgottenFavoritesLazyGridState, favoritesSnap, navController, playerConnection, menuState, haptic) }
                }

                SimilarRecommendationsContainer(viewModel, mediaMetadata, isPlaying, navController, playerConnection, menuState, haptic, scope)

                homePage?.sections?.forEach { section ->
                    item { HomePageSectionTitle(section, navController) }
                    item { HomePageSectionContent(section, mediaMetadata, isPlaying, navController, playerConnection, menuState, haptic, scope) }
                }

                if (isLoading) item { HomeLoadingShimmer() }
            }

            Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        }
    }
}

@Composable
fun ActionCard(title: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YouTubeGridItemWrapper(item: YTItem, mediaMetadata: MediaMetadata?, isPlaying: Boolean, navController: NavController, playerConnection: PlayerConnection, menuState: MenuState, haptic: HapticFeedback, scope: CoroutineScope) {
    YouTubeGridItem(item = item, isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id), isPlaying = isPlaying, coroutineScope = scope, thumbnailRatio = 1f, modifier = Modifier.combinedClickable(
        onClick = {
            when (item) {
                is SongItem -> playerConnection.playQueue(YouTubeQueue(item.id, item.toMediaMetadata()))
                is AlbumItem -> navController.navigate("album/${item.id}")
                is ArtistItem -> navController.navigate("artist/${item.id}")
                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
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
                }
            }
        }
    ))
}
