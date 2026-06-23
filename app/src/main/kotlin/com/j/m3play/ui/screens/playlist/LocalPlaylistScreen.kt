package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.palette.graphics.Palette

import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.viewmodels.LocalPlaylistViewModel

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
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
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
        if (query.text.isEmpty()) songs else songs.filter { 
            it.song.song.title.contains(query.text, true) || it.song.artists.any { art -> art.name.contains(query.text, true) }
        }
    }
    
    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { ItemWrapper(it) }.toMutableStateList()
    }

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
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

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
    else if (selection) BackHandler { selection = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .drawBehind {
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
                if (!isSearching && playlist != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(24.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = playlist!!.thumbnails.firstOrNull(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = playlist!!.playlist.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // ACTION ROW (White Buttons)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle Button
                            Surface(
                                shape = CircleShape, color = Color.White,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.shuffled().map { it.song.toMediaItem() }))
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, tint = Color.Black, modifier = Modifier.size(24.dp)) }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            // Play Button
                            Button(
                                onClick = { 
                                    if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() })) 
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            // Download Button
                            Surface(
                                shape = CircleShape, color = Color.White,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    if (songs.isNotEmpty()) {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        }
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.download), null, tint = Color.Black, modifier = Modifier.size(24.dp)) }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECONDARY ACTIONS
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            val isSaved = playlist!!.playlist.bookmarkedAt != null
                            IconButton(onClick = { database.transaction { update(playlist!!.playlist.toggleLike()) } }) {
                                Icon(painterResource(if (isSaved) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (isSaved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                            
                            IconButton(onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Opening Options") } }) {
                                Icon(painterResource(if (playlist!!.playlist.isEditable) R.drawable.edit else R.drawable.sync), contentDescription = null)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
                            Text(text = "${songs.size} tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            itemsIndexed(wrappedSongs, key = { _, wrap -> wrap.item.map.id }) { index, songWrapper ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        if (target == SwipeToDismissBoxValue.EndToStart) {
                            coroutineScope.launch(Dispatchers.IO) { database.withTransaction { delete(songWrapper.item.map) } }
                            true
                        } else if (target == SwipeToDismissBoxValue.StartToEnd) {
                            playerConnection.addToQueue(listOf(songWrapper.item.song.toMediaItem()))
                            coroutineScope.launch { snackbarHostState.showSnackbar("Added to Queue") }
                            false
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        }
                        Box(modifier = Modifier.fillMaxSize().background(color)) 
                    }
                ) {
                    SongListItem(
                        song = songWrapper.item.song,
                        isActive = songWrapper.item.song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSelected = songWrapper.isSelected && selection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selection) {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    } else {
                                        if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() }, index))
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
                                menuState.show { SongMenu(originalSong = songWrapper.item.song, navController = navController, onDismiss = menuState::dismiss) }
                            }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                        }
                    )
                }
            }
        }

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
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song },
                                songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map },
                                onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }
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
