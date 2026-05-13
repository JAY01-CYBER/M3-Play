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

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.CONTENT_TYPE_LIST
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.Playlist
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.viewmodels.LocalFilter
import com.j.m3play.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()
    val lazyListState = rememberLazyListState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.drop(1).collect { keyboardController?.hide() }
    }
    
    LaunchedEffect(query) { 
        viewModel.query.value = query 
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 80.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .let { base -> if (isLandscape) base.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)) else base }
    ) {
        // Exact Vivi Style Sticky Header for Chips
        stickyHeader {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            ) {
                ChipsRow(
                    chips = listOf(
                        LocalFilter.ALL to stringResource(R.string.filter_all),
                        LocalFilter.SONG to stringResource(R.string.filter_songs),
                        LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                        LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                        LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
                    ),
                    currentValue = searchFilter,
                    onValueUpdate = { viewModel.filter.value = it },
                )
            }
        }

        result.map.forEach { (filter, items) ->
            if (result.filter == LocalFilter.ALL) {
                item(key = filter) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { viewModel.filter.value = filter }
                            .padding(start = 12.dp, end = 18.dp),
                    ) {
                        Text(
                            text = stringResource(
                                when (filter) {
                                    LocalFilter.SONG -> R.string.filter_songs
                                    LocalFilter.ALBUM -> R.string.filter_albums
                                    LocalFilter.ARTIST -> R.string.filter_artists
                                    LocalFilter.PLAYLIST -> R.string.filter_playlists
                                    LocalFilter.ALL -> error("")
                                }
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(painter = painterResource(R.drawable.navigate_next), contentDescription = null)
                    }
                }
            }

            // FIX: Removed .distinctBy{ it.id } to fix compilation error
            itemsIndexed(
                items = items,
                contentType = { CONTENT_TYPE_LIST }
            ) { index, item ->
                val shape = getLocalGroupedShape(index, items.size)
                val modifierBase = Modifier
                    .padding(horizontal = 16.dp, vertical = 1.dp)
                    .clip(shape)
                    .background(if (pureBlack) Color.DarkGray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                
                when (item) {
                    is Song -> SongListItem(
                        song = item,
                        showInLibraryIcon = true,
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = { 
                                    menuState.show { 
                                        SongMenu(originalSong = item, navController = navController, onDismiss = { onDismiss(); menuState.dismiss() }, isFromCache = isFromCache) 
                                    } 
                                }
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null)
                            }
                        },
                        modifier = modifierBase.combinedClickable(
                            onClick = {
                                if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                else {
                                    val songs = result.map.getOrDefault(LocalFilter.SONG, emptyList()).filterIsInstance<Song>().map { it.toMediaItem() }
                                    playerConnection.playQueue(ListQueue(title = context.getString(R.string.queue_searched_songs), items = songs, startIndex = songs.indexOfFirst { it.mediaId == item.id }))
                                }
                            },
                            onLongClick = { 
                                menuState.show { 
                                    SongMenu(originalSong = item, navController = navController, onDismiss = { onDismiss(); menuState.dismiss() }, isFromCache = isFromCache) 
                                } 
                            }
                        )
                    )
                    is Album -> AlbumListItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        modifier = modifierBase.clickable { onDismiss(); navController.navigate("album/${item.id}") }
                    )
                    is Artist -> ArtistListItem(
                        artist = item,
                        modifier = modifierBase.clickable { onDismiss(); navController.navigate("artist/${item.id}") }
                    )
                    is Playlist -> PlaylistListItem(
                        playlist = item,
                        modifier = modifierBase.clickable { onDismiss(); navController.navigate("local_playlist/${item.id}") }
                    )
                }
            }
        }

        if (result.query.isNotEmpty() && result.map.isEmpty()) {
            item(key = "no_result") { 
                EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) 
            }
        }
    }
}

// Function to handle Vivi-style grouped rounded corners for Local lists
@Composable
fun getLocalGroupedShape(index: Int, size: Int): Shape {
    return when {
        size == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }
}
