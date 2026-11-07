/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.PlaylistEditLockKey
import com.j.m3play.constants.PlaylistSongSortDescendingKey
import com.j.m3play.constants.PlaylistSongSortType
import com.j.m3play.constants.PlaylistSongSortTypeKey
import com.j.m3play.constants.SwipeToSongKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.PlaylistSong
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.move
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.YouTube
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.utils.completed
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.LocalMixQueue
import com.j.m3play.ui.component.AssignTagsDialog
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EditPlaylistDialog
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LocalPlaylistViewModel

// HAZE BLUR FOR TOP APP BAR
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.material3.Material3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    
    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    // SMART COLOR LOGIC BASED ON LUMINANCE
    val isLightBackground = animatedExtractedColor.luminance() > 0.5f
    val smartTextColor = if (isLightBackground) Color.Black else Color.White
    val smartIconColor = if (isLightBackground) Color.Black.copy(alpha = 0.8f) else Color.White

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)
    val swipeToSongEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)
    var showAssignTagsDialog by remember { mutableStateOf(false) }

    if (showAssignTagsDialog && playlist != null) {
        AssignTagsDialog(database = database, playlistId = playlist!!.id, onDismiss = { showAssignTagsDialog = false })
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()
    
    val hazeState = rememberHazeState() // FOR TOP APP BAR

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { song ->
            song.song.song.title.contains(query.text, ignoreCase = true) ||
            song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    
    var selection by remember { mutableStateOf(false) }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
                downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                downloads[it.song.id]?.state == Download.STATE_COMPLETED
            }) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        playlist?.let { playlistData ->
            EditPlaylistDialog(
                initialName = playlistData.playlist.name,
                initialThumbnailUrl = playlistData.playlist.thumbnailUrl,
                fallbackThumbnails = playlistData.songThumbnails.filterNotNull(),
                onDismiss = { showEditDialog = false },
                onSave = { name, thumbnailUrl ->
                    database.query {
                        update(
                            playlistData.playlist.copy(
                                name = name,
                                thumbnailUrl = thumbnailUrl,
                                lastUpdateTime = LocalDateTime.now(),
                            )
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistData.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                        }
                    }
                ) { Text(text = stringResource(android.R.string.ok)) }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query { playlist?.let { delete(it.playlist) } }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) { Text(text = stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            val hasContent = current != null && (current.songCount > 0 || current.playlist.remoteSongCount != 0)
            if (hasContent && !isSearching && !selection) 2 else 0
        }
    }
    
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - headerItems) to (to.index - headerItems)
            } else {
                currentDragInfo.first to (to.index - headerItems)
            }
            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction { move(viewModel.playlistId, from, to) }
                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(viewModel.playlist.value?.playlist?.browseId!!, setVideoId, successorSetVideoId)
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                .allowHardware(false)
                .build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }
                    val extractedColors = PlayerColorExtractor.extractGradientColors(palette, defaultColor.toArgb())
                    dominantColor = extractedColors.firstOrNull() ?: defaultColor
                }
            }
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching || selection } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier.fillMaxSize().background(animatedExtractedColor)
        ) {
            // Full-screen backdrop source for the playlist glass controls.
            // Kept as a sibling of LazyColumn so effects never sample an ancestor source.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState, zIndex = -2f, key = "playlist-glass-backdrop")
            ) {
                // Keep this Haze source color-only. Playlist artwork belongs only in the hero header.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(animatedExtractedColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.40f to animatedExtractedColor.copy(alpha = 0.12f),
                                    0.58f to animatedExtractedColor.copy(alpha = 0.62f),
                                    0.82f to animatedExtractedColor.copy(alpha = 0.94f),
                                    1.0f to animatedExtractedColor
                                )
                            )
                        )
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f, key = "playlist-content"), // HAZE SOURCE ON LIST
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (isSearching || selection) {
                    item(key = "search_spacer", contentType = "header") { Spacer(modifier = Modifier.height(topPadding)) }
                }

                playlist?.let { playlist ->
                    if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.music_note,
                                text = stringResource(R.string.playlist_is_empty)
                            )
                        }
                    } else {
                        if (!isSearching && !selection) {
                            item(key = "header", contentType = "header") {
                                Box {
                                    LocalPlaylistGlossyHeader(
                                        playlist = playlist,
                                        songs = songs,
                                        navController = navController,
                                        menuState = menuState,
                                        solidBgColor = animatedExtractedColor,
                                        smartTextColor = smartTextColor,
                                        smartIconColor = smartIconColor,
                                        hazeState = hazeState,
                                        onSearchClick = { isSearching = true },
                                        downloadState = downloadState,
                                        editable = editable,
                                        coroutineScope = coroutineScope,
                                        snackbarHostState = snackbarHostState,
                                        onShowEditDialog = { showEditDialog = true },
                                        onshowDeletePlaylistDialog = { showDeletePlaylistDialog = true },
                                        onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                    )
                                }
                            }

                            item(key = "sort_header") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                ) {
                                    SortHeader(
                                        sortType = sortType,
                                        sortDescending = sortDescending,
                                        onSortTypeChange = onSortTypeChange,
                                        onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { sortType ->
                                            when (sortType) {
                                                PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                                PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                                PlaylistSongSortType.NAME -> R.string.sort_by_name
                                                PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                                PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                        IconButton(
                                            onClick = { locked = !locked },
                                            onLongClick = {},
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                                contentDescription = null,
                                                tint = smartIconColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!selection) {
                    itemsIndexed(
                        items = if (isSearching) filteredSongs else mutableSongs,
                        key = { _, song -> song.map.id },
                    ) { index, song ->
                        ReorderableItem(
                            state = reorderableState,
                            key = song.map.id,
                            modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        ) {
                            val currentItem by rememberUpdatedState(song)
                            fun deleteFromPlaylist() {
                                val map = currentItem.map
                                val browseId = playlist?.playlist?.browseId
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.withTransaction {
                                        move(map.playlistId, map.position, Int.MAX_VALUE)
                                        delete(map.copy(position = Int.MAX_VALUE))
                                    }
                                    if (browseId != null) {
                                        val setVideoId = map.setVideoId ?: database.getSetVideoId(map.songId)?.setVideoId
                                        if (setVideoId != null) {
                                            YouTube.removeFromPlaylist(browseId, map.songId, setVideoId)
                                        }
                                    }
                                }
                            }

                            val dismissBoxState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance },
                                confirmValueChange = { targetValue -> targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress }
                            )
                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                    processedDismiss = true
                                    deleteFromPlaylist()
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                            }

                            val content: @Composable () -> Unit = {
                                SongListItem(
                                    song = song.song,
                                    viewCountText = viewCounts[song.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                    isActive = song.song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    showInLibraryIcon = true,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song.song,
                                                        playlistSong = song,
                                                        playlistBrowseId = playlist?.playlist?.browseId,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                            onLongClick = {}
                                        ) { Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor) }
                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                            IconButton(
                                                onClick = { },
                                                onLongClick = {},
                                                modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f },
                                            ) { Icon(painterResource(R.drawable.drag_handle), null, tint = smartIconColor) }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (song.song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist!!.playlist.name,
                                                            items = songs.map { it.song.toMediaItem() },
                                                            startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) selection = true
                                                wrappedSongs.forEach { it.isSelected = false }
                                                wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true
                                            },
                                        )
                                )
                            }
                            if (locked || selection || swipeToSongEnabled) { content() } else {
                                SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.map.id },
                    ) { index, songWrapper ->
                        ReorderableItem(
                            state = reorderableState,
                            key = songWrapper.item.map.id,
                            modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        ) {
                            val currentItem by rememberUpdatedState(songWrapper.item)
                            fun deleteFromPlaylist() {
                                val map = currentItem.map
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.withTransaction {
                                        move(map.playlistId, map.position, Int.MAX_VALUE)
                                        delete(map.copy(position = Int.MAX_VALUE))
                                    }
                                }
                            }
                            val dismissBoxState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance },
                                confirmValueChange = { targetValue -> targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress }
                            )
                            var processedDismiss2 by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss2 && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                    processedDismiss2 = true
                                    deleteFromPlaylist()
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) processedDismiss2 = false
                            }

                            val content: @Composable () -> Unit = {
                                SongListItem(
                                    song = songWrapper.item.song,
                                    viewCountText = viewCounts[songWrapper.item.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                    isActive = songWrapper.item.song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    showInLibraryIcon = true,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = songWrapper.item.song,
                                                        playlistBrowseId = playlist?.playlist?.browseId,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                            onLongClick = {}
                                        ) { Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor) }
                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                            IconButton(
                                                onClick = { },
                                                onLongClick = {},
                                                modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f },
                                            ) { Icon(painterResource(R.drawable.drag_handle), null, tint = smartIconColor) }
                                        }
                                    },
                                    isSelected = songWrapper.isSelected && selection,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (!selection) {
                                                    if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = playlist!!.playlist.name,
                                                                items = songs.map { it.song.toMediaItem() },
                                                                startIndex = index,
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    songWrapper.isSelected = !songWrapper.isSelected
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                    wrappedSongs.forEach { it.isSelected = false }
                                                    songWrapper.isSelected = true
                                                }
                                            },
                                        )
                                )
                            }
                            if (locked || !editable || swipeToSongEnabled) { content() } else {
                                SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                            }
                        }
                    }
                }
            }

            DraggableScrollbar(
                modifier = Modifier
                    .padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues())
                    .align(Alignment.CenterEnd),
                scrollState = lazyListState,
                headerItems = headerItems
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
                            input = HazeInput.Backdrop(hazeState),
                            style = HazeBlurStyle.Material3(
                                containerColor = animatedExtractedColor.copy(alpha = 0.52f)
                            ) {
                                blurRadius(48.dp)
                                noiseFactor(0.16f)
                                colorEffects(
                                    listOf(
                                        HazeColorEffect.tint(
                                            animatedExtractedColor.copy(alpha = 0.48f)
                                        )
                                    )
                                )
                            }
                        )
                        .fillMaxWidth(), // Haze draws the frosted layer; no opaque overlay above it
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    title = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            Text(pluralStringResource(R.plurals.n_song, count, count), color = smartTextColor)
                        } else if (isSearching) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = smartTextColor.copy(alpha = 0.7f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = smartTextColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = smartTextColor
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        } else {
                            Text(playlist?.playlist?.name.orEmpty(), color = smartTextColor)
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
                        ) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = smartIconColor) }
                    },
                    actions = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            IconButton(onClick = {
                                if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                                else wrappedSongs.forEach { it.isSelected = true }
                            }, onLongClick = {}) {
                                Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = smartIconColor)
                            }
                            IconButton(onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song },
                                        songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map },
                                        onDismiss = menuState::dismiss,
                                        clearAction = {
                                            selection = false
                                            wrappedSongs.clear()
                                        }
                                    )
                                }
                            }, onLongClick = {}) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor)
                            }
                        } else if (!isSearching) {
                            IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                                Icon(painterResource(R.drawable.search), null, tint = smartIconColor)
                            }
                        }
                    }
                )
            }

            PullToRefreshDefaults.Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun LocalPlaylistGlossyHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    navController: NavController,
    menuState: com.j.m3play.ui.component.MenuState,
    solidBgColor: Color,
    smartTextColor: Color,
    smartIconColor: Color,
    hazeState: HazeState,
    onSearchClick: () -> Unit,
    downloadState: Int,
    editable: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onShowEditDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight / 2)) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState, zIndex = -1f, key = "playlist-artwork")) {
                if (playlist.thumbnails.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (playlist.thumbnails.size == 1) {
                    AsyncImage(
                        model = playlist.thumbnails[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                            AsyncImage(
                                model = playlist.thumbnails.getOrNull(index),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.align(alignment).fillMaxSize(0.5f),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(screenHeight * 0.35f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(0.0f to Color.Transparent, 0.4f to solidBgColor.copy(alpha = 0.4f), 0.8f to solidBgColor.copy(alpha = 0.9f), 1.0f to solidBgColor))
                )
                
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(playlist.playlist.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = smartTextColor, maxLines = 2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = smartTextColor, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Playlist • 2026", style = MaterialTheme.typography.bodyMedium, color = smartTextColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIQUID GLASS BUTTON
                LiquidGlassIconButton(
                    hazeState = hazeState, 
                    painter = painterResource(R.drawable.arrow_back), 
                    modifier = Modifier.size(48.dp), 
                    onClick = { navController.navigateUp() }
                )
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .liquidGlass(hazeState, RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSearchClick, onLongClick = {}) { Icon(painterResource(R.drawable.search), "Search", tint = smartIconColor) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp)) 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIQUID GLASS BUTTON
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() }))
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.shuffle), null, tint = smartIconColor, modifier = Modifier.size(20.dp)) }

                Box(
                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(RoundedCornerShape(50)).background(smartTextColor).clickable {
                        playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() }))
                    }.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        val invertedColor = if (smartTextColor == Color.White) Color.Black else Color.White
                        Icon(painterResource(R.drawable.play), null, tint = invertedColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = invertedColor)
                    }
                }

                var showMenu by remember { mutableStateOf(false) }

                // LIQUID GLASS BUTTON
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            showMenu = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor, modifier = Modifier.size(20.dp))
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (editable) "Edit" else "Sync") },
                            onClick = {
                                showMenu = false
                                if (editable) {
                                    onShowEditDialog()
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val playlistPage = YouTube.playlist(playlist.playlist.browseId!!).completed().getOrNull() ?: return@launch
                                        database.transaction {
                                            clearPlaylist(playlist.id)
                                            playlistPage.songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { position, song ->
                                                PlaylistSongMap(songId = song.id, playlistId = playlist.id, position = position, setVideoId = song.setVideoId)
                                            }.forEach(::insert)
                                        }
                                        withContext(Dispatchers.Main) { snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced)) }
                                    }
                                }
                            },
                            leadingIcon = { Icon(painterResource(if (editable) R.drawable.edit else R.drawable.sync), null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (editable) "Delete" else "Download") },
                            onClick = {
                                showMenu = false
                                if (editable) {
                                    onshowDeletePlaylistDialog()
                                } else {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                                        Download.STATE_DOWNLOADING -> songs.forEach { DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, it.song.id, false) }
                                        else -> songs.forEach {
                                            val req = DownloadRequest.Builder(it.song.id, it.song.id.toUri()).setCustomCacheKey(it.song.id).setData(it.song.song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, req, false)
                                        }
                                    }
                                }
                            },
                            leadingIcon = { Icon(painterResource(if (editable) R.drawable.delete else R.drawable.download), null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { playerConnection.playQueue(LocalMixQueue(database, playlist.id, 50)) }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.mix), "Start Mix", modifier = Modifier.size(20.dp), tint = smartIconColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Mix", color = smartTextColor)
                    }
                }
            }

            val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) playlist.playlist.remoteSongCount else playlist.songCount
            val nSongs = pluralStringResource(R.plurals.n_song, songCount, songCount)
            val durationText = if (playlistLength > 0) makeTimeString(playlistLength * 1000L) else ""

            Text(
                text = if (durationText.isNotEmpty()) "$nSongs • $durationText" else nSongs,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = smartTextColor
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
