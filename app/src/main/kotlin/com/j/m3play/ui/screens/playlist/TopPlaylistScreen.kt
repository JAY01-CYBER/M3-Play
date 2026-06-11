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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImage
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
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

    if (isSearching) { BackHandler { isSearching = false; query = TextFieldValue() } } else if (selection) { BackHandler { selection = false } }

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
            content = { Text(text = stringResource(R.string.remove_download_playlist_confirm, name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(onClick = { showRemoveDownloadDialog = false; songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) } }) { Text(text = stringResource(android.R.string.ok)) }
            },
        )
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter { wrapper -> val song = wrapper.item; song.song.title.contains(query.text, true) || song.artists.any { it.name.contains(query.text, true) } }
    }

    val lazyListState = rememberLazyListState()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientAlpha by remember { derivedStateOf { if (lazyListState.firstVisibleItemIndex == 0) { (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f) } else 0f } }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val headerItems by remember { derivedStateOf { val currentSongs = songs; if (currentSongs != null && currentSongs.isNotEmpty() && !isSearching) 2 else 0 } }
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    // Bouncy Animations Setup
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(targetValue = if (isPlayPressed) 0.92f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))

    val shuffleInteractionSource = remember { MutableInteractionSource() }
    val isShufflePressed by shuffleInteractionSource.collectIsPressedAsState()
    val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.92f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
        // --- PREMIUM AMBIENT BLUR BACKGROUND ---
        if (!disableBlur) {
            val coverUrl = songs?.firstOrNull()?.song?.thumbnailUrl
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).align(Alignment.TopCenter).zIndex(-1f)
            ) {
                AsyncImage(
                    model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
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
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                } else {
                    if (!isSearching) {
                        // --- PREMIUM HEADER CONTENT ---
                        item(key = "header") {
                            val scrollOffset = if (lazyListState.firstVisibleItemIndex == 0) lazyListState.firstVisibleItemScrollOffset.toFloat() else 1000f
                            val alphaProgress = (1f - (scrollOffset / 800f)).coerceIn(0f, 1f)
                            val scaleProgress = (1f - (scrollOffset / 1200f)).coerceIn(0.85f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(top = systemBarsTopPadding + AppBarHeight).graphicsLayer { alpha = alphaProgress }
                            ) {
                                // Cover Art
                                Box(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)) {
                                    Surface(
                                        modifier = Modifier.size(260.dp).graphicsLayer { scaleX = scaleProgress; scaleY = scaleProgress }.shadow(elevation = 32.dp, shape = RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(20.dp)
                                    ) { AsyncImage(model = songs!!.firstOrNull()?.song?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                }

                                Text(text = name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    MetadataChip(icon = R.drawable.music_note, text = pluralStringResource(R.plurals.n_song, songs!!.size, songs!!.size))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    MetadataChip(icon = R.drawable.timer, text = makeTimeString(likeLength * 1000L))
                                }
                                Spacer(modifier = Modifier.height(28.dp))

                                // --- PRIMARY ACTION BUTTONS (Play & Shuffle) ---
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { playerConnection.playQueue(ListQueue(title = name, items = songs!!.map { it.toMediaItem() })) },
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(56.dp).graphicsLayer { scaleX = playScale; scaleY = playScale }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.play), contentDescription = stringResource(R.string.play), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = { playerConnection.playQueue(ListQueue(title = name, items = songs!!.shuffled().map { it.toMediaItem() })) },
                                        shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(56.dp).graphicsLayer { scaleX = shuffleScale; scaleY = shuffleScale }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.shuffle), contentDescription = stringResource(R.string.shuffle), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // --- SECONDARY ACTION BUTTONS ---
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                                                Download.STATE_DOWNLOADING -> songs!!.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                                                else -> songs!!.forEach { song -> val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.title.toByteArray()).build(); DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false) }
                                            }
                                        },
                                        shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> Icon(painter = painterResource(R.drawable.offline), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                                else -> Icon(painter = painterResource(R.drawable.download), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    Surface(
                                        onClick = { playerConnection.addToQueue(items = songs!!.map { it.toMediaItem() }) },
                                        shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)
                                    ) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.queue_music), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) } }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    item(key = "sortHeader") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                            SortHeader(
                                sortType = sortType, sortDescending = false, onSortTypeChange = { viewModel.topPeriod.value = it }, onSortDescendingChange = {},
                                sortTypeText = { sortType -> when (sortType) { MyTopFilter.ALL_TIME -> R.string.all_time; MyTopFilter.DAY -> R.string.past_24_hours; MyTopFilter.WEEK -> R.string.past_week; MyTopFilter.MONTH -> R.string.past_month; MyTopFilter.YEAR -> R.string.past_year } },
                                modifier = Modifier.weight(1f), showDescending = false,
                            )
                        }
                    }

                    itemsIndexed(items = filteredSongs, key = { _, song -> song.item.id }) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item, albumIndex = index + 1, isActive = songWrapper.item.song.id == mediaMetadata?.id, isPlaying = isPlaying, showInLibraryIcon = true,
                            trailingContent = { androidx.compose.material3.IconButton(onClick = { menuState.show { SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) } },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (!selection) { if (songWrapper.item.song.id == mediaMetadata?.id) { playerConnection.player.togglePlayPause() } else { playerConnection.playQueue(ListQueue(title = name, items = songs!!.map { it.toMediaItem() }, startIndex = songs!!.indexOfFirst { it.id == songWrapper.item.id })) } } else { songWrapper.isSelected = !songWrapper.isSelected } },
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) { selection = true; wrappedSongs.forEach { it.isSelected = false }; songWrapper.isSelected = true } }
                            ).animateItem(fadeInSpec = tween(400), placementSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy))
                        )
                    }
                }
            }
        }

        DraggableScrollbar(modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd), scrollState = lazyListState, headerItems = headerItems)

        val appBarContainerColor by animateColorAsState(targetValue = if (showTopBarTitle || isSearching || selection) MaterialTheme.colorScheme.surface else Color.Transparent, animationSpec = tween(300), label = "appBarColor")

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarContainerColor, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            title = {
                when {
                    selection -> { val count = wrappedSongs.count { it.isSelected }; Text(text = pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge) }
                    isSearching -> { TextField(value = query, onValueChange = { query = it }, placeholder = { Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)) }
                    showTopBarTitle -> { Text(text = name, style = MaterialTheme.typography.titleLarge) }
                }
            },
            navigationIcon = {
                IconButton(onClick = { when { isSearching -> { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }; selection -> { selection = false }; else -> { navController.navigateUp() } } }, onLongClick = { if (!isSearching && !selection) navController.backToMain() }) { Icon(painter = painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), contentDescription = null) }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    androidx.compose.material3.IconButton(onClick = { if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }) { Icon(painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), contentDescription = null) }
                    androidx.compose.material3.IconButton(onClick = { menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item }, onDismiss = menuState::dismiss, clearAction = { selection = false }) } }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                } else if (!isSearching) {
                    androidx.compose.material3.IconButton(onClick = { isSearching = true }) { Icon(painter = painterResource(R.drawable.search), contentDescription = null) }
                }
            }
        )
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
