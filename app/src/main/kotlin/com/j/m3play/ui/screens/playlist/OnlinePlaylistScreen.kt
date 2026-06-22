package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.rememberPreference
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
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // States
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = surfaceColor.toArgb()
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    
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

    // Gradient Extractor
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
            }
        } else gradientColors = emptyList()
    }

    // Pagination
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                viewModel.loadMoreSongs()
            }
        }
    }

    // Handlers
    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    Box(modifier = Modifier.fillMaxSize().pullToRefresh(pullRefreshState, isRefreshing, viewModel::refresh)) {
        BasePlaylistScreen(
            title = playlist?.title.orEmpty(),
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
                if (!isLoading && playlist != null && !isSearching) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(240.dp).shadow(24.dp, RoundedCornerShape(20.dp), spotColor = gradientColors.firstOrNull() ?: Color.Black),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            AsyncImage(model = playlist!!.thumbnail, contentDescription = null, contentScale = ContentScale.Crop)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = playlist!!.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        
                        playlist!!.author?.let { artist ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val mixEndpoint = playlist!!.shuffleEndpoint ?: playlist!!.radioEndpoint
                            if (mixEndpoint != null) {
                                Button(onClick = { playerConnection.playQueue(YouTubeQueue(mixEndpoint)) }, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f).height(50.dp)) {
                                    Icon(painterResource(R.drawable.mix), null, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Mix")
                                }
                            }
                            
                            // Save Button
                            if (playlist!!.id != "LM") {
                                val isSaved = dbPlaylist?.playlist?.bookmarkedAt != null
                                Button(
                                    onClick = { /* ViewModel logic to save */ }, 
                                    shape = RoundedCornerShape(24.dp), 
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = if(isSaved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer), 
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(painterResource(if(isSaved) R.drawable.favorite else R.drawable.favorite_border), null)
                                }
                            }
                        }
                    }
                }
            },
            listContent = {
                items(items = wrappedSongs, key = { it.item.second.id }) { songWrapper ->
                    YouTubeListItem(
                        item = songWrapper.item.second,
                        isActive = mediaMetadata?.id == songWrapper.item.second.id,
                        isPlaying = isPlaying,
                        isSelected = songWrapper.isSelected && selection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .bounceClickable(
                                onClick = {
                                    if (selection) songWrapper.isSelected = !songWrapper.isSelected
                                    else {
                                        if (songWrapper.item.second.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else {
                                            playerConnection.service.getAutomix(playlistId = playlist!!.id)
                                            playerConnection.playQueue(YouTubeQueue(songWrapper.item.second.endpoint ?: WatchEndpoint(videoId = songWrapper.item.second.id), songWrapper.item.second.toMediaMetadata()))
                                        }
                                    }
                                },
                                onLongClick = { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                            ),
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = {
                                menuState.show {
                                    YouTubeSongMenu(song = songWrapper.item.second, navController = navController, onDismiss = menuState::dismiss)
                                }
                            }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                        }
                    )
                }
            }
        )

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing, state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
        )
    }
}
