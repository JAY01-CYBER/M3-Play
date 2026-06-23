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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    ) {
        // 1. IMMERSIVE BLURRED BACKGROUND
        if (playlist?.thumbnail != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
            ) {
                AsyncImage(
                    model = playlist!!.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f), 
                                    surfaceColor.copy(alpha = 0.6f),
                                    surfaceColor
                                )
                            )
                        )
                )
            }
        }

        // 2. MAIN SCROLLABLE CONTENT
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
                            .padding(top = 70.dp, bottom = 16.dp),
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
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        playlist!!.author?.let { artist ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Playlist • ${songs.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 3. ACTION ROW (Shuffle, PLAY, Download)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    val mixEndpoint = playlist!!.shuffleEndpoint ?: playlist!!.radioEndpoint
                                    if (mixEndpoint != null) playerConnection.playQueue(YouTubeQueue(mixEndpoint))
                                    else if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.title, songs.shuffled().map { it.toMediaItem() }))
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
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
                            
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
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
                                    Icon(painterResource(R.drawable.download), contentDescription = "Download", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "${songs.size} tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 4. SONGS LIST
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

        // 5. CUSTOM GLASS TOP APP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isSearching) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                        IconButton(onClick = { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
                        }
                        TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("Search...", color = Color.White.copy(alpha = 0.5f)) }, singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else if (selection) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        IconButton(onClick = { selection = false; wrappedSongs.forEach { it.isSelected = false } }) {
                            Icon(painterResource(R.drawable.close), contentDescription = null, tint = Color.White)
                        }
                        val count = wrappedSongs.count { it.isSelected }
                        Text("$count Selected", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val allSelected = wrappedSongs.all { it.isSelected }
                            wrappedSongs.forEach { it.isSelected = !allSelected }
                        }) { Icon(painterResource(if (wrappedSongs.all { it.isSelected }) R.drawable.deselect else R.drawable.select_all), tint = Color.White, contentDescription = null) }
                        IconButton(onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaMetadata() },
                                    onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }, currentItems = emptyList()
                                )
                            }
                        }) { Icon(painterResource(R.drawable.more_vert), tint = Color.White, contentDescription = null) }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            if (playlist?.id != "LM") {
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
                                    Icon(painterResource(if (isSaved) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (isSaved) MaterialTheme.colorScheme.error else Color.White)
                                }
                            }
                            IconButton(onClick = { isSearching = true }) {
                                Icon(painterResource(R.drawable.search), contentDescription = null, tint = Color.White)
                            }
                            IconButton(onClick = {
                                playlist?.let {
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = it, songs = songs, coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss, selectAction = { selection = true },
                                            canSelect = true, snackbarHostState = snackbarHostState
                                        )
                                    }
                                }
                            }) {
                                Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
