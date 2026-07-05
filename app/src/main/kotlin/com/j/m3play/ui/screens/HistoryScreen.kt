/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V4     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HistorySource
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.db.entities.EventWithSong
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.DateAgo
import com.j.m3play.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var selection by remember { mutableStateOf(false) }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val historySource by viewModel.historySource.collectAsState()
    val events by viewModel.events.collectAsState()
    val historyPage by viewModel.historyPage

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    fun dateAgoToString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> context.getString(R.string.today)
            DateAgo.Yesterday -> context.getString(R.string.yesterday)
            DateAgo.ThisWeek -> context.getString(R.string.this_week)
            DateAgo.LastWeek -> context.getString(R.string.last_week)
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    class WrappedHistoryItem(val item: EventWithSong) {
        var isSelected by mutableStateOf(false)
    }

    val filteredEvents = remember(events, query) {
        if (query.text.isEmpty()) {
            events
        } else {
            events.mapValues { (_, songs) ->
                songs.filter { event ->
                    event.song.song.title.contains(query.text, ignoreCase = true) ||
                            event.song.artists.any {
                                it.name.contains(query.text, ignoreCase = true)
                            }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    val filteredRemoteContent = remember(historyPage, query) {
        if (query.text.isEmpty()) {
            historyPage?.sections
        } else {
            historyPage?.sections?.map { section ->
                section.copy(
                    songs = section.songs.filter { song ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.any { it.name.contains(query.text, ignoreCase = true) }
                    }
                )
            }?.filter { it.songs.isNotEmpty() }
        }
    }

    val wrappedItemsMap = remember(filteredEvents) {
        filteredEvents.mapValues { (_, events) ->
            events.map { WrappedHistoryItem(it) }.toMutableStateList()
        }
    }

    val allWrappedItems = remember(wrappedItemsMap) {
        wrappedItemsMap.values.flatten()
    }

    val lazyListState = rememberLazyListState()

    val isFabVisible = if (historySource == HistorySource.REMOTE) {
        filteredRemoteContent?.any { it.songs.isNotEmpty() } == true
    } else {
        allWrappedItems.isNotEmpty()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Row(modifier = Modifier.clip(CircleShape)) {
                            // Local Tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (historySource == HistorySource.LOCAL) MaterialTheme.colorScheme.surface 
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.historySource.value = HistorySource.LOCAL }
                                    .padding(horizontal = 32.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.local_history),
                                    fontWeight = if (historySource == HistorySource.LOCAL) FontWeight.Bold else FontWeight.Medium,
                                    color = if (historySource == HistorySource.LOCAL) MaterialTheme.colorScheme.onSurface 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Remote Tab (Only if logged in)
                            if (isLoggedIn) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (historySource == HistorySource.REMOTE) MaterialTheme.colorScheme.surface 
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            viewModel.historySource.value = HistorySource.REMOTE
                                            viewModel.fetchRemoteHistory()
                                        }
                                        .padding(horizontal = 32.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.remote_history),
                                        fontWeight = if (historySource == HistorySource.REMOTE) FontWeight.Bold else FontWeight.Medium,
                                        color = if (historySource == HistorySource.REMOTE) MaterialTheme.colorScheme.onSurface 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                filteredRemoteContent?.forEach { section ->
                    stickyHeader {
                        NavigationTitle(
                            title = section.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    items(
                        items = section.songs,
                        key = { "${section.title}_${it.id}_${section.songs.indexOf(it)}" }
                    ) { song ->
                        YouTubeListItem(
                            item = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                    onLongClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            } else {
                filteredEvents.forEach { (dateAgo, events) ->
                    stickyHeader {
                        NavigationTitle(
                            title = dateAgoToString(dateAgo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    val currentDateWrappedItems = wrappedItemsMap[dateAgo] ?: emptyList()
                    
                    itemsIndexed(
                        items = currentDateWrappedItems,
                        key = { index, wrappedItem -> "${dateAgo}_${wrappedItem.item.event.id}_$index" }
                    ) { index, wrappedItem ->
                        val event = wrappedItem.item
                        
                        SongListItem(
                            song = event.song,
                            isActive = event.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            isSelected = wrappedItem.isSelected && selection,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        if (!selection) {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = event.song,
                                                    event = event.event,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (event.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = dateAgoToString(dateAgo),
                                                        items = currentDateWrappedItems.map { it.item.song.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        } else {
                                            wrappedItem.isSelected = !wrappedItem.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            allWrappedItems.forEach { it.isSelected = false }
                                            wrappedItem.isSelected = true
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            }
        }

        // New Pill-Shaped Extended FAB with Text and Icon
        androidx.compose.animation.AnimatedVisibility(
            visible = isFabVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 72.dp), // Adding extra padding for mini-player
            enter = androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.scaleOut()
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (historySource == HistorySource.REMOTE && historyPage != null) {
                        val songs = filteredRemoteContent?.flatMap { it.songs } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.history),
                                    items = songs.map { it.toMediaItem() }.shuffled()
                                )
                            )
                        }
                    } else {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.history),
                                items = allWrappedItems.map { it.item.song.toMediaItem() }.shuffled()
                            )
                        )
                    }
                },
                icon = { 
                    Icon(
                        painter = painterResource(R.drawable.shuffle), 
                        contentDescription = null
                    ) 
                },
                text = { Text("Shuffle") },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    // Changed to CenterAlignedTopAppBar
    CenterAlignedTopAppBar(
        title = {
            if (selection) {
                val count = allWrappedItems.count { it.isSelected }
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (isSearching) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.history),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Your recently played songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                        }

                        selection -> {
                            selection = false
                        }

                        else -> {
                            navController.navigateUp()
                        }
                    }
                },
                onLongClick = {
                    if (!isSearching && !selection) {
                        navController.backToMain()
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = allWrappedItems.count { it.isSelected }
                IconButton(
                    onClick = {
                        if (count == allWrappedItems.size) {
                            allWrappedItems.forEach { it.isSelected = false }
                        } else {
                            allWrappedItems.forEach { it.isSelected = true }
                        }
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == allWrappedItems.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionMediaMetadataMenu(
                                songSelection = allWrappedItems
                                    .filter { it.isSelected }
                                    .map { it.item.song.toMediaItem().metadata!! },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false },
                                currentItems = emptyList()
                            )
                        }
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            } else if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true },
                    onLongClick = {},
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null
                    )
                }
                // (3 dot menu se hataya gaya jaisa aapne bola)
            }
        }
    )
}
