/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.models.MediaMetadata
import com.j.m3play.spotify.SpotifyMapper
import com.j.m3play.spotify.SpotifyPlaybackResolver
import com.j.m3play.spotify.SpotifyPlaylistQueue
import com.j.m3play.spotify.SpotifyPlaylistViewModel
import com.j.m3play.spotify.models.SpotifyTrack
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.EmptyPlaceholder
import com.j.m3play.ui.component.ExpressivePullToRefreshBox
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.SpotifyTrackListItem
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.resize
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

// HAZE BLUR IMPORTS
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<MediaMetadata?>(null) }
    
    val playlist = state.playlist
    val tracks = state.tracks
    val lazyListState = rememberLazyListState()
    val hazeState = rememberHazeState()
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var resolvingTrackId by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    val filteredTracks = remember(tracks, query.text) {
        if (query.text.isBlank()) tracks else tracks.filter { track ->
            track.name.contains(query.text, ignoreCase = true) ||
            track.artists.any { artist -> artist.name.contains(query.text, ignoreCase = true) } ||
            track.album?.name?.contains(query.text, ignoreCase = true) == true
        }
    }

    val loadedDurationMs = remember(tracks) { tracks.sumOf { track -> track.durationMs.toLong() } }
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

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

    val thumbnailUrl = remember(playlist) {
        playlist?.let { SpotifyMapper.getPlaylistThumbnail(it)?.resize(544, 544) }
    }

    LaunchedEffect(thumbnailUrl) {
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

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching } }

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    }

    fun playPlaylist(startIndex: Int = 0, shuffled: Boolean = false) {
        val currentPlaylist = playlist ?: return
        val queueTracks = if (shuffled) tracks.shuffled() else tracks
        if (queueTracks.isEmpty()) return
        val boundedStartIndex = startIndex.coerceIn(queueTracks.indices)
        val preloadTrack = queueTracks[boundedStartIndex]
        if (resolvingTrackId != null) return

        coroutineScope.launch {
            resolvingTrackId = preloadTrack.id
            try {
                val preloadItem = SpotifyPlaybackResolver.resolveToMetadata(preloadTrack)
                playerConnection?.playQueue(
                    SpotifyPlaylistQueue(
                        playlistId = currentPlaylist.id,
                        title = currentPlaylist.name,
                        initialTracks = queueTracks,
                        startIndex = boundedStartIndex,
                        preloadItem = preloadItem,
                    ),
                )
            } finally {
                resolvingTrackId = null
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        ExpressivePullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::reload,
            modifier = Modifier.fillMaxSize().background(animatedExtractedColor),
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

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f, key = "playlist-content"),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (isSearching) {
                    item(key = "top_inset", contentType = "header") { Spacer(modifier = Modifier.height(systemBarsTopPadding + 64.dp)) }
                }

                if (!isSearching) {
                    playlist?.let { currentPlaylist ->
                        item(key = "header") {
                            SpotifyPlaylistGlossyHeader(
                                playlist = currentPlaylist,
                                thumbnailUrl = thumbnailUrl,
                                trackCount = currentPlaylist.tracks?.total ?: tracks.size,
                                loadedDurationMs = loadedDurationMs,
                                navController = navController,
                                solidBgColor = animatedExtractedColor,
                                smartTextColor = smartTextColor,
                                smartIconColor = smartIconColor,
                                hazeState = hazeState,
                                onSearchClick = { isSearching = true },
                                onPlay = { playPlaylist() },
                                onShuffle = { playPlaylist(shuffled = true) },
                                onRefresh = { viewModel.reload() }
                            )
                        }
                    }
                }

                if (state.isLoading && tracks.isEmpty()) {
                    item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(color = smartTextColor)
                        }
                    }
                }

                state.errorMessage?.let { error ->
                    item(key = "error") {
                        Text(
                            text = error, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }

                if (!state.isLoading && state.errorMessage == null && filteredTracks.isEmpty()) {
                    item(key = "empty") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(if (query.text.isBlank()) R.string.spotify_no_tracks else R.string.ai_model_no_results),
                        )
                    }
                }

                itemsIndexed(
                    items = filteredTracks,
                    key = { index, track -> "spotify_track_${track.id}_$index" },
                    contentType = { _, _ -> "spotify_track" },
                ) { index, track ->
                    val trackIsActive = remember(track, mediaMetadata) { track.isResolvedAs(mediaMetadata) }
                    val trackIsResolving = resolvingTrackId == track.id

                    SpotifyTrackListItem(
                        track = track,
                        isActive = trackIsActive || trackIsResolving,
                        isPlaying = isPlaying && !trackIsResolving,
                        trailingContent = {
                            if (trackIsResolving) CircularWavyProgressIndicator(modifier = Modifier.size(24.dp), color = smartTextColor)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = resolvingTrackId == null || trackIsActive) {
                                if (trackIsActive) playerConnection?.player?.togglePlayPause()
                                else {
                                    val startIndex = tracks.indexOfFirst { item -> item.id == track.id }.takeIf { itemIndex -> itemIndex >= 0 } ?: index
                                    playPlaylist(startIndex = startIndex)
                                }
                            },
                    )
                }
            }

            DraggableScrollbar(
                modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd),
                scrollState = lazyListState,
                headerItems = if (!isSearching && playlist != null) 1 else 0
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
                        if (isSearching) {
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
                            Text(playlist?.name ?: stringResource(R.string.spotify_playlists), color = smartTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSearching) { isSearching = false; query = TextFieldValue() }
                                else navController.navigateUp()
                            },
                            onLongClick = { if (!isSearching) navController.backToMain() },
                        ) { Icon(painterResource(if (isSearching) R.drawable.close else R.drawable.arrow_back), null, tint = smartIconColor) }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }, onLongClick = {}) {
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
private fun SpotifyPlaylistGlossyHeader(
    playlist: com.j.m3play.spotify.models.SpotifyPlaylist,
    thumbnailUrl: String?,
    trackCount: Int,
    loadedDurationMs: Long,
    navController: NavController,
    solidBgColor: Color,
    smartTextColor: Color,
    smartIconColor: Color,
    hazeState: HazeState,
    onSearchClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight / 2)) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState, zIndex = -1f, key = "playlist-artwork")) {
                if (thumbnailUrl != null) {
                    AsyncImage(model = thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                    Text(playlist.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = smartTextColor, maxLines = 2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    playlist.owner?.displayName?.takeIf(String::isNotBlank)?.let { owner ->
                        Text(owner, style = MaterialTheme.typography.titleSmall, color = smartTextColor, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text("Playlist • Spotify", style = MaterialTheme.typography.bodyMedium, color = smartTextColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    playlist.description?.takeIf(String::isNotBlank)?.let { description ->
                        Text(description, style = MaterialTheme.typography.bodyMedium, color = smartTextColor.copy(alpha = 0.82f), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
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
                    androidx.compose.material3.IconButton(onClick = onSearchClick) { Icon(painterResource(R.drawable.search), "Search", tint = smartIconColor) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp)) 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).liquidGlass(hazeState, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onShuffle() },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.shuffle), null, tint = smartIconColor, modifier = Modifier.size(20.dp)) }

                Box(
                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(RoundedCornerShape(50)).background(smartTextColor).clickable { onPlay() }.padding(horizontal = 24.dp),
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
                            text = { Text("Refresh playlist") },
                            onClick = { showMenu = false; onRefresh() },
                            leadingIcon = { Icon(painterResource(R.drawable.sync), null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Start mix") },
                            onClick = { showMenu = false; onShuffle() },
                            leadingIcon = { Icon(painterResource(R.drawable.mix), null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            val nSongs = pluralStringResource(R.plurals.n_song, trackCount, trackCount)
            val durationText = if (loadedDurationMs > 0) makeTimeString(loadedDurationMs) else ""

            Text(text = if (durationText.isNotEmpty()) "$nSongs • $durationText" else nSongs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = smartTextColor)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun SpotifyTrack.isResolvedAs(mediaMetadata: MediaMetadata?): Boolean {
    if (mediaMetadata == null) return false
    val durationMatches = durationMs <= 0 || mediaMetadata.duration <= 0 || abs(durationMs.toLong() - mediaMetadata.duration * 1000L) <= 1_000L
    val albumMatches = album?.let { spotifyAlbum ->
        val currentAlbum = mediaMetadata.album ?: return false
        spotifyAlbum.id.isNotBlank() && spotifyAlbum.id == currentAlbum.id || spotifyAlbum.name.equals(currentAlbum.title, ignoreCase = true)
    } ?: true
    val artistMatches = artists.isEmpty() || mediaMetadata.artists.isEmpty() || artists.any { spotifyArtist ->
        mediaMetadata.artists.any { artist -> spotifyArtist.name.equals(artist.name, ignoreCase = true) }
    }
    val thumbnailMatches = SpotifyMapper.getTrackThumbnail(this)?.let { thumbnail -> thumbnail == mediaMetadata.thumbnailUrl } ?: true
    return durationMatches && albumMatches && artistMatches && thumbnailMatches
}
