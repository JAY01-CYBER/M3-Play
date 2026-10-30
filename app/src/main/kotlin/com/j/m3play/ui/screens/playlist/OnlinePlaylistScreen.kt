/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.j.m3play.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.layerBackdrop
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.component.rememberBackdrop
import com.j.m3play.ui.component.shimmer.ButtonPlaceholder
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel
import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState
import com.valentinilk.shimmer.shimmer

// HAZE 2.0 IMPORTS
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.material3.Material3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hazeState = rememberHazeState()
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }

    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { index, song -> index to song }
        else songs.mapIndexed { index, song -> index to song }.filter { (_, song) ->
            song.title.contains(query.text, ignoreCase = true) || song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) { BackHandler { isSearching = false; query = TextFieldValue() } } 
    else if (selection) { BackHandler { selection = false } }

    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }.toMutableStateList()

    val playlistPaletteState = rememberPaletteState()
    var playlistArtworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlistArtworkBitmap, playlist?.thumbnail) {
        val bitmap = playlistArtworkBitmap
        if (bitmap != null && playlist?.thumbnail != null && paletteGeneratedFor != playlist?.thumbnail) {
            playlistPaletteState.generate(bitmap)
            paletteGeneratedFor = playlist?.thumbnail
        }
    }
    
    LaunchedEffect(playlistPaletteState.palette) {
        val palette = playlistPaletteState.palette
        if (palette != null) {
            val vibrant = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
            dominantColor = vibrant?.let { Color(it) } ?: defaultColor
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                viewModel.loadMoreSongs()
            }
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching || selection } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier.fillMaxSize().background(animatedExtractedColor)
        ) {
            LazyColumn(
                state = lazyListState,
                // HAZE 2.x API Update: haze -> hazeSource
                modifier = Modifier.hazeSource(state = hazeState),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (isSearching || selection) {
                    item(key = "search_spacer", contentType = "header") { Spacer(modifier = Modifier.height(topPadding)) }
                }

                playlist.let { currentPlaylist ->
                    if (isLoading) {
                        item(key = "shimmer") {
                            ShimmerHost {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.padding(bottom = 20.dp).size(240.dp).shimmer().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onSurface))
                                    TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                                        Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                        ButtonPlaceholder(modifier = Modifier.weight(1f).height(52.dp))
                                        Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                repeat(6) { ListItemPlaceHolder() }
                            }
                        }
                    } else if (currentPlaylist != null) {
                        if (!isSearching && !selection) {
                            item(key = "header") {
                                Box(modifier = Modifier.animateItem()) {
                                    OnlinePlaylistGlossyHeader(
                                        playlist = currentPlaylist,
                                        songs = songs,
                                        dbPlaylist = dbPlaylist,
                                        navController = navController,
                                        coroutineScope = coroutineScope,
                                        solidBgColor = animatedExtractedColor,
                                        onSearchClick = { isSearching = true },
                                        onImageBitmapReady = { playlistArtworkBitmap = it }
                                    )
                                }
                            }
                        }

                        if (songs.isEmpty() && !isLoading && error == null) {
                            item(key = "empty") {
                                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.empty_playlist), style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(R.string.empty_playlist_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                            YouTubeListItem(
                                item = song.item.second,
                                viewCountText = viewCounts[song.item.second.id]?.let { formatCompactCount(it.toLong()) },
                                isActive = mediaMetadata?.id == song.item.second.id,
                                isPlaying = isPlaying,
                                isSelected = song.isSelected && selection,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(song = song.item.second, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                        },
                                        onLongClick = {}
                                    ) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (song.item.second.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                                else {
                                                    currentPlaylist.id.let { pid -> playerConnection.service.getAutomix(playlistId = pid) }
                                                    playerConnection.playQueue(YouTubeQueue(song.item.second.endpoint ?: WatchEndpoint(videoId = song.item.second.id), song.item.second.toMediaMetadata()))
                                                }
                                            } else song.isSelected = !song.isSelected
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
                                        },
                                    ).animateItem(),
                            )
                        }

                        if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                            item(key = "loading_more") { ShimmerHost { repeat(2) { ListItemPlaceHolder() } } }
                        }
                    } else {
                        val isPrivatePlaylist = error?.contains("PLAYLIST_PRIVATE") == true
                        item(key = "error") {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isPrivatePlaylist) {
                                    Image(painterResource(R.drawable.anime_blank), null, modifier = Modifier.size(120.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(stringResource(R.string.playlist_private_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(R.string.playlist_private_desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                } else {
                                    Text(if (error != null) stringResource(R.string.error_unknown) else stringResource(R.string.playlist_not_found), style = MaterialTheme.typography.titleLarge, color = if (error != null) MaterialTheme.colorScheme.error else Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(if (error != null) error!! else stringResource(R.string.playlist_not_found_desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                    if (error != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { viewModel.retry() }) { Text(stringResource(R.string.retry)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DraggableScrollbar(
                modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd),
                scrollState = lazyListState,
                headerItems = if (!isSearching && playlist != null && !selection) 1 else 0
            )

            AnimatedVisibility(
                visible = showTopBarTitle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    modifier = Modifier
                        .hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = HazeBlurStyle.Material3()
                        )
                        .background(animatedExtractedColor.copy(alpha = 0.6f)),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            Text(pluralStringResource(R.plurals.n_song, count, count), color = Color.White)
                        } else if (isSearching) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        } else {
                            Text(playlist?.title.orEmpty(), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSearching) {
                                    isSearching = false
                                    query = TextFieldValue()
                                } else if (selection) {
                                    selection = false
                                } else {
                                    navController.navigateUp()
                                }
                            },
                            onLongClick = {
                                if (!isSearching) navController.backToMain()
                            },
                        ) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = Color.White) }
                    },
                    actions = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            IconButton(onClick = {
                                if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                                else wrappedSongs.forEach { it.isSelected = true }
                            }, onLongClick = {}) {
                                Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = Color.White)
                            }
                            
                            IconButton(onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaItem().metadata!! },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                        currentItems = emptyList()
                                    )
                                }
                            }, onLongClick = {}) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                            }
                        } else if (!isSearching) {
                            IconButton(
                                onClick = {
                                    playlist?.let {
                                        val shareText = "https://music.youtube.com/playlist?list=${it.id}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                                        context.startActivity(Intent.createChooser(sendIntent, it.title))
                                    }
                                },
                                onLongClick = {}
                            ) { Icon(painterResource(R.drawable.share), null, tint = Color.White) }
                            
                            IconButton(onClick = { isSearching = true }, onLongClick = {}) { Icon(painterResource(R.drawable.search), null, tint = Color.White) }
                            
                            IconButton(onClick = {
                                playlist?.let { currentPlaylist ->
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = currentPlaylist,
                                            songs = songs,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                            selectAction = { selection = true },
                                            canSelect = true,
                                            snackbarHostState = snackbarHostState,
                                        )
                                    }
                                }
                            }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                        }
                    }
                )
            }

            PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun OnlinePlaylistGlossyHeader(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    navController: NavController,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    solidBgColor: Color,
    onSearchClick: () -> Unit,
    onImageBitmapReady: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val artworkBackdrop = rememberBackdrop()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight / 2)) {
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val loadedBitmap = runCatching { state.result.image.toBitmap().asImageBitmap() }.getOrNull()
                        if (loadedBitmap != null) onImageBitmapReady(loadedBitmap)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(screenHeight * 0.35f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(0.0f to Color.Transparent, 0.4f to solidBgColor.copy(alpha = 0.4f), 0.8f to solidBgColor.copy(alpha = 0.9f), 1.0f to solidBgColor))
                )
                
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(playlist.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(playlist.author?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = playlist.author?.id != null) {
                        playlist.author?.id?.let { authorId -> navController.navigate("artist/$authorId") }
                    }.padding(horizontal = 8.dp, vertical = 4.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Playlist • 2026", style = MaterialTheme.typography.bodyMedium, color = Color(0xC4FFFFFF), textAlign = TextAlign.Center)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(backdrop = artworkBackdrop, painter = painterResource(R.drawable.arrow_back), modifier = Modifier.size(48.dp), onClick = { navController.navigateUp() })
                Row(modifier = Modifier.height(48.dp).liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isLiked = dbPlaylist?.playlist?.bookmarkedAt != null
                    androidx.compose.material3.IconButton(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (dbPlaylist != null) {
                                database.withTransaction { update(dbPlaylist.playlist, playlist); update(dbPlaylist.playlist.toggleLike()) }
                            } else {
                                database.withTransaction {
                                    val playlistEntity = PlaylistEntity(
                                        name = playlist.title, browseId = playlist.id, thumbnailUrl = playlist.thumbnail, isEditable = playlist.isEditable,
                                        remoteSongCount = playlist.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                                        playEndpointParams = playlist.playEndpoint?.params, shuffleEndpointParams = playlist.shuffleEndpoint?.params, radioEndpointParams = playlist.radioEndpoint?.params
                                    ).toggleLike()
                                    insert(playlistEntity)
                                    songs.map { it.toMediaMetadata() }.onEach { insert(it) }.mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index, setVideoId = song.setVideoId) }.forEach { insert(it) }
                                }
                            }
                        }
                    }) { Icon(painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border), "Like", tint = if (isLiked) MaterialTheme.colorScheme.error else Color.White) }
                    androidx.compose.material3.IconButton(onClick = onSearchClick) { Icon(painterResource(R.drawable.search), "Search", tint = Color.White) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp)) 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(artworkBackdrop, CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        playlist.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                    },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(20.dp)) }

                Box(
                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(RoundedCornerShape(50)).background(Color.White).clickable {
                        playlist.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) } ?: run {
                            songs.firstOrNull()?.let { firstSong -> playerConnection.playQueue(YouTubeQueue(firstSong.endpoint ?: WatchEndpoint(videoId = firstSong.id), firstSong.toMediaMetadata())) }
                        }
                    }.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        val isThisPlaying = isPlaying && mediaMetadata?.album?.id == playlist.id
                        Icon(painterResource(if (isThisPlaying) R.drawable.pause else R.drawable.play), null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isThisPlaying) "Pause" else "Play", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(artworkBackdrop, CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        menuState.show {
                            YouTubePlaylistMenu(
                                playlist = playlist, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss, snackbarHostState = SnackbarHostState()
                            )
                        }
                    },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }

            Spacer(Modifier.height(20.dp)) 
            val totalDuration = songs.sumOf { it.duration ?: 0 }
            val durationText = if (totalDuration > 0) makeTimeString(totalDuration * 1000L) else ""
            val trackCountText = playlist.songCountText ?: "${songs.size} tracks"
            Text(if (durationText.isNotEmpty()) "$trackCountText • $durationText" else trackCountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
        }
    }
}
