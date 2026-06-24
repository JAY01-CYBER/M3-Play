package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
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
import androidx.palette.graphics.Palette
import java.time.LocalDateTime

import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.YouTube
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.LocalMixQueue
import com.j.m3play.ui.component.AssignTagsDialog
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.EditPlaylistDialog
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.makeTimeString
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

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showAssignTagsDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { 
            it.song.song.title.contains(query.text, true) || it.song.artists.any { art -> art.name.contains(query.text, true) }
        }
    }
    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { ItemWrapper(it) }.toMutableStateList() }

    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                val extractedInt = palette.getMutedColor(0).takeIf { it != 0 } 
                    ?: palette.getDarkMutedColor(0).takeIf { it != 0 } 
                    ?: palette.getDominantColor(surfaceColor.toArgb())
                dominantColor = Color(extractedInt)
            }
        }
    }

    val luminance = (0.299 * dominantColor.red + 0.587 * dominantColor.green + 0.114 * dominantColor.blue)
    val isLight = luminance > 0.5f
    val textColor = if (isLight) Color.Black else Color.White
    val secondaryTextColor = if (isLight) Color.DarkGray else Color.White.copy(alpha = 0.7f)

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
    else if (selection) BackHandler { selection = false }

    if (showAssignTagsDialog && playlist != null) {
        AssignTagsDialog(database = database, playlistId = playlist!!.id, onDismiss = { showAssignTagsDialog = false })
    }

    if (showEditDialog && playlist != null) {
        EditPlaylistDialog(
            initialName = playlist!!.playlist.name,
            initialThumbnailUrl = playlist!!.playlist.thumbnailUrl,
            fallbackThumbnails = playlist!!.songThumbnails.filterNotNull(),
            onDismiss = { showEditDialog = false },
            onSave = { name, thumbnailUrl ->
                database.query { update(playlist!!.playlist.copy(name = name, thumbnailUrl = thumbnailUrl, lastUpdateTime = LocalDateTime.now())) }
                viewModel.viewModelScope.launch(Dispatchers.IO) { playlist!!.playlist.browseId?.let { YouTube.renamePlaylist(it, name) } }
            }
        )
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = { Text(text = "Are you sure you want to delete '${playlist?.playlist?.name}'?", style = MaterialTheme.typography.bodyLarge) },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text("Cancel") }
                TextButton(onClick = {
                    showDeletePlaylistDialog = false
                    database.query { playlist?.let { delete(it.playlist) } }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) } }
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(dominantColor)) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)
        ) {
            item {
                if (!isSearching && playlist != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(260.dp).shadow(32.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(model = playlist!!.thumbnails.firstOrNull(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = playlist!!.playlist.name,
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.SansSerif, letterSpacing = (-0.5).sp),
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = textColor, modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape, color = textColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.shuffled().map { it.song.toMediaItem() }))
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, tint = textColor, modifier = Modifier.size(24.dp)) } }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Button(
                                onClick = { if (songs.isNotEmpty()) playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() })) },
                                shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = dominantColor),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Surface(
                                shape = CircleShape, color = textColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    if (songs.isNotEmpty()) {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        }
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                    }
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.download), null, tint = textColor, modifier = Modifier.size(24.dp)) } }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            itemsIndexed(wrappedSongs, key = { _, wrap -> wrap.item.map.id }) { index, songWrapper ->
                CompositionLocalProvider(LocalContentColor provides textColor) {
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
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (selection) { songWrapper.isSelected = !songWrapper.isSelected } 
                                        else {
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
                                IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item.song, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor) }
                            }
                        )
                    }
                }
            }

            if (songs.isNotEmpty() && !isSearching) {
                item {
                    val duration = songs.map { it.song.song.duration.toLong() }.sum() * 1000L
                    Text(
                        text = "${songs.size} songs, ${makeTimeString(duration)}",
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif),
                        modifier = Modifier.padding(top = 16.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }
        }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = dominantColor),
            title = {
                AnimatedContent(
                    targetState = when {
                        selection -> 0
                        isSearching -> 1
                        else -> 2
                    },
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "TopBarState"
                ) { state ->
                    when (state) {
                        0 -> Text("${wrappedSongs.count { it.isSelected }} Selected", style = MaterialTheme.typography.titleLarge.copy(color = textColor))
                        1 -> TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("Search...", color = secondaryTextColor) }, singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                        2 -> Text(playlist?.playlist?.name ?: "", color = textColor)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                    else if (selection) { selection = false; wrappedSongs.forEach { it.isSelected = false } }
                    else navController.navigateUp()
                }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), contentDescription = null, tint = textColor) }
            },
            actions = {
                if (selection) {
                    IconButton(onClick = {
                        val allSelected = wrappedSongs.all { it.isSelected }
                        wrappedSongs.forEach { it.isSelected = !allSelected }
                    }) { Icon(painterResource(if (wrappedSongs.all { it.isSelected }) R.drawable.deselect else R.drawable.select_all), contentDescription = null, tint = textColor) }
                    
                    IconButton(onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song },
                                songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map },
                                onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }
                            )
                        }
                    }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null, tint = textColor) }
                    
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor)
                        }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Add to Queue") },
                                onClick = { playerConnection.addToQueue(songs.map { it.song.toMediaItem() }); showOptionsMenu = false; coroutineScope.launch { snackbarHostState.showSnackbar("Added to Queue") } },
                                leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Assign Tags") },
                                onClick = { showAssignTagsDialog = true; showOptionsMenu = false },
                                leadingIcon = { Icon(painterResource(R.drawable.tag), null) }
                            )
                            if (playlist?.playlist?.isEditable == true) {
                                DropdownMenuItem(
                                    text = { Text("Edit Playlist") },
                                    onClick = { showEditDialog = true; showOptionsMenu = false },
                                    leadingIcon = { Icon(painterResource(R.drawable.edit), null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Playlist") },
                                    onClick = { showDeletePlaylistDialog = true; showOptionsMenu = false },
                                    leadingIcon = { Icon(painterResource(R.drawable.delete), null) }
                                )
                            }
                            if (playlist?.playlist?.browseId != null) {
                                DropdownMenuItem(
                                    text = { Text("Sync Playlist") },
                                    onClick = { showOptionsMenu = false; coroutineScope.launch { snackbarHostState.showSnackbar("Syncing...") } },
                                    leadingIcon = { Icon(painterResource(R.drawable.sync), null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Export Playlist") },
                                onClick = { showOptionsMenu = false; coroutineScope.launch { snackbarHostState.showSnackbar("Export Feature coming soon!") } },
                                leadingIcon = { Icon(painterResource(R.drawable.share), null) }
                            )
                        }
                    }
                }
            }
        )
        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
