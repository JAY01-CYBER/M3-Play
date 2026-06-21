/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2.1   │
 * ╰────────────────────────────────────────────╯
 */

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.zIndex
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
import com.j.m3play.constants.MyTopFilter
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
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.TopPlaylistViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) { songs?.fastSumBy { it.song.duration } ?: 0 }

    val wrappedSongs = remember(songs) { songs?.map { item -> ItemWrapper(item) }?.toMutableStateList() ?: mutableStateListOf() }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    var selection by remember { mutableStateOf(false) }
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (selection) BackHandler { selection = false }

    val sortType by viewModel.topPeriod.collectAsState()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); songs?.let { addAll(it) } }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) Download.STATE_COMPLETED
            else if (songs?.all { downloads[it.song.id]?.state == Download.STATE_QUEUED || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING || downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) Download.STATE_DOWNLOADING
            else Download.STATE_STOPPED
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = { Text(stringResource(R.string.remove_download_playlist_confirm, name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter { wrapper -> val song = wrapper.item; song.song.title.contains(query.text, true) || song.artists.any { it.name.contains(query.text, true) } }
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
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                }
            }
        } else gradientColors = emptyList()
    }

    // ⭐ Header Fade Alpha to prevent overlap
    val headerAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                (1f - (lazyListState.firstVisibleItemScrollOffset / 400f)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) else 0f } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val transparentAppBar by remember { derivedStateOf { !disableBlur && !selection && !showTopBarTitle } }
    val headerItems by remember { derivedStateOf { val currentSongs = songs; if (currentSongs != null && currentSongs.isNotEmpty() && !isSearching) 2 else 0 } }
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().fillMaxSize(0.6f).align(Alignment.TopCenter).zIndex(-1f)
                    .drawBehind {
                        val headerColor = gradientColors.getOrNull(0) ?: surfaceColor
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(headerColor.copy(alpha = 0.45f * gradientAlpha), surfaceColor.copy(alpha = 0.8f * gradientAlpha), surfaceColor),
                                startY = 0f, endY = size.height
                            )
                        )
                    }
            )
        }

        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                } else {
                    if (!isSearching) {
                        item(key = "header") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = systemBarsTopPadding + 48.dp)
                                    .graphicsLayer { alpha = headerAlpha } // ⭐ Fade applied
                                    .padding(horizontal = 24.dp).padding(bottom = 16.dp)
                            ) {
                                Box(modifier = Modifier.size(240.dp).shadow(elevation = 32.dp, shape = RoundedCornerShape(12.dp), ambientColor = gradientColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary, spotColor = gradientColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary)) {
                                    AsyncImage(model = songs!!.firstOrNull()?.song?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(text = name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songs!!.size, songs!!.size))
                                    Spacer(Modifier.width(12.dp))
                                    MetadataChip(icon = R.drawable.timer, text = makeTimeString(likeLength * 1000L))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { playerConnection.playQueue(ListQueue(title = name, items = songs!!.map { it.toMediaItem() })) },
                                        shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.play), null, Modifier.size(24.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.play), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { playerConnection.playQueue(ListQueue(title = name, items = songs!!.shuffled().map { it.toMediaItem() })) },
                                        shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.shuffle), null, Modifier.size(24.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.shuffle), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> { showRemoveDownloadDialog = true }
                                                Download.STATE_DOWNLOADING -> { songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) } }
                                                else -> { songs!!.forEach { song -> val request = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.title.toByteArray()).build(); DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false) } }
                                            }
                                        }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                                else -> Icon(painterResource(R.drawable.download), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                    Surface(onClick = { playerConnection.addToQueue(items = songs!!.map { it.toMediaItem() }) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.queue_music), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // ⭐ Pill Design Sort Header
                    item(key = "sortHeader") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    SortHeader(
                                        sortType = sortType, sortDescending = false, onSortTypeChange = { viewModel.topPeriod.value = it }, onSortDescendingChange = {},
                                        sortTypeText = { sortType -> when (sortType) { MyTopFilter.ALL_TIME -> R.string.all_time; MyTopFilter.DAY -> R.string.past_24_hours; MyTopFilter.WEEK -> R.string.past_week; MyTopFilter.MONTH -> R.string.past_month; MyTopFilter.YEAR -> R.string.past_year } },
                                        modifier = Modifier, // No weight
                                        showDescending = false,
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(items = filteredSongs, key = { _, song -> song.item.id }) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item, albumIndex = index + 1, isActive = songWrapper.item.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                            trailingContent = { androidx.compose.material3.IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (!selection) { if (songWrapper.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(title = name, items = songs!!.map { it.toMediaItem() }, startIndex = songs!!.indexOfFirst { it.id == songWrapper.item.id })) } else songWrapper.isSelected = !songWrapper.isSelected },
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true },
                            )
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
                    selection -> { val count = wrappedSongs.count { it.isSelected }; Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge) }
                    isSearching -> {
                        TextField(
                            value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium) }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium, shape = CircleShape, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp).focusRequester(focusRequester)
                        )
                    }
                    showTopBarTitle -> { Text(name, style = MaterialTheme.typography.titleLarge) }
                }
            },
            navigationIcon = {
                IconButton(onClick = { when { isSearching -> { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }; selection -> selection = false; else -> navController.navigateUp() } }, onLongClick = { if (!isSearching && !selection) navController.backToMain() }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null) }
            },
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

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
