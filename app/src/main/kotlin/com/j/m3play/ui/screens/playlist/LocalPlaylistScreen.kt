/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Base: MetroList Original Structure        │
 * │  Style: M3PLAY::UI::EXPRESSIVE::PREMIUM    │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.innertube.YouTube
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.LocalSyncUtils
import com.j.m3play.R
import com.j.m3play.constants.DarkModeKey
import com.j.m3play.constants.PlaylistEditLockKey
import com.j.m3play.constants.PlaylistSongSortDescendingKey
import com.j.m3play.constants.PlaylistSongSortType
import com.j.m3play.constants.PlaylistSongSortTypeKey
import com.j.m3play.constants.SwipeToRemoveSongKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.PlaylistSong
import com.j.m3play.extensions.move
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.ActionPromptDialog
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.OverlayEditButton
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.component.TextFieldDialog
import com.j.m3play.ui.menu.CustomThumbnailMenu
import com.j.m3play.ui.menu.LocalPlaylistMenu
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.reportException
import com.j.m3play.viewmodels.LocalPlaylistViewModel
import com.yalantis.ucrop.UCrop
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.playlistSongs.collectAsStateWithLifecycle()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { song -> song.song.song.title.contains(query.text, ignoreCase = true) || song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) } }
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
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = { Icon(painterResource(R.drawable.edit), null) }, title = { Text(stringResource(R.string.edit_playlist)) }, onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(playlistEntity.name, TextRange(playlistEntity.name.length)),
                onDone = { name ->
                    database.query { update(playlistEntity.copy(name = name, lastUpdateTime = LocalDateTime.now())) }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) } }
                }
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

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40 } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item(key = "empty_placeholder") { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            LocalPlaylistHeader(
                                playlist = playlist, songs = songs, onShowEditDialog = { showEditDialog = true }, onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                onshowDeletePlaylistDialog = { showDeletePlaylistDialog = true }, onStartSearch = { isSearching = true }, snackbarHostState = snackbarHostState
                            )
                        }
                    }

                    item(key = "controls_row") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.wrapContentWidth()) {
                                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    SortHeader(
                                        sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { sortType -> when (sortType) { PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom; PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSongSortType.NAME -> R.string.sort_by_name; PlaylistSongSortType.ARTIST -> R.string.sort_by_artist; PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                                        modifier = Modifier
                                    )
                                }
                            }
                            if (editable) {
                                Surface(onClick = { locked = !locked }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }

            val displayedSongs = if (isSearching) filteredSongs else mutableSongs

            itemsIndexed(items = displayedSongs, key = { _, song -> song.map.id }) { index, song ->
                ReorderableItem(state = reorderableState, key = song.map.id) {
                    val currentItem by rememberUpdatedState(song)
                    fun deleteFromPlaylist() {
                        val browseId = playlist?.playlist?.browseId
                        val setVideoId = currentItem.map.setVideoId
                        val songId = currentItem.map.songId
                        val playlistId = currentItem.map.playlistId
                        database.transaction { move(playlistId, currentItem.map.position, Int.MAX_VALUE); delete(currentItem.map.copy(position = Int.MAX_VALUE)) }
                        if (browseId != null) {
                            syncUtils.scheduleRemoveFromPlaylist(browseId, songId, playlistId) {
                                var sVideoId: String? = setVideoId
                                if (sVideoId == null) { for (attempt in 0 until 10) { sVideoId = database.getSetVideoId(songId)?.setVideoId; if (sVideoId != null) break; delay(3_000L) } }
                                sVideoId
                            }
                        }
                    }

                    val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
                    val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it })
                    var processedDismiss by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (swipeRemoveEnabled && !processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss = true; deleteFromPlaylist() }
                        if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                    }

                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(song.map.id) else selection.remove(Integer.valueOf(song.map.id)) }

                    val content: @Composable () -> Unit = {
                        SongListItem(
                            song = song.song, isActive = song.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) Checkbox(checked = selection.contains(song.map.id), onCheckedChange = onCheckedChange)
                                else {
                                    IconButton(onClick = { menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) { IconButton(onClick = { }, modifier = Modifier.draggableHandle()) { Icon(painterResource(R.drawable.drag_handle), null) } }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (inSelectMode) onCheckedChange(!selection.contains(song.map.id)) else if (song.song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id })) },
                                onLongClick = {
                                    if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; onCheckedChange(true); selectionAnchorMapId = song.map.id }
                                    else {
                                        val anchorIndex = selectionAnchorMapId?.let { anchorMapId -> displayedSongs.indexOfFirst { it.map.id == anchorMapId } } ?: -1
                                        if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorMapId = song.map.id }
                                        else { val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex; for (rangeIndex in range) { val rangeMapId = displayedSongs[rangeIndex].map.id; if (rangeMapId !in selection) selection.add(rangeMapId) } }
                                    }
                                }
                            )
                        )
                    }
                    if (locked || inSelectMode || !swipeRemoveEnabled) Box { content() } else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = 2)

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (!isScrolled) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (inSelectMode) Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size), style = MaterialTheme.typography.titleLarge)
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium) }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium, shape = CircleShape,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) Text(playlist?.playlist?.name.orEmpty())
            },
            navigationIcon = {
                if (inSelectMode) IconButton(onClick = onExitSelectionMode) { Icon(painterResource(R.drawable.close), null) }
                else IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue() } else navController.navigateUp() }, onLongClick = { if (!isSearching) navController.backToMain() }) { Icon(painterResource(R.drawable.arrow_back), null) }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(checked = selection.size == songs.size && selection.isNotEmpty(), onCheckedChange = { if (selection.size == songs.size) selection.clear() else { selection.clear(); selection.addAll(songs.map { it.map.id }) } })
                    IconButton(enabled = selection.isNotEmpty(), onClick = { menuState.show { SelectionSongMenu(songSelection = selection.mapNotNull { mId -> songs.find { it.map.id == mId }?.song }, songPosition = selection.mapNotNull { mId -> songs.find { it.map.id == mId }?.map }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist, songs: List<PlaylistSong>, onShowEditDialog: () -> Unit, onShowRemoveDownloadDialog: () -> Unit, onshowDeletePlaylistDialog: () -> Unit, onStartSearch: () -> Unit, snackbarHostState: SnackbarHostState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()

    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist.playlist.isEditable
    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail by remember { mutableStateOf(playlist.thumbnails.firstOrNull()?.let { it.contains("studio_square_thumbnail") || it.contains("content://") } ?: false) }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 48.dp).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            Surface(modifier = Modifier.size(240.dp).shadow(elevation = 24.dp, shape = RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp)) {
                AsyncImage(model = overrideThumbnail.value ?: playlist.thumbnails.firstOrNull(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
            }
            if (editable) OverlayEditButton(visible = true, alignment = Alignment.BottomEnd, onClick = { if (isCustomThumbnail) menuState.show { CustomThumbnailMenu(onEdit = { pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)) }, onRemove = { overrideThumbnail.value = null; isCustomThumbnail = false; database.query { update(playlist.playlist.copy(thumbnailUrl = null)) } }, onDismiss = menuState::dismiss) } else showEditNoteDialog = true })
        }

        Text(text = playlist.playlist.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songs.size, songs.size))
            if (playlistLength > 0) { Spacer(Modifier.width(12.dp)); MetadataChip(icon = R.drawable.timer, text = makeTimeString(playlistLength * 1000L)) }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() })) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(24.dp)) }
            }
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() })) }, color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painterResource(R.drawable.play), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            }
            Surface(onClick = { menuState.show { LocalPlaylistMenu(playlist = playlist, songs = songs, context = context, downloadState = downloadState, onEdit = onShowEditDialog, onSync = {}, onDelete = onshowDeletePlaylistDialog, onDownload = {}, onQueue = { playerConnection.addToQueue(songs.map { it.song.toMediaItem() }) }, onDismiss = menuState::dismiss) } }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.more_vert), null, modifier = Modifier.size(24.dp)) }
            }
        }
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

fun uriToByteArray(context: Context, uri: Uri): ByteArray? = try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (_: SecurityException) { null }
