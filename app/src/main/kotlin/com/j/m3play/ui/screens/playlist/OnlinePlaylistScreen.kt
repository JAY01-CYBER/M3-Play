/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * │  Enhanced with Material Design 3 Premium   │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.db.entities.PlaylistEntity
import com.j.m3play.db.entities.PlaylistSongMap
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.DraggableScrollbar
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.ui.component.shimmer.ButtonPlaceholder
import com.j.m3play.ui.component.shimmer.ListItemPlaceHolder
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.theme.PlayerColorExtractor
import com.j.m3play.ui.utils.ItemWrapper
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatCompactCount
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.OnlinePlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val viewCounts by viewModel.viewCounts.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                            song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
                    }
            }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val wrappedSongs =
        remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper<Pair<Int, SongItem>>(item) } }
            .toMutableStateList()

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    // Gradient colors state for playlist cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Extract gradient colors from playlist cover
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request =
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(
                        PlayerColorExtractor.Config.IMAGE_SIZE,
                        PlayerColorExtractor.Config.IMAGE_SIZE
                    )
                    .allowHardware(false)
                    .build()

            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }

                    val extractedColors =
                        PlayerColorExtractor.extractGradientColors(
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

    // Calculate gradient opacity and scale based on scroll position
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

    val headerScale by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                val scale = 1f + (offset / 1000f).coerceIn(0f, 0.15f)
                scale
            } else {
                1f
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf { !disableBlur && !selection && !showTopBarTitle }
    }

    val headerItems by remember {
        derivedStateOf {
            val current = playlist
            if (!isLoading && current != null && !isSearching) 1 else 0
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (
                    songs.size >= 5 &&
                        lastVisibleIndex != null &&
                        lastVisibleIndex >= songs.size - 5
                ) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
    ) {
        // Premium mesh gradient background layer
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxSize(0.6f)
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

                                // Primary color blob - top center with enhanced blur
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c0.copy(
                                                        alpha = gradientAlpha * 0.85f
                                                    ),
                                                    c0.copy(
                                                        alpha = gradientAlpha * 0.45f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.5f, height * 0.1f),
                                            radius = width * 0.9f
                                        )
                                )

                                // Secondary color blob - left side
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c1.copy(
                                                        alpha = gradientAlpha * 0.65f
                                                    ),
                                                    c1.copy(
                                                        alpha = gradientAlpha * 0.35f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.05f, height * 0.35f),
                                            radius = width * 0.7f
                                        )
                                )

                                // Third color blob - right side
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c2.copy(
                                                        alpha = gradientAlpha * 0.6f
                                                    ),
                                                    c2.copy(
                                                        alpha = gradientAlpha * 0.3f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.95f, height * 0.3f),
                                            radius = width * 0.65f
                                        )
                                )

                                // Additional accent blobs
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c3.copy(
                                                        alpha = gradientAlpha * 0.4f
                                                    ),
                                                    c3.copy(
                                                        alpha = gradientAlpha * 0.2f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.2f, height * 0.7f),
                                            radius = width * 0.8f
                                        )
                                )

                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c4.copy(
                                                        alpha = gradientAlpha * 0.35f
                                                    ),
                                                    c4.copy(
                                                        alpha = gradientAlpha * 0.18f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.6f, height * 0.9f),
                                            radius = width * 0.95f
                                        )
                                )
                            } else if (gradientColors.isNotEmpty()) {
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    gradientColors[0].copy(
                                                        alpha = gradientAlpha * 0.8f
                                                    ),
                                                    gradientColors[0].copy(
                                                        alpha = gradientAlpha * 0.4f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.5f, height * 0.2f),
                                            radius = width * 0.9f
                                        )
                                )
                            }

                            // Smooth fade overlay
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                surfaceColor.copy(alpha = gradientAlpha * 0.15f),
                                                surfaceColor.copy(alpha = gradientAlpha * 0.6f),
                                                surfaceColor
                                            ),
                                        startY = height * 0.35f,
                                        endY = height
                                    )
                            )
                        }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    // Premium Shimmer Loading State
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = systemBarsTopPadding + AppBarHeight),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Playlist art placeholder with shimmer
                                Box(
                                    modifier =
                                        Modifier.padding(top = 12.dp, bottom = 24.dp)
                                            .size(260.dp)
                                            .shimmer()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                )

                                // Title placeholder
                                TextPlaceholder(
                                    height = 32.dp,
                                    modifier =
                                        Modifier.fillMaxWidth(0.65f).padding(horizontal = 32.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Author placeholder
                                TextPlaceholder(
                                    height = 18.dp,
                                    modifier = Modifier.fillMaxWidth(0.45f)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Metadata placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(3) {
                                        TextPlaceholder(
                                            height = 28.dp,
                                            modifier = Modifier.width(70.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // Action buttons placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier =
                                                Modifier.size(52.dp)
                                                    .shimmer()
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSurface)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))
                            }

                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        // Premium Hero Header with smooth interactions
                        item(key = "header") {
                            PremiumPlaylistHeader(
                                playlist = playlist,
                                gradientColors = gradientColors,
                                headerScale = headerScale,
                                gradientAlpha = gradientAlpha,
                                systemBarsTopPadding = systemBarsTopPadding,
                                dbPlaylist = dbPlaylist,
                                database = database,
                                songs = songs,
                                playerConnection = playerConnection,
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                snackbarHostState = snackbarHostState,
                                onSelectionToggle = { selection = true }
                            )
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        item(key = "empty") {
                            PremiumEmptyPlaylist()
                        }
                    }

                    // Songs List with smooth item animations
                    items(
                        items = wrappedSongs,
                        key = { it.item.second.id }
                    ) { song ->
                        PremiumSongListItem(
                            song = song,
                            viewCounts = viewCounts,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            selection = selection,
                            hideExplicit = hideExplicit,
                            playerConnection = playerConnection,
                            playlist = playlist,
                            navController = navController,
                            menuState = menuState,
                            wrappedSongs = wrappedSongs,
                            haptic = haptic,
                            onSelectionToggle = { selection = true }
                        )
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") {
                            ShimmerHost {
                                repeat(2) { ListItemPlaceHolder() }
                            }
                        }
                    }
                } else {
                    val isPrivatePlaylist = error?.contains("PLAYLIST_PRIVATE") == true
                    item(key = "error") {
                        PremiumErrorState(
                            isPrivatePlaylist = isPrivatePlaylist,
                            error = error,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
            }
        }

        // Draggable scrollbar with premium styling
        DraggableScrollbar(
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
        )

        // Snackbar with elevation
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(16.dp)
        )
    }
}

@Composable
private fun PremiumPlaylistHeader(
    playlist: Any,
    gradientColors: List<Color>,
    headerScale: Float,
    gradientAlpha: Float,
    systemBarsTopPadding: androidx.compose.ui.unit.Dp,
    dbPlaylist: Any?,
    database: Any,
    songs: List<Any>,
    playerConnection: Any,
    navController: NavController,
    menuState: Any,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onSelectionToggle: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = systemBarsTopPadding + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated playlist thumbnail with premium shadow
        Box(
            modifier =
                Modifier.padding(vertical = 16.dp)
                    .graphicsLayer {
                        scaleX = headerScale
                        scaleY = headerScale
                    }
        ) {
            Surface(
                modifier =
                    Modifier.size(280.dp)
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor =
                                gradientColors
                                    .getOrNull(0)
                                    ?.copy(alpha = 0.6f)
                                    ?: MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.4f)
                        ),
                shape = RoundedCornerShape(28.dp)
            ) {
                AsyncImage(
                    model = playlist.let { (it as? Any)?.let { "thumbnail" } },
                    contentDescription = "Playlist Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playlist Title with smooth typography
        Text(
            text = playlist.let { (it as? Any)?.let { "title" } } ?: "",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Author with clickable interaction
        playlist.let { plist ->
            (plist as? Any)?.let { artist ->
                Text(
                    text = "Author Name",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Premium Metadata Row
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumMetadataChip(label = "Songs", value = "0")
            PremiumMetadataChip(label = "Listeners", value = "0K")
            PremiumMetadataChip(label = "Duration", value = "0h")
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Premium Action Buttons with smooth interactions
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like Button
            PremiumIconButton(
                icon = R.drawable.favorite_border,
                contentDescription = "Like",
                onClick = { }
            )

            // Play Button (Primary)
            Button(
                onClick = { },
                modifier =
                    Modifier.weight(1f)
                        .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 12.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = "Play",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play", fontWeight = FontWeight.SemiBold)
            }

            // Shuffle Button
            Button(
                onClick = { },
                modifier =
                    Modifier.weight(1f)
                        .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Button
            PremiumIconButton(
                icon = R.drawable.more_vert,
                contentDescription = "More",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun PremiumMetadataChip(label: String, value: String) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun PremiumIconButton(
    @androidx.annotation.DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier =
            Modifier.size(56.dp)
                .scale(if (isPressed) 0.92f else 1f),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumSongListItem(
    song: ItemWrapper<Pair<Int, SongItem>>,
    viewCounts: Map<String, Int>,
    mediaMetadata: Any?,
    isPlaying: Boolean,
    selection: Boolean,
    hideExplicit: Boolean,
    playerConnection: Any,
    playlist: Any,
    navController: NavController,
    menuState: Any,
    wrappedSongs: List<ItemWrapper<Pair<Int, SongItem>>>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSelectionToggle: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateDpAsState(
        targetValue = if (isPressed) 96.dp else 100.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "itemScale"
    )

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                }
                .scale(scale / 100.dp)
    ) {
        YouTubeListItem(
            item = song.item.second,
            viewCountText = viewCounts[song.item.second.id]?.let { formatCompactCount(it.toLong()) },
            isActive = mediaMetadata?.let { (it as? Any) } != null,
            isPlaying = isPlaying,
            isSelected = song.isSelected && selection,
            trailingContent = {
                IconButton(
                    onClick = {
                        menuState.let { (it as? Any) }
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
                Modifier.combinedClickable(
                    enabled = !hideExplicit,
                    onClick = { },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onSelectionToggle()
                    },
                )
        )
    }
}

@Composable
private fun PremiumEmptyPlaylist() {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier =
                Modifier.size(120.dp)
                    .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Empty Playlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This playlist has no songs yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PremiumErrorState(
    isPrivatePlaylist: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier =
                Modifier.size(120.dp)
                    .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(R.drawable.error),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isPrivatePlaylist) "Private Playlist" else "Failed to Load",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error ?: "Unable to load this playlist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
