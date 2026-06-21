/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for premium music experience      │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V4     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.PlaylistEditLockKey
import com.j.m3play.constants.PlaylistSongSortDescendingKey
import com.j.m3play.constants.PlaylistSongSortType
import com.j.m3play.constants.PlaylistSongSortTypeKey
import com.j.m3play.constants.SwipeToSongKey
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
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.EditPlaylistDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.AssignTagsDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.screens.playlist.PlaylistSuggestionsSection
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LocalPlaylistViewModel
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
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)
    val swipeToSongEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    var showAssignTagsDialog by remember { mutableStateOf(false) }

    if (showAssignTagsDialog && playlist != null) { AssignTagsDialog(database = database, playlistId = playlist!!.id, onDismiss = { showAssignTagsDialog = false }) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { song -> song.song.song.title.contains(query.text, true) || song.song.artists.fastAny { it.name.contains(query.text, true) } }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(saver = listSaver<MutableList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf() }
    var selectionAnchorMapId by rememberSaveable { mutableStateOf<Int?>(null) }
    val onExitSelectionMode = { inSelectMode = false; selection.clear(); selectionAnchorMapId = null }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (inSelectMode) BackHandler(onBack = onExitSelectionMode)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        selection.fastForEachReversed { mapId -> if (songs.find { it.map.id == mapId } == null) selection.remove(Integer.valueOf(mapId)) }
        if (selectionAnchorMapId != null && songs.none { it.map.id == selectionAnchorMapId }) { selectionAnchorMapId = songs.firstOrNull { it.map.id in selection }?.map?.id }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); addAll(songs) }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) Download.STATE_COMPLETED
            else if (songs.all { downloads[it.song.id]?.state == Download.STATE_QUEUED || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING || downloads[it.song.id]?.state == Download.STATE_COMPLETED }) Download.STATE_DOWNLOADING
            else Download.STATE_STOPPED
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        playlist?.let { playlistData ->
            EditPlaylistDialog(
                initialName = playlistData.playlist.name, initialThumbnailUrl = playlistData.playlist.thumbnailUrl, fallbackThumbnails = playlistData.songThumbnails.filterNotNull(),
                onDismiss = { showEditDialog = false },
                onSave = { name, thumbnailUrl ->
                    database.query { update(playlistData.playlist.copy(name = name, thumbnailUrl = thumbnailUrl, lastUpdateTime = LocalDateTime.now())) }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlistData.playlist.browseId?.let { YouTube.renamePlaylist(it, name) } }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = { Text(stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    if (!editable) database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                    songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = { Text(stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showDeletePlaylistDialog = false
                    database.query { playlist?.let { delete(it.playlist) } }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) } }
                    navController.popBackStack()
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems = 2
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState, scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) (from.index - headerItems) to (to.index - headerItems) else currentDragInfo.first to (to.index - headerItems)
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
                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId -> YouTube.moveSongPlaylist(viewModel.playlist.value?.playlist?.browseId!!, setVideoId, successorSetVideoId) }
                    }
                }
                dragInfo = null
            }
        }
    }

    var dominantColor by remember { mutableStateOf<Color?>(null) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    dominantColor = Color(palette.getDominantColor(fallbackColor))
                }
            }
        }
    }

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40 } }
    val imageScrollOffset by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 0f } }
    val headerAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 400f)).coerceIn(0f, 1f) else 0f } }
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) else 0f } }

    val showTopBarTitle by remember { derivedStateOf { isScrolled } }
    val transparentAppBar by remember { derivedStateOf { !disableBlur && !inSelectMode && !isScrolled } }

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor).pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)) {
        if (!disableBlur) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(0.6f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                    val headerColor = dominantColor ?: surfaceColor
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(headerColor.copy(alpha = 0.45f * gradientAlpha), surfaceColor.copy(alpha = 0.8f * gradientAlpha), surfaceColor),
                            startY = 0f, endY = size.height
                        )
                    )
                }
            )
        }

        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                } else {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + 48.dp)
                                    .graphicsLayer { alpha = headerAlpha }.padding(horizontal = 24.dp).padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).graphicsLayer { translationY = imageScrollOffset * 0.5f }) {
                                    if (playlist.thumbnails.size == 1) {
                                        Surface(modifier = Modifier.size(240.dp).shadow(elevation = 32.dp, shape = RoundedCornerShape(12.dp), ambientColor = dominantColor ?: MaterialTheme.colorScheme.primary, spotColor = dominantColor ?: MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) {
                                            AsyncImage(model = playlist.thumbnails[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    } else if (playlist.thumbnails.size > 1) {
                                        Surface(modifier = Modifier.size(240.dp).shadow(elevation = 32.dp, shape = RoundedCornerShape(12.dp), ambientColor = dominantColor ?: MaterialTheme.colorScheme.primary, spotColor = dominantColor ?: MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                                                    AsyncImage(model = playlist.thumbnails.getOrNull(index), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(alignment).size(120.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(modifier = Modifier.size(240.dp).shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                }

                                Text(text = playlist.playlist.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) playlist.playlist.remoteSongCount else playlist.songCount
                                    MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songCount, songCount))
                                    Spacer(Modifier.width(12.dp))
                                    if (playlistLength > 0) MetadataChip(icon = R.drawable.timer, text = makeTimeString(playlistLength * 1000L))
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() })) },
                                        shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.play), null, Modifier.size(24.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.play), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() })) },
                                        shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.shuffle), null, Modifier.size(24.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.shuffle), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    if (editable) {
                                        Surface(onClick = { showDeletePlaylistDialog = true }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp)) }
                                        }
                                    } else {
                                        val isLiked = playlist.playlist.bookmarkedAt != null
                                        Surface(onClick = { database.transaction { update(playlist.playlist.toggleLike()) } }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    Surface(onClick = {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> { showRemoveDownloadDialog = true }
                                            Download.STATE_DOWNLOADING -> { songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) } }
                                            else -> { songs.forEach { song -> val req = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.song.title.toByteArray()).build(); DownloadService.sendAddDownload(context, ExoDownloadService::class.java, req, false) } }
                                        }
                                    }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                                else -> Icon(painterResource(R.drawable.download), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    Surface(onClick = { playerConnection.playQueue(LocalMixQueue(database = database, playlistId = playlist.id, maxMixSize = 50)) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.mix), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                                    }

                                    Surface(onClick = {
                                        if (editable) showEditDialog = true
                                        else if (playlist.playlist.browseId != null) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val playlistPage = YouTube.playlist(playlist.playlist.browseId).completed().getOrNull() ?: return@launch
                                                database.transaction {
                                                    clearPlaylist(playlist.id)
                                                    playlistPage.songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { position, song -> PlaylistSongMap(songId = song.id, playlistId = playlist.id, position = position, setVideoId = song.setVideoId) }.forEach(::insert)
                                                }
                                            }
                                            coroutineScope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced)) }
                                        }
                                    }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(painterResource(if (editable) R.drawable.edit else R.drawable.sync), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    item(key = "sort_header") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.wrapContentWidth()) {
                                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    SortHeader(
                                        sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { sortType -> when (sortType) { PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom; PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSongSortType.NAME -> R.string.sort_by_name; PlaylistSongSortType.ARTIST -> R.string.sort_by_artist; PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                                        modifier = Modifier,
                                    )
                                }
                            }

                            if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                Surface(onClick = { locked = !locked }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val displayedSongs = if (isSearching) filteredSongs else mutableSongs

            itemsIndexed(items = displayedSongs, key = { _, song -> song.map.id }) { index, song ->
                ReorderableItem(state = reorderableState, key = song.map.id, modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
                    val currentItem by rememberUpdatedState(song)
                    fun deleteFromPlaylist() {
                        val map = currentItem.map
                        coroutineScope.launch(Dispatchers.IO) {
                            database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) }
                            if (playlist?.playlist?.browseId != null) {
                                val setVideoId = map.setVideoId ?: database.getSetVideoId(map.songId)?.setVideoId
                                if (setVideoId != null) YouTube.removeFromPlaylist(playlist?.playlist?.browseId!!, map.songId, setVideoId)
                            }
                        }
                    }

                    val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it }, confirmValueChange = { it == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress })
                    var processedDismiss by remember { mutableStateOf(false) }
                    val swipeRemoveEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)
                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss = true; deleteFromPlaylist() }
                        if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                    }

                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(song.map.id) else selection.remove(Integer.valueOf(song.map.id)) }

                    val content: @Composable () -> Unit = {
                        SongListItem(
                            song = song.song, viewCountText = viewCounts[song.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                            isActive = song.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) Checkbox(checked = selection.contains(song.map.id), onCheckedChange = onCheckedChange)
                                else {
                                    IconButton(onClick = { menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                        IconButton(onClick = { }, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) { Icon(painterResource(R.drawable.drag_handle), null) }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = {
                                    if (inSelectMode) onCheckedChange(!selection.contains(song.map.id))
                                    else if (song.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id }))
                                },
                                onLongClick = {
                                    if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; onCheckedChange(true); selectionAnchorMapId = song.map.id }
                                    else {
                                        val anchorIndex = selectionAnchorMapId?.let { anchorMapId -> displayedSongs.indexOfFirst { it.map.id == anchorMapId } } ?: -1
                                        if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorMapId = song.map.id }
                                        else { val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex; for (rIndex in range) { val rMapId = displayedSongs[rIndex].map.id; if (rMapId !in selection) selection.add(rMapId) } }
                                    }
                                }
                            )
                        )
                    }

                    if (locked || inSelectMode || swipeRemoveEnabled) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                }
            }

            if (!inSelectMode && !isSearching) {
                item { PlaylistSuggestionsSection(modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (inSelectMode) Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search)) }, singleLine = true, shape = CircleShape,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) Text(playlist?.playlist?.name.orEmpty())
            },
            navigationIcon = {
                IconButton(onClick = { if (inSelectMode) onExitSelectionMode() else if (isSearching) { isSearching = false; query = TextFieldValue() } else navController.navigateUp() }) {
                    Icon(painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), null)
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(checked = selection.size == songs.size && selection.isNotEmpty(), onCheckedChange = { if (selection.size == songs.size) selection.clear() else { selection.clear(); selection.addAll(songs.map { it.map.id }) } })
                    IconButton(enabled = selection.isNotEmpty(), onClick = { menuState.show { SelectionSongMenu(songSelection = songs.filter { it.map.id in selection }.map { it.song }, songPosition = songs.filter { it.map.id in selection }.map { it.map }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
