package com.j.m3play.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.LocalMixQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
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
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    
    // UI Styling States
    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    var onDominantTextColor by remember { mutableStateOf(Color.White) }

    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                val extractedInt = palette.getVibrantColor(palette.getDominantColor(surfaceColor.toArgb()))
                dominantColor = Color(extractedInt)
                
                val luminance = (0.299 * Color(extractedInt).red + 0.587 * Color(extractedInt).green + 0.114 * Color(extractedInt).blue)
                onDominantTextColor = if (luminance > 0.5) Color.Black else Color.White
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
                if (!isSearching && playlist != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(24.dp, RoundedCornerShape(16.dp), spotColor = dominantColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            AsyncImage(
                                model = playlist!!.thumbnails.firstOrNull(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = playlist!!.playlist.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { playerConnection.playQueue(LocalMixQueue(database, playlist!!.id, 50)) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dominantColor != surfaceColor) dominantColor else MaterialTheme.colorScheme.primary,
                                contentColor = onDominantTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .height(56.dp)
                        ) {
                            Icon(painterResource(R.drawable.mix), null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Start Mix", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            itemsIndexed(wrappedSongs, key = { _, wrap -> wrap.item.map.id }) { index, songWrapper ->
                SongListItem(
                    song = songWrapper.item.song,
                    isActive = songWrapper.item.song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    isSelected = songWrapper.isSelected && selection,
                    modifier = Modifier.fillMaxWidth(),
                    trailingContent = {
                        IconButton(onClick = {
                            menuState.show { SongMenu(originalSong = songWrapper.item.song, navController = navController, onDismiss = menuState::dismiss) }
                        }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                    }
                )
            }
        }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            title = {
                AnimatedVisibility(visible = isSearching, enter = fadeIn(), exit = fadeOut()) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...", style = MaterialTheme.typography.titleMedium) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                    else navController.navigateUp()
                }) { Icon(painterResource(R.drawable.arrow_back), contentDescription = null) }
            },
            actions = {
                if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null) }
                    IconButton(onClick = {
                        // Options menu trigger for sync/edit logic can be added here
                    }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                }
            }
        )

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing, state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
