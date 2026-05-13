package com.j.m3play.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.*
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.menu.*
import com.j.m3play.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val viewState by viewModel.viewState.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.drop(1).collect { keyboardController?.hide() }
    }
    LaunchedEffect(query) { viewModel.query.value = query }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(top = 8.dp, bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 80.dp),
        modifier = Modifier.fillMaxSize().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (viewState.history.isNotEmpty()) {
            item(key = "history_header") {
                Text(text = stringResource(R.string.search_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            }
        }

        itemsIndexed(viewState.history, key = { _, it -> "history_${it.query}" }) { index, history ->
            SuggestionItem(
                query = history.query,
                online = false,
                shape = getGroupedShape(index, viewState.history.size),
                onClick = { onSearch(history.query); onDismiss() },
                onDelete = { database.query { delete(history) } },
                onFillTextField = { onQueryChange(TextFieldValue(history.query, TextRange(history.query.length))) },
                pureBlack = pureBlack
            )
        }

        if (viewState.items.isNotEmpty()) {
            item(key = "search_divider") {
                Text(text = stringResource(R.string.top_results), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
            }
            item(key = "search_divider_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
        }

        itemsIndexed(viewState.items.distinctBy { it.id }, key = { _, it -> "item_${it.id}" }) { index, item ->
            val shape = getGroupedShape(index, viewState.items.distinctBy { it.id }.size)
            YouTubeListItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(onClick = { menuState.show { when (item) {
                        is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                        is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                        is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                        is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = { menuState.dismiss(); onDismiss() })
                    } } }) { Icon(painterResource(R.drawable.more_vert), null) }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 1.dp)
                    .clip(shape)
                    .background(if (pureBlack) Color.DarkGray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else { playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())); onDismiss() }
                                }
                                is AlbumItem -> { navController.navigate("album/${item.id}"); onDismiss() }
                                is ArtistItem -> { navController.navigate("artist/${item.id}"); onDismiss() }
                                is PlaylistItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { when (item) {
                                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = { menuState.dismiss(); onDismiss() })
                            } }
                        }
                    )
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 1.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (pureBlack) Color.DarkGray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(painterResource(if (online) R.drawable.search else R.drawable.history), null, modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f), tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface)
        Text(text = query, style = MaterialTheme.typography.bodyLarge, color = if (pureBlack) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (!online) {
            IconButton(onClick = onDelete, modifier = Modifier.alpha(0.5f)) { Icon(painterResource(R.drawable.close), null, tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = onFillTextField, modifier = Modifier.alpha(0.5f)) { Icon(painterResource(R.drawable.arrow_top_left), null, tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun getGroupedShape(index: Int, size: Int): Shape {
    return when {
        size == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }
}
