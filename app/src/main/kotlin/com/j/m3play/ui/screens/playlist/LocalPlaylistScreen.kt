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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
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
import com.j.m3play.ui.component.*
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
    val filteredSongs = remember(songs, query) { if (query.text.isEmpty()) songs else songs.filter { s -> s.song.song.title.contains(query.text, true) || s.song.artists.fastAny { it.name.contains(query.text, true) } } }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    var selection by remember { mutableStateOf(false) }
    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { ItemWrapper(it) } }.toMutableStateList()

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); addAll(songs) }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) Download.STATE_COMPLETED
                else if (songs.all { downloads[it.song.id]?.state in listOf(Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_COMPLETED) }) Download.STATE_DOWNLOADING
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
            }
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = { Text(stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { showDeletePlaylistDialog = false; database.query { playlist?.let { delete(it.playlist) } }; viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) } }; navController.popBackStack() }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems by remember { derivedStateOf { val current = playlist; if (current != null && (current.songCount > 0 || current.playlist.remoteSongCount != 0) && !isSearching) 2 else 0 } }
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

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
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
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
                }
            }
        } else gradientColors = emptyList()
    }

    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) else 0f } }
    val transparentAppBar by remember { derivedStateOf { !disableBlur && !selection && !showTopBarTitle } }

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor).pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(0.55f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                    val width = size.width; val height = size.height
                    if (gradientColors.size >= 3) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.75f), gradientColors[0].copy(alpha = gradientAlpha * 0.4f), Color.Transparent), center = Offset(width * 0.5f, height * 0.15f), radius = width * 0.8f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[1].copy(alpha = gradientAlpha * 0.55f), gradientColors[1].copy(alpha = gradientAlpha * 0.3f), Color.Transparent), center = Offset(width * 0.1f, height * 0.4f), radius = width * 0.6f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[2].copy(alpha = gradientAlpha * 0.5f), gradientColors[2].copy(alpha = gradientAlpha * 0.25f), Color.Transparent), center = Offset(width * 0.9f, height * 0.35f), radius = width * 0.55f))
                    } else if (gradientColors.isNotEmpty()) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.7f), gradientColors[0].copy(alpha = gradientAlpha * 0.35f), Color.Transparent), center = Offset(width * 0.5f, height * 0.25f), radius = width * 0.85f))
                    }
                    drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = gradientAlpha * 0.22f), surfaceColor.copy(alpha = gradientAlpha * 0.55f), surfaceColor), startY = height * 0.4f, endY = height))
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
                                modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + 48.dp).padding(horizontal = 20.dp).padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                                    if (playlist.thumbnails.size == 1) {
                                        Surface(modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(24.dp), spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), shape = RoundedCornerShape(24.dp)) {
                                            AsyncImage(model = playlist.thumbnails[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    } else if (playlist.thumbnails.size > 1) {
                                        Surface(modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(24.dp), spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), shape = RoundedCornerShape(24.dp)) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                                                    AsyncImage(model = playlist.thumbnails.getOrNull(index), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(alignment).size(130.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                }

                                Text(text = playlist.playlist.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                val songCountText = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) pluralStringResource(R.plurals.n_song, playlist.playlist.remoteSongCount, playlist.playlist.remoteSongCount) else pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
                                val durationText = if (playlistLength > 0) " • ${makeTimeString(playlistLength * 1000L)}" else ""
                                Text(text = "$songCountText$durationText", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { playerConnection.playQueue(ListQueue(playlist.playlist.name, songs.map { it.song.toMediaItem() })) }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) {
                                        Icon(painterResource(R.drawable.play), stringResource(R.string.play), modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Play", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(onClick = { playerConnection.playQueue(ListQueue(playlist.playlist.name, songs.shuffled().map { it.song.toMediaItem() })) }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) {
                                        Icon(painterResource(R.drawable.shuffle), stringResource(R.string.shuffle), modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Shuffle", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    if (editable) {
                                        Surface(onClick = { showDeletePlaylistDialog = true }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                                        }
                                    } else {
                                        val liked = playlist.playlist.bookmarkedAt != null
                                        Surface(onClick = { database.transaction { update(playlist.playlist.toggleLike()) } }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                                                Download.STATE_DOWNLOADING -> songs.forEach { s -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, s.song.id, false) }
                                                else -> songs.forEach { s -> DownloadService.sendAddDownload(context, ExoDownloadService::class.java, DownloadRequest.Builder(s.song.id, s.song.id.toUri()).setCustomCacheKey(s.song.id).setData(s.song.song.title.toByteArray()).build(), false) }
                                            }
                                        }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                                                else -> Icon(painterResource(R.drawable.download), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (editable) showEditDialog = true
                                            else if (playlist.playlist.browseId != null) {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val page = YouTube.playlist(playlist.playlist.browseId).completed().getOrNull() ?: return@launch
                                                    database.transaction { clearPlaylist(playlist.id); page.songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { p, s -> PlaylistSongMap(songId = s.id, playlistId = playlist.id, position = p, setVideoId = s.setVideoId) }.forEach(::insert) }
                                                }
                                                coroutineScope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced)) }
                                            }
                                        }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (editable) R.drawable.edit else R.drawable.sync), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    item(key = "sort_header") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                            SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { t -> when (t) { PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom; PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSongSortType.NAME -> R.string.sort_by_name; PlaylistSongSortType.ARTIST -> R.string.sort_by_artist; PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } }, modifier = Modifier.weight(1f))
                            if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                IconButton(onClick = { locked = !locked }, onLongClick = {}, modifier = Modifier.padding(horizontal = 6.dp)) { Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null) }
                            }
                        }
                    }
                }
            }

            if (!selection) {
                itemsIndexed(items = if (isSearching) filteredSongs else mutableSongs, key = { _, song -> song.map.id }) { index, song ->
                    ReorderableItem(state = reorderableState, key = song.map.id, modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
                        val currentItem by rememberUpdatedState(song)
                        fun deleteFromPlaylist() {
                            val map = currentItem.map; val browseId = playlist?.playlist?.browseId
                            coroutineScope.launch(Dispatchers.IO) {
                                database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) }
                                if (browseId != null) { val setVideoId = map.setVideoId ?: database.getSetVideoId(map.songId)?.setVideoId; if (setVideoId != null) YouTube.removeFromPlaylist(browseId, map.songId, setVideoId) }
                            }
                        }
                        val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it }, confirmValueChange = { it == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress })
                        var processedDismiss by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) { val dv = dismissBoxState.currentValue; if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss = true; deleteFromPlaylist() }; if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false }
                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = song.song, viewCountText = viewCounts[song.song.id]?.let { formatCompactCount(it.toLong()) }, isActive = song.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(onClick = { menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) { IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) { Icon(painterResource(R.drawable.drag_handle), null) } }
                                },
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = { if (song.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() }, songs.indexOfFirst { it.map.id == song.map.id })) },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true }
                                )
                            )
                        }
                        if (locked || selection || swipeToSongEnabled) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                    }
                }
            } else {
                itemsIndexed(items = wrappedSongs, key = { _, song -> song.item.map.id }) { index, songWrapper ->
                    ReorderableItem(state = reorderableState, key = songWrapper.item.map.id, modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
                        val currentItem by rememberUpdatedState(songWrapper.item)
                        fun deleteFromPlaylist() { coroutineScope.launch(Dispatchers.IO) { database.withTransaction { move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE); delete(currentItem.map.copy(position = Int.MAX_VALUE)) } } }
                        val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it }, confirmValueChange = { it == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress })
                        var processedDismiss2 by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) { val dv = dismissBoxState.currentValue; if (!processedDismiss2 && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss2 = true; deleteFromPlaylist() }; if (dv == SwipeToDismissBoxValue.Settled) processedDismiss2 = false }
                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = songWrapper.item.song, viewCountText = viewCounts[songWrapper.item.song.id]?.let { formatCompactCount(it.toLong()) }, isActive = songWrapper.item.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item.song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) { Icon(painterResource(R.drawable.drag_handle), null) }
                                },
                                isSelected = songWrapper.isSelected && selection,
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = { if (!selection) { if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() }, index)) } else songWrapper.isSelected = !songWrapper.isSelected },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                                )
                            )
                        }
                        if (locked || !editable || swipeToSongEnabled) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                    }
                }
            }
            if (!selection && !isSearching) item { PlaylistSuggestionsSection(modifier = Modifier.padding(vertical = 16.dp)) }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (selection) Text(pluralStringResource(R.plurals.n_song, wrappedSongs.count { it.isSelected }, wrappedSongs.count { it.isSelected }), style = MaterialTheme.typography.titleLarge)
                else if (isSearching) TextField(value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                else if (showTopBarTitle) Text(playlist?.playlist?.name.orEmpty())
            },
            navigationIcon = { IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue() } else if (selection) selection = false else navController.navigateUp() }, onLongClick = { if (!isSearching) navController.backToMain() }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null) } },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = { if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }
                    IconButton(onClick = { menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song }, songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map }, onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}
