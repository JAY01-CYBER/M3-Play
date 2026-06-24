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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AutoPlaylistSongSortDescendingKey
import com.j.m3play.constants.AutoPlaylistSongSortType
import com.j.m3play.constants.AutoPlaylistSongSortTypeKey
import com.j.m3play.db.entities.Song
import com.j.m3play.db.entities.SongEntity
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.AutoPlaylistViewModel

private fun SongEntity.asSong() = Song(song = this, artists = emptyList())

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlistName = if (viewModel.playlist == "liked") stringResource(R.string.liked) else stringResource(R.string.offline)
    
    val songs by viewModel.likedSongs.collectAsState(null)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AutoPlaylistSongSortTypeKey, AutoPlaylistSongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AutoPlaylistSongSortDescendingKey, true)

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val wrappedSongs = remember(songs) { songs?.map { ItemWrapper(it.asSong()) }?.toMutableStateList() ?: mutableStateListOf() }
    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter {
            it.item.song.title.contains(query.text, true) || it.item.artists.any { art -> art.name.contains(query.text, true) }
        }
    }

    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.thumbnailUrl
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

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
    else if (selection) BackHandler { selection = false }

    Box(modifier = Modifier.fillMaxSize().background(dominantColor)) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                if (!isSearching && songs?.isNotEmpty() == true) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 70.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(280.dp).shadow(32.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            AsyncImage(
                                model = songs!!.firstOrNull()?.thumbnailUrl,
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.SansSerif, letterSpacing = (-0.5).sp),
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape, color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    playerConnection.playQueue(ListQueue(playlistName, songs!!.shuffled().map { it.asSong().toMediaItem() }))
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(24.dp)) } }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Button(
                                onClick = { playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.asSong().toMediaItem() })) },
                                shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play All", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Surface(
                                shape = CircleShape, color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    songs!!.forEach { songEntity ->
                                        val downloadRequest = DownloadRequest.Builder(songEntity.id, songEntity.id.toUri()).setCustomCacheKey(songEntity.id).setData(songEntity.title.toByteArray()).build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                    }
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.download), null, tint = Color.White, modifier = Modifier.size(24.dp)) } }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (!isSearching && songs?.isNotEmpty() == true) {
                item {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        SortHeader(
                            sortType = sortType, sortDescending = sortDescending,
                            onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                            sortTypeText = { t ->
                                when (t) {
                                    AutoPlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                    AutoPlaylistSongSortType.NAME -> R.string.sort_by_name
                                    AutoPlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                    AutoPlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                }
                            },
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }
            
            itemsIndexed(filteredSongs, key = { _, wrap -> wrap.item.song.id }) { index, songWrapper ->
                CompositionLocalProvider(LocalContentColor provides Color.White) {
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
                                        else playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.asSong().toMediaItem() }, index))
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
                            IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item.song, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White) }
                        }
                    )
                }
            }

            if (songs?.isNotEmpty() == true && !isSearching) {
                item {
                    val duration = songs!!.sumOf { (it.duration ?: 0).toLong() } * 1000L
                    Text(
                        text = "${songs!!.size} songs, ${makeTimeString(duration)}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif),
                        modifier = Modifier.padding(top = 16.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }
        }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = dominantColor),
            title = {
                if (selection) { Text("${wrappedSongs.count { it.isSelected }} Selected", style = MaterialTheme.typography.titleLarge.copy(color = Color.White)) } 
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...", color = Color.White.copy(alpha = 0.6f)) }, singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                    else if (selection) { selection = false; wrappedSongs.forEach { it.isSelected = false } }
                    else navController.navigateUp()
                }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), contentDescription = null, tint = Color.White) }
            },
            actions = {
                if (selection) {
                    IconButton(onClick = {
                        val allSelected = wrappedSongs.all { it.isSelected }
                        wrappedSongs.forEach { it.isSelected = !allSelected }
                    }) { Icon(painterResource(if (wrappedSongs.all { it.isSelected }) R.drawable.deselect else R.drawable.select_all), contentDescription = null, tint = Color.White) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null, tint = Color.White) }
                    
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White) }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            DropdownMenuItem(
                                text = { Text("Add to Queue") },
                                onClick = { songs?.let { playerConnection.addToQueue(it.map { s -> s.asSong().toMediaItem() }) }; showOptionsMenu = false; coroutineScope.launch { snackbarHostState.showSnackbar("Added to Queue") } },
                                leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                            )
                        }
                    }
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
