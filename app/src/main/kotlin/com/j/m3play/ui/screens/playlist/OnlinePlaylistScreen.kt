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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.withContext
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
import com.j.m3play.ui.theme.PlayerColorExtractor
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

    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            if (!isLoading && current != null && !isSearching) 1 else 0
        }
    }

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

    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) { val offset = lazyListState.firstVisibleItemScrollOffset; (1f - (offset / 600f)).coerceIn(0f, 1f) } else { 0f } } }

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
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(0.55f).align(Alignment.TopCenter).zIndex(-1f).drawBehind {
                    val width = size.width
                    val height = size.height

                    if (gradientColors.size >= 3) {
                        val c0 = gradientColors[0]; val c1 = gradientColors[1]; val c2 = gradientColors[2]
                        val c3 = gradientColors.getOrElse(3) { c0 }; val c4 = gradientColors.getOrElse(4) { c1 }
                        drawRect(brush = Brush.radialGradient(colors = listOf(c0.copy(alpha = gradientAlpha * 0.75f), c0.copy(alpha = gradientAlpha * 0.4f), Color.Transparent), center = Offset(width * 0.5f, height * 0.15f), radius = width * 0.8f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c1.copy(alpha = gradientAlpha * 0.55f), c1.copy(alpha = gradientAlpha * 0.3f), Color.Transparent), center = Offset(width * 0.1f, height * 0.4f), radius = width * 0.6f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c2.copy(alpha = gradientAlpha * 0.5f), c2.copy(alpha = gradientAlpha * 0.25f), Color.Transparent), center = Offset(width * 0.9f, height * 0.35f), radius = width * 0.55f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c3.copy(alpha = gradientAlpha * 0.35f), c3.copy(alpha = gradientAlpha * 0.18f), Color.Transparent), center = Offset(width * 0.25f, height * 0.65f), radius = width * 0.75f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(c4.copy(alpha = gradientAlpha * 0.3f), c4.copy(alpha = gradientAlpha * 0.15f), Color.Transparent), center = Offset(width * 0.55f, height * 0.85f), radius = width * 0.9f))
                    } else if (gradientColors.isNotEmpty()) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.7f), gradientColors[0].copy(alpha = gradientAlpha * 0.35f), Color.Transparent), center = Offset(width * 0.5f, height * 0.25f), radius = width * 0.85f))
                    }
                    drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = gradientAlpha * 0.22f), surfaceColor.copy(alpha = gradientAlpha * 0.55f), surfaceColor), startY = height * 0.4f, endY = height))
                }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
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
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                    AsyncImage(model = playlist.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.8f), surfaceColor), startY = 0f, endY = Float.POSITIVE_INFINITY)))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
                                    Text(text = playlist.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    playlist.author?.let { artist ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val metaText = if (playlist.songCountText != null) "Playlist • ${playlist.songCountText}" else "Playlist"
                                    Text(text = metaText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                        Surface(onClick = { playerConnection.playQueue(YouTubeQueue(shuffleEndpoint)) }, shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.size(52.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.shuffle), null) }
                                        }
                                    } ?: Spacer(modifier = Modifier.size(52.dp))

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Button(
                                        onClick = {
                                            if (isPlaylistPlaying) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playlist.playEndpoint?.let {
                                                    playerConnection.playQueue(YouTubeQueue(it))
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(52.dp).width(130.dp)
                                    ) {
                                        Icon(painterResource(if (showPause) R.drawable.pause else R.drawable.play), null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (showPause) "Pause" else "Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        Surface(onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) }, shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.size(52.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.mix), null) }
                                        }
                                    } ?: Spacer(modifier = Modifier.size(52.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "${songs.size} tracks",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                    textAlign = TextAlign.Start
                                )
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

                    items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                        val isActive = mediaMetadata?.id == song.item.second.id
                        val isSelected = song.isSelected && selection

                        YouTubeListItem(
                            item = song.item.second,
                            viewCountText = viewCounts[song.item.second.id]?.let { formatCompactCount(it.toLong()) },
                            isActive = isActive,
                            isPlaying = isPlaying,
                            isSelected = isSelected,
                            trailingContent = {
                                IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song.item.second, navController = navController, onDismiss = menuState::dismiss) } }, onLongClick = {}) {
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
                                .padding(horizontal = 8.dp, vertical = 2.dp),
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
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

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
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).background(darkOverlay, RoundedCornerShape(50))
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) { isSearching = false; query = TextFieldValue() }
                        else if (selection) { selection = false }
                        else { navController.navigateUp() }
                    },
                    onLongClick = {},
                    modifier = Modifier.padding(start = 8.dp).background(if(!isTopBarSolid) darkOverlay else Color.Transparent, CircleShape)
                ) {
                    Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = if(!isTopBarSolid) Color.White else MaterialTheme.colorScheme.onSurface)
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

        PullToRefreshDefaults.Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter))
    }
}
