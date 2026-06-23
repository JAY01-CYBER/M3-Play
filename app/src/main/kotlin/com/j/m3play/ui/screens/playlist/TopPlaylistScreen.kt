package com.j.m3play.ui.screens.playlist

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import kotlinx.coroutines.withContext

import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.MyTopFilter
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.ItemWrapper
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
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                if (!isSearching && songs?.isNotEmpty() == true) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(24.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = songs!!.firstOrNull()?.song?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action Row (White buttons)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape, color = Color.White,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
                                    playerConnection.playQueue(ListQueue(playlistName, songs!!.shuffled().map { it.toMediaItem() }))
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null, tint = Color.Black, modifier = Modifier.size(24.dp)) }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Button(
                                onClick = { playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.toMediaItem() })) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play All", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
                            Text(text = "${songs!!.size} tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isSearching && songs?.isNotEmpty() == true) {
                item {
                    SortHeader(
                        sortType = sortType, sortDescending = false, showDescending = false,
                        onSortTypeChange = { viewModel.topPeriod.value = it },
                        onSortDescendingChange = {},
                        sortTypeText = { t ->
                            when (t) {
                                MyTopFilter.ALL_TIME -> R.string.all_time
                                MyTopFilter.DAY -> R.string.past_24_hours
                                MyTopFilter.WEEK -> R.string.past_week
                                MyTopFilter.MONTH -> R.string.past_month
                                MyTopFilter.YEAR -> R.string.past_year
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            
            itemsIndexed(filteredSongs, key = { _, wrap -> wrap.item.id }) { index, songWrapper ->
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
                                if (selection) {
                                    songWrapper.isSelected = !songWrapper.isSelected
                                } else {
                                    if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else playerConnection.playQueue(ListQueue(playlistName, songs!!.map { it.toMediaItem() }, index))
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
                            menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) }
                        }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                    }
                )
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
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                onDismiss = menuState::dismiss, clearAction = { selection = false; wrappedSongs.clear() }
                            )
                        }
                    }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), contentDescription = null) }
                }
            }
        )
    }
}
