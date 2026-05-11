/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Glossy Premium Home Screen Integration    │
 * │  Signature: M3PLAY::UI::GLOSSY_EXPRESSIVE  │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.j.m3play.LocalListenTogetherManager
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.ShowHomeCategoryChipsKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.EpisodeItem
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.PodcastItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.LocalBottomSheetPageState
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.TimeGreetingCard
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.menu.YouTubeArtistMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CommunityPlaylistItem
import com.j.m3play.viewmodels.DailyDiscoverItem
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

// ==========================================
// GLOSSY CUSTOM COMPONENTS
// ==========================================

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
    val playsString = stringResource(R.string.plays)

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

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = dailyDiscover.recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = buildString {
                                append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                                if (playCount > 0) {
                                    append(" • $playCount $playsString")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    val messages = listOf(
                        R.string.daily_discover_sounds_like,
                        R.string.daily_discover_because_you_listen_to,
                        R.string.daily_discover_similar_to,
                        R.string.daily_discover_based_on,
                        R.string.daily_discover_for_fans_of,
                    )
                    val messageRes = remember(dailyDiscover.seed.id) {
                        messages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % messages.size]
                    }

                    Text(
                        text = stringResource(
                            messageRes,
                            "${dailyDiscover.seed.title} • ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.width(320.dp).height(420.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.playlist.author?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                item.songs.take(3).forEach { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onSongClick(song) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(model = song.thumbnail.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                IconButton(onClick = { item.playlist.playEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                    Icon(painter = painterResource(R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { item.playlist.radioEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)) {
                    Icon(painter = painterResource(R.drawable.radio), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ==========================================
// MAIN HOME SCREEN
// ==========================================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // Original M3Play + Metrolist Datasources
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

    // Official podcast API data
    val savedPodcastShows by viewModel.savedPodcastShows.collectAsState()
    val episodesForLater by viewModel.episodesForLater.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    val quickPicksLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    var cachedPodcasts by remember { mutableStateOf<List<PodcastItem>>(emptyList()) }

    val featuredPodcasts = remember(homePage, selectedChip) {
        if (selectedChip == null) {
            cachedPodcasts = emptyList()
            emptyList()
        } else {
            val newPodcasts = homePage?.sections
                ?.flatMap { it.items }
                ?.filterIsInstance<EpisodeItem>()
                ?.mapNotNull { episode ->
                    episode.podcast?.let { podcast ->
                        PodcastItem(id = podcast.id, title = podcast.name, author = episode.author, episodeCountText = null, thumbnail = episode.thumbnail, playEndpoint = null, shuffleEndpoint = null)
                    }
                }?.distinctBy { it.id }?.shuffled()?.take(10) ?: emptyList()

            if (newPodcasts.isNotEmpty()) {
                cachedPodcasts = newPodcasts
            }
            cachedPodcasts
        }
    }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItemWrapper(
            item = item, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController,
            playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope,
        )
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler { viewModel.toggleChip(selectedChip) }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(forgottenFavorites) { forgottenFavoritesLazyGridState.scrollToItem(0) }
    LaunchedEffect(quickPicks) { quickPicksLazyGridState.scrollToItem(0) }

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxSize()) {
        
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(colors = listOf(color1.copy(alpha = 0.38f), color1.copy(alpha = 0.24f), color1.copy(alpha = 0.14f), color1.copy(alpha = 0.06f), Color.Transparent), center = Offset(width * 0.15f, height * 0.1f), radius = width * 0.55f)
                        val brush2 = Brush.radialGradient(colors = listOf(color2.copy(alpha = 0.34f), color2.copy(alpha = 0.2f), color2.copy(alpha = 0.11f), color2.copy(alpha = 0.05f), Color.Transparent), center = Offset(width * 0.85f, height * 0.2f), radius = width * 0.65f)
                        val brush3 = Brush.radialGradient(colors = listOf(color3.copy(alpha = 0.3f), color3.copy(alpha = 0.17f), color3.copy(alpha = 0.09f), color3.copy(alpha = 0.04f), Color.Transparent), center = Offset(width * 0.3f, height * 0.45f), radius = width * 0.6f)
                        val brush4 = Brush.radialGradient(colors = listOf(color4.copy(alpha = 0.26f), color4.copy(alpha = 0.14f), color4.copy(alpha = 0.08f), color4.copy(alpha = 0.03f), Color.Transparent), center = Offset(width * 0.7f, height * 0.5f), radius = width * 0.7f)
                        val brush5 = Brush.radialGradient(colors = listOf(color5.copy(alpha = 0.22f), color5.copy(alpha = 0.12f), color5.copy(alpha = 0.06f), color5.copy(alpha = 0.02f), Color.Transparent), center = Offset(width * 0.5f, height * 0.75f), radius = width * 0.8f)

                        val overlayBrush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.22f), surfaceColor.copy(alpha = 0.55f), surfaceColor),
                            startY = height * 0.4f, endY = height
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = brush3)
                            drawRect(brush = brush4)
                            drawRect(brush = brush5)
                            drawRect(brush = overlayBrush)
                        }
                    }
            ) {}
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(lazyGridState = forgottenFavoritesLazyGridState, positionInLayout = { layoutSize, itemSize -> (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f) })
            }
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(lazyGridState = quickPicksLazyGridState, positionInLayout = { layoutSize, itemSize -> (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f) })
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                if (showHomeCategoryChips) {
                    item {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = { viewModel.toggleChip(it) }
                        )
                    }
                }

                item {
                    TimeGreetingCard(onSearchClick = { runCatching { navController.navigate("search/") } })
                }

                // EXPRESSIVE ACTION CARDS
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(title = "Liked", icon = R.drawable.favorite, onClick = { runCatching { navController.navigate("auto_playlist/liked") } }, modifier = Modifier.weight(1f))
                        ActionCard(title = "Downloads", icon = R.drawable.download, onClick = { runCatching { navController.navigate("auto_playlist/downloaded") } }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(title = "History", icon = R.drawable.history, onClick = { runCatching { navController.navigate("history") } }, modifier = Modifier.weight(1f))
                        ActionCard(title = if (isLoggedIn) "Account" else "Library", icon = if (isLoggedIn) R.drawable.person else R.drawable.library_music, onClick = {
                            if (isLoggedIn) runCatching { navController.navigate("account") } else runCatching { navController.navigate("library") }
                        }, modifier = Modifier.weight(1f))
                    }
                }

                // PODCAST SECTIONS (METROLIST)
                if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                    if (savedPodcastShows.isNotEmpty()) {
                        item(key = "00_your_shows_title") { NavigationTitle(title = stringResource(R.string.your_shows), onClick = { navController.navigate("youtube_browse/FEmusic_library_non_music_audio_list") }) }
                        item(key = "00_your_shows_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(savedPodcastShows) { podcast -> ytGridItem(podcast) } } }
                    }

                    if (episodesForLater.isNotEmpty()) {
                        item(key = "00_episodes_for_later_title") { NavigationTitle(title = stringResource(R.string.episodes_for_later), onClick = { navController.navigate("online_playlist/SE") }) }
                        item(key = "00_episodes_for_later_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(episodesForLater) { episode -> ytGridItem(episode) } } }
                    }

                    if (featuredPodcasts.isNotEmpty() && savedPodcastShows.isEmpty()) {
                        item(key = "0_podcast_channels_title") { NavigationTitle(title = stringResource(R.string.podcast_channels)) }
                        item(key = "0_podcast_channels_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(featuredPodcasts) { podcast -> ytGridItem(podcast) } } }
                    }
                }

                // DAILY DISCOVER (METROLIST CAROUSEL)
                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                    item(key = "daily_discover_title") {
                        val title = stringResource(R.string.your_daily_discover)
                        NavigationTitle(
                            title = title, modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                val queueItems = discoverList.mapNotNull { (it.recommendation as? SongItem)?.toMediaMetadata() }
                                if (queueItems.isNotEmpty()) playerConnection.playQueue(ListQueue(title = title, items = queueItems.map { it.toMediaItem() }))
                            }
                        )
                    }

                    item(key = "daily_discover_content") {
                        Box(modifier = Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            val carouselState = rememberCarouselState { discoverList.size }
                            HorizontalMultiBrowseCarousel(state = carouselState, preferredItemWidth = 320.dp, itemSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(320.dp)) { i ->
                                val item = discoverList[i]
                                DailyDiscoverCard(
                                    dailyDiscover = item,
                                    onClick = {
                                        if (!isListenTogetherGuest) {
                                            val song = item.recommendation as? SongItem
                                            val mediaMetadata = song?.toMediaMetadata()
                                            if (mediaMetadata != null) playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), mediaMetadata))
                                        }
                                    },
                                    navController = navController,
                                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                                )
                            }
                        }
                    }
                }
                
                // COMMUNITY PLAYLISTS (METROLIST)
                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item { NavigationTitle(title = stringResource(R.string.from_the_community), modifier = Modifier.animateItem()) }
                    item {
                        CommunityPlaylistsSection(playlists = playlists, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, modifier = Modifier.animateItem())
                    }
                }

                // QUICK PICKS (METROLIST GRID)
                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    item { NavigationTitle(title = stringResource(R.string.quick_picks), modifier = Modifier.animateItem()) }
                    item {
                        QuickPicksSection(quickPicks = picks, mediaMetadata = mediaMetadata, isPlaying = isPlaying, horizontalLazyGridItemWidth = horizontalLazyGridItemWidth, lazyGridState = quickPicksLazyGridState, snapLayoutInfoProvider = quickPicksSnapLayoutInfoProvider, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, modifier = Modifier.animateItem())
                    }
                }

                // METRO SPEED DIAL
                metroSpeedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = stringResource(R.string.speed_dial), modifier = Modifier.animateItem()) }
                    item { MetroSpeedDialSection(items = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                // SPEED DIAL
                speedDialSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                    item { NavigationTitle(title = stringResource(R.string.speed_dial), modifier = Modifier.animateItem()) }
                    item { SpeedDialSection(speedDialSongs = songs, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                // KEEP LISTENING
                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = stringResource(R.string.keep_listening), modifier = Modifier.animateItem()) }
                    item { KeepListeningSection(keepListening = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                }

                // ACCOUNT PLAYLISTS
                AccountPlaylistsContainer(viewModel = viewModel, accountName = accountName, accountImageUrl = url, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)

                // FORGOTTEN FAVORITES
                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                    item { NavigationTitle(title = stringResource(R.string.forgotten_favorites), modifier = Modifier.animateItem()) }
                    item { ForgottenFavoritesSection(forgottenFavorites = favorites, mediaMetadata = mediaMetadata, isPlaying = isPlaying, horizontalLazyGridItemWidth = horizontalLazyGridItemWidth, lazyGridState = forgottenFavoritesLazyGridState, snapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                // SIMILAR RECS (METROLIST)
                SimilarRecommendationsContainer(viewModel = viewModel, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)

                // HOME PAGE DYNAMIC SECTIONS
                homePage?.sections?.forEach { section ->
                    item { HomePageSectionTitle(section = section, navController = navController, modifier = Modifier.animateItem()) }
                    item { HomePageSectionContent(section = section, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                }

                if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                    item { HomeLoadingShimmer(modifier = Modifier.animateItem()) }
                }
            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "action_card_scale")
    val alpha by animateFloatAsState(targetValue = if (isPressed) 0.9f else 1f, animationSpec = tween(durationMillis = 120), label = "action_card_alpha")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(999.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}
