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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.SongSortDescendingKey
import com.j.m3play.constants.SongSortType
import com.j.m3play.constants.SongSortTypeKey
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    
    val wrappedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sortedSongs = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song -> song.artists.joinToString("") { it.name } }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }.let { if (sortDescending) it.reversed() else it }
        sortedSongs.map { ItemWrapper(it) }.toMutableStateList()
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selection by remember { mutableStateOf(false) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = surfaceColor.toArgb()
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val lazyListState = rememberLazyListState()

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter {
            it.item.title.contains(query.text, true) || it.item.artists.any { art -> art.name.contains(query.text, true) }
        }
    }

    LaunchedEffect(cachedSongs) {
        val thumbnailUrl = cachedSongs.firstOrNull()?.thumbnailUrl
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
        title = stringResource(R.string.cached_playlist),
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
            if (!isSearching && cachedSongs.isNotEmpty()) {
                PlaylistHeroHeader(
                    playlistName = stringResource(R.string.cached_playlist),
                    thumbnails = listOfNotNull(cachedSongs.firstOrNull()?.thumbnailUrl),
                    gradientColors = gradientColors,
                    songCount = cachedSongs.size,
                    totalDurationMs = 0L,
                    onPlay = { playerConnection.playQueue(ListQueue("Cache Songs", cachedSongs.map { it.toMediaItem() })) },
                    onShuffle = { playerConnection.playQueue(ListQueue("Cache Songs", cachedSongs.shuffled().map { it.toMediaItem() })) }
                )
            }
        },
        listContent = {
            if (!isSearching && cachedSongs.isNotEmpty()) {
                item {
                    SortHeader(
                        sortType = sortType, sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { t ->
                            when (t) {
                                SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                SongSortType.NAME -> R.string.sort_by_name
                                SongSortType.ARTIST -> R.string.sort_by_artist
                                SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            
            itemsIndexed(filteredSongs, key = { _, wrap -> wrap.item.id }) { index, songWrapper ->
                SongListItem(
                    song = songWrapper.item,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
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
                                    if (songWrapper.item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else playerConnection.playQueue(ListQueue("Cache Songs", cachedSongs.map { it.toMediaItem() }, index))
                                }
                            },
                            onLongClick = { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true }
                        ),
                    trailingContent = {
                        androidx.compose.material3.IconButton(onClick = {
                            menuState.show {
                                SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss, isFromCache = true)
                            }
                        }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) }
                    }
                )
            }
        }
    )
}
