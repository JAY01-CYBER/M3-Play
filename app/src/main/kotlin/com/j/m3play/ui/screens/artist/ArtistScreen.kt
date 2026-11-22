/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.j.m3play.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.ArtistEntity
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.AlbumItem
import com.j.m3play.innertube.models.ArtistItem
import com.j.m3play.innertube.models.PlaylistItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LiquidGlassIconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.liquidGlass
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.menu.YouTubeArtistMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.resize
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.ArtistViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior, 
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val systemBarsTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Base background color
    val defaultColor = Color(0xFF121212)
    var dominantColor by remember { mutableStateOf(defaultColor) }

    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    // Smart Luminance Logic
    val isLightBackground = animatedExtractedColor.luminance() > 0.5f
    val smartTextColor = if (isLightBackground) Color.Black else Color.White
    val smartIconColor = if (isLightBackground) Color.Black.copy(alpha = 0.8f) else Color.White

    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: stringResource(R.string.unknown_artist)

    LaunchedEffect(libraryArtist) { showLocal = libraryArtist?.artist?.isLocal == true }

    LaunchedEffect(thumbnail) {
        if (thumbnail != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnail)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

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
        }
    }

    val hazeState = rememberHazeState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.fillMaxSize().background(animatedExtractedColor)) {
            // HAZE SOURCE ON ROOT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState, zIndex = -2f, key = "artist-glass-backdrop")
            ) {
                Box(modifier = Modifier.fillMaxSize().background(animatedExtractedColor))
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f, key = "artist-content"),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).union(WindowInsets.ime).asPaddingValues(),
            ) {
                // 1. HERO SECTION
                item(key = "header") {
                    ArtistGlossyHeader(
                        artistName = artistName,
                        thumbnail = thumbnail,
                        isSubscribed = libraryArtist?.artist?.bookmarkedAt != null,
                        showLocal = showLocal,
                        hasSongs = librarySongs.isNotEmpty(),
                        hasRadio = artistPage?.artist?.radioEndpoint != null,
                        hazeState = hazeState,
                        solidBgColor = animatedExtractedColor,
                        smartTextColor = smartTextColor,
                        smartIconColor = smartIconColor,
                        navController = navController,
                        onPlayClick = {
                            if (!showLocal) {
                                val topSongs = artistPage?.sections?.firstOrNull { it.items.firstOrNull() is SongItem }?.items?.filterIsInstance<SongItem>()
                                if (!topSongs.isNullOrEmpty()) {
                                    playerConnection.playQueue(ListQueue(title = artistName, items = topSongs.map { it.toMediaItem() }, startIndex = 0))
                                } else {
                                    artistPage?.artist?.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                                }
                            }
                            else if (librarySongs.isNotEmpty()) {
                                playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.shuffled().map { it.toMediaItem() }))
                            }
                        },
                        onSubscribeClick = {
                            database.transaction {
                                val artist = libraryArtist?.artist
                                if (artist != null) {
                                    update(artist.toggleLike())
                                } else {
                                    artistPage?.artist?.let {
                                        insert(
                                            ArtistEntity(
                                                id = it.id, 
                                                name = it.title, 
                                                channelId = it.channelId, 
                                                thumbnailUrl = it.thumbnail
                                            ).toggleLike()
                                        )
                                    }
                                }
                            }
                        },
                        onRadioClick = {
                            artistPage?.artist?.radioEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                        }
                    )
                }

                // 2. TOP SONGS SECTION
                item(key = "top_songs_header") {
                    val songsSection = artistPage?.sections?.firstOrNull { it.items.firstOrNull() is SongItem }
                    val moreEndpoint = songsSection?.moreEndpoint
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .clickable(enabled = showLocal || moreEndpoint != null) { 
                                if (showLocal) {
                                    navController.navigate("artist/${viewModel.artistId}/songs") 
                                } else if (moreEndpoint != null) {
                                    navController.navigate("artist/${viewModel.artistId}/items?browseId=${moreEndpoint.browseId}&params=${moreEndpoint.params}")
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.songs) + " >",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = smartTextColor
                        )
                    }
                }

                if (showLocal) {
                    val filteredLibrarySongs = if (hideExplicit) librarySongs.filter { !it.song.explicit } else librarySongs
                    itemsIndexed(items = filteredLibrarySongs.take(5), key = { _, song -> song.id }) { index, song ->
                        val itemInteractionSource = remember { MutableInteractionSource() }
                        SongListItem(
                            song = song, showInLibraryIcon = true, isActive = song.id == mediaMetadata?.id, isPlaying = isPlaying,
                            trailingContent = { androidx.compose.material3.IconButton(onClick = { menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) }}) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = smartIconColor) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    indication = LocalIndication.current,
                                    interactionSource = itemInteractionSource,
                                    onClick = { 
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause() 
                                        } else {
                                            playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.map { it.toMediaItem() }, startIndex = index)) 
                                        }
                                    },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }
                                )
                        )
                    }
                } else {
                    // YTM Top Songs
                    val songsSection = artistPage?.sections?.firstOrNull { it.items.firstOrNull() is SongItem }
                    val topSongs = songsSection?.items?.filterIsInstance<SongItem>()?.distinctBy { it.id }?.take(5) ?: emptyList()
                    
                    itemsIndexed(items = topSongs, key = { _, item -> item.id }) { index, song ->
                        val itemInteractionSource = remember { MutableInteractionSource() }
                        YouTubeListItem(
                            item = song, isActive = mediaMetadata?.id == song.id, isPlaying = isPlaying,
                            trailingContent = { androidx.compose.material3.IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = smartIconColor) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    indication = LocalIndication.current,
                                    interactionSource = itemInteractionSource,
                                    onClick = { 
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause() 
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistName,
                                                    items = topSongs.map { it.toMediaItem() },
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }
                                )
                        )
                    }
                }

                // 3. OTHER SECTIONS (Albums, Singles, etc.)
                if (!showLocal) {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty() && section.items.firstOrNull() !is SongItem) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { section.moreEndpoint?.let { navController.navigate("artist/${viewModel.artistId}/items?browseId=${it.browseId}&params=${it.params}") } }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = section.title, 
                                        style = MaterialTheme.typography.titleLarge, 
                                        fontWeight = FontWeight.Bold, 
                                        color = smartTextColor,
                                        modifier = Modifier.weight(1f).basicMarquee()
                                    )
                                }
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(items = section.items.distinctBy { it.id }, key = { it.id }) { item ->
                                        val itemInteractionSource = remember { MutableInteractionSource() }
                                        YouTubeGridItem(
                                            item = item, isActive = when (item) { is SongItem -> mediaMetadata?.id == item.id; is AlbumItem -> mediaMetadata?.album?.id == item.id; else -> false },
                                            isPlaying = isPlaying, coroutineScope = coroutineScope,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    interactionSource = itemInteractionSource,
                                                    indication = LocalIndication.current,
                                                    onClick = { 
                                                        when (item) { 
                                                            is SongItem -> playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                                            is AlbumItem -> navController.navigate("album/${item.id}") 
                                                            is ArtistItem -> navController.navigate("artist/${item.id}") 
                                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}") 
                                                            else -> {}
                                                        } 
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            when (item) {
                                                                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                                else -> {}
                                                            }
                                                        }
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // FLOATING GLASSY TOP BUTTONS (Always on top without background bar)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(top = systemBarsTopPadding),
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .liquidGlass(hazeState, CircleShape)
                        .clickable { navController.navigateUp() },
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(R.drawable.arrow_back), null, tint = Color.White) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Share Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .liquidGlass(hazeState, CircleShape)
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND; type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, artistPage?.artist?.shareLink ?: "https://music.youtube.com/channel/${viewModel.artistId}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(R.drawable.share), null, tint = Color.White) }
                    
                    // More Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .liquidGlass(hazeState, CircleShape)
                            .clickable {
                                menuState.show { 
                                    YouTubeArtistMenu(
                                        artist = ArtistItem(
                                            id = viewModel.artistId, 
                                            title = artistName, 
                                            thumbnail = thumbnail ?: "",
                                            shuffleEndpoint = artistPage?.artist?.shuffleEndpoint, 
                                            radioEndpoint = artistPage?.artist?.radioEndpoint
                                        ), 
                                        onDismiss = menuState::dismiss
                                    ) 
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(R.drawable.more_vert), null, tint = Color.White) }
                }
            }

            // EXTENDED FAB FOR LIBRARY/ONLINE
            AnimatedVisibility(
                visible = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true,
                enter = scaleIn(), exit = scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(LocalPlayerAwareWindowInsets.current).padding(bottom = 16.dp, end = 16.dp)
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(if (showLocal) "Online" else "Library", style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(painterResource(if (showLocal) R.drawable.language else R.drawable.library_music), null, modifier = Modifier.size(24.dp)) },
                    onClick = { showLocal = !showLocal; if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM() },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    expanded = true
                )
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        }
    }
}

@Composable
private fun ArtistGlossyHeader(
    artistName: String,
    thumbnail: String?,
    isSubscribed: Boolean,
    showLocal: Boolean,
    hasSongs: Boolean,
    hasRadio: Boolean,
    hazeState: HazeState,
    solidBgColor: Color,
    smartTextColor: Color,
    smartIconColor: Color,
    navController: NavController,
    onPlayClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    onRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(screenHeight * 0.6f)) {
            
            // Hero Image Backdrop with perfectly smooth fade to Solid Background
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail.resize(1000, 1000), 
                    contentDescription = null, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Deep Smooth Gradient Fade 
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent, 
                            0.85f to solidBgColor.copy(alpha = 0.85f),
                            1.0f to solidBgColor
                        )
                    )
            )
            
            // Center Layout matching The Weeknd photo
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glassy Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = smartTextColor.copy(alpha = 0.15f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Icon(painterResource(R.drawable.music_note), contentDescription = null, modifier = Modifier.size(16.dp), tint = smartTextColor)
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Artist", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = smartTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Huge Name
                Text(
                    text = artistName, 
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold), 
                    color = smartTextColor, 
                    maxLines = 2, 
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Button Row (Radio -> Play -> Star)
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val radioInteractionSource = remember { MutableInteractionSource() }
                    // Info/Radio Button (Liquid Glass Haze Blur)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .liquidGlass(hazeState, CircleShape)
                            .clickable(interactionSource = radioInteractionSource, indication = LocalIndication.current, role = Role.Button, onClick = onRadioClick),
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(R.drawable.radio), null, tint = smartIconColor, modifier = Modifier.size(24.dp)) }

                    // Play Button (Large Solid Circle)
                    Box(
                        modifier = Modifier.size(76.dp).clip(CircleShape).background(smartTextColor)
                            .clickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        val invertedColor = if (smartTextColor == Color.White) Color.Black else Color.White
                        Icon(painterResource(R.drawable.play), null, tint = invertedColor, modifier = Modifier.size(36.dp))
                    }

                    val subscribeInteractionSource = remember { MutableInteractionSource() }
                    // Subscribe/Like Button (Liquid Glass Haze Blur)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .liquidGlass(hazeState, CircleShape)
                            .clickable(interactionSource = subscribeInteractionSource, indication = LocalIndication.current, role = Role.Button, onClick = onSubscribeClick),
                        contentAlignment = Alignment.Center
                    ) { 
                        Icon(painterResource(if (isSubscribed) R.drawable.favorite else R.drawable.favorite_border), null, tint = if (isSubscribed) MaterialTheme.colorScheme.error else smartIconColor, modifier = Modifier.size(24.dp)) 
                    }
                }
            }
        }
    }
}
