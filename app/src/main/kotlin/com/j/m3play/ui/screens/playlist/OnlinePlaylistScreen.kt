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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
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
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel

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
    val focusManager = LocalFocusManager.current
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
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs =
        remember(songs, query) {
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
        BackHandler { selection = false }
    }

    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }.toMutableStateList()

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50 } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val isTopBarSolid by remember { derivedStateOf { isScrolled || isSearching || selection } }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.background
    val darkOverlay = Color.Black.copy(alpha = 0.4f)
    val isLiked = dbPlaylist?.playlist?.bookmarkedAt != null
    
    val isPlaylistPlaying = remember(songs, mediaMetadata) { songs.fastAny { it.id == mediaMetadata?.id } }
    val showPause = isPlaylistPlaying && isPlaying

    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                }
            }
        } else { gradientColors = emptyList() }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) { viewModel.loadMoreSongs() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
    ) {
        LazyColumn(
            state = lazyListState,
            // 0 content padding so image reaches top edge perfectly
            contentPadding = PaddingValues(0.dp),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).shimmer().background(MaterialTheme.colorScheme.onSurface.copy(0.1f)))
                                Spacer(modifier = Modifier.height(24.dp))
                                TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f))
                                Spacer(modifier = Modifier.height(8.dp))
                                TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(0.1f)))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Box(modifier = Modifier.height(48.dp).width(120.dp).shimmer().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.onSurface.copy(0.1f)))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(0.1f)))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                // 1. Full-Width Edge-to-Edge Hero Image
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                    AsyncImage(model = playlist.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    // Smooth Gradient Fade Bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Transparent,
                                                        surfaceColor.copy(alpha = 0.5f),
                                                        surfaceColor
                                                    )
                                                )
                                            )
                                    )
                                    // Title & Info Inside Artwork
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = playlist.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, textAlign = TextAlign.Center)
                                        playlist.author?.let { artist ->
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = artist.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val metaText = if (playlist.songCountText != null) "Playlist • ${playlist.songCountText}" else "Playlist"
                                        Text(text = metaText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                                    }
                                }

                                // 2. SimpMusic Action Row (Shuffle, Play/Pause Pill, Radio/Mix)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Shuffle Circle
                                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                                .clickable { playerConnection.playQueue(YouTubeQueue(shuffleEndpoint)) },
                                            contentAlignment = Alignment.Center
                                        ) { Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                                    } ?: Spacer(modifier = Modifier.size(48.dp))

                                    // White Play/Pause Pill
                                    Box(
                                        modifier = Modifier.height(48.dp).widthIn(min = 120.dp).clip(RoundedCornerShape(24.dp)).background(Color.White)
                                            .clickable {
                                                if (isPlaylistPlaying) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playlist.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                                                }
                                            }
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(if (showPause) R.drawable.pause else R.drawable.play), null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (showPause) "Pause" else "Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    // Mix/Radio Circle
                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                                .clickable { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) },
                                            contentAlignment = Alignment.Center
                                        ) { Icon(painterResource(R.drawable.mix), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                                    } ?: Spacer(modifier = Modifier.size(48.dp))
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        item(key = "empty") {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.empty_playlist), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }

                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.second.id }
                    ) { index, song ->
                        val isActive = mediaMetadata?.id == song.item.second.id
                        val isSelected = song.isSelected && selection

                        Column(modifier = Modifier.fillMaxWidth()) {
                            YouTubeListItem(
                                item = song.item.second,
                                viewCountText = viewCounts[song.item.second.id]?.let { formatCompactCount(it.toLong()) },
                                isActive = isActive,
                                isPlaying = isPlaying,
                                isSelected = isSelected,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { YouTubeSongMenu(song = song.item.second, navController = navController, onDismiss = menuState::dismiss) } },
                                        onLongClick = {}
                                    ) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .background(when { isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f); isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f); else -> Color.Transparent })
                                    .combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (isActive) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.service.getAutomix(playlistId = playlist.id)
                                                    playerConnection.playQueue(YouTubeQueue(song.item.second.endpoint ?: WatchEndpoint(videoId = song.item.second.id), song.item.second.toMediaMetadata()))
                                                }
                                            } else { song.isSelected = !song.isSelected }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) { selection = true }
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            // SimpMusic style divider
                            if (index < wrappedSongs.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") { ShimmerHost { repeat(2) { ListItemPlaceHolder() } } }
                    }
                } else {
                    val isPrivatePlaylist = error?.contains("PLAYLIST_PRIVATE") == true
                    item(key = "error") {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isPrivatePlaylist) {
                                Image(painterResource(R.drawable.anime_blank), null, modifier = Modifier.size(120.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.playlist_private_title), style = MaterialTheme.typography.titleLarge)
                            } else {
                                Text(text = if (error != null) stringResource(R.string.error_unknown) else stringResource(R.string.playlist_not_found), style = MaterialTheme.typography.titleLarge, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                if (error != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.retry() }) { Text(stringResource(R.string.retry)) }
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)))
            }
        }

        // 5. SimpMusic Style Top App Bar (Transparent until scroll)
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isTopBarSolid) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge)
                } else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) },
                        singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            if (query.text.isNotEmpty()) {
                                androidx.compose.material3.IconButton(onClick = { query = TextFieldValue() }) {
                                    Icon(painterResource(R.drawable.close), null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).background(darkOverlay, RoundedCornerShape(50))
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                        else if (selection) { selection = false }
                        else { navController.navigateUp() }
                    },
                    onLongClick = {},
                    modifier = Modifier.padding(start = 8.dp).background(if(!isTopBarSolid && !isSearching) darkOverlay else Color.Transparent, CircleShape)
                ) {
                    Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = if(!isTopBarSolid && !isSearching) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = { if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }, onLongClick = {}) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }
                    IconButton(onClick = { menuState.show { SelectionMediaMetadataMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaItem().metadata!! }, onDismiss = menuState::dismiss, clearAction = { selection = false }, currentItems = emptyList()) } }, onLongClick = {}) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) {
                    Row(
                        modifier = Modifier.padding(end = 8.dp).background(if(!isTopBarSolid) darkOverlay else Color.Transparent, RoundedCornerShape(50)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playlist != null && playlist?.id != "LM") {
                            IconButton(
                                onClick = {
                                    if (dbPlaylist?.playlist == null) {
                                        database.transaction {
                                            val playlistEntity = PlaylistEntity(name = playlist!!.title, browseId = playlist!!.id, thumbnailUrl = playlist!!.thumbnail, isEditable = playlist!!.isEditable).toggleLike()
                                            insert(playlistEntity)
                                            songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index) }.forEach(::insert)
                                        }
                                    } else {
                                        database.transaction { val currentPlaylist = dbPlaylist!!.playlist; update(currentPlaylist, playlist!!); update(currentPlaylist.toggleLike()) }
                                    }
                                },
                                onLongClick = {}
                            ) {
                                Icon(painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (isLiked) Color.Red else if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                            Icon(painterResource(R.drawable.search), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(
                            onClick = {
                                if (playlist != null) { menuState.show { YouTubePlaylistMenu(playlist = playlist!!, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss, selectAction = { selection = true }, canSelect = true, snackbarHostState = snackbarHostState) } }
                            },
                            onLongClick = {}
                        ) {
                            Icon(painterResource(R.drawable.more_vert), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars))
    }
}
