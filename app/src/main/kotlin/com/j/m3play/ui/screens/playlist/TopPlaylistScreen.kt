package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.withContext
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.MyTopFilter
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
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
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val songs by viewModel.topSongs.collectAsState(null)
    val sortType by viewModel.topPeriod.collectAsState()
    val maxSize = viewModel.top
    val name = "${stringResource(R.string.my_top)} $maxSize"

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = surfaceColor.toArgb()
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val lazyListState = rememberLazyListState()

    val wrappedSongs = remember(songs) {
        songs?.map { ItemWrapper(it) }?.toMutableStateList() ?: mutableStateListOf()
    }
    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter {
            it.item.song.title.contains(query.text, true) || it.item.artists.any { art -> art.name.contains(query.text, true) }
        }
    }

    val likeLength = remember(songs) { songs?.sumOf { it.song.duration } ?: 0 }

    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.song?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            result?.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
            }
        } else gradientColors = emptyList()
    }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    BasePlaylistScreen(
        title = name,
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
            if (!isSearching && songs?.isNotEmpty() == true) {
                PlaylistHeroHeader(
                    playlistName = name,
                    thumbnails = listOfNotNull(songs!!.firstOrNull()?.song?.thumbnailUrl),
                    gradientColors = gradientColors,
                    songCount = songs!!.size,
                    totalDurationMs = likeLength * 1000L,
                    onPlay = { playerConnection.playQueue(ListQueue(name, songs!!.map { it.toMediaItem() })) },
                    onShuffle = { playerConnection.playQueue(ListQueue(name, songs!!.shuffled().map { it.toMediaItem() })) }
                )
            }
        },
        listContent = {
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
                        .bounceClickable(
                            onClick = {
                                if (selection) songWrapper.isSelected = !songWrapper.isSelected
                                else {
                                    if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else playerConnection.playQueue(ListQueue(name, songs!!.map { it.toMediaItem() }, index))
                                }
                            },
                            onLongClick = { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                        ),
                    trailingContent = {
                        androidx.compose.material3.IconButton(onClick = {
                            menuState.show {
                                SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss)
                            }
                        }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                    }
                )
            }
        }
    )
}
