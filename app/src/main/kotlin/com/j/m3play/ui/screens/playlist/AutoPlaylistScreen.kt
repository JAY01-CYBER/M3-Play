package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.AutoPlaylistSongSortDescendingKey
import com.j.m3play.constants.AutoPlaylistSongSortType
import com.j.m3play.constants.AutoPlaylistSongSortTypeKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.AutoPlaylistViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist = if (viewModel.playlist == "liked") stringResource(R.string.liked) else stringResource(R.string.offline)

    val songs by viewModel.likedSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val likeLength = remember(songs) { songs?.fastSumBy { it.song.duration } ?: 0 }
    val playlistType = when (viewModel.playlist) { "liked" -> PlaylistType.LIKE; "downloaded" -> PlaylistType.DOWNLOAD; else -> PlaylistType.OTHER }

    val wrappedSongs = remember(songs) { songs?.map { item -> ItemWrapper(item) }?.toMutableStateList() ?: mutableStateListOf() }
    var selection by remember { mutableStateOf(false) }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    val (sortType, onSortTypeChange) = rememberEnumPreference(AutoPlaylistSongSortTypeKey, AutoPlaylistSongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AutoPlaylistSongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { if (playlistType == PlaylistType.LIKE) viewModel.syncLikedSongs() } }

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); songs?.let { addAll(it) } }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) Download.STATE_COMPLETED
                else if (songs?.all { downloads[it.song.id]?.state in listOf(Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_COMPLETED) } == true) Download.STATE_DOWNLOADING
                else Download.STATE_STOPPED
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = { Text(text = stringResource(R.string.remove_download_playlist_confirm, playlist), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { showRemoveDownloadDialog = false; songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) } }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { w -> w.item.song.title.contains(query.text, true) || w.item.artists.any { it.name.contains(query.text, true) } }
    }

    val lazyListState = rememberLazyListState()
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.song?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate() }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
                }
            }
        } else gradientColors = emptyList()
    }

    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) else 0f } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val transparentAppBar by remember { derivedStateOf { !disableBlur && !selection && !showTopBarTitle } }
    val headerItems by remember { derivedStateOf { if (songs != null && songs!!.isNotEmpty() && !isSearching) 2 else 0 } }
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().drawBehind {
                    val width = size.width; val height = size.height * 0.55f
                    if (gradientColors.size >= 3) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.75f), gradientColors[0].copy(alpha = gradientAlpha * 0.4f), Color.Transparent), center = Offset(width * 0.5f, height * 0.15f), radius = width * 0.8f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[1].copy(alpha = gradientAlpha * 0.55f), gradientColors[1].copy(alpha = gradientAlpha * 0.3f), Color.Transparent), center = Offset(width * 0.1f, height * 0.4f), radius = width * 0.6f))
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[2].copy(alpha = gradientAlpha * 0.5f), gradientColors[2].copy(alpha = gradientAlpha * 0.25f), Color.Transparent), center = Offset(width * 0.9f, height * 0.35f), radius = width * 0.55f))
                    } else if (gradientColors.isNotEmpty()) {
                        drawRect(brush = Brush.radialGradient(colors = listOf(gradientColors[0].copy(alpha = gradientAlpha * 0.7f), gradientColors[0].copy(alpha = gradientAlpha * 0.35f), Color.Transparent), center = Offset(width * 0.5f, height * 0.25f), radius = width * 0.85f))
                    }
                    drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = gradientAlpha * 0.22f), surfaceColor.copy(alpha = gradientAlpha * 0.55f), surfaceColor), startY = size.height * 0.35f, endY = size.height))
                }
            )
        }

        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            if (songs != null) {
                if (songs!!.isEmpty()) item { EmptyPlaceholder(R.drawable.music_note, stringResource(R.string.playlist_is_empty)) }
                else {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + 48.dp).padding(horizontal = 20.dp).padding(bottom = 16.dp)
                            ) {
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                                    Surface(
                                        modifier = Modifier.size(260.dp).shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(24.dp)
                                    ) { AsyncImage(model = songs!!.firstOrNull()?.song?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                }
                                Text(text = playlist, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "${songs!!.size} Songs • ${makeTimeString(likeLength * 1000L)}", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { playerConnection.playQueue(ListQueue(playlist, songs!!.map { it.toMediaItem() })) }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) { Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Play", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                                    OutlinedButton(onClick = { playerConnection.playQueue(ListQueue(playlist, songs!!.shuffled().map { it.toMediaItem() })) }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) { Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Shuffle", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                                                Download.STATE_DOWNLOADING -> songs!!.forEach { s -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, s.song.id, false) }
                                                else -> songs!!.forEach { s -> DownloadService.sendAddDownload(context, ExoDownloadService::class.java, DownloadRequest.Builder(s.song.id, s.song.id.toUri()).setCustomCacheKey(s.song.id).setData(s.song.title.toByteArray()).build(), false) }
                                            }
                                        },
                                        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                                else -> Icon(painterResource(R.drawable.download), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    Surface(onClick = { playerConnection.addToQueue(songs!!.map { it.toMediaItem() }) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.queue_music), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    item(key = "sortHeader") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)) {
                            SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { t -> when (t) { AutoPlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date; AutoPlaylistSongSortType.NAME -> R.string.sort_by_name; AutoPlaylistSongSortType.ARTIST -> R.string.sort_by_artist; AutoPlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time } }, modifier = Modifier.weight(1f))
                        }
                    }

                    itemsIndexed(filteredSongs, key = { _, song -> song.item.id }) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item, isActive = songWrapper.item.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true, isSelected = songWrapper.isSelected && selection,
                            trailingContent = { IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } },
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (!selection) { if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(playlist, songs!!.map { it.toMediaItem() }, songs!!.indexOfFirst { it.id == songWrapper.item.id })) } else songWrapper.isSelected = !songWrapper.isSelected },
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true } }
                            ).animateItem()
                        )
                    }
                }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                when {
                    selection -> Text(pluralStringResource(R.plurals.n_song, wrappedSongs.count { it.isSelected }, wrappedSongs.count { it.isSelected }), style = MaterialTheme.typography.titleLarge)
                    isSearching -> TextField(value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                    showTopBarTitle -> Text(playlist, style = MaterialTheme.typography.titleLarge)
                }
            },
            navigationIcon = { IconButton(onClick = { when { isSearching -> { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }; selection -> selection = false; else -> navController.navigateUp() } }, onLongClick = { if (!isSearching && !selection) navController.backToMain() }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null) } },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    androidx.compose.material3.IconButton(onClick = { if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }
                    androidx.compose.material3.IconButton(onClick = { menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item }, onDismiss = menuState::dismiss, clearAction = { selection = false }) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                } else if (!isSearching) {
                    androidx.compose.material3.IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
                }
            }
        )
    }
}

enum class PlaylistType { LIKE, DOWNLOAD, OTHER }
