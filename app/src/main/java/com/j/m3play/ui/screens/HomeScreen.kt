package com.j.m3play.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.j.m3play.constants.AccountNameKey
import com.j.m3play.constants.GridThumbnailHeight
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.ListItemHeight
import com.j.m3play.constants.ListThumbnailSize
import com.j.m3play.constants.ThumbnailCornerRadius
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
import com.j.m3play.ui.component.AlbumGridItem
import com.j.m3play.ui.component.ArtistGridItem
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.HideOnScrollFAB
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.SongGridItem
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeGridItem
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
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.constants.HapticsEnabledKey
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
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
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
                item {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        ChipsRow(
                            chips = listOfNotNull(
                                Pair("history", stringResource(R.string.history)),
                                Pair("stats", stringResource(R.string.stats)),
                                Pair("liked", stringResource(R.string.liked)),
                                Pair("downloads", stringResource(R.string.offline)),
                                if (isLoggedIn) Pair(
                                    "account",
                                    stringResource(R.string.account)
                                ) else null
                            ),
                            currentValue = "",
                            onValueUpdate = { value ->
                                when (value) {
                                    "history" -> navController.navigate("history")
                                    "stats" -> navController.navigate("stats")
                                    "liked" -> navController.navigate("auto_playlist/liked")
                                    "downloads" -> navController.navigate("auto_playlist/downloaded")
                                    "account" -> if (isLoggedIn) navController.navigate("account")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicksList ->
                    item {
                        val quickPicksTitle = stringResource(R.string.quick_picks)
                        NavigationTitle(
                            title = quickPicksTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = quickPicksTitle,
                                        items = quickPicksList.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    item {
                        LazyHorizontalGrid(
                            state = quickPicksLazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                                .animateItem()
                        ) {
                            items(
                                items = quickPicksList.distinctBy { it.id },
                                key = { it.id }
                            ) { originalSong ->
                                val index = quickPicksList.indexOf(originalSong)
                                
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
