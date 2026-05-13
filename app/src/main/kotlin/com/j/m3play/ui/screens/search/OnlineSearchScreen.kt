/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

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

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { keyboardController?.hide() }
    }

    LaunchedEffect(query) { viewModel.query.value = query }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 80.dp 
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // --- Search History Section ---
        if (viewState.history.isNotEmpty()) {
            item(key = "history_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(10.dp))
                    Text(text = stringResource(R.string.search_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        itemsIndexed(viewState.history, key = { _, it -> "history_${it.query}" }) { _, history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = { onSearch(history.query); onDismiss() },
                onDelete = { database.query { delete(history) } },
                onFillTextField = { onQueryChange(TextFieldValue(history.query, TextRange(history.query.length))) },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        // --- Top Results Section ---
        if (viewState.items.isNotEmpty() && viewState.history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(10.dp))
                    Text(text = stringResource(R.string.top_results), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        itemsIndexed(viewState.items.distinctBy { it.id }, key = { _, it -> "item_${it.id}" }) { _, item ->
            YouTubeListItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = { menuState.dismiss(); onDismiss() })
                                }
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                        onDismiss()
                                    }
                                }
                                is AlbumItem -> { navController.navigate("album/${item.id}"); onDismiss() }
                                is ArtistItem -> { navController.navigate("artist/${item.id}"); onDismiss() }
                                is PlaylistItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                }
                            }
                        }
                    )
                    .animateItem()
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                tint = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = if (pureBlack) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(onClick = onDelete, modifier = Modifier.alpha(0.6f)) {
                Icon(painter = painterResource(R.drawable.close), contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        IconButton(onClick = onFillTextField, modifier = Modifier.alpha(0.6f)) {
            Icon(painter = painterResource(R.drawable.arrow_top_left), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
