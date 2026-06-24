package com.j.m3play.ui.screens.playlist

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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.j.m3play.constants.MyTopFilter
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
import com.j.m3play.viewmodels.TopPlaylistViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val songs by viewModel.topSongs.collectAsState(null)
    val sortType by viewModel.topPeriod.collectAsState()
    val maxSize = viewModel.top
    val playlistName = "${stringResource(R.string.my_top)} $maxSize"

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOptionsMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val wrappedSongs = remember(songs) {
        songs?.map { ItemWrapper(it) }?.toMutableStateList() ?: mutableStateListOf()
    }
    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter {
            it.item.song.title.contains(query.text, true) || it.item.artists.any { art -> art.name.contains(query.text, true) }
        }
    }

    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.song?.thumbnailUrl
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
                            modifier = Modifier.size(260.dp).shadow(32.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = songs!!.firstOrNull()?.song?.thumbnailUrl,
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = playlistName,
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
                                shape = CircleShape, color = textColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    playerConnection.playQueue(ListQueue(playlistName, songs!!.shuffled().map { it.song.toMediaItem() }))
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, tint = textColor, modifier = Modifier.size(24.dp)) } }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Button(
                                onClick = { playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.song.toMediaItem() })) },
                                shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = dominantColor),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play All", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Surface(
                                shape = CircleShape, color = textColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    songs!!.forEach { song ->
                                        val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.title.toByteArray()).build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                    }
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Downloading Playlist...") }
                                }
                            ) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.download), null, tint = textColor, modifier = Modifier.size(24.dp)) } }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
                            Text(text = "${songs!!.size} tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }

            if (!isSearching && songs?.isNotEmpty() == true) {
                item {
                    CompositionLocalProvider(LocalContentColor provides textColor) {
                        SortHeader(
                            sortType = sortType, sortDescending = false, showDescending = false,
                            onSortTypeChange = { viewModel.topPeriod.value = it }, onSortDescendingChange = {},
                            sortTypeText = { t ->
                                when (t) { MyTopFilter.ALL_TIME -> R.string.all_time; MyTopFilter.DAY -> R.string.past_24_hours; MyTopFilter.WEEK -> R.string.past_week; MyTopFilter.MONTH -> R.string.past_month; MyTopFilter.YEAR -> R.string.past_year }
                            },
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }
            
            itemsIndexed(filteredSongs, key = { _, wrap -> wrap.item.song.id }) { index, songWrapper ->
                CompositionLocalProvider(LocalContentColor provides textColor) {
                    SongListItem(
                        song = songWrapper.item,
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
                                        else playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.song.toMediaItem() }, index))
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
                            IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor) }
                        }
                    )
                }
            }

            if (songs?.isNotEmpty() == true && !isSearching) {
                item {
                    val duration = songs!!.map { (it.song.duration ?: 0).toLong() }.sum() * 1000L
                    Text(
                        text = "${songs!!.size} songs, ${makeTimeString(duration)}",
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
                        2 -> Text(playlistName, color = textColor)
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
                                onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }
                            )
                        }
                    }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null, tint = textColor) }
                    
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = textColor) }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            DropdownMenuItem(
                                text = { Text("Add to Queue") },
                                onClick = { songs?.let { playerConnection.addToQueue(it.map { s -> s.song.toMediaItem() }) }; showOptionsMenu = false; coroutineScope.launch { snackbarHostState.showSnackbar("Added to Queue") } },
                                leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showOptionsMenu = false
                                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Listen to My Top $maxSize tracks!") }
                                    context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.share), null) }
                            )
                        }
                    }
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
