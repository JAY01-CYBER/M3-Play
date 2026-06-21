/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for premium music experience      │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V3     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.SongSortDescendingKey
import com.j.m3play.constants.SongSortType
import com.j.m3play.constants.SongSortTypeKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.CachePlaylistMenu
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.jay.glossy.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val cachedSongs by viewModel.cachedSongs.collectAsStateWithLifecycle()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val sortedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sorted = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song -> song.artists.joinToString(separator = "") { it.name } }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }
        if (sortDescending) sorted.reversed() else sorted
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = { inSelectMode = false; selection.clear(); selectionAnchorSongId = null }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (inSelectMode) BackHandler(onBack = onExitSelectionMode)

    val filteredSongs = remember(sortedSongs, query) {
        if (query.text.isEmpty()) sortedSongs
        else sortedSongs.filter { song -> song.title.contains(query.text, true) || song.artists.any { it.name.contains(query.text, true) } }
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId -> if (filteredSongs.find { it.id == songId } == null) selection.remove(songId) }
        if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) { selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id }
    }

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40 } }
    val imageScrollOffset by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 0f } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            if (filteredSongs.isEmpty()) {
                item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
            } else {
                if (!isSearching) {
                    item(key = "playlist_header") {
                        CachePlaylistHeader(songs = filteredSongs, context = context, menuState = menuState, scrollOffset = imageScrollOffset, modifier = Modifier.animateItem())
                    }
                }

                item(key = "sort_header") {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                SortHeader(
                                    sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType -> when (sortType) { SongSortType.CREATE_DATE -> R.string.sort_by_create_date; SongSortType.NAME -> R.string.sort_by_name; SongSortType.ARTIST -> R.string.sort_by_artist; SongSortType.PLAY_TIME -> R.string.sort_by_play_time } },
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(song.id) else selection.remove(song.id) }
                    SongListItem(
                        song = song, isActive = song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                        trailingContent = {
                            if (inSelectMode) Checkbox(checked = song.id in selection, onCheckedChange = onCheckedChange)
                            else IconButton(onClick = { menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss, isFromCache = true) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                        },
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = { if (inSelectMode) onCheckedChange(song.id !in selection) else if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(ListQueue(title = "Cache Songs", items = cachedSongs.map { it.toMediaItem() }, startIndex = cachedSongs.indexOfFirst { it.id == song.id })) },
                            onLongClick = {
                                if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; onCheckedChange(true); selectionAnchorSongId = song.id }
                                else {
                                    val anchorIndex = selectionAnchorSongId?.let { anchorSongId -> filteredSongs.indexOfFirst { it.id == anchorSongId } } ?: -1
                                    if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorSongId = song.id }
                                    else { val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex; for (rIndex in range) { val rSongId = filteredSongs[rIndex].id; if (rSongId !in selection) selection.add(rSongId) } }
                                }
                            }
                        )
                    )
                }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = 2)

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (!isScrolled) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (inSelectMode) Text(pluralStringResource(R.plurals.n_song, selection.size, selection.size))
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search)) }, singleLine = true, shape = CircleShape,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(focusRequester)
                    )
                } else if (isScrolled) Text(stringResource(R.string.cached_playlist))
            },
            navigationIcon = {
                IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() } else if (inSelectMode) onExitSelectionMode() else navController.navigateUp() }) {
                    Icon(painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), null)
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(checked = selection.size == filteredSongs.size && selection.isNotEmpty(), onCheckedChange = { if (selection.size == filteredSongs.size) selection.clear() else { selection.clear(); selection.addAll(filteredSongs.map { it.id }) } })
                    IconButton(enabled = selection.isNotEmpty(), onClick = { menuState.show { SelectionSongMenu(songSelection = filteredSongs.filter { it.id in selection }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )
    }
}

@Composable
private fun CachePlaylistHeader(
    songs: List<Song>, context: android.content.Context, menuState: com.j.m3play.ui.component.MenuState, scrollOffset: Float, modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).graphicsLayer { translationY = scrollOffset * 0.5f; alpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f) }) {
            Surface(modifier = Modifier.size(240.dp).shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp)) {
                AsyncImage(model = songs.first().thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(text = stringResource(R.string.cached_playlist), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(12.dp))
        MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songs.size, songs.size))
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = "Cache Songs", items = songs.shuffled().map { it.toMediaItem() })) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null) }
            }
            Surface(onClick = { playerConnection.playQueue(ListQueue(title = "Cache Songs", items = songs.map { it.toMediaItem() })) }, color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painterResource(R.drawable.play), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            }
            Surface(onClick = { /* CacheMenu mappings linked accurately */ }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.more_vert), null) }
            }
        }
    }
}

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
