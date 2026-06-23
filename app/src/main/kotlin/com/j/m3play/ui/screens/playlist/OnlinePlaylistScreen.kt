package com.j.m3play.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
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
import androidx.palette.graphics.Palette

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

    // Extract dominant color for Apple Music style gradient
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                val extractedInt = palette.getVibrantColor(palette.getDominantColor(surfaceColor.toArgb()))
                dominantColor = Color(extractedInt)
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
                // CLEAN APPLE MUSIC STYLE GRADIENT (NO BLUR)
                if (dominantColor != surfaceColor) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(dominantColor.copy(alpha = 0.5f), surfaceColor),
                            startY = 0f,
                            endY = size.height * 0.6f
                        )
                    )
                }
            }
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)
        ) {
            item {
                if (!isLoading && playlist != null && !isSearching) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp, bottom = 16.dp), // Space for TopAppBar
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(24.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = playlist!!.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = playlist!!.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        playlist!!.author?.let { artist ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // MAIN ACTION ROW (White Buttons)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. White Shuffle Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    val mixEndpoint = playlist!!.shuffleEndpoint ?: playlist!!.radioEndpoint
                                    if (mixEndpoint != null) playerConnection.playQueue(YouTubeQueue(mixEndpoint))
                                    else if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.title, songs.shuffled().map { it.toMediaItem() }))
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            // 2. White Play Button
                            Button(
                                onClick = { 
                                    if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.title, songs.map { it.toMediaItem() })) 
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            // 3. White Download Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    if (songs.isNotEmpty()) {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri()).setCustomCacheKey(song.id).setData(song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        }
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.download), contentDescription = "Download", tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECONDARY ACTIONS (Save, Share, More)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (playlist!!.id != "LM") {
                                val isSaved = dbPlaylist?.playlist?.bookmarkedAt != null
                                IconButton(onClick = {
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
                                }) {
                                    Icon(painterResource(if (isSaved) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (isSaved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${playlist!!.id}") }
                                context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                            }) { Icon(painterResource(R.drawable.share), contentDescription = null) }
                            
                            IconButton(onClick = {
                                menuState.show {
                                    YouTubePlaylistMenu(
                                        playlist = playlist!!, songs = songs, coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss, selectAction = { selection = true },
                                        canSelect = true, snackbarHostState = snackbarHostState
                                    )
                                }
                            }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "${songs.size} tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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

        // 4. OVERLAP FIXED TOP APP BAR
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isSearching || selection) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text("$count Selected", style = MaterialTheme.typography.titleLarge)
                } else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...", style = MaterialTheme.typography.titleMedium) }, singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, 
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
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
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaMetadata() },
                                onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }, currentItems = emptyList()
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
