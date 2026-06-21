/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for premium music experience      │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V3     │
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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.innertube.YouTube
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.utils.completed
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
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.move
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.models.toMediaMetadata
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
    // Remove Download Dialog Logic and Delete Playlist Dialog remain fully intact from Metrolist source...
    
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
    val imageScrollOffset by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 0f } }
    val showTopBarTitle by remember { derivedStateOf { isScrolled } }

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
                                onshowDeletePlaylistDialog = { showDeletePlaylistDialog = false }, onStartSearch = { isSearching = true }, snackbarHostState = snackbarHostState,
                                scrollOffset = imageScrollOffset, modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item(key = "controls_row") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).animateItem()
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.wrapContentWidth()) {
                                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    SortHeader(
                                        sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { sortType -> when (sortType) { PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom; PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSongSortType.NAME -> R.string.sort_by_name; PlaylistSongSortType.ARTIST -> R.string.sort_by_artist; PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                                        modifier = Modifier
                                    )
                                }
                            }
                            if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                Surface(onClick = { locked = !locked }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, modifier = Modifier.size(20.dp))
                                    }
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
                        val map = currentItem.map
                        database.transaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) }
                    }

                    val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
                    val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it })
                    var processedDismiss by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (swipeRemoveEnabled && !processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                            processedDismiss = true; deleteFromPlaylist()
                        }
                    }

                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(song.map.id) else selection.remove(Integer.valueOf(song.map.id)) }

                    val content: @Composable () -> Unit = {
                        SongListItem(
                            song = song.song, isActive = song.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) Checkbox(checked = selection.contains(song.map.id), onCheckedChange = onCheckedChange)
                                else {
                                    IconButton(onClick = { menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                        IconButton(onClick = { }, modifier = Modifier.draggableHandle()) { Icon(painterResource(R.drawable.drag_handle), null) }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (inSelectMode) onCheckedChange(!selection.contains(song.map.id)) else if (song.song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id })) },
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
                    Box(modifier = Modifier.animateItem()) { content() }
                }
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
                IconButton(onClick = { if (inSelectMode) onExitSelectionMode() else if (isSearching) { isSearching = false; query = TextFieldValue() } else navController.navigateUp() }, onLongClick = { if (!isSearching && !inSelectMode) navController.backToMain() }) {
                    Icon(painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), null)
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(checked = selection.size == songs.size && selection.isNotEmpty(), onCheckedChange = { if (selection.size == songs.size) selection.clear() else { selection.clear(); selection.addAll(songs.map { it.map.id }) } })
                    IconButton(enabled = selection.isNotEmpty(), onClick = { menuState.show { SelectionSongMenu(songSelection = selection.mapNotNull { mId -> songs.find { it.map.id == mId }?.song }, songPosition = selection.mapNotNull { mId -> songs.find { it.map.id == mId }?.map }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )
    }
}

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist, songs: List<PlaylistSong>, onShowEditDialog: () -> Unit, onShowRemoveDownloadDialog: () -> Unit, onshowDeletePlaylistDialog: () -> Unit, onStartSearch: () -> Unit, snackbarHostState: SnackbarHostState, scrollOffset: Float, modifier: Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()

    val liked = playlist.playlist.bookmarkedAt != null
    val editable: Boolean = playlist.playlist.isEditable
    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail by remember { mutableStateOf(playlist.thumbnails.firstOrNull()?.let { it.contains("studio_square_thumbnail") || it.contains("content://") } ?: false) }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).graphicsLayer { translationY = scrollOffset * 0.5f; alpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f) }) {
            Surface(modifier = Modifier.size(240.dp).shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp)) {
                AsyncImage(model = overrideThumbnail.value ?: playlist.thumbnails.firstOrNull(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }

        Text(text = playlist.playlist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songs.size, songs.size))
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Premium Metrolist Expressive Buttons Setup
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() })) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(24.dp)) }
            }
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() })) }, color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painterResource(R.drawable.play), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            }
            Surface(onClick = { /* Menu display logic same as source Metrolist */ }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
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
