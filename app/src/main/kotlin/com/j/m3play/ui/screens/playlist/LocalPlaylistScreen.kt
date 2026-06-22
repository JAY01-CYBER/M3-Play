package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
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
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.PlaylistSong
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.LocalMixQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
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
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = surfaceColor.toArgb()
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val editable = playlist?.playlist?.isEditable == true

    // Wrapped list for state tracking
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { 
            it.song.song.title.contains(query.text, true) || it.song.artists.any { art -> art.name.contains(query.text, true) }
        }
    }
    
    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { ItemWrapper(it) }.toMutableStateList()
    }

    // Gradient Extractor
    LaunchedEffect(playlist?.thumbnails) {
        val thumbnailUrl = playlist?.thumbnails?.firstOrNull()
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
            }
        } else gradientColors = emptyList()
    }

    // Handlers
    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    BasePlaylistScreen(
        title = playlist?.playlist?.name.orEmpty(),
        lazyListState = lazyListState,
        gradientColors = gradientColors,
        surfaceColor = surfaceColor,
        isSearching = isSearching,
        searchQuery = query,
        onSearchQueryChange = { query = it },
        onSearchToggle = { isSearching = !isSearching; if (!isSearching) query = TextFieldValue() },
        isSelectionMode = selection,
        selectionCount = wrappedSongs.count { it.isSelected },
        onClearSelection = { selection = false; wrappedSongs.forEach { it.isSelected = false } },
        onSelectAll = { wrappedSongs.forEach { it.isSelected = true } },
        onBack = { navController.navigateUp() },
        disableBlur = disableBlur,
        headerContent = {
            if (!isSearching && playlist != null) {
                PlaylistHeroHeader(
                    playlistName = playlist!!.playlist.name,
                    thumbnails = playlist!!.thumbnails,
                    gradientColors = gradientColors,
                    songCount = songs.size,
                    totalDurationMs = songs.sumOf { it.song.song.duration } * 1000L,
                    onPlay = { playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() })) },
                    onShuffle = { playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.shuffled().map { it.song.toMediaItem() })) }
                )
            }
        },
        listContent = {
            itemsIndexed(wrappedSongs, key = { _, wrap -> wrap.item.map.id }) { index, songWrapper ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        if (target == SwipeToDismissBoxValue.EndToStart) {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.withTransaction { delete(songWrapper.item.map) }
                            }
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
                        Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp))) 
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
                            .clip(RoundedCornerShape(20.dp)) // Modern UI rounding
                            .bounceClickable( // Replaces generic combinedClickable with nice scale animation
                                onClick = {
                                    if (selection) songWrapper.isSelected = !songWrapper.isSelected
                                    else {
                                        if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(ListQueue(playlist!!.playlist.name, songs.map { it.song.toMediaItem() }, index))
                                    }
                                },
                            ),
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = {
                                menuState.show {
                                    SongMenu(originalSong = songWrapper.item.song, navController = navController, onDismiss = menuState::dismiss)
                                }
                            }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                        }
                    )
                }
            }
        }
    )

    // Pull to Refresh indicator positioning overlay
    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing, state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// Sub-component for clean Hero Header
@Composable
fun PlaylistHeroHeader(
    playlistName: String, thumbnails: List<String>, gradientColors: List<Color>,
    songCount: Int, totalDurationMs: Long, onPlay: () -> Unit, onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail with nice soft shadow
        Surface(
            modifier = Modifier.size(240.dp).shadow(24.dp, RoundedCornerShape(20.dp), spotColor = gradientColors.firstOrNull() ?: Color.Black),
            shape = RoundedCornerShape(20.dp)
        ) {
            AsyncImage(model = thumbnails.firstOrNull(), contentDescription = null, contentScale = ContentScale.Crop)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = playlistName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(0.7f)) {
                Text("$songCount Songs", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            if (totalDurationMs > 0) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(0.7f)) {
                    Text(makeTimeString(totalDurationMs), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onPlay, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f).height(50.dp)) {
                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
            Button(onClick = onShuffle, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer), modifier = Modifier.weight(1f).height(50.dp)) {
                Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(24.dp))
            }
        }
    }
}
