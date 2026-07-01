/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Glossy Premium Home Screen Integration    │
 * │  Signature: M3PLAY::UI::GLOSSY_EXPRESSIVE  │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.j.m3play.LocalAnimatedVisibilityScope
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.LocalSharedTransitionScope
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.ShowHomeCategoryChipsKey
import com.j.m3play.db.entities.Song
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.ExpressivePullToRefreshBox
import com.j.m3play.ui.component.LocalBottomSheetPageState
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.TimeGreetingCard
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.menu.YouTubePlaylistMenu
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CommunityPlaylistItem
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GlossyCarouselCard(
    song: Song,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = { menuState.dismiss() }) }
                },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            var imageModifier: Modifier = Modifier.fillMaxSize()
            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    imageModifier = imageModifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "image_${song.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 300f) }
                    )
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.song.thumbnailUrl?.replace(Regex("w\\d+-h\\d+"), "w544-h544"))
                    .build(), 
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.85f)),
                        )
                    )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom, 
                ) {
                    Text(text = "Based on your history", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(bottom = 2.dp))
                    Text(text = song.song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  
    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val metroSpeedDialItems by viewModel.metroSpeedDialItems.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState(initial = emptyList())
    val homePage by viewModel.homePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) { BackHandler { viewModel.toggleChip(selectedChip) } }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) { viewModel.toggleChip(selectedChip) }
    }

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxSize()) {
        
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.75f) 
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .graphicsLayer() 
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(colors = listOf(color1.copy(alpha = 0.35f), color1.copy(alpha = 0.20f), color1.copy(alpha = 0.10f), Color.Transparent), center = Offset(width * 0.15f, height * 0.1f), radius = width * 0.60f)
                        val brush2 = Brush.radialGradient(colors = listOf(color2.copy(alpha = 0.30f), color2.copy(alpha = 0.18f), color2.copy(alpha = 0.08f), Color.Transparent), center = Offset(width * 0.85f, height * 0.2f), radius = width * 0.70f)
                        val brush3 = Brush.radialGradient(colors = listOf(color3.copy(alpha = 0.25f), color3.copy(alpha = 0.15f), color3.copy(alpha = 0.05f), Color.Transparent), center = Offset(width * 0.3f, height * 0.45f), radius = width * 0.65f)
                        val brush4 = Brush.radialGradient(colors = listOf(color4.copy(alpha = 0.20f), color4.copy(alpha = 0.12f), color4.copy(alpha = 0.04f), Color.Transparent), center = Offset(width * 0.7f, height * 0.5f), radius = width * 0.75f)
                        val brush5 = Brush.radialGradient(colors = listOf(color5.copy(alpha = 0.18f), color5.copy(alpha = 0.10f), color5.copy(alpha = 0.03f), Color.Transparent), center = Offset(width * 0.5f, height * 0.75f), radius = width * 0.85f)

                        val overlayBrush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.35f), surfaceColor.copy(alpha = 0.75f), surfaceColor),
                            startY = height * 0.35f, endY = height
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = brush3)
                            drawRect(brush = brush4)
                            drawRect(brush = brush5)
                            drawRect(brush = overlayBrush)
                        }
                    }
            ) {}
        }

        ExpressivePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazylistState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    if (showHomeCategoryChips) {
                        item(key = "chips", contentType = "chips") {
                            ChipsRow(chips = homePage?.chips.orEmpty().map { it to it.title }, currentValue = selectedChip, onValueUpdate = { viewModel.toggleChip(it) })
                        }
                    }

                    item(key = "greeting", contentType = "greeting") { 
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        
                        TimeGreetingCard(
                            onMixClick = { 
                                // YouTube Music Time-based Mix Endpoints
                                val endpoint = when (currentHour) {
                                    in 4..11 -> WatchEndpoint(playlistId = "RDCLAK5uy_mVKCQKuK8r-0E97QYt9y2DXZm4Xm6X6J8") // Morning Mix (Upbeat)
                                    in 12..16 -> WatchEndpoint(playlistId = "RDCLAK5uy_l4bFnb1uQ9gQh1P-rW6Yn8U9Cj9c2Q1_Y") // Afternoon Mix (Pop/Energy)
                                    in 17..20 -> WatchEndpoint(playlistId = "RDCLAK5uy_n7Fq_XU2W-B6q8A9j3H0Z_Y3a3_x9_M8U") // Evening Mix (Chill/Acoustic)
                                    else -> WatchEndpoint(playlistId = "RDCLAK5uy_kP3P_XU2W-B6q8A9j3H0Z_Y3a3_x9_M8U") // Night/Late Night Mix (Lofi/Sleep)
                                }

                                try {
                                    // Play the native YouTube Time-Based Mix
                                    playerConnection.playQueue(YouTubeQueue(endpoint))
                                } catch (e: Exception) {
                                    // Fallback if playlist fails: Play a radio from Quick Picks 
                                    quickPicks?.firstOrNull()?.let { firstPick ->
                                        playerConnection.playQueue(YouTubeQueue.radio(firstPick.toMediaMetadata()))
                                    }
                                }
                            }
                        ) 
                    }
                    
                    item(key = "actions_1", contentType = "actions") {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionCard(title = "Liked", icon = R.drawable.favorite, onClick = { runCatching { navController.navigate("auto_playlist/liked") } }, modifier = Modifier.weight(1f))
                            ActionCard(title = "Downloads", icon = R.drawable.download, onClick = { runCatching { navController.navigate("auto_playlist/downloaded") } }, modifier = Modifier.weight(1f))
                        }
                    }

                    item(key = "actions_2", contentType = "actions") {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionCard(title = "History", icon = R.drawable.history, onClick = { runCatching { navController.navigate("history") } }, modifier = Modifier.weight(1f))
                            ActionCard(title = if (isLoggedIn) "Account" else "Library", icon = if (isLoggedIn) R.drawable.person else R.drawable.library_music, onClick = { if (isLoggedIn) runCatching { navController.navigate("account") } else runCatching { navController.navigate("library") } }, modifier = Modifier.weight(1f))
                        }
                    }
                    
                    communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                        item(key = "community_title", contentType = "title") { 
                            NavigationTitle(title = stringResource(R.string.from_the_community), modifier = Modifier.animateItem()) 
                        }
                        item(key = "community_row", contentType = "row") {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.animateItem()) {
                                items(items = playlists, key = { it.playlist.id }, contentType = { "community_card" }) { item ->
                                    CommunityPlaylistCard(item = item, onClick = { navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}") }, onSongClick = { song: SongItem -> playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())) }, onMenuClick = { song: SongItem -> menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }, onSaveClick = { menuState.show { YouTubePlaylistMenu(playlist = item.playlist, coroutineScope = scope, onDismiss = menuState::dismiss) } })
                                }
                            }
                        }
                    }

                    quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                        item(key = "quick_picks_title", contentType = "title") { 
                            NavigationTitle(title = stringResource(R.string.quick_picks), modifier = Modifier.animateItem()) 
                        }
                        item(key = "quick_picks_carousel", contentType = "carousel") {
                            Box(modifier = Modifier.fillMaxWidth().height(290.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                val carouselState = rememberCarouselState { minOf(picks.size, 10) }
                                HorizontalMultiBrowseCarousel(state = carouselState, preferredItemWidth = 280.dp, itemSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(290.dp)) { i ->
                                    GlossyCarouselCard(song = picks[i], onClick = { playerConnection.playQueue(YouTubeQueue.radio(picks[i].toMediaMetadata())) }, navController = navController, modifier = Modifier.maskClip(RoundedCornerShape(24.dp)))
                                }
                            }
                        }
                    }

                    metroSpeedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                        item(key = "metro_speed_dial_title", contentType = "title") { NavigationTitle(title = stringResource(R.string.speed_dial), modifier = Modifier.animateItem()) }
                        item(key = "metro_speed_dial_section", contentType = "section") { MetroSpeedDialSection(items = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                    }

                    speedDialSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                        item(key = "speed_dial_title", contentType = "title") { NavigationTitle(title = stringResource(R.string.speed_dial), modifier = Modifier.animateItem()) }
                        item(key = "speed_dial_section", contentType = "section") { SpeedDialSection(speedDialSongs = songs, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                    }

                    keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                        item(key = "keep_listening_title", contentType = "title") { NavigationTitle(title = stringResource(R.string.keep_listening), modifier = Modifier.animateItem()) }
                        item(key = "keep_listening_section", contentType = "section") { KeepListeningSection(keepListening = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                    }

                    AccountPlaylistsContainer(viewModel = viewModel, accountName = accountName, accountImageUrl = url, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)

                    forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                        item(key = "forgotten_favorites_title", contentType = "title") { NavigationTitle(title = stringResource(R.string.forgotten_favorites), modifier = Modifier.animateItem()) }
                        item(key = "forgotten_favorites_section", contentType = "section") { 
                            ForgottenFavoritesSection(
                                forgottenFavorites = favorites, 
                                mediaMetadata = mediaMetadata, 
                                isPlaying = isPlaying, 
                                navController = navController, 
                                playerConnection = playerConnection, 
                                menuState = menuState, 
                                haptic = haptic
                            ) 
                        }
                    }

                    SimilarRecommendationsContainer(viewModel = viewModel, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)

                    homePage?.sections?.forEachIndexed { index, section ->
                        item(key = "section_title_${section.title}_$index", contentType = "title") { HomePageSectionTitle(section = section, navController = navController, modifier = Modifier.animateItem()) }
                        item(key = "section_content_${section.title}_$index", contentType = "section") { HomePageSectionContent(section = section, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                    }

                    if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                        item(key = "loading_wavy", contentType = "loading") { HomeWavyLoading(modifier = Modifier.animateItem()) }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "action_card_scale")
    val alpha by animateFloatAsState(targetValue = if (isPressed) 0.8f else 1f, animationSpec = tween(durationMillis = 120), label = "action_card_alpha")

    Card(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .height(48.dp),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
