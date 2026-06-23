package com.j.m3play.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.viewmodels.OnlinePlaylistViewModel
import com.j.m3play.ui.theme.PlayerColorExtractor

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    var onDominantTextColor by remember { mutableStateOf(Color.White) }
    
    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter { (_, s) ->
            s.title.contains(query.text, true) || s.artists.any { it.name.contains(query.text, true) }
        }
    }
    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { ItemWrapper(it) }.toMutableStateList() }

    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                val extractedInt = palette.getVibrantColor(palette.getDominantColor(surfaceColor.toArgb()))
                dominantColor = Color(extractedInt)
                
                val luminance = (0.299 * Color(extractedInt).red + 0.587 * Color(extractedInt).green + 0.114 * Color(extractedInt).blue)
                onDominantTextColor = if (luminance > 0.5) Color.Black else Color.White
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                viewModel.loadMoreSongs()
            }
        }
    }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
    else if (selection) BackHandler { selection = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .drawBehind {
                if (dominantColor != surfaceColor) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(dominantColor.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.15f),
                            radius = size.width * 0.9f
                        )
                    )
                }
            }
            .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                if (!isLoading && playlist != null && !isSearching) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(280.dp).shadow(24.dp, RoundedCornerShape(16.dp), spotColor = dominantColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            AsyncImage(
                                model = playlist!!.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(text = playlist!!.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                        
                        playlist!!.author?.let { artist ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // ROW 1: PRIMARY ACTIONS
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val mixEndpoint = playlist!!.shuffleEndpoint ?: playlist!!.radioEndpoint
                            if (mixEndpoint != null) {
                                Button(
                                    onClick = { playerConnection.playQueue(YouTubeQueue(mixEndpoint)) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (dominantColor != surfaceColor) dominantColor else MaterialTheme.colorScheme.primary, contentColor = onDominantTextColor),
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(painterResource(R.drawable.mix), null, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Start Mix", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (songs.isNotEmpty()) {
                                Button(
                                    onClick = { playerConnection.playQueue(ListQueue(playlist!!.title, songs.map { it.toMediaItem() })) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (dominantColor != surfaceColor) dominantColor else MaterialTheme.colorScheme.primary, contentColor = onDominantTextColor),
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROW 2: SECONDARY ACTIONS
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (playlist!!.id != "LM") {
                                val isSaved = dbPlaylist?.playlist?.bookmarkedAt != null
                                SecondaryActionButton(
                                    icon = if (isSaved) R.drawable.favorite else R.drawable.favorite_border,
                                    color = if (isSaved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = {
                                        if (dbPlaylist?.playlist == null) {
                                            database.transaction {
                                                val playlistEntity = PlaylistEntity(name = playlist!!.title, browseId = playlist!!.id, thumbnailUrl = playlist!!.thumbnail, isEditable = playlist!!.isEditable, playEndpointParams = playlist!!.playEndpoint?.params, shuffleEndpointParams = playlist!!.shuffleEndpoint?.params, radioEndpointParams = playlist!!.radioEndpoint?.params).toggleLike()
                                                insert(playlistEntity)
                                                songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index) }.forEach(::insert)
                                            }
                                        } else {
                                            database.transaction {
                                                val currentPlaylist = dbPlaylist!!.playlist
                                                update(currentPlaylist, playlist!!)
                                                update(currentPlaylist.toggleLike())
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                            }
                            if (songs.isNotEmpty()) {
                                SecondaryActionButton(
                                    icon = R.drawable.download,
                                    onClick = {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri()).setCustomCacheKey(song.id).setData(song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        }
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                    }
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                            }
                            SecondaryActionButton(
                                icon = R.drawable.share,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${playlist!!.id}") }
                                    context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                                }
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            SecondaryActionButton(
                                icon = R.drawable.more_vert,
                                onClick = {
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = playlist!!, songs = songs, coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss, selectAction = { selection = true },
                                            canSelect = true, snackbarHostState = snackbarHostState
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            items(items = wrappedSongs, key = { it.item.second.id }) { songWrapper ->
                YouTubeListItem(
                    item = songWrapper.item.second,
                    isActive = mediaMetadata?.id == songWrapper.item.second.id,
                    isPlaying = isPlaying,
                    isSelected = songWrapper.isSelected && selection,
                    modifier = Modifier
                        .fillMaxWidth()
                        // 1. YAHAN CLICK AUR LONG CLICK WAPAS ADD KIYA HAI
                        .combinedClickable(
                            onClick = {
                                if (selection) {
                                    songWrapper.isSelected = !songWrapper.isSelected
                                } else {
                                    if (songWrapper.item.second.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId = playlist!!.id)
                                        playerConnection.playQueue(YouTubeQueue(songWrapper.item.second.endpoint ?: WatchEndpoint(videoId = songWrapper.item.second.id), songWrapper.item.second.toMediaMetadata()))
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!selection) selection = true
                                wrappedSongs.forEach { it.isSelected = false }
                                songWrapper.isSelected = true
                            }
                        ),
                    trailingContent = {
                        IconButton(onClick = {
                            menuState.show {
                                YouTubeSongMenu(song = songWrapper.item.second, navController = navController, onDismiss = menuState::dismiss)
                            }
                        }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                    }
                )
            }
        }

        // Top App Bar
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text("$count Selected", style = MaterialTheme.typography.titleLarge)
                } else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...", style = MaterialTheme.typography.titleMedium) }, singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                    else if (selection) { selection = false; wrappedSongs.forEach { it.isSelected = false } }
                    else navController.navigateUp()
                }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), contentDescription = null) }
            },
            actions = {
                if (selection) {
                    IconButton(onClick = {
                        val allSelected = wrappedSongs.all { it.isSelected }
                        wrappedSongs.forEach { it.isSelected = !allSelected }
                    }) { Icon(painterResource(if (wrappedSongs.all { it.isSelected }) R.drawable.deselect else R.drawable.select_all), contentDescription = null) }
                    
                    IconButton(onClick = {
                        menuState.show {
                            SelectionMediaMetadataMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }.mapNotNull { it.item.second.toMediaItem().mediaMetadata },
                                onDismiss = menuState::dismiss, clearAction = { selection = false }, currentItems = emptyList()
                            )
                        }
                    }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null) }
                }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SecondaryActionButton(icon: Int, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
    Surface(
        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick, modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
    }
}
