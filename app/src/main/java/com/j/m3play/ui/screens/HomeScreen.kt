package com.j.m3play.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.ArtistItem
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.WatchEndpoint
import com.arturo254.innertube.models.YTItem
import com.arturo254.innertube.utils.parseCookieString
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.LocalItem
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.LocalAlbumRadio
import com.j.m3play.playback.queues.YouTubeAlbumRadio
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.component.shimmer.GridItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.*
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.Haptics
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticsEnabled by rememberPreference(HapticsEnabledKey, defaultValue = true)

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    
    val heroItems = remember(explorePage, isRefreshing) {
        buildList {
            explorePage?.newReleaseAlbums
                ?.shuffled()
                ?.take(5)
                ?.forEach { album ->
                    add(
                        M3PlayHeroItem(
                            id = album.id,
                            title = album.title,
                            subtitle = "New Release",
                            imageUrl = album.thumbnail
                        )
                    )
                }
        }
    }
    
    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (hapticsEnabled) Haptics.click(haptic, context)
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            if (hapticsEnabled) Haptics.longPress(haptic, context)
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (hapticsEnabled) Haptics.click(haptic, context)
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            if (hapticsEnabled) Haptics.longPress(haptic, context)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (hapticsEnabled) Haptics.click(haptic, context)
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            if (hapticsEnabled) Haptics.longPress(haptic, context)
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        if (hapticsEnabled) Haptics.click(haptic, context)
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        if (hapticsEnabled) Haptics.longPress(haptic, context)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                    .graphicsLayer {
                        rotationZ = if (isRefreshing) 360f else 0f
                        scaleX = 1f + (pullRefreshState.progress * 0.3f)
                        scaleY = 1f + (pullRefreshState.progress * 0.3f)
                    }
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = quickPicksLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                // Time Greeting Card
                item {
                    TimeGreetingCard(
                        onSearchClick = {
                            navController.navigate("explore")
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem()
                    )
                }
                
                // Action Cards Row 1
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Liked",
                            icon = R.drawable.favorite,
                            onClick = {
                                navController.navigate("auto_playlist/liked")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionCard(
                            title = "Downloads",
                            icon = R.drawable.download,
                            onClick = {
                                navController.navigate("auto_playlist/downloaded")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Cards Row 2
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            title = "History",
                            icon = R.drawable.history,
                            onClick = {
                                navController.navigate("history")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionCard(
                            title = if (isLoggedIn) "Account" else "Library",
                            icon = if (isLoggedIn) R.drawable.person else R.drawable.library_music,
                            onClick = {
                                if (isLoggedIn) {
                                    navController.navigate("account")
                                } else {
                                    navController.navigate("library")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Spacer
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Hero Carousel with Entrance Animation
                item {
                    // Carousel entrance animation
                    val carouselAlpha = remember { Animatable(0f) }
                    val carouselOffset = remember { Animatable(50f) }
                    
                    LaunchedEffect(Unit) {
                        carouselAlpha.animateTo(1f, animationSpec = tween(500))
                        carouselOffset.animateTo(0f, animationSpec = tween(500))
                    }
                    
                    M3PlayHeroCarousel(
                        items = heroItems,
                        onItemClick = { item ->
                            navController.navigate("album/${item.id}")
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = carouselAlpha.value
                                translationY = carouselOffset.value
                            }
                            .animateItem()
                    )
                }

                // Spacer
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Quick Picks Section with Staggered Entrance
                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicksList ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .animateItem(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.quick_picks),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = stringResource(R.string.quick_picks),
                                                items = quickPicksList.distinctBy { it.id }.map { it.toMediaItem() }
                                            )
                                        )
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Play all",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        LazyHorizontalGrid(
                            state = quickPicksLazyGridState,
                            rows = GridCells.Fixed(4),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .animateItem()
                        ) {
                            items(
                                items = quickPicksList.distinctBy { it.id },
                                key = { it.id }
                            ) { originalSong ->
                                val index = quickPicksList.indexOf(originalSong)
                                
                                // Staggered entrance animation
                                val animatedAlpha = remember { Animatable(0f) }
                                val animatedOffset = remember { Animatable(50f) }
                                
                                LaunchedEffect(Unit) {
                                    delay(index * 50L)
                                    animatedAlpha.animateTo(1f, animationSpec = tween(300))
                                    animatedOffset.animateTo(0f, animationSpec = tween(300))
                                }
                                
                                val song by database.song(originalSong.id)
                                    .collectAsState(initial = originalSong)

                                val subtitleText = song!!.artists.joinToString { it.name }

                                M3PlayQuickPickCard(
                                    song = song!!,
                                    subtitle = subtitleText,
                                    onClick = {
                                        if (hapticsEnabled) Haptics.click(haptic, context)
                                        if (song!!.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song!!.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onMenuClick = {
                                        if (hapticsEnabled) Haptics.click(haptic, context)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song!!,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .padding(vertical = 3.dp)
                                        .graphicsLayer {
                                            alpha = animatedAlpha.value
                                            translationY = animatedOffset.value
                                        }
                                )
                            }
                        }
                    }
                }
                    
                // Keep Listening Section
                keepListening?.takeIf { it.isNotEmpty() }?.let { keepListeningList ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        val rows = if (keepListeningList.size > 6) 2 else 1
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(rows),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((GridThumbnailHeight + with(LocalDensity.current) {
                                    MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                            MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                                }) * rows)
                                .animateItem()
                        ) {
                            items(keepListeningList) {
                                localGridItem(it)
                            }
                        }
                    }
                }

                // Account Playlists Section
                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylistsList ->
                    item {
                        NavigationTitle(
                            label = stringResource(R.string.your_ytb_playlists),
                            title = accountName,
                            thumbnail = {
                                if (url != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(url)
                                            .crossfade(false)
                                            .build(),
                                        placeholder = painterResource(id = R.drawable.person),
                                        error = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(ListThumbnailSize)
                                    )
                                }
                            },
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = accountPlaylistsList.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                // Similar Recommendations
                similarRecommendations?.forEach { section ->
                    item {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = section.title.title,
                            thumbnail = section.title.thumbnailUrl?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (section.title is Artist) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            onClick = {
                                when (section.title) {
                                    is Song -> navController.navigate("album/${section.title.album!!.id}")
                                    is Album -> navController.navigate("album/${section.title.id}")
                                    is Artist -> navController.navigate("artist/${section.title.id}")
                                    is Playlist -> {}
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(section.items) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                // Home Page Sections
                homePage?.sections?.forEach { section ->
                    item {
                        NavigationTitle(
                            title = section.title,
                            label = section.label,
                            thumbnail = section.thumbnail?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(section.items) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                // New Release Albums
                explorePage?.newReleaseAlbums?.let { newReleaseAlbumsList ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.new_release_albums),
                            onClick = {
                                navController.navigate("new_release")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = newReleaseAlbumsList,
                                key = { it.id }
                            ) { album ->
                                YouTubeGridItem(
                                    item = album,
                                    isActive = mediaMetadata?.album?.id == album.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${album.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = album,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }
                        }
                    }
                }

                // Shimmer Loader
                if (isLoading && homePage?.sections.isNullOrEmpty()) {
                    item(key = "loading_shimmer") {
                        ShimmerHost(
                            modifier = Modifier.animateItem()
                        ) {
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .width(250.dp),
                            )
                            LazyRow {
                                items(4) {
                                    GridItemPlaceHolder()
                                }
                            }
                        }
                    }
                }

                // Forgotten Favorites with Staggered Entrance
                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavoritesList ->
                    item {
                        val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                        NavigationTitle(
                            title = forgottenFavoritesTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = forgottenFavoritesTitle,
                                        items = forgottenFavoritesList.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    item {
                        val rows = min(4, forgottenFavoritesList.size)
                        LazyHorizontalGrid(
                            state = forgottenFavoritesLazyGridState,
                            rows = GridCells.Fixed(rows),
                            flingBehavior = rememberSnapFlingBehavior(
                                forgottenFavoritesSnapLayoutInfoProvider
                            ),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * rows)
                                .animateItem()
                        ) {
                            items(
                                items = forgottenFavoritesList.distinctBy { it.id },
                                key = { it.id }
                            ) { originalSong ->
                                val index = forgottenFavoritesList.indexOf(originalSong)
                                
                                // Staggered entrance animation
                                val animatedAlpha = remember { Animatable(0f) }
                                val animatedOffset = remember { Animatable(50f) }
                                
                                LaunchedEffect(Unit) {
                                    delay(index * 50L)
                                    animatedAlpha.animateTo(1f, animationSpec = tween(300))
                                    animatedOffset.animateTo(0f, animationSpec = tween(300))
                                }
                                
                                val song by database.song(originalSong.id)
                                    .collectAsState(initial = originalSong)

                                SongListItem(
                                    song = song!!,
                                    showInLibraryIcon = true,
                                    isActive = song!!.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                if (hapticsEnabled) Haptics.click(haptic, context)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .graphicsLayer {
                                            alpha = animatedAlpha.value
                                            translationY = animatedOffset.value
                                        }
                                        .combinedClickable(
                                            onClick = {
                                                if (hapticsEnabled) Haptics.click(haptic, context)
                                                if (song!!.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                                }
                                            },
                                            onLongClick = {
                                                if (hapticsEnabled) Haptics.longPress(haptic, context)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            HideOnScrollFAB(
                visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
                lazyListState = lazylistState,
                icon = R.drawable.shuffle,
                onClick = {
                    val local = when {
                        allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                        allLocalItems.isNotEmpty() -> true
                        else -> false
                    }
                    scope.launch(Dispatchers.Main) {
                        if (local) {
                            when (val luckyItem = allLocalItems.random()) {
                                is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is Album -> {
                                    val albumWithSongs = withContext(Dispatchers.IO) {
                                        database.albumWithSongs(luckyItem.id).first()
                                    }
                                    albumWithSongs?.let {
                                        playerConnection.playQueue(LocalAlbumRadio(it))
                                    }
                                }

                                is Artist -> {}
                                is Playlist -> {}
                            }
                        } else {
                            when (val luckyItem = allYtItems.random()) {
                                is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                                is ArtistItem -> luckyItem.radioEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }

                                is PlaylistItem -> luckyItem.playEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                            }
                        }
                    }
                }
            )
            
            // Scroll to Top Button
            val isScrolled by remember {
                derivedStateOf { lazylistState.firstVisibleItemIndex > 0 }
            }
            
            AnimatedVisibility(
                visible = isScrolled,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(0.dp, 0.dp, 16.dp, 80.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            lazylistState.animateScrollToItem(0)
                            if (hapticsEnabled) Haptics.click(haptic, context)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
