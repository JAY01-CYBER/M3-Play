/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
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
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalDownloadUtil
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.Album
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.playback.ExoDownloadService
import com.j.m3play.playback.queues.LocalAlbumRadio
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.SongListItem
import com.j.m3play.ui.component.YouTubeGridItem
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
import com.valentinilk.shimmer.shimmer

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
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        val thumbnailUrl = albumWithSongs?.album?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request)
            }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
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
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

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

    val lazyListState = rememberLazyListState()

    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            !disableBlur && !selection && !showTopBarTitle
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
    ) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.55f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height

                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            val c3 = gradientColors.getOrElse(3) { c0 }
                            val c4 = gradientColors.getOrElse(4) { c1 }
                            
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c0.copy(alpha = gradientAlpha * 0.75f),
                                        c0.copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.15f),
                                    radius = width * 0.8f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c1.copy(alpha = gradientAlpha * 0.55f),
                                        c1.copy(alpha = gradientAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.1f, height * 0.4f),
                                    radius = width * 0.6f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c2.copy(alpha = gradientAlpha * 0.5f),
                                        c2.copy(alpha = gradientAlpha * 0.25f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.9f, height * 0.35f),
                                    radius = width * 0.55f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c3.copy(alpha = gradientAlpha * 0.35f),
                                        c3.copy(alpha = gradientAlpha * 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.25f, height * 0.65f),
                                    radius = width * 0.75f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c4.copy(alpha = gradientAlpha * 0.3f),
                                        c4.copy(alpha = gradientAlpha * 0.15f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.55f, height * 0.85f),
                                    radius = width * 0.9f
                                )
                            )
                        } else if (gradientColors.isNotEmpty()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.25f),
                                    radius = width * 0.85f
                                )
                            )
                        }

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                    surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                    surfaceColor
                                ),
                                startY = height * 0.4f,
                                endY = height
                            )
                        )
                    }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            val albumWithSongs = albumWithSongs
            val hasSongs = albumWithSongs?.songs?.isNotEmpty() == true
            if (hasSongs) {
                // Hero Header
                item(key = "header") {
                    
                    // 🔴 DYNAMIC M3 EXPRESSIVE COLORS
                    val dominantColor = gradientColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary
                    val expressiveGlassColor = dominantColor.copy(alpha = 0.15f) 
                    val expressivePlayColor = dominantColor.copy(alpha = 0.25f)
                    val onExpressiveColor = MaterialTheme.colorScheme.onSurface

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = systemBarsTopPadding + AppBarHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Album Art - Bada aur soft shadow ke sath
                        Box(
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(260.dp)
                                    .shadow(
                                        elevation = 32.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.6f)
                                            ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                AsyncImage(
                                    model = albumWithSongs.album.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Album Title
                        Text(
                            text = albumWithSongs.album.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Artist Name + Verified Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        ).toSpanStyle()
                                    ) {
                                        albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                            val link = LinkAnnotation.Clickable(artist.id) {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                            withLink(link) {
                                                append(artist.name)
                                            }
                                            if (index != albumWithSongs.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Fixed: Using Compose built-in CheckCircle icon
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle, 
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Metadata Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            albumWithSongs.album.year?.let { year ->
                                MetadataChip(
                                    icon = R.drawable.calendar_today,
                                    text = year.toString(),
                                    backgroundColor = expressiveGlassColor,
                                    contentColor = onExpressiveColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            MetadataChip(
                                icon = R.drawable.music_note,
                                text = pluralStringResource(R.plurals.n_song, wrappedSongs.size, wrappedSongs.size),
                                backgroundColor = expressiveGlassColor,
                                contentColor = onExpressiveColor
                            )

                            val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                            if (totalDuration > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                MetadataChip(
                                    icon = R.drawable.timer,
                                    text = makeTimeString(totalDuration * 1000L),
                                    backgroundColor = expressiveGlassColor,
                                    contentColor = onExpressiveColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 🔴 EXPRESSIVE ACTION BUTTONS ROW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Like Button
                            ActionButtonCircular(
                                icon = if (albumWithSongs.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border,
                                backgroundColor = expressiveGlassColor,
                                tint = if (albumWithSongs.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else onExpressiveColor,
                                onClick = {
                                    database.query { update(albumWithSongs.album.toggleLike()) }
                                }
                            )

                            // Play Button Pill
                            Surface(
                                onClick = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                                },
                                shape = RoundedCornerShape(50),
                                color = expressivePlayColor,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = stringResource(R.string.play),
                                        tint = onExpressiveColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.play),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = onExpressiveColor
                                    )
                                }
                            }

                            // Shuffle Button
                            ActionButtonCircular(
                                icon = R.drawable.shuffle,
                                backgroundColor = expressiveGlassColor,
                                tint = onExpressiveColor,
                                onClick = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())))
                                }
                            )

                            // Download Button (Maintains the CircularProgressIndicator logic)
                            Surface(
                                onClick = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                            albumWithSongs.songs.forEach { song ->
                                                DownloadService.sendRemoveDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    song.id,
                                                    false,
                                                )
                                            }
                                        }
                                        else -> {
                                            albumWithSongs.songs.forEach { song ->
                                                val downloadRequest =
                                                    DownloadRequest
                                                        .Builder(song.id, song.id.toUri())
                                                        .setCustomCacheKey(song.id)
                                                        .setData(song.song.title.toByteArray())
                                                        .build()
                                                DownloadService.sendAddDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    downloadRequest,
                                                    false,
                                                )
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = expressiveGlassColor,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Download.STATE_DOWNLOADING -> {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp),
                                                color = onExpressiveColor
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                                tint = onExpressiveColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 🔴 EXPRESSIVE "About this album" Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = expressiveGlassColor,
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = Album(albumWithSongs.album, albumWithSongs.artists),
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = dominantColor.copy(alpha = 0.25f), 
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Fixed: Using Compose built-in Star icon
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = onExpressiveColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "About this album",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = onExpressiveColor
                                    )
                                    Text(
                                        text = "A powerful statement of truth, ambition and self-belief.", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onExpressiveColor.copy(alpha = 0.7f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Fixed: Using Compose built-in KeyboardArrowRight icon
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = onExpressiveColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 🔴 Songs Header with "View All"
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
                            fontWeight = FontWeight.Bold
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = { /* Handle view all if needed */ }
                        ) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Songs List
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
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = songWrapper.item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                                onLongClick = {}
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
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
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs, startIndex = index),
                                            )
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
                            ),
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
                                        .padding(top = systemBarsTopPadding + AppBarHeight),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp, bottom = 20.dp)
                                            .size(240.dp)
                                            .shimmer()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )

                                    TextPlaceholder(
                                        height = 28.dp,
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .padding(horizontal = 32.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextPlaceholder(
                                        height = 20.dp,
                                        modifier = Modifier.fillMaxWidth(0.4f)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(3) {
                                            TextPlaceholder(
                                                height = 32.dp,
                                                modifier = Modifier.width(70.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                        )
                                        ButtonPlaceholder(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        )
                                        ButtonPlaceholder(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                        )
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
                                    .padding(top = systemBarsTopPadding + AppBarHeight)
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
                                    .padding(top = systemBarsTopPadding + AppBarHeight)
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

        val topAppBarColors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        } else {
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        }

        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            colors = topAppBarColors,
            scrollBehavior = scrollBehavior,
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (showTopBarTitle) {
                    Text(
                        text = albumWithSongs?.album?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
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
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false }
                                )
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionButtonCircular(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
