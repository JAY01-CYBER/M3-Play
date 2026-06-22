/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Style: YT Music Simple & Premium          │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.imageLoader
import coil3.toBitmap
import androidx.palette.graphics.Palette
import com.valentinilk.shimmer.shimmer
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
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
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

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

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = kotlinx.coroutines.withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                }
            }
        } else gradientColors = emptyList()
    }

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40 } }
    val imageScrollOffset by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 0f } }
    val headerAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 400f)).coerceIn(0f, 1f) else 0f } }
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) else 0f } }

    val showTopBarTitle by remember { derivedStateOf { isScrolled } }
    val transparentAppBar by remember { derivedStateOf { !inSelectMode && !isScrolled } }
    val headerItems by remember { derivedStateOf { val current = playlist; if (!isLoading && current != null && !isSearching) 1 else 0 } }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) viewModel.loadMoreSongs()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(surfaceColor).pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
    ) {
        if (gradientColors.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(0.5f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                    val headerColor = gradientColors.getOrNull(0) ?: surfaceColor
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(headerColor.copy(alpha = 0.5f * gradientAlpha), surfaceColor),
                            startY = 0f, endY = size.height
                        )
                    )
                }
            )
        }

        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()) {
            playlist.let { playlist ->
                if (isLoading) {
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).size(240.dp).shimmer().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurface))
                                TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(50.dp))
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(50.dp))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 48.dp)
                                    .graphicsLayer { alpha = headerAlpha }.padding(horizontal = 24.dp).padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).graphicsLayer { translationY = imageScrollOffset * 0.5f }) {
                                    Surface(
                                        modifier = Modifier.size(240.dp).shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { AsyncImage(model = playlist.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                }

                                Text(text = playlist.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))

                                playlist.author?.let { artist ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary).toSpanStyle()) {
                                                if (artist.id != null) { val link = LinkAnnotation.Clickable(artist.id!!) { navController.navigate("artist/${artist.id}") }; withLink(link) { append(artist.name) } } else append(artist.name)
                                            }
                                        },
                                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val songCountText = playlist.songCountText ?: pluralStringResource(R.plurals.n_song, songs.size, songs.size)
                                Text(
                                    text = songCountText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                        Button(
                                            onClick = { playerConnection.playQueue(YouTubeQueue(shuffleEndpoint)) },
                                            shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(50.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                                Icon(painterResource(R.drawable.shuffle), null, Modifier.size(24.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.shuffle), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                    }

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        FilledTonalButton(
                                            onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) },
                                            shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(50.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                                Icon(painterResource(R.drawable.radio), null, Modifier.size(24.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.radio), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (playlist.id != "LM") {
                                        Surface(
                                            onClick = {
                                                if (dbPlaylist?.playlist == null) {
                                                    database.transaction {
                                                        val playlistEntity = PlaylistEntity(name = playlist.title, browseId = playlist.id, thumbnailUrl = playlist.thumbnail, isEditable = playlist.isEditable, playEndpointParams = playlist.playEndpoint?.params, shuffleEndpointParams = playlist.shuffleEndpoint?.params, radioEndpointParams = playlist.radioEndpoint?.params).toggleLike()
                                                        insert(playlistEntity)
                                                        songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index) }.forEach(::insert)
                                                    }
                                                } else database.transaction { val currentPlaylist = dbPlaylist!!.playlist; update(currentPlaylist, playlist); update(currentPlaylist.toggleLike()) }
                                            },
                                            shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                                        }
                                    }

                                    val mixEndpoint = playlist.shuffleEndpoint ?: playlist.radioEndpoint
                                    if (mixEndpoint != null) {
                                        Surface(onClick = { playerConnection.playQueue(YouTubeQueue(mixEndpoint)) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.mix), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                                        }
                                    }

                                    Surface(
                                        onClick = { menuState.show { YouTubePlaylistMenu(playlist = playlist, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss, selectAction = { inSelectMode = true }, canSelect = true, snackbarHostState = snackbarHostState) } },
                                        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.more_vert), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        item(key = "empty") {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.empty_playlist), style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = stringResource(R.string.empty_playlist_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    itemsIndexed(items = filteredSongs, key = { _, songItem -> songItem.second.id }) { index, songItem ->
                        val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(songItem.second.id) else selection.remove(songItem.second.id) }
                        val explicitEnabled = !hideExplicit || !songItem.second.explicit
                        
                        YouTubeListItem(
                            item = songItem.second, viewCountText = viewCounts[songItem.second.id]?.let { count -> formatCompactCount(count.toLong()) },
                            isActive = mediaMetadata?.id == songItem.second.id, isPlaying = isPlaying, isSelected = inSelectMode && songItem.second.id in selection,
                            modifier = Modifier.combinedClickable(
                                enabled = explicitEnabled,
                                onClick = {
                                    if (inSelectMode) onCheckedChange(songItem.second.id !in selection)
                                    else if (songItem.second.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else { playerConnection.service.getAutomix(playlistId = playlist.id); playerConnection.playQueue(YouTubeQueue(songItem.second.endpoint ?: WatchEndpoint(videoId = songItem.second.id), songItem.second.toMediaMetadata())) }
                                },
                                onLongClick = {
                                    if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; onCheckedChange(true); selectionAnchorSongId = songItem.second.id }
                                    else {
                                        val anchorIndex = selectionAnchorSongId?.let { anchorSongId -> filteredSongs.indexOfFirst { it.second.id == anchorSongId } } ?: -1
                                        if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorSongId = songItem.second.id }
                                        else { 
                                            val anchorInt = anchorIndex.toInt()
                                            val indexInt = index.toInt()
                                            val range = if (anchorInt <= indexInt) anchorInt..indexInt else indexInt..anchorInt
                                            for (rIndex in range) { val rSongId = filteredSongs[rIndex].second.id; if (rSongId !in selection) selection.add(rSongId) } 
                                        }
                                    }
                                }
                            ),
                            trailingContent = {
                                if (inSelectMode) Checkbox(checked = songItem.second.id in selection, onCheckedChange = onCheckedChange)
                                else IconButton(onClick = { menuState.show { YouTubeSongMenu(song = songItem.second, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                            }
                        )
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") { ShimmerHost { repeat(2) { ListItemPlaceHolder() } } }
                    }
                } else {
                    val isPrivatePlaylist = error?.contains("PLAYLIST_PRIVATE") == true
                    item(key = "error") {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isPrivatePlaylist) {
                                Image(painter = painterResource(R.drawable.anime_blank), contentDescription = null, modifier = Modifier.size(120.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = stringResource(R.string.playlist_private_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = stringResource(R.string.playlist_private_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            } else {
                                Text(text = if (error != null) stringResource(R.string.error_unknown) else stringResource(R.string.playlist_not_found), style = MaterialTheme.typography.titleLarge, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = if (error != null) error!! else stringResource(R.string.playlist_not_found_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (error != null) { Spacer(modifier = Modifier.height(16.dp)); Button(onClick = { viewModel.retry() }) { Text(stringResource(R.string.retry)) } }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (inSelectMode) { val count = selection.size; Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge) }
                else if (isSearching) {
                    TextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium) }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium, shape = CircleShape, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp).focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) Text(playlist?.title.orEmpty())
            },
            navigationIcon = {
                IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue() } else if (inSelectMode) onExitSelectionMode() else navController.navigateUp() }, onLongClick = { if (!isSearching && !inSelectMode) navController.backToMain() }) { Icon(painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), null) }
            },
            actions = {
                if (inSelectMode) {
                    val count = selection.size
                    IconButton(onClick = { if (count == filteredSongs.size) selection.clear() else { selection.clear(); selection.addAll(filteredSongs.map { it.second.id }) } }) { Icon(painterResource(if (count == filteredSongs.size) R.drawable.deselect else R.drawable.select_all), null) }
                    IconButton(onClick = { menuState.show { SelectionMediaMetadataMenu(songSelection = filteredSongs.filter { it.second.id in selection }.map { it.second.toMediaItem().metadata!! }, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode, currentItems = emptyList()) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
                }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}
