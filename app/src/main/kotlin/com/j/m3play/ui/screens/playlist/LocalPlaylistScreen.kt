/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.compose.AsyncImage
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.LocalSyncUtils
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
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
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.LocalPlaylistViewModel
import com.valentinilk.shimmer.shimmer
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime
import kotlin.math.abs

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

    if (showAssignTagsDialog && playlist != null) {
        AssignTagsDialog(database = database, playlistId = playlist!!.id, onDismiss = { showAssignTagsDialog = false })
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs
        else songs.filter { song -> song.song.song.title.contains(query.text, ignoreCase = true) || song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) } }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    var selection by remember { mutableStateOf(false) }

    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }.toMutableStateList()

    if (isSearching) { BackHandler { isSearching = false; query = TextFieldValue() } } 
    else if (selection) { BackHandler { selection = false } }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

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
                initialName = playlistData.playlist.name, initialThumbnailUrl = playlistData.playlist.thumbnailUrl, fallbackThumbnails = playlistData.songThumbnails.filterNotNull(), onDismiss = { showEditDialog = false },
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
            content = { Text(text = stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist?.name ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    if (!editable) database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                    songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                }) { Text(text = stringResource(android.R.string.ok)) }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = { Text(text = stringResource(R.string.delete_playlist_confirm, playlist?.playlist?.name ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showDeletePlaylistDialog = false
                    database.query { playlist?.let { delete(it.playlist) } }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) } }
                    navController.popBackStack()
                }) { Text(text = stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems by remember { derivedStateOf { val current = playlist; val hasContent = current != null && (current.songCount > 0 || current.playlist.remoteSongCount != 0); if (hasContent && !isSearching) 2 else 0 } }
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
                val currentBrowseId = viewModel.playlist.value?.playlist?.browseId
                if (currentBrowseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId
                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId -> YouTube.moveSongPlaylist(currentBrowseId, setVideoId, successorSetVideoId) }
                    }
                }
                dragInfo = null
            }
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val baseDark = Color(0xFF121212)

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    val colorInt = palette.getDarkMutedColor(palette.getDominantColor(baseDark.toArgb()))
                    gradientColors = listOf(Color(colorInt))
                }
            }
        } else if (playlist != null) {
            val hash = playlist!!.playlist.name.hashCode()
            val hue = ((hash and 0xFF) / 255f) * 360f
            gradientColors = listOf(hsvToColor(hue, 0.6f, 0.3f))
        } else { 
            gradientColors = emptyList() 
        }
    }

    val bgColor by remember(gradientColors) {
        derivedStateOf {
            val color = gradientColors.firstOrNull() ?: baseDark
            lerp(color, baseDark, 0.85f)
        }
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                playlist?.let { playlist ->
                    if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                        item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                    } else {
                        if (!isSearching) {
                            item(key = "header") {
                                Column(
                                    modifier = Modifier.fillMaxWidth().animateItem(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(systemBarsTopPadding + AppBarHeight))

                                    // Square Thumbnail with rounded corners
                                    Surface(
                                        modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    ) {
                                        if (playlist.thumbnails.size == 1) {
                                            AsyncImage(model = playlist.thumbnails[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else if (playlist.thumbnails.size > 1) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                                                    AsyncImage(model = playlist.thumbnails.getOrNull(index), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(alignment).size(130.dp))
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.5f))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Text Information (White)
                                    Text(
                                        text = playlist.playlist.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val metadataText = buildString {
                                        append("Local Playlist")
                                        if (playlistLength > 0) append(" • ${makeTimeString(playlistLength * 1000L)}")
                                    }
                                    Text(
                                        text = metadataText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Integrated Search Bar below text
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .height(48.dp)
                                            .clickable { isSearching = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.search), contentDescription = null, tint = Color.White)
                                            Spacer(Modifier.width(12.dp))
                                            Text(stringResource(R.string.search), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Action Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Shuffle
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                                .clickable { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() })) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }

                                        // Play Pill (White Background, Black Text)
                                        Box(
                                            modifier = Modifier
                                                .height(56.dp)
                                                .widthIn(min = 160.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White)
                                                .clickable { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() })) }
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.play),
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(28.dp),
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Play",
                                                    color = Color.Black,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Download Status
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                                .clickable {
                                                    when (downloadState) {
                                                        Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                                                        Download.STATE_DOWNLOADING -> songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                                                        else -> songs.forEach { song ->
                                                            val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.song.title.toByteArray()).build()
                                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                                else -> Icon(painterResource(R.drawable.download), null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                    
                                    val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) playlist.playlist.remoteSongCount else playlist.songCount
                                    Text(
                                        text = pluralStringResource(R.plurals.n_song, songCount ?: 0, songCount ?: 0),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        item(key = "sort_header") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                SortHeader(
                                    sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType -> when (sortType) { PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom; PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSongSortType.NAME -> R.string.sort_by_name; PlaylistSongSortType.ARTIST -> R.string.sort_by_artist; PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                                    modifier = Modifier.weight(1f),
                                )
                                if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                                    IconButton(onClick = { locked = !locked }, onLongClick = {}, modifier = Modifier.padding(horizontal = 6.dp)) { Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, tint = Color.White.copy(alpha = 0.7f)) }
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
                                val map = currentItem.map
                                val currentBrowseId = playlist?.playlist?.browseId
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) }
                                    if (currentBrowseId != null) {
                                        val setVideoId = map.setVideoId ?: database.getSetVideoId(map.songId)?.setVideoId
                                        if (setVideoId != null) YouTube.removeFromPlaylist(currentBrowseId, map.songId, setVideoId)
                                    }
                                }
                            }

                            val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it }, confirmValueChange = { it == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress })
                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss = true; deleteFromPlaylist() }
                                if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                            }

                            val content: @Composable () -> Unit = {
                                val isActive = song.song.id == mediaMetadata?.id
                                val itemModifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                
                                SongListItem(
                                    song = song.song,
                                    viewCountText = viewCounts[song.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                    isActive = isActive, isPlaying = isPlaying, showInLibraryIcon = true,
                                    trailingContent = {
                                        IconButton(onClick = { menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White.copy(alpha = 0.7f)) }
                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) { IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) { Icon(painterResource(R.drawable.drag_handle), null, tint = Color.White.copy(alpha = 0.7f)) } }
                                    },
                                    modifier = itemModifier.combinedClickable(
                                        onClick = {
                                            if (isActive) playerConnection.player.togglePlayPause()
                                            else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id }))
                                        },
                                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true }
                                    )
                                }

                            if (locked || selection || swipeToSongEnabled) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                        }
                    }
                } else {
                    itemsIndexed(items = wrappedSongs, key = { _, song -> song.item.map.id }) { index, songWrapper ->
                        ReorderableItem(state = reorderableState, key = songWrapper.item.map.id, modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
                            val currentItem by rememberUpdatedState(songWrapper.item)
                            fun deleteFromPlaylist() {
                                val map = currentItem.map
                                coroutineScope.launch(Dispatchers.IO) { database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) } }
                            }

                            val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it }, confirmValueChange = { it == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress })
                            var processedDismiss2 by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss2 && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss2 = true; deleteFromPlaylist() }
                                if (dv == SwipeToDismissBoxValue.Settled) processedDismiss2 = false
                            }

                            val content: @Composable () -> Unit = {
                                val isActive = songWrapper.item.song.id == mediaMetadata?.id
                                val itemModifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                
                                SongListItem(
                                    song = songWrapper.item.song,
                                    viewCountText = viewCounts[songWrapper.item.song.id]?.let { count -> formatCompactCount(count.toLong()) },
                                    isActive = isActive, isPlaying = isPlaying, isSelected = songWrapper.isSelected && selection, showInLibraryIcon = true,
                                    trailingContent = {
                                        IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item.song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White.copy(alpha = 0.7f)) }
                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) { IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) { Icon(painterResource(R.drawable.drag_handle), null, tint = Color.White.copy(alpha = 0.7f)) } }
                                    },
                                    modifier = itemModifier.combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (isActive) playerConnection.player.togglePlayPause()
                                                else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = index))
                                            } else { songWrapper.isSelected = !songWrapper.isSelected }
                                        },
                                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                                    )
                                }

                            if (locked || !editable || swipeToSongEnabled) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                        }
                    }
                }

                if (!selection && !isSearching) { item { PlaylistSuggestionsSection(modifier = Modifier.padding(vertical = 16.dp)) } }
            }

            DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSearching || showTopBarTitle) bgColor.copy(alpha = 0.95f) else Color.Transparent,
                    scrolledContainerColor = bgColor.copy(alpha = 0.95f)
                ),
                title = {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(text = pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    } else if (isSearching) {
                        TextField(value = query, onValueChange = { query = it }, placeholder = { Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.7f)) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                    } else if (showTopBarTitle) {
                        Text(playlist?.playlist?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue() }
                        else if (selection) { selection = false }
                        else { navController.navigateUp() }
                    }, onLongClick = { if (!isSearching) navController.backToMain() }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = Color.White) }
                },
                actions = {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(onClick = {
                            if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                            else wrappedSongs.forEach { it.isSelected = true }
                        }, onLongClick = {}) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = Color.White) }
                        IconButton(onClick = { menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song }, songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map }, onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                    } else if (!isSearching) {
                        if (editable) {
                            IconButton(onClick = { showEditDialog = true }, onLongClick = {}) { Icon(painterResource(R.drawable.edit), null, tint = Color.White) }
                            IconButton(onClick = { showDeletePlaylistDialog = true }, onLongClick = {}) { Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error) }
                        } else {
                            val liked = playlist?.playlist?.bookmarkedAt != null
                            IconButton(
                                onClick = { database.transaction { playlist?.let { update(it.playlist.toggleLike()) } } },
                                onLongClick = {}
                            ) {
                                Icon(painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (liked) MaterialTheme.colorScheme.error else Color.White)
                            }
                            val currentBrowseId = playlist?.playlist?.browseId
                            if (currentBrowseId != null) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val playlistPage = YouTube.playlist(currentBrowseId).completed().getOrNull() ?: return@launch
                                            database.transaction {
                                                clearPlaylist(playlist!!.id)
                                                playlistPage.songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { position, song -> PlaylistSongMap(songId = song.id, playlistId = playlist!!.id, position = position, setVideoId = song.setVideoId) }.forEach(::insert)
                                            }
                                        }
                                        coroutineScope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced)) }
                                    },
                                    onLongClick = {}
                                ) { Icon(painterResource(R.drawable.sync), null, tint = Color.White) }
                            }
                        }
                        IconButton(onClick = { isSearching = true }, onLongClick = {}) { Icon(painterResource(R.drawable.search), null, tint = Color.White) }
                    }
                }
            )

            PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
        }
    }
}

private fun generateGradientFromTitle(title: String): List<Color> {
    val hash = title.hashCode()
    val hue1 = ((hash and 0xFF) / 255f) * 360f
    val hue2 = (((hash shr 8) and 0xFF) / 255f) * 360f
    return listOf(hsvToColor(hue1, 0.7f, 0.9f), hsvToColor(hue2, 0.7f, 0.85f))
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val h = hue / 60f
    val c = value * saturation
    val x = c * (1 - abs((h % 2) - 1))
    val m = value - c
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    return Color(red = (r + m), green = (g + m), blue = (b + m), alpha = 1f)
}
