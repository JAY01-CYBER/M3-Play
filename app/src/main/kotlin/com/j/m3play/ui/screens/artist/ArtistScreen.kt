/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V4.0   │
 * │  Features: Sticky Headers, Marquee, Scale  │
 * ╰────────────────────────────────────────────╯
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
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
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.DisableBlurKey
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
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.resize
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.ArtistViewModel

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

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val surfaceColor = MaterialTheme.colorScheme.surface

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = surfaceColor.toArgb()

    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: stringResource(R.string.unknown_artist)

    // ADVANCED: Dynamic Theme based on Image Palette
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val dynamicPrimary by animateColorAsState(targetValue = gradientColors.firstOrNull() ?: defaultPrimary, label = "dynamicPrimary")
    val dynamicContainer = dynamicPrimary.copy(alpha = 0.15f)
    
    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

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
                        Palette.from(bitmap).generate()
                    }
                    gradientColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
                }
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 150
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
        
        // --- 1. TOP RIGHT BACKGROUND IMAGE FADE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .align(Alignment.TopEnd)
        ) {
            AsyncImage(
                model = thumbnail?.resize(1200, 1200),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f)
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.horizontalGradient(listOf(surfaceColor, Color.Transparent), startX = 0f, endX = size.width * 0.6f))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, surfaceColor, surfaceColor), startY = size.height * 0.3f, endY = size.height))
                    }
            )
        }

        // --- 2. MAIN SCROLLABLE CONTENT ---
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 120.dp), 
            modifier = Modifier.fillMaxSize()
        ) {
            
            item { Spacer(modifier = Modifier.height(systemBarsTopPadding + 64.dp)) }

            // --- HERO SECTION ---
            item(key = "hero_section") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(0.45f).aspectRatio(1f)) {
                        AsyncImage(
                            model = thumbnail?.resize(600, 600),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape).border(6.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp)
                                .size(28.dp).clip(CircleShape).background(dynamicPrimary).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.done), contentDescription = "Verified", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(0.55f)) {
                        // ADVANCED: Marquee text for long names
                        Text(
                            text = artistName, 
                            style = MaterialTheme.typography.headlineLarge, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            maxLines = 1, 
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(percent = 50), color = dynamicContainer) {
                            Text(text = "ARTIST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = dynamicPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Composer • Singer • Live Performer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val description = artistPage?.description ?: "Official channel of $artistName."
                        var isExpanded by rememberSaveable { mutableStateOf(false) }
                        Column(modifier = Modifier.combinedClickable(onClick = { isExpanded = !isExpanded }, onLongClick = {})) {
                            Text(
                                text = if (!isExpanded && description.length > 55) description.take(55).trimEnd() + "..." else description,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
                            )
                            if (!isExpanded && description.length > 55) {
                                Text(text = stringResource(R.string.more), style = MaterialTheme.typography.labelSmall, color = dynamicPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- STATS ROW ---
            item(key = "stats_row") {
                Spacer(modifier = Modifier.height(24.dp))
                val songCount = if (showLocal) librarySongs.size else artistPage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.distinctBy { it.id }?.size ?: librarySongs.size
                val albumCount = if (showLocal) libraryAlbums.size else artistPage?.sections?.flatMap { it.items }?.filterIsInstance<AlbumItem>()?.distinctBy { it.id }?.size ?: libraryAlbums.size

                Surface(
                    shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItemCol(icon = R.drawable.library_music, value = "${songCount}+", label = "Songs", primaryColor = dynamicPrimary)
                        Divider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        StatItemCol(icon = R.drawable.album, value = "${albumCount}+", label = "Albums", primaryColor = dynamicPrimary)
                        Divider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        StatItemCol(icon = R.drawable.calendar_today, value = "25+", label = "Years", primaryColor = dynamicPrimary)
                    }
                }
            }

            // --- ACTION BUTTONS ---
            item(key = "action_buttons") {
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                    
                    // Subscribe Button (Dynamic Color)
                    Button(
                        onClick = {
                            database.transaction {
                                val artist = libraryArtist?.artist
                                if (artist != null) update(artist.toggleLike())
                                else artistPage?.artist?.let { insert(ArtistEntity(id = it.id, name = it.title, channelId = it.channelId, thumbnailUrl = it.thumbnail).toggleLike()) }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) dynamicPrimary else dynamicContainer,
                            contentColor = if (isSubscribed) Color.White else dynamicPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(painterResource(if (isSubscribed) R.drawable.done else R.drawable.add), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }

                    // Shuffle Button (Dynamic Color)
                    Button(
                        onClick = {
                            if (!showLocal) artistPage?.artist?.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                            else if (librarySongs.isNotEmpty()) playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.shuffled().map { it.toMediaItem() }))
                        },
                        enabled = if (showLocal) librarySongs.isNotEmpty() else artistPage?.artist?.shuffleEndpoint != null,
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(painterResource(R.drawable.shuffle), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.shuffle), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }

                    // Radio Button
                    val radioEndpoint = artistPage?.artist?.radioEndpoint
                    if (!showLocal && radioEndpoint != null) {
                        OutlinedButton(
                            onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(painterResource(R.drawable.radio), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.radio), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- DYNAMIC LIST ITEMS ---
            if (showLocal) {
                if (librarySongs.isNotEmpty()) {
                    // ADVANCED: Sticky Header for Songs
                    stickyHeader {
                        NavigationTitle(
                            title = stringResource(R.string.songs), onClick = { navController.navigate("artist/${viewModel.artistId}/songs") },
                            modifier = Modifier.fillMaxWidth().background(surfaceColor)
                        )
                    }
                    val filteredLibrarySongs = if (hideExplicit) librarySongs.filter { !it.song.explicit } else librarySongs
                    itemsIndexed(items = filteredLibrarySongs.take(5)) { index, song ->
                        // ADVANCED: Click Bounce Animation
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "scale")
                        
                        SongListItem(
                            song = song, showInLibraryIcon = true, isActive = song.id == mediaMetadata?.id, isPlaying = isPlaying,
                            trailingContent = { IconButton(onClick = { menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) }}) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceColor)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                    onClick = { if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(title = artistName, items = librarySongs.map { it.toMediaItem() }, startIndex = index)) },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) } }
                                )
                        )
                    }
                }
            } else {
                artistPage?.sections?.fastForEach { section ->
                    if (section.items.isNotEmpty()) {
                        // ADVANCED: Sticky Header for Sections
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceColor)
                                    .clickable { section.moreEndpoint?.let { navController.navigate("artist/${viewModel.artistId}/items?browseId=${it.browseId}&params=${it.params}") } }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = section.title, 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f).basicMarquee()
                                )
                                if (section.moreEndpoint != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                        Text(text = stringResource(R.string.view_all), style = MaterialTheme.typography.labelLarge, color = dynamicPrimary)
                                        Icon(painterResource(R.drawable.arrow_forward), contentDescription = null, tint = dynamicPrimary, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                    if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                        items(items = section.items.distinctBy { it.id }) { song ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "scale")

                            YouTubeListItem(
                                item = song as SongItem, isActive = mediaMetadata?.id == song.id, isPlaying = isPlaying,
                                trailingContent = { IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), contentDescription = null) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceColor)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = LocalIndication.current,
                                        onClick = { if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata())) },
                                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }
                                    )
                            )
                        }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().background(surfaceColor)
                            ) {
                                items(items = section.items.distinctBy { it.id }) { item ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "scale")

                                    YouTubeGridItem(
                                        item = item, isActive = when (item) { is SongItem -> mediaMetadata?.id == item.id; is AlbumItem -> mediaMetadata?.album?.id == item.id; else -> false },
                                        isPlaying = isPlaying, coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .graphicsLayer { scaleX = scale; scaleY = scale }
                                            .combinedClickable(
                                                interactionSource = interactionSource,
                                                indication = LocalIndication.current,
                                                onClick = { when (item) { is SongItem -> playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata())); is AlbumItem -> navController.navigate("album/${item.id}"); is ArtistItem -> navController.navigate("artist/${item.id}"); is PlaylistItem -> navController.navigate("online_playlist/${item.id}") } },
                                                onLongClick = {}
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp).fillMaxWidth().background(surfaceColor)) }
        }

        // --- 3. TOP APP BAR WITH COLLAPSING MINI AVATAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = systemBarsTopPadding + 12.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = navController::navigateUp,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                // ADVANCED: Mini Avatar and Marquee Name that fades in
                val topBarAlpha by animateFloatAsState(targetValue = if (transparentAppBar) 0f else 1f, tween(300), label = "alpha")
                AnimatedVisibility(visible = !transparentAppBar, enter = scaleIn(), exit = scaleOut()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp).alpha(topBarAlpha)
                    ) {
                        AsyncImage(
                            model = thumbnail?.resize(100, 100),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp).basicMarquee() // Auto scroll long names
                        )
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = {
                        viewModel.artistPage?.artist?.shareLink?.let { link ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Artist Link", link)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(painterResource(R.drawable.link), contentDescription = "Copy Link", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.artistPage?.artist?.shareLink ?: "https://music.youtube.com/channel/${viewModel.artistId}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(painterResource(R.drawable.share), contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // --- 4. PILL-SHAPED FAB ---
        AnimatedVisibility(
            visible = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            ExtendedFloatingActionButton(
                text = { 
                    Text(
                        text = if (showLocal) "Online" else "Library",
                        style = MaterialTheme.typography.labelLarge
                    ) 
                },
                icon = { 
                    Icon(
                        painter = painterResource(if (showLocal) R.drawable.language else R.drawable.library_music), 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                onClick = { 
                    showLocal = !showLocal
                    if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM() 
                },
                shape = CircleShape,
                containerColor = dynamicContainer,
                contentColor = dynamicPrimary,
                expanded = true
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).align(Alignment.BottomCenter))
    }
}

@Composable
private fun StatItemCol(
    icon: Int,
    value: String,
    label: String,
    primaryColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
