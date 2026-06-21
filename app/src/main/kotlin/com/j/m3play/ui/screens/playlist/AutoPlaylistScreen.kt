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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.AutoPlaylistSongSortDescendingKey
import com.j.m3play.constants.AutoPlaylistSongSortType
import com.j.m3play.constants.AutoPlaylistSongSortTypeKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.AutoPlaylistViewModel

enum class PlaylistType {
    LIKE, DOWNLOAD, OTHER
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist =
        if (viewModel.playlist == "liked") stringResource(R.string.liked) else stringResource(R.string.offline)

    val songs by viewModel.likedSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val playlistType = when (viewModel.playlist) {
        "liked" -> PlaylistType.LIKE
        "downloaded" -> PlaylistType.DOWNLOAD
        else -> PlaylistType.OTHER
    }

    val wrappedSongs = remember(songs) {
        songs?.map { item -> ItemWrapper(item) }?.toMutableStateList() ?: mutableStateListOf()
    }

    var selection by remember { mutableStateOf(false) }

    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(AutoPlaylistSongSortTypeKey, AutoPlaylistSongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AutoPlaylistSongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                if (playlistType == PlaylistType.LIKE) {
                    isRefreshing = true
                    viewModel.syncLikedSongs()
                    isRefreshing = false
                }
            }
        }
    }

    // Play/Pause Smart Button logic
    val isPlaylistPlaying = remember(songs, mediaMetadata) { songs?.fastAny { it.song.id == mediaMetadata?.id } == true }
    val showPause = isPlaylistPlaying && isPlaying

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); songs?.let { addAll(it) } }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) Download.STATE_COMPLETED
                else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                        downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                        downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true) Download.STATE_DOWNLOADING
                else Download.STATE_STOPPED
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { wrapper ->
            val song = wrapper.item
            song.song.title.contains(query.text, true) || song.artists.any { it.name.contains(query.text, true) }
        }
    }

    val lazyListState = rememberLazyListState()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.background
    val darkOverlay = Color.Black.copy(alpha = 0.4f)

    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.song?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate()
                    }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                }
            }
        } else { gradientColors = emptyList() }
    }

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50 } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val isTopBarSolid by remember { derivedStateOf { isScrolled || isSearching || selection } }

    val headerItems by remember { derivedStateOf { if (songs != null && songs!!.isNotEmpty() && !isSearching) 2 else 0 } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = {
                if (playlistType == PlaylistType.LIKE) {
                    isRefreshing = true
                    viewModel.syncLikedSongs()
                    isRefreshing = false
                }
            }),
    ) {
        LazyColumn(
            state = lazyListState,
            // 0 content padding at top so image goes edge-to-edge
            contentPadding = PaddingValues(0.dp),
        ) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item {
                        EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty))
                    }
                } else {
                    if (!isSearching) {
                        // SimpMusic Style Hero Header
                        item(key = "header") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. Edge-to-Edge Hero Image with Inner Text
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                    AsyncImage(
                                        model = songs!!.firstOrNull()?.song?.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Smooth Gradient Fade at Bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Transparent,
                                                        surfaceColor.copy(alpha = 0.5f),
                                                        surfaceColor
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Text inside the artwork
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = playlist,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val countText = pluralStringResource(R.plurals.n_song, songs!!.size, songs!!.size)
                                        val durationText = if (likeLength > 0) " • ${makeTimeString(likeLength * 1000L)}" else ""
                                        Text(
                                            text = "Playlist • $countText$durationText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }

                                // 2. SimpMusic Style Action Row (Below Artwork)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Shuffle Circle
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                playerConnection.playQueue(ListQueue(title = playlist, items = songs!!.shuffled().map { it.toMediaItem() }))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }

                                    // Play/Pause Pill
                                    Box(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .widthIn(min = 120.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color.White)
                                            .clickable {
                                                if (isPlaylistPlaying) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(ListQueue(title = playlist, items = songs!!.map { it.toMediaItem() }, startIndex = 0))
                                                }
                                            }
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(if (showPause) R.drawable.pause else R.drawable.play), null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (showPause) "Pause" else "Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    // Download Circle
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                when (downloadState) {
                                                    Download.STATE_COMPLETED -> { showRemoveDownloadDialog = true }
                                                    Download.STATE_DOWNLOADING -> {
                                                        songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                                                    }
                                                    else -> {
                                                        songs!!.forEach { song ->
                                                            val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.title.toByteArray()).build()
                                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = Color(0xFF00A0CB), modifier = Modifier.size(22.dp))
                                            Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = Color.White)
                                            else -> Icon(painterResource(R.drawable.download), null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Sort Header (only active if needed, simple flat style)
                    item(key = "sortHeader") {
                        if (isSearching) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                SortHeader(
                                    sortType = sortType, sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            AutoPlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            AutoPlaylistSongSortType.NAME -> R.string.sort_by_name
                                            AutoPlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                            AutoPlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // 4. Flat Edge-to-Edge List Items with SimpMusic Dividers
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        val isActive = songWrapper.item.song.id == mediaMetadata?.id
                        val isSelected = songWrapper.isSelected && selection

                        Column(modifier = Modifier.fillMaxWidth()) {
                            SongListItem(
                                song = songWrapper.item,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) } },
                                        onLongClick = {}
                                    ) { Icon(painterResource(R.drawable.more_vert), null) }
                                },
                                isSelected = isSelected,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    })
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (isActive) { playerConnection.player.togglePlayPause() } 
                                                else { playerConnection.playQueue(ListQueue(title = playlist, items = songs!!.map { it.toMediaItem() }, startIndex = songs!!.indexOfFirst { it.id == songWrapper.item.id })) }
                                            } else { songWrapper.isSelected = !songWrapper.isSelected }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                                        },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            // Elegant SimpMusic Divider
                            if (index < filteredSongs.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)))
            }
        }

        DraggableScrollbar(
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.CenterEnd),
            scrollState = lazyListState, headerItems = headerItems
        )

        // 5. YT Music/SimpMusic Style Translucent Top App Bar
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isTopBarSolid) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge)
                    }
                    isSearching -> {
                        TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) },
                            singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                            trailingIcon = { if (query.text.isNotEmpty()) { androidx.compose.material3.IconButton(onClick = { query = TextFieldValue() }) { Icon(painterResource(R.drawable.close), null) } } },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    showTopBarTitle -> { Text(playlist, style = MaterialTheme.typography.titleLarge) }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
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
                    IconButton(
                        onClick = { if (count == wrappedSongs.size) { wrappedSongs.forEach { it.isSelected = false } } else { wrappedSongs.forEach { it.isSelected = true } } },
                        onLongClick = {}
                    ) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }

                    IconButton(
                        onClick = { menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item }, onDismiss = menuState::dismiss, clearAction = { selection = false }) } },
                        onLongClick = {}
                    ) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) {
                    Row(
                        modifier = Modifier.padding(end = 8.dp).background(if(!isTopBarSolid) darkOverlay else Color.Transparent, RoundedCornerShape(50)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                            Icon(painterResource(R.drawable.search), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        )
        
        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing, state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars),
        )
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter),
        )
    }
}
