/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.SongSortDescendingKey
import com.j.m3play.constants.SongSortType
import com.j.m3play.constants.SongSortTypeKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.SortHeader
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime

import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val wrappedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sortedSongs = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song ->
                song.artists.joinToString(separator = "") { artist -> artist.name }
            }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }.let { if (sortDescending) it.reversed() else it }

        sortedSongs.map { song -> ItemWrapper(song) }
    }.toMutableStateList()

    var selection by remember { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    val hazeState = rememberHazeState()

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { wrapper ->
            val song = wrapper.item
            song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
        }
    }

    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }

    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    // SMART COLOR LOGIC BASED ON LUMINANCE
    val isLightBackground = animatedExtractedColor.luminance() > 0.5f
    val smartTextColor = if (isLightBackground) Color.Black else Color.White
    val smartIconColor = if (isLightBackground) Color.Black.copy(alpha = 0.8f) else Color.White

    LaunchedEffect(cachedSongs) {
        val thumbnailUrl = cachedSongs.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context).data(thumbnailUrl).size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE).allowHardware(false).build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate()
                    }
                    val extractedColors = PlayerColorExtractor.extractGradientColors(palette, defaultColor.toArgb())
                    dominantColor = extractedColors.firstOrNull() ?: defaultColor
                }
            }
        } else dominantColor = defaultColor
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching || selection } }
    val headerItems by remember { derivedStateOf { if (filteredSongs.isNotEmpty() && !isSearching && !selection) 2 else 0 } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.fillMaxSize().background(animatedExtractedColor)) {
            // FULL-SCREEN HAZE BACKDROP
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState, zIndex = -2f, key = "playlist-glass-backdrop")
            ) {
                Box(modifier = Modifier.fillMaxSize().background(animatedExtractedColor))
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.40f to animatedExtractedColor.copy(alpha = 0.12f),
                                0.58f to animatedExtractedColor.copy(alpha = 0.62f),
                                0.82f to animatedExtractedColor.copy(alpha = 0.94f),
                                1.0f to animatedExtractedColor
                            )
                        )
                    )
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f, key = "playlist-content"),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (isSearching || selection) {
                    item(key = "search_spacer", contentType = "header") { Spacer(modifier = Modifier.height(topPadding)) }
                }

                if (filteredSongs.isEmpty() && !isSearching) {
                    item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
                } else if (filteredSongs.isEmpty() && isSearching) {
                    item { EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found)) }
                } else {
                    if (!isSearching && !selection) {
                        item(key = "header") {
                            CachePlaylistGlossyHeader(
                                title = stringResource(R.string.cached_playlist),
                                artwork = filteredSongs.firstOrNull()?.item?.thumbnailUrl,
                                songs = filteredSongs.map { it.item },
                                navController = navController,
                                playerConnection = playerConnection,
                                solidBgColor = animatedExtractedColor,
                                smartTextColor = smartTextColor,
                                smartIconColor = smartIconColor,
                                hazeState = hazeState,
                                onSearchClick = { isSearching = true }
                            )
                        }
                        item(key = "sortHeader") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME -> R.string.sort_by_name
                                            SongSortType.ARTIST -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    itemsIndexed(items = filteredSongs, key = { _, songWrapper -> songWrapper.item.id }) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            isSelected = songWrapper.isSelected && selection,
                            showInLibraryIcon = true,
                            trailingContent = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                                isFromCache = true,
                                            )
                                        }
                                    },
                                ) { Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = "Cache Songs",
                                                        items = cachedSongs.map { it.toMediaItem() },
                                                        startIndex = cachedSongs.indexOfFirst { it.id == songWrapper.item.id }
                                                    )
                                                )
                                            }
                                        } else songWrapper.isSelected = !songWrapper.isSelected
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            songWrapper.isSelected = true
                                        }
                                    },
                                )
                                .animateItem()
                        )
                    }
                }
            }

            DraggableScrollbar(
                modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd),
                scrollState = lazyListState,
                headerItems = headerItems
            )

            AnimatedVisibility(
                visible = showTopBarTitle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    modifier = Modifier.hazeBlur(
                        input = HazeInput.Backdrop(hazeState),
                        style = HazeBlurStyle.Material3(containerColor = animatedExtractedColor.copy(alpha = 0.52f)) {
                            blurRadius(48.dp)
                            noiseFactor(0.16f)
                            colorEffects(listOf(HazeColorEffect.tint(animatedExtractedColor.copy(alpha = 0.48f))))
                        }
                    ).fillMaxWidth(),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    title = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            Text(pluralStringResource(R.plurals.n_song, count, count), color = smartTextColor)
                        } else if (isSearching) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = smartTextColor.copy(alpha = 0.7f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = smartTextColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent, cursorColor = smartTextColor
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        } else {
                            Text(stringResource(R.string.cached_playlist), color = smartTextColor)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSearching) { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                                else if (selection) selection = false
                                else navController.navigateUp()
                            },
                            onLongClick = { if (!isSearching && !selection) navController.backToMain() },
                        ) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = smartIconColor) }
                    },
                    actions = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                                    else wrappedSongs.forEach { it.isSelected = true }
                                }
                            ) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = smartIconColor) }
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false }
                                        )
                                    }
                                }
                            ) { Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor) }
                        } else if (!isSearching) {
                            androidx.compose.material3.IconButton(onClick = { isSearching = true }) {
                                Icon(painterResource(R.drawable.search), null, tint = smartIconColor)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CachePlaylistGlossyHeader(
    title: String,
    artwork: String?,
    songs: List<Song>,
    navController: NavController,
    playerConnection: com.j.m3play.playback.PlayerConnection,
    solidBgColor: Color,
    smartTextColor: Color,
    smartIconColor: Color,
    hazeState: HazeState,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight / 2)) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState, zIndex = -1f, key = "playlist-artwork")) {
                if (artwork != null) {
                    AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(screenHeight * 0.35f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(0.0f to Color.Transparent, 0.4f to solidBgColor.copy(alpha = 0.4f), 0.8f to solidBgColor.copy(alpha = 0.9f), 1.0f to solidBgColor))
                )
                
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = smartTextColor, maxLines = 2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = smartTextColor, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Offline Cache • 2026", style = MaterialTheme.typography.bodyMedium, color = smartTextColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(hazeState = hazeState, painter = painterResource(R.drawable.arrow_back), modifier = Modifier.size(48.dp), onClick = { navController.navigateUp() })
                Row(modifier = Modifier.height(48.dp).liquidGlass(hazeState, RoundedCornerShape(24.dp)).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = onSearchClick) { Icon(painterResource(R.drawable.search), "Search", tint = smartIconColor) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp)) 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            playerConnection.playQueue(ListQueue(title = title, items = songs.shuffled().map { it.toMediaItem() }))
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.shuffle), null, tint = smartIconColor, modifier = Modifier.size(20.dp)) }

                Box(
                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(RoundedCornerShape(50)).background(smartTextColor).clickable {
                        playerConnection.playQueue(ListQueue(title = title, items = songs.map { it.toMediaItem() }))
                    }.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        val invertedColor = if (smartTextColor == Color.White) Color.Black else Color.White
                        Icon(painterResource(R.drawable.play), null, tint = invertedColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = invertedColor)
                    }
                }

                var showMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.more_vert), null, tint = smartIconColor, modifier = Modifier.size(20.dp))
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add to queue") },
                            onClick = { showMenu = false; playerConnection.addToQueue(items = songs.map { it.toMediaItem() }) },
                            leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            val nSongs = pluralStringResource(R.plurals.n_song, songs.size, songs.size)
            Text(text = nSongs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = smartTextColor)
            Spacer(Modifier.height(8.dp))
        }
    }
}
