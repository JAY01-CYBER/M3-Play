/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
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
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.AlbumWithSongs
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.LocalAlbumRadio
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.component.shimmer.ButtonPlaceholder
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.AlbumMenu
import com.j.m3play.ui.menu.SelectionSongMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.AlbumUiState
import com.j.m3play.viewmodels.AlbumViewModel

// HAZE BLUR IMPORTS
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
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }

    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    // SMART COLOR LOGIC BASED ON LUMINANCE
    val isLightBackground = animatedExtractedColor.luminance() > 0.5f
    val smartTextColor = if (isLightBackground) Color.Black else Color.White
    val smartIconColor = if (isLightBackground) Color.Black.copy(alpha = 0.8f) else Color.White

    LaunchedEffect(albumWithSongs) {
        val thumbnailUrl = albumWithSongs?.album?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                .allowHardware(false)
                .build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA).generate()
                    }
                    val extractedColors = PlayerColorExtractor.extractGradientColors(palette, surfaceColor.toArgb())
                    dominantColor = extractedColors.firstOrNull() ?: surfaceColor
                }
            }
        } else dominantColor = surfaceColor
    }

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }

    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = { Text(text = stringResource(R.string.remove_download_playlist_confirm, albumWithSongs?.album?.title ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        albumWithSongs?.songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                        }
                    }
                ) { Text(text = stringResource(android.R.string.ok)) }
            },
        )
    }

    val lazyListState = rememberLazyListState()
    val hazeState = rememberHazeState()

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || selection } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedExtractedColor), 
        ) {
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

            val localAlbumWithSongs = albumWithSongs

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f, key = "playlist-content"),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (selection) {
                    item(key = "top_inset") {
                        Spacer(modifier = Modifier.height(topPadding))
                    }
                }
                val hasSongs = localAlbumWithSongs?.songs?.isNotEmpty() == true
                
                if (hasSongs && localAlbumWithSongs != null) {
                    if (!selection) {
                        item(key = "header") {
                            AlbumGlossyHeader(
                                albumWithSongs = localAlbumWithSongs,
                                wrappedSongsSize = wrappedSongs.size,
                                navController = navController,
                                playerConnection = playerConnection,
                                solidBgColor = animatedExtractedColor,
                                smartTextColor = smartTextColor,
                                smartIconColor = smartIconColor,
                                hazeState = hazeState,
                                downloadState = downloadState,
                                onRemoveDownloadRequest = { showRemoveDownloadDialog = true },
                                menuState = menuState
                            )
                        }
                    }

                    item(key = "songs_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.songs),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = smartTextColor
                            )
                        }
                    }

                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            albumIndex = index + 1,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                        tint = smartIconColor
                                    )
                                }
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                localAlbumWithSongs?.let { album ->
                                                    playerConnection.playQueue(
                                                        LocalAlbumRadio(album, startIndex = index),
                                                    )
                                                }
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    },
                                )
                                .animateItem(),
                        )
                    }

                    if (otherVersions.isNotEmpty()) {
                        item(key = "other_versions_header") {
                            NavigationTitle(
                                title = stringResource(R.string.other_versions),
                            )
                        }
                        item(key = "other_versions_list") {
                            LazyRow {
                                items(
                                    items = otherVersions.distinctBy { it.id },
                                    key = { it.id },
                                ) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = mediaMetadata?.album?.id == item.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = scope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (val state = uiState) {
                        AlbumUiState.Loading,
                        AlbumUiState.Content -> {
                            item(key = "shimmer") {
                                ShimmerHost {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = AppBarHeight + 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(160.dp)
                                                    .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
                                            )
                                            
                                            Spacer(modifier = Modifier.width(16.dp))
                                            
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.4f))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.9f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.6f))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    TextPlaceholder(height = 24.dp, modifier = Modifier.width(70.dp))
                                                    TextPlaceholder(height = 24.dp, modifier = Modifier.width(70.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            ButtonPlaceholder(modifier = Modifier.weight(1.2f).height(52.dp))
                                            ButtonPlaceholder(modifier = Modifier.weight(1f).height(52.dp))
                                            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    repeat(6) {
                                        ListItemPlaceHolder()
                                    }
                                }
                            }
                        }

                        AlbumUiState.Empty -> {
                            item(key = "empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = AppBarHeight + 12.dp)
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_album),
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.empty_album_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        is AlbumUiState.Error -> {
                            item(key = "error") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = AppBarHeight + 12.dp)
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (state.isNotFound) stringResource(R.string.album_not_found) else stringResource(R.string.error_unknown),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (state.isNotFound) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (state.isNotFound) stringResource(R.string.album_not_found_desc) else stringResource(R.string.error_unknown),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.retry() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showTopBarTitle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    modifier = Modifier
                        .hazeBlur(
                            input = HazeInput.Backdrop(hazeState),
                            style = HazeBlurStyle.Material3(
                                containerColor = animatedExtractedColor.copy(alpha = 0.52f)
                            ) {
                                blurRadius(48.dp)
                                noiseFactor(0.16f)
                                colorEffects(listOf(HazeColorEffect.tint(animatedExtractedColor.copy(alpha = 0.48f))))
                            }
                        )
                        .fillMaxWidth(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, 
                        scrolledContainerColor = Color.Transparent
                    ),
                    title = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            Text(
                                text = pluralStringResource(R.plurals.n_song, count, count),
                                style = MaterialTheme.typography.titleLarge,
                                color = smartTextColor
                            )
                        } else {
                            Text(
                                text = localAlbumWithSongs?.album?.title.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                color = smartTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (selection) {
                                    selection = false
                                } else {
                                    navController.navigateUp()
                                }
                            },
                            onLongClick = {
                                if (!selection) {
                                    navController.backToMain()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (selection) R.drawable.close else R.drawable.arrow_back),
                                contentDescription = null,
                                tint = smartIconColor
                            )
                        }
                    },
                    actions = {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (count == wrappedSongs.size) {
                                        wrappedSongs.forEach { it.isSelected = false }
                                    } else {
                                        wrappedSongs.forEach { it.isSelected = true }
                                    }
                                },
                            ) {
                                Icon(painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), contentDescription = null, tint = smartIconColor)
                            }

                            androidx.compose.material3.IconButton(
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false }
                                        )
                                    }
                                },
                            ) {
                                Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = smartIconColor)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.IconButton(
                                    onClick = { 
                                        localAlbumWithSongs?.let { current -> 
                                            database.query { update(current.album.toggleLike()) } 
                                        } 
                                    },
                                ) {
                                    val isBookmarked = localAlbumWithSongs?.album?.bookmarkedAt != null
                                    Icon(
                                        painter = painterResource(if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border),
                                        contentDescription = null,
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.error else smartIconColor
                                    )
                                }
                                
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        localAlbumWithSongs?.let { current ->
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = Album(current.album, current.artists),
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                        tint = smartIconColor
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumGlossyHeader(
    albumWithSongs: AlbumWithSongs,
    wrappedSongsSize: Int,
    navController: NavController,
    playerConnection: com.j.m3play.playback.PlayerConnection,
    solidBgColor: Color,
    smartTextColor: Color,
    smartIconColor: Color,
    hazeState: HazeState,
    downloadState: Int,
    onRemoveDownloadRequest: () -> Unit,
    menuState: com.j.m3play.ui.component.MenuState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight / 2)) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState, zIndex = -1f, key = "playlist-artwork")) {
                AsyncImage(model = albumWithSongs.album.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(screenHeight * 0.35f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(0.0f to Color.Transparent, 0.4f to solidBgColor.copy(alpha = 0.4f), 0.8f to solidBgColor.copy(alpha = 0.9f), 1.0f to solidBgColor))
                )
                
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(albumWithSongs.album.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = smartTextColor, maxLines = 2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = MaterialTheme.typography.titleSmall.copy(color = smartTextColor).toSpanStyle()) {
                                    albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                        val link = LinkAnnotation.Clickable(artist.id) { navController.navigate("artist/${artist.id}") }
                                        withLink(link) { append(artist.name) }
                                        if (index != albumWithSongs.artists.lastIndex) append(", ")
                                    }
                                }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Verified", tint = smartTextColor, modifier = Modifier.size(16.dp))
                    }

                    albumWithSongs.album.year?.let { year ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Album • $year", style = MaterialTheme.typography.bodyMedium, color = smartTextColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(hazeState = hazeState, painter = painterResource(R.drawable.arrow_back), modifier = Modifier.size(48.dp), onClick = { navController.navigateUp() })
                Row(modifier = Modifier.height(48.dp).liquidGlass(hazeState, RoundedCornerShape(24.dp)).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isBookmarked = albumWithSongs.album.bookmarkedAt != null
                    androidx.compose.material3.IconButton(onClick = { database.query { update(albumWithSongs.album.toggleLike()) } }) { 
                        Icon(painterResource(if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border), "Like", tint = if (isBookmarked) MaterialTheme.colorScheme.error else smartIconColor) 
                    }
                    androidx.compose.material3.IconButton(onClick = {
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = Album(albumWithSongs.album, albumWithSongs.artists),
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }) { 
                        Icon(painterResource(R.drawable.more_vert), "More", tint = smartIconColor) 
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp)) 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            playerConnection.playQueue(LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())))
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.shuffle), null, tint = smartIconColor, modifier = Modifier.size(20.dp)) }

                Box(
                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(RoundedCornerShape(50)).background(smartTextColor).clickable {
                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
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
                            onClick = { showMenu = false; playerConnection.addToQueue(items = albumWithSongs.songs.map { it.toMediaItem() }) },
                            leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = {
                                showMenu = false
                                when (downloadState) {
                                    Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> onRemoveDownloadRequest()
                                    else -> albumWithSongs.songs.forEach { song ->
                                        val request = DownloadRequest.Builder(song.id, song.id.toUri()).setCustomCacheKey(song.id).setData(song.song.title.toByteArray()).build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false)
                                    }
                                }
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.download), null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            val nSongs = pluralStringResource(R.plurals.n_song, wrappedSongsSize, wrappedSongsSize)
            val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
            val durationText = if (totalDuration > 0) makeTimeString(totalDuration * 1000L) else ""

            Text(text = if (durationText.isNotEmpty()) "$nSongs • $durationText" else nSongs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = smartTextColor)
            Spacer(Modifier.height(8.dp))
        }
    }
}
