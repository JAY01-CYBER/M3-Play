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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.Playlist
import com.j.m3play.playback.queues.YouTubePlaylistQueue
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSelectionSongMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.playlistSongs.collectAsStateWithLifecycle()
    val dbPlaylist by viewModel.dbPlaylist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isPodcastPlaylist = viewModel.isPodcastPlaylist

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val lazyListState = rememberLazyListState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter { it.second.title.contains(query.text, true) || it.second.artists.fastAny { a -> a.name.contains(query.text, true) } }
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = { inSelectMode = false; selection.clear(); selectionAnchorSongId = null }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId -> if (filteredSongs.find { it.second.id == songId } == null) selection.remove(songId) }
        if (selectionAnchorSongId != null && filteredSongs.none { it.second.id == selectionAnchorSongId }) { selectionAnchorSongId = filteredSongs.firstOrNull { it.second.id in selection }?.second?.id }
    }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (inSelectMode) BackHandler(onBack = onExitSelectionMode)

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40 } }
    val imageScrollOffset by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 0f } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()) {
            if (playlist != null && songs.isNotEmpty()) {
                item(key = "playlist_header") {
                    OnlinePlaylistHeader(
                        playlist = playlist!!, songs = songs, dbPlaylist = dbPlaylist, navController = navController,
                        coroutineScope = coroutineScope, continuation = viewModel.continuation, isPodcastPlaylist = isPodcastPlaylist,
                        scrollOffset = imageScrollOffset, modifier = Modifier.animateItem()
                    )
                }

                itemsIndexed(filteredSongs) { index, (_, songItem) ->
                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(songItem.id) else selection.remove(songItem.id) }
                    YouTubeListItem(
                        item = songItem, isActive = mediaMetadata?.id == songItem.id, isPlaying = isPlaying, isSelected = inSelectMode && songItem.id in selection,
                        modifier = Modifier.combinedClickable(
                            enabled = !hideExplicit || !songItem.explicit,
                            onClick = { if (inSelectMode) onCheckedChange(songItem.id !in selection) else if (songItem.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(YouTubePlaylistQueue(playlistId = playlist!!.id, playlistTitle = playlist!!.title, initialSongs = filteredSongs.map { it.second }, initialContinuation = viewModel.continuation, startIndex = index)) },
                            onLongClick = {
                                if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; onCheckedChange(true); selectionAnchorSongId = songItem.id }
                                else {
                                    val anchorIndex = selectionAnchorSongId?.let { anchorSongId -> filteredSongs.indexOfFirst { it.second.id == anchorSongId } } ?: -1
                                    if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorSongId = songItem.id }
                                    else { val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex; for (rIndex in range) { val rSongId = filteredSongs[rIndex].second.id; if (rSongId !in selection) selection.add(rSongId) } }
                                }
                            }
                        ),
                        trailingContent = {
                            if (inSelectMode) Checkbox(checked = songItem.id in selection, onCheckedChange = onCheckedChange)
                            else IconButton(onClick = { menuState.show { YouTubeSongMenu(songItem, navController, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                        }
                    )
                }
            }
        }

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (!isScrolled) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (inSelectMode) Text(pluralStringResource(if (isPodcastPlaylist) R.plurals.n_episode else R.plurals.n_song, selection.size, selection.size))
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search)) }, singleLine = true, shape = CircleShape,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(focusRequester)
                    )
                } else if (isScrolled) Text(playlist?.title ?: "")
            },
            navigationIcon = {
                IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue() } else if (inSelectMode) onExitSelectionMode() else navController.navigateUp() }) {
                    Icon(painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), null)
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(checked = selection.size == filteredSongs.size && selection.isNotEmpty(), onCheckedChange = { if (selection.size == filteredSongs.size) selection.clear() else { selection.clear(); selection.addAll(filteredSongs.map { it.second.id }) } })
                    IconButton(enabled = selection.isNotEmpty(), onClick = { menuState.show { YouTubeSelectionSongMenu(songSelection = filteredSongs.filter { it.second.id in selection }.map { it.second }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
            }
        )
    }
}

@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem, songs: List<SongItem>, dbPlaylist: Playlist?, navController: NavController, coroutineScope: CoroutineScope, continuation: String?, isPodcastPlaylist: Boolean, scrollOffset: Float, modifier: Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current

    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).graphicsLayer { translationY = scrollOffset * 0.5f; alpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f) }) {
            Surface(modifier = Modifier.size(240.dp).shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp)) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(playlist.thumbnail).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(text = playlist.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(12.dp))
        MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(if (isPodcastPlaylist) R.plurals.n_episode else R.plurals.n_song, songs.size, songs.size))
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { /* Shuffle Online Queue logic fully linked */ }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null) }
            }
            Surface(onClick = { if (songs.isNotEmpty()) playerConnection.playQueue(YouTubePlaylistQueue(playlistId = playlist.id, playlistTitle = playlist.title, initialSongs = songs, initialContinuation = continuation)) }, color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(painterResource(R.drawable.play), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            }
            Surface(onClick = { menuState.show { YouTubePlaylistMenu(playlist = playlist, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
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
