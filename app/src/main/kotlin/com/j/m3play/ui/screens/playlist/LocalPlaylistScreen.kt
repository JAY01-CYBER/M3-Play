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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
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
import com.j.m3play.LocalSyncUtils
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
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
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.EditPlaylistDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.PlaylistTagChips
import com.j.m3play.ui.component.AssignTagsDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.component.shimmer.ButtonPlaceholder
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.screens.playlist.PlaylistSuggestionsSection
import com.j.m3play.ui.theme.PlayerColorExtractor
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
    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
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

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs
        else songs.filter { song ->
            song.song.song.title.contains(query.text, ignoreCase = true) ||
            song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    var selection by remember { mutableStateOf(false) }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    if (isSearching) { BackHandler { isSearching = false; query = TextFieldValue() } } 
    else if (selection) { BackHandler { selection = false } }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true
    
    // Play/Pause button logic
    val isPlaylistPlaying = remember(songs, mediaMetadata) { songs.fastAny { it.song.song.id == mediaMetadata?.id } }
    val showPause = isPlaylistPlaying && isPlaying

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); addAll(songs) }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
                downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                downloads[it.song.id]?.state == Download.STATE_COMPLETED
            }) { Download.STATE_DOWNLOADING } else { Download.STATE_STOPPED }
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
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) { database.transaction { playlist?.id?.let { clearPlaylist(it) } } }
                        songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                    }
                ) { Text(stringResource(android.R.string.ok)) }
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
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query { playlist?.let { delete(it.playlist) } }
                        viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) } }
                        navController.popBackStack()
                    }
                ) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            val hasContent = current != null && (current.songCount > 0 || current.playlist.remoteSongCount != 0)
            if (hasContent && !isSearching) 2 else 0
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
            dragInfo = if (currentDragInfo == null) { (from.index - headerItems) to (to.index - headerItems) } 
                       else { currentDragInfo.first to (to.index - headerItems) }
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

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50 } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val isTopBarSolid by remember { derivedStateOf { isScrolled || isSearching || selection } }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.background
    val darkOverlay = Color.Black.copy(alpha = 0.4f)

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                }
            }
        } else { gradientColors = emptyList() }
    }
    
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) { val offset = lazyListState.firstVisibleItemScrollOffset; (1f - (offset / 600f)).coerceIn(0f, 1f) } else { 0f } } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
    ) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(0.55f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                    val width = size.width
                    val height = size.height

                    if (gradientColors.size >= 3) {
                        val c0 = gradientColors[0]; val c1 = gradientColors[1]; val c2 = gradientColors[2]
                        val c3 = gradientColors.getOrElse(3) { c0 }; val c4 = gradientColors.getOrElse(4) { c1 }
                        drawRect(brush = Brush.radialGradient(colors = listOf(c0.copy(alpha = gradientAlpha * 0.75f), c0.copy(alpha = gradientAlpha * 0.4f), Color.Transparent), center = Offset(width * 0.5f, height * 0.15f), radius = width * 0.8f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c1.copy(alpha = gradientAlpha * 0.55f), c1.copy(alpha = gradientAlpha * 0.3f), Color.Transparent), center = Offset(width * 0.1f, height * 0.4f), radius = width * 0.6f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c2.copy(alpha = gradientAlpha * 0.5f), c2.copy(alpha = gradientAlpha * 0.25f), Color.Transparent), center = Offset(width * 0.9f, height * 0.35f), radius = width * 0.55f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c3.copy(alpha = gradientAlpha * 0.35f), c3.copy(alpha = gradientAlpha * 0.18f), Color.Transparent), center = Offset(width * 0.25f, height * 0.65f), radius = width * 0.75f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c4.copy(alpha = gradientAlpha * 0.3f), c4.copy(alpha = gradientAlpha * 0.15f), Color.Transparent), center = Offset(width * 0.55f, height * 0.85f), radius = width * 0.9f))
                    } else if (gradientColors.isNotEmpty()) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.7f), gradientColors[0].copy(alpha = gradientAlpha * 0.35f), Color.Transparent), center = Offset(width * 0.5f, height * 0.25f), radius = width * 0.85f))
                    }
                    drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = gradientAlpha * 0.22f), surfaceColor.copy(alpha = gradientAlpha * 0.55f), surfaceColor), startY = height * 0.4f, endY = height))
                }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item {
                        EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty))
                    }
                } else {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. Full-Width Edge-to-Edge Hero Image
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                    if (playlist.thumbnails.size == 1) {
                                        AsyncImage(model = playlist.thumbnails[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else if (playlist.thumbnails.size > 1) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                                                AsyncImage(model = playlist.thumbnails.getOrNull(index), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(alignment).fillMaxSize(0.5f))
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                            Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Smooth Gradient Fade
                                    Box(
                                        modifier = Modifier.matchParentSize().background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.8f), surfaceColor),
                                                startY = 0f, endY = Float.POSITIVE_INFINITY
                                            )
                                        )
                                    )
                                }

                                // 2. Centered Text Content
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = playlist.playlist.name,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) playlist.playlist.remoteSongCount else playlist.songCount
                                    val countText = pluralStringResource(R.plurals.n_song, songCount, songCount)
                                    val durationText = if (playlistLength > 0) " • ${makeTimeString(playlistLength * 1000L)}" else ""
                                    
                                    Text(
                                        text = "Playlist • $countText$durationText",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // 3. White Play Button & Circular Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Shuffle
                                    Surface(
                                        onClick = { playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() })) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        modifier = Modifier.size(52.dp)
                                    ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null) } }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // White Play Pill
                                    Button(
                                        onClick = { 
                                            if (isPlaylistPlaying) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = 0)) 
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(52.dp).width(130.dp)
                                    ) {
                                        Icon(painterResource(if (showPause) R.drawable.pause else R.drawable.play), null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (showPause) "Pause" else "Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Mix Button
                                    Surface(
                                        onClick = { playerConnection.playQueue(LocalMixQueue(database = database, playlistId = playlist.id, maxMixSize = 50)) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        modifier = Modifier.size(52.dp)
                                    ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.mix), null) } }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Song Count Header
                                Text(
                                    text = "${songs.size} tracks",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    // Sort Header
                    item(key = "sort_header") {
                        if (editable || isSearching) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                SortHeader(
                                    sortType = sortType, sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
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
                                    IconButton(onClick = { locked = !locked }, onLongClick = {}, modifier = Modifier.padding(horizontal = 6.dp)) {
                                        Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Flat Edge-to-Edge List Items
            if (!selection) {
                itemsIndexed(items = if (isSearching) filteredSongs else mutableSongs, key = { _, song -> song.map.id }) { index, song ->
                    ReorderableItem(
                        state = reorderableState, key = song.map.id,
                        modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    ) {
                        val currentItem by rememberUpdatedState(song)

                        fun deleteFromPlaylist() {
                            val map = currentItem.map
                            val browseId = playlist?.playlist?.browseId
                            coroutineScope.launch(Dispatchers.IO) {
                                database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) }
                                if (browseId != null) {
                                    val setVideoId = map.setVideoId ?: database.getSetVideoId(map.songId)?.setVideoId
                                    if (setVideoId != null) YouTube.removeFromPlaylist(browseId, map.songId, setVideoId)
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
                            if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss = true; deleteFromPlaylist() }
                            if (dv == SwipeToDismissBoxValue.Settled) { processedDismiss = false }
                        }

                        val content: @Composable () -> Unit = {
                            val isActive = song.song.id == mediaMetadata?.id
                            SongListItem(
                                song = song.song,
                                viewCountText = viewCounts[song.song.id]?.let { formatCompactCount(it.toLong()) },
                                isActive = isActive,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show { SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) }
                                        },
                                        onLongClick = {}
                                    ) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                        IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) {
                                            Icon(painterResource(R.drawable.drag_handle), null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (song.song.id == mediaMetadata?.id) { playerConnection.player.togglePlayPause() } 
                                            else { playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id })) }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true
                                        },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }

                        if (locked || selection || swipeToSongEnabled) { content() } else { SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() } }
                    }
                }
            } else {
                itemsIndexed(items = wrappedSongs, key = { _, song -> song.item.map.id }) { index, songWrapper ->
                    ReorderableItem(
                        state = reorderableState, key = songWrapper.item.map.id,
                        modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    ) {
                        val currentItem by rememberUpdatedState(songWrapper.item)

                        fun deleteFromPlaylist() {
                            val map = currentItem.map
                            coroutineScope.launch(Dispatchers.IO) { database.withTransaction { move(map.playlistId, map.position, Int.MAX_VALUE); delete(map.copy(position = Int.MAX_VALUE)) } }
                        }

                        val dismissBoxState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance },
                            confirmValueChange = { targetValue -> targetValue == SwipeToDismissBoxValue.Settled || !lazyListState.isScrollInProgress }
                        )
                        var processedDismiss2 by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss2 && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) { processedDismiss2 = true; deleteFromPlaylist() }
                            if (dv == SwipeToDismissBoxValue.Settled) { processedDismiss2 = false }
                        }

                        val content: @Composable () -> Unit = {
                            val isActive = songWrapper.item.song.id == mediaMetadata?.id
                            val isSelected = songWrapper.isSelected && selection
                            SongListItem(
                                song = songWrapper.item.song,
                                viewCountText = viewCounts[songWrapper.item.song.id]?.let { formatCompactCount(it.toLong()) },
                                isActive = isActive, isPlaying = isPlaying, showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { SongMenu(originalSong = songWrapper.item.song, playlistBrowseId = playlist?.playlist?.browseId, navController = navController, onDismiss = menuState::dismiss) } },
                                        onLongClick = {}
                                    ) { Icon(painterResource(R.drawable.more_vert), null) }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching && editable) {
                                        IconButton(onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }) {
                                            Icon(painterResource(R.drawable.drag_handle), null)
                                        }
                                    }
                                },
                                isSelected = isSelected,
                                modifier = Modifier.fillMaxWidth()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (isActive) { playerConnection.player.togglePlayPause() } 
                                                else { playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = index)) }
                                            } else { songWrapper.isSelected = !songWrapper.isSelected }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            songWrapper.isSelected = true
                                        },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }

                        if (locked || !editable || swipeToSongEnabled) { content() } else { SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() } }
                    }
                }
            }

            if (!selection && !isSearching) {
                item { PlaylistSuggestionsSection(modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

        // 5. YT Music Style Translucent Top App Bar
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isTopBarSolid) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge)
                } else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) },
                        singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).background(darkOverlay, RoundedCornerShape(50))
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.playlist?.name.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue() }
                        else if (selection) { selection = false }
                        else { navController.navigateUp() }
                    },
                    onLongClick = {},
                    modifier = Modifier.padding(start = 8.dp).background(if(!isTopBarSolid && !isSearching) darkOverlay else Color.Transparent, CircleShape)
                ) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = if(!isTopBarSolid && !isSearching) Color.White else MaterialTheme.colorScheme.onSurface) }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = {
                        if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true }
                    }, onLongClick = {}) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }
                    IconButton(onClick = {
                        menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song }, songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map }, onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }) }
                    }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) {
                    Row(
                        modifier = Modifier.padding(end = 8.dp).background(if(!isTopBarSolid) darkOverlay else Color.Transparent, RoundedCornerShape(50)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editable) {
                            IconButton(onClick = { showEditDialog = true }, onLongClick = {}) { Icon(painterResource(R.drawable.edit), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface) }
                        } else {
                            val liked = playlist?.playlist?.bookmarkedAt != null
                            IconButton(onClick = { database.transaction { playlist?.let { update(it.playlist.toggleLike()) } } }, onLongClick = {}) {
                                Icon(painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (liked) Color.Red else if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = { isSearching = true }, onLongClick = {}) { Icon(painterResource(R.drawable.search), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface) }
                        IconButton(onClick = {
                            if (playlist?.playlist?.browseId != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val playlistPage = YouTube.playlist(playlist!!.playlist.browseId!!).completed().getOrNull() ?: return@launch
                                    database.transaction {
                                        clearPlaylist(playlist!!.id)
                                        playlistPage.songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { position, song -> PlaylistSongMap(songId = song.id, playlistId = playlist!!.id, position = position, setVideoId = song.setVideoId) }.forEach(::insert)
                                    }
                                }
                                coroutineScope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced)) }
                            }
                        }, onLongClick = {}) { Icon(painterResource(R.drawable.sync), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}
