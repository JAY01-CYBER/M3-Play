/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  PREMIUM REDESIGN V2 - Anti-Cut Parallax   │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
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
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

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
            songs.mapIndexed { index, song -> index to song }.filter { (_, song) -> song.title.contains(query.text, ignoreCase = true) || song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) } }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) { BackHandler { isSearching = false; query = TextFieldValue() } } else if (selection) { BackHandler { selection = false } }

    val wrappedSongs = remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }.toMutableStateList()
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) { (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) } else 0f } }

    val headerItems by remember { derivedStateOf { val current = playlist; if (!isLoading && current != null && !isSearching) 1 else 0 } }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) { viewModel.loadMoreSongs() }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(surfaceColor).pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
    ) {
        // --- PREMIUM AMBIENT BLUR BACKGROUND ---
        if (!disableBlur) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).align(Alignment.TopCenter).zIndex(-1f)
            ) {
                AsyncImage(
                    model = playlist?.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(radiusX = 120.dp, radiusY = 120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded).graphicsLayer { alpha = gradientAlpha * 0.8f }
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.5f), surfaceColor),
                            startY = 0f, endY = with(LocalDensity.current) { 450.dp.toPx() }
                        )
                    )
                )
            }
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + AppBarHeight), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).size(240.dp).shimmer().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onSurface))
                                TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(2) { TextPlaceholder(height = 32.dp, modifier = Modifier.width(80.dp)) } }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                                    Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    Box(modifier = Modifier.size(48.dp).shimmer().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        // --- PREMIUM HEADER CONTENT ---
                        item(key = "header") {
                            // NO parallax translation Y to avoid the clipping/cut
                            val scrollOffset = if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 1000f
                            val alphaProgress = (1f - (scrollOffset / 800f)).coerceIn(0f, 1f)
                            val scaleProgress = (1f - (scrollOffset / 1200f)).coerceIn(0.85f, 1f)

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + AppBarHeight).graphicsLayer { alpha = alphaProgress },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Cover Art
                                Box(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)) {
                                    Surface(
                                        modifier = Modifier.size(260.dp).graphicsLayer { scaleX = scaleProgress; scaleY = scaleProgress }.shadow(elevation = 32.dp, shape = RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(20.dp)
                                    ) { AsyncImage(model = playlist.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                }

                                Text(text = playlist.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))

                                playlist.author?.let { artist ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary).toSpanStyle()) {
                                                if (artist.id != null) { val link = LinkAnnotation.Clickable(artist.id!!) { navController.navigate("artist/${artist.id}") }; withLink(link) { append(artist.name) } } else { append(artist.name) }
                                            }
                                        },
                                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                playlist.songCountText?.let { songCountText ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { MetadataChip(icon = R.drawable.music_note, text = songCountText) }
                                }
                                Spacer(modifier = Modifier.height(28.dp))

                                // --- PRIMARY ACTION BUTTONS (Play & Shuffle) ---
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { if (songs.isNotEmpty()) { playerConnection.playQueue(YouTubeQueue(songs.first().endpoint ?: WatchEndpoint(videoId = songs.first().id), songs.first().toMediaMetadata())) } },
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Icon(painter = painterResource(R.drawable.play), contentDescription = stringResource(R.string.play), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = { playlist.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) } },
                                        enabled = playlist.shuffleEndpoint != null,
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Icon(painter = painterResource(R.drawable.shuffle), contentDescription = stringResource(R.string.shuffle), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // --- SECONDARY ACTION BUTTONS ---
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    if (playlist.id != "LM") {
                                        Surface(
                                            onClick = {
                                                if (dbPlaylist?.playlist == null) {
                                                    database.transaction {
                                                        val playlistEntity = PlaylistEntity(name = playlist.title, browseId = playlist.id, thumbnailUrl = playlist.thumbnail, isEditable = playlist.isEditable, playEndpointParams = playlist.playEndpoint?.params, shuffleEndpointParams = playlist.shuffleEndpoint?.params, radioEndpointParams = playlist.radioEndpoint?.params).toggleLike()
                                                        insert(playlistEntity)
                                                        songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song -> PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index) }.forEach(::insert)
                                                    }
                                                } else { database.transaction { val currentPlaylist = dbPlaylist!!.playlist; update(currentPlaylist, playlist); update(currentPlaylist.toggleLike()) } }
                                            },
                                            shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)
                                        ) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) } }
                                    }

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        Surface(onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.radio), contentDescription = stringResource(R.string.radio), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) }
                                        }
                                    }

                                    val mixEndpoint = playlist.shuffleEndpoint ?: playlist.radioEndpoint
                                    if (mixEndpoint != null) {
                                        Surface(onClick = { playerConnection.playQueue(YouTubeQueue(mixEndpoint)) }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.mix), contentDescription = "Start Mix", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) }
                                        }
                                    }

                                    Surface(
                                        onClick = { menuState.show { YouTubePlaylistMenu(playlist = playlist, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss, selectAction = { selection = true }, canSelect = true, snackbarHostState = snackbarHostState) } },
                                        shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)
                                    ) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) } }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
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

                    items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                        YouTubeListItem(
                            item = song.item.second, viewCountText = viewCounts[song.item.second.id]?.let { count -> formatCompactCount(count.toLong()) }, isActive = mediaMetadata?.id == song.item.second.id, isPlaying = isPlaying, isSelected = song.isSelected && selection,
                            trailingContent = { IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song.item.second, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) } },
                            modifier = Modifier.combinedClickable(
                                enabled = !hideExplicit || !song.item.second.explicit,
                                onClick = { if (!selection) { if (song.item.second.id == mediaMetadata?.id) { playerConnection.player.togglePlayPause() } else { playerConnection.service.getAutomix(playlistId = playlist.id); playerConnection.playQueue(YouTubeQueue(song.item.second.endpoint ?: WatchEndpoint(videoId = song.item.second.id), song.item.second.toMediaMetadata())) } } else { song.isSelected = !song.isSelected } },
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) { selection = true }; wrappedSongs.forEach { it.isSelected = false }; song.isSelected = true }
                            ).animateItem()
                        )
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) { item(key = "loading_more") { ShimmerHost { repeat(2) { ListItemPlaceHolder() } } } }
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

        val appBarContainerColor by animateColorAsState(targetValue = if (showTopBarTitle || isSearching || selection) MaterialTheme.colorScheme.surface else Color.Transparent, animationSpec = tween(300), label = "appBarColor")

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarContainerColor, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                if (selection) { val count = wrappedSongs.count { it.isSelected }; Text(text = pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge) }
                else if (isSearching) { TextField(value = query, onValueChange = { query = it }, placeholder = { Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)) }
                else if (showTopBarTitle) { Text(playlist?.title.orEmpty()) }
            },
            navigationIcon = {
                IconButton(onClick = { if (isSearching) { isSearching = false; query = TextFieldValue() } else if (selection) { selection = false } else { navController.navigateUp() } }, onLongClick = { if (!isSearching && !selection) { navController.backToMain() } }) { Icon(painter = painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), contentDescription = null) }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = { if (count == wrappedSongs.size) { wrappedSongs.forEach { it.isSelected = false } } else { wrappedSongs.forEach { it.isSelected = true } } }, onLongClick = {}) { Icon(painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), contentDescription = null) }
                    IconButton(onClick = { menuState.show { SelectionMediaMetadataMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.second.toMediaItem().metadata!! }, onDismiss = menuState::dismiss, clearAction = { selection = false }, currentItems = emptyList()) } }, onLongClick = {}) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }, onLongClick = {}) { Icon(painter = painterResource(R.drawable.search), contentDescription = null) }
                }
            }
        )

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
