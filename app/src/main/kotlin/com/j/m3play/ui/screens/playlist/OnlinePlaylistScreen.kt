/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ButtonPlaceholder
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) {
            songs.mapIndexed { index, song -> index to song }
        } else {
            songs
                .mapIndexed { index, song -> index to song }
                .filter { (_, song) ->
                    song.title.contains(query.text, ignoreCase = true) ||
                        song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
                }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }.toMutableStateList()

    // Base colors
    val baseDark = Color(0xFF121212)
    var extractedColor by remember { mutableStateOf<Color?>(null) }

    // Color extraction logic
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(128)
                .allowHardware(false)
                .build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            val bitmap = result?.image?.toBitmap()
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                // Get dominant color and fallback to dark muted
                val colorInt = palette.getDarkMutedColor(palette.getDominantColor(baseDark.toArgb()))
                extractedColor = Color(colorInt)
            }
        } else if (playlist != null) {
            // Fallback generation
            val hash = playlist!!.title.hashCode()
            val hue = ((hash and 0xFF) / 255f) * 360f
            extractedColor = hsvToColor(hue, 0.6f, 0.3f)
        }
    }

    // Creating that premium dark tinted background based on the cover art
    val bgColor by remember(extractedColor) {
        derivedStateOf {
            val color = extractedColor ?: baseDark
            // Blend heavily with black to keep it dark mode friendly
            lerp(color, baseDark, 0.85f)
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val headerItems by remember { derivedStateOf { val current = playlist; if (!isLoading && current != null && !isSearching) 1 else 0 } }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    // Force white text globally inside this Box
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                playlist.let { playlist ->
                    if (isLoading) {
                        item(key = "shimmer") {
                            ShimmerHost {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + AppBarHeight), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(260.dp).shimmer().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    TextPlaceholder(height = 32.dp, modifier = Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                                    ) {
                                        Box(modifier = Modifier.size(56.dp).shimmer().clip(CircleShape).background(Color.White.copy(0.1f)))
                                        ButtonPlaceholder(modifier = Modifier.height(56.dp).width(160.dp).clip(RoundedCornerShape(50)))
                                        Box(modifier = Modifier.size(56.dp).shimmer().clip(CircleShape).background(Color.White.copy(0.1f)))
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                                repeat(6) { ListItemPlaceHolder() }
                            }
                        }
                    } else if (playlist != null) {
                        if (!isSearching) {
                            item(key = "header") {
                                Column(
                                    modifier = Modifier.fillMaxWidth().animateItem(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(systemBarsTopPadding + AppBarHeight))

                                    // Square Thumbnail with rounded corners
                                    Surface(
                                        modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    ) {
                                        if (playlist.thumbnail != null) {
                                            AsyncImage(
                                                model = playlist.thumbnail,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.5f))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Text Information
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    playlist.author?.name?.let { authorName ->
                                        Text(
                                            text = authorName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Integrated Search Bar below text
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .height(48.dp)
                                            .clickable { isSearching = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.search), contentDescription = null, tint = Color.White)
                                            Spacer(Modifier.width(12.dp))
                                            Text(stringResource(R.string.search), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Action Row
                                    val isThisPlaying = isPlaying && songs.any { it.id == mediaMetadata?.id }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Shuffle
                                        val mixEndpoint = playlist.shuffleEndpoint ?: playlist.radioEndpoint
                                        if (mixEndpoint != null) {
                                            Box(
                                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                                    .clickable { playerConnection.playQueue(YouTubeQueue(mixEndpoint)) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(56.dp))
                                        }

                                        // Play Pill Button (White Background, Black Text)
                                        Box(
                                            modifier = Modifier
                                                .height(56.dp)
                                                .widthIn(min = 160.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White)
                                                .clickable {
                                                    if (isThisPlaying) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        val firstSong = songs.firstOrNull()
                                                        if (firstSong != null) {
                                                            playerConnection.service.getAutomix(playlistId = playlist.id)
                                                            playerConnection.playQueue(YouTubeQueue(firstSong.endpoint ?: WatchEndpoint(videoId = firstSong.id), firstSong.toMediaMetadata()))
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(if (isThisPlaying) R.drawable.pause else R.drawable.play),
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(28.dp),
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isThisPlaying) "Pause" else "Play",
                                                    color = Color.Black,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Download Action
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                                .clickable { /* Handle download action */ },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(painterResource(R.drawable.download), contentDescription = "Download", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))

                                    playlist.songCountText?.let { countText ->
                                        Text(
                                            text = countText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                            textAlign = TextAlign.Start
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        if (songs.isEmpty() && !isLoading && error == null) {
                            item(key = "empty") {
                                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = stringResource(R.string.empty_playlist), style = MaterialTheme.typography.titleLarge, color = Color.White)
                                }
                            }
                        }

                        items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                            val isActive = mediaMetadata?.id == song.item.second.id
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)

                            CompositionLocalProvider(LocalContentColor provides Color.White) {
                                YouTubeListItem(
                                    item = song.item.second,
                                    viewCountText = viewCounts[song.item.second.id]?.let { count -> formatCompactCount(count.toLong()) },
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    isSelected = song.isSelected && selection,
                                    trailingContent = {
                                        IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song.item.second, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) {
                                            Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                        }
                                    },
                                    modifier = itemModifier.combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (isActive) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.service.getAutomix(playlistId = playlist.id)
                                                    playerConnection.playQueue(YouTubeQueue(song.item.second.endpoint ?: WatchEndpoint(videoId = song.item.second.id), song.item.second.toMediaMetadata()))
                                                }
                                            } else {
                                                song.isSelected = !song.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
                                        }
                                    ).animateItem()
                                )
                            }
                        }

                        if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                            item(key = "loading_more") { ShimmerHost { repeat(2) { ListItemPlaceHolder() } } }
                        }
                    } else {
                        item(key = "error") {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.playlist_not_found), style = MaterialTheme.typography.titleLarge, color = Color.White)
                            }
                        }
                    }
                }
            }

            DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

            // Transparent TopAppBar (Title hides unless scrolling or searching)
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSearching || showTopBarTitle) bgColor.copy(alpha = 0.95f) else Color.Transparent,
                    scrolledContainerColor = bgColor.copy(alpha = 0.95f)
                ),
                title = {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(text = pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    } else if (isSearching) {
                        TextField(
                            value = query, onValueChange = { query = it }, placeholder = { Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.7f)) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else if (showTopBarTitle) {
                        Text(playlist?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue() }
                        else if (selection) { selection = false }
                        else { navController.navigateUp() }
                    }, onLongClick = {}) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = Color.White) }
                },
                actions = {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(onClick = {
                            if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                            else wrappedSongs.forEach { it.isSelected = true }
                        }, onLongClick = {}) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = Color.White) }
                        IconButton(onClick = { menuState.show { SelectionMediaMetadataMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaItem().metadata!! }, onDismiss = menuState::dismiss, clearAction = { selection = false }, currentItems = emptyList()) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                    } else if (!isSearching) {
                        if (playlist != null && playlist!!.id != "LM") {
                            IconButton(
                                onClick = {
                                    if (dbPlaylist?.playlist == null) {
                                        database.transaction {
                                            val playlistEntity = PlaylistEntity(name = playlist!!.title, browseId = playlist!!.id, thumbnailUrl = playlist!!.thumbnail, isEditable = playlist!!.isEditable, playEndpointParams = playlist!!.playEndpoint?.params, shuffleEndpointParams = playlist!!.shuffleEndpoint?.params, radioEndpointParams = playlist!!.radioEndpoint?.params).toggleLike()
                                            insert(playlistEntity)
                                            songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index) }.forEach(::insert)
                                        }
                                    } else {
                                        database.transaction {
                                            val currentPlaylist = dbPlaylist!!.playlist
                                            update(currentPlaylist, playlist!!)
                                            update(currentPlaylist.toggleLike())
                                        }
                                    }
                                },
                                onLongClick = {}
                            ) {
                                val isLiked = dbPlaylist?.playlist?.bookmarkedAt != null
                                Icon(
                                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                                    contentDescription = null,
                                    tint = if (isLiked) Color.Red else Color.White
                                )
                            }
                        }
                        IconButton(onClick = {
                            menuState.show { YouTubePlaylistMenu(playlist = playlist!!, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss, selectAction = { selection = true }, canSelect = true, snackbarHostState = snackbarHostState) }
                        }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                    }
                }
            )

            PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
        }
    }
}

private fun generateGradientFromTitle(title: String): List<Color> {
    val hash = title.hashCode()
    val hue1 = ((hash and 0xFF) / 255f) * 360f
    val hue2 = (((hash shr 8) and 0xFF) / 255f) * 360f
    return listOf(hsvToColor(hue1, 0.7f, 0.9f), hsvToColor(hue2, 0.7f, 0.85f))
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val h = hue / 60f
    val c = value * saturation
    val x = c * (1 - abs((h % 2) - 1))
    val m = value - c
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    return Color(red = (r + m), green = (g + m), blue = (b + m), alpha = 1f)
}
