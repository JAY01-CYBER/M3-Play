/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.ArtistEntity
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.*
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.component.shimmer.*
import com.j.m3play.ui.menu.*
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.resize
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.ArtistViewModel
import kotlinx.coroutines.launch

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

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }

    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: stringResource(R.string.unknown_artist)
    
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    val headerHeight = 380.dp
    
    // Transparent AppBar check
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // --- 1. PARALLAX HEADER IMAGE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .graphicsLayer {
                    translationY = if (lazyListState.firstVisibleItemIndex == 0) {
                        -lazyListState.firstVisibleItemScrollOffset * 0.5f
                    } else {
                        -headerHeight.toPx()
                    }
                }
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail.resize(1200, 1200),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Gradient Overlay for Text Readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 300f
                        )
                    )
            )

            // Artist Title & Tags (Floating on Image)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 64.dp) // Adjusted to stay above the curve
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Placeholder verified badge
                    Icon(
                        painter = painterResource(R.drawable.done), // Replace with verified icon if you have one
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary, // Or use a static blue/red color
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Composer • Singer • Performer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // --- 2. SCROLLABLE CONTENT (OVERLAPPING SHEET) ---
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            
            // Spacer to show the image
            item {
                Spacer(modifier = Modifier.height(headerHeight - 40.dp))
            }

            // MAIN SHEET CONTENT
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(surfaceColor)
                        .padding(top = 24.dp)
                ) {
                    
                    // Center Dash indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    val description = artistPage?.description
                    if (!description.isNullOrBlank()) {
                        var isExpanded by rememberSaveable { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .combinedClickable(onClick = { isExpanded = !isExpanded }, onLongClick = {})
                        ) {
                            Text(
                                text = if (!isExpanded && description.length > 100) {
                                    description.take(100).trimEnd() + "…"
                                } else {
                                    description
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isExpanded && description.length > 100) {
                                Text(
                                    text = "More",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Row (Redesigned matching photo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val songCount = if (showLocal) librarySongs.size else artistPage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.distinctBy { it.id }?.size ?: librarySongs.size
                        val albumCount = if (showLocal) libraryAlbums.size else artistPage?.sections?.flatMap { it.items }?.filterIsInstance<AlbumItem>()?.distinctBy { it.id }?.size ?: libraryAlbums.size

                        StatItemBox(
                            icon = R.drawable.library_music, // Use music note icon
                            value = "${songCount}+",
                            label = "Songs",
                            modifier = Modifier.weight(1f)
                        )
                        StatItemBox(
                            icon = R.drawable.album, // Use album disc icon
                            value = "${albumCount}+",
                            label = "Albums",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                        
                        // Subscribe Button (Light color)
                        FilledTonalButton(
                            onClick = {
                                database.transaction {
                                    val artist = libraryArtist?.artist
                                    if (artist != null) {
                                        update(artist.toggleLike())
                                    } else {
                                        artistPage?.artist?.let {
                                            insert(ArtistEntity(id = it.id, name = it.title, channelId = it.channelId, thumbnailUrl = it.thumbnail).toggleLike())
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Icon(painterResource(if (isSubscribed) R.drawable.done else R.drawable.add), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe))
                        }

                        // Shuffle Button (Solid Color)
                        Button(
                            onClick = {
                                if (!showLocal) {
                                    artistPage?.artist?.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                                } else if (librarySongs.isNotEmpty()) {
                                    playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.shuffled().map { it.toMediaItem() }))
                                }
                            },
                            enabled = if (showLocal) librarySongs.isNotEmpty() else artistPage?.artist?.shuffleEndpoint != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Icon(painterResource(R.drawable.shuffle), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.shuffle))
                        }
                    }

                    // Radio Button
                    if (!showLocal) {
                        artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                            OutlinedButton(
                                onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .height(48.dp)
                            ) {
                                Icon(painterResource(R.drawable.radio), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.radio))
                            }
                        }
                    }
                }
            }

            // --- 3. DYNAMIC LIST ITEMS ---
            // Note: Background added to ensure image doesn't bleed through
            if (showLocal) {
                // Local Content Logic
                if (librarySongs.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.songs),
                            onClick = { navController.navigate("artist/${viewModel.artistId}/songs") },
                            modifier = Modifier.background(surfaceColor)
                        )
                    }
                    val filteredLibrarySongs = if (hideExplicit) librarySongs.filter { !it.song.explicit } else librarySongs
                    itemsIndexed(items = filteredLibrarySongs.take(5)) { index, song ->
                        SongListItem(
                            song = song,
                            showInLibraryIcon = true,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(onClick = { menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) }}) {
                                    Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceColor)
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.map { it.toMediaItem() }, startIndex = index))
                                    },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }
                                )
                        )
                    }
                }
            } else {
                // YouTube Content Logic
                artistPage?.sections?.fastForEach { section ->
                    if (section.items.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = section.title,
                                onClick = section.moreEndpoint?.let {
                                    { navController.navigate("artist/${viewModel.artistId}/items?browseId=${it.browseId}&params=${it.params}") }
                                },
                                modifier = Modifier.background(surfaceColor)
                            )
                        }
                    }

                    if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                        items(items = section.items.distinctBy { it.id }) { song ->
                            YouTubeListItem(
                                item = song as SongItem,
                                isActive = mediaMetadata?.id == song.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }) {
                                        Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .background(surfaceColor)
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                            else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
                                        },
                                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }
                                    )
                            )
                        }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.background(surfaceColor)
                            ) {
                                items(items = section.items.distinctBy { it.id }) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = when (item) {
                                            is SongItem -> mediaMetadata?.id == item.id
                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                            else -> false
                                        },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                }
                                            },
                                            onLongClick = {}
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            item { Spacer(modifier = Modifier.height(32.dp).fillMaxWidth().background(surfaceColor)) }
        }

        // --- 4. TOP APP BAR (Fades in on scroll) ---
        TopAppBar(
            title = {
                val animatedAlpha by animateFloatAsState(targetValue = if (!transparentAppBar) 1f else 0f, label = "titleAlpha")
                Text(
                    text = artistName,
                    modifier = Modifier.alpha(animatedAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = if (transparentAppBar) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(painterResource(R.drawable.share), contentDescription = null, tint = if (transparentAppBar) Color.White else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {}) {
                    Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = if (transparentAppBar) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = if (transparentAppBar) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            else TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }
}

/**
 * Redesigned Stat Box Component
 */
@Composable
private fun StatItemBox(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
