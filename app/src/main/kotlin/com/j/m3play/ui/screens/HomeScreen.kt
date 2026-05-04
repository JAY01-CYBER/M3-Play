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
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
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
import coil3.request.crossfade
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.ShowHomeCategoryChipsKey
import com.j.m3play.db.entities.Song
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.CommunityPlaylistItem
import com.j.m3play.viewmodels.HomeViewModel

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) 
                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        modifier = modifier.width(320.dp).height(420.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(model = item.songs.getOrNull(0)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                            AsyncImage(model = item.songs.getOrNull(1)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(model = item.songs.getOrNull(2)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                            AsyncImage(model = item.songs.getOrNull(3)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = item.playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.playlist.author?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                item.songs.take(3).forEach { song ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = song.thumbnail.replace(Regex("w\\d+-h\\d+"), "w120-h120"), contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                IconButton(onClick = { item.playlist.playEndpoint?.let { LocalPlayerConnection.current?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                    Icon(painter = painterResource(R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { item.playlist.radioEndpoint?.let { LocalPlayerConnection.current?.playQueue(YouTubeQueue(it)) } }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)) {
                    Icon(painter = painterResource(R.drawable.radio), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyCarouselCard(song: Song, onClick: () -> Unit, navController: NavController, modifier: Modifier = Modifier) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).combinedClickable(onClick = onClick, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = { menuState.dismiss() }) } }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.song.thumbnailUrl?.replace(Regex("w\\d+-h\\d+"), "w544-h544")).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (maxWidth > 200.dp) {
                Box(modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.9f)))))
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = song.song.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                    Text(text = "Recommended based on your history", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
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
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()

    // Animation for custom refresh icon rotation
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!disableBlur) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxSize(0.7f).align(Alignment.TopCenter).zIndex(-1f).drawWithCache {
                val surfaceColor = MaterialTheme.colorScheme.surface
                onDrawBehind {
                    drawRect(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), surfaceColor), endY = size.height))
                }
            })
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh)) {
            val itemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val itemWidth = maxWidth * itemWidthFactor

            LazyColumn(state = lazylistState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                if (showHomeCategoryChips) {
                    item { ChipsRow(chips = homePage?.chips.orEmpty().map { it to it.title }, currentValue = selectedChip, onValueUpdate = { viewModel.toggleChip(it) }) }
                }
                item { TimeGreetingCard(onSearchClick = { runCatching { navController.navigate("search/") } }) }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionCard(title = "Liked", icon = R.drawable.favorite, onClick = { navController.navigate("auto_playlist/liked") }, modifier = Modifier.weight(1f))
                        ActionCard(title = "Downloads", icon = R.drawable.download, onClick = { navController.navigate("auto_playlist/downloaded") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionCard(title = "History", icon = R.drawable.history, onClick = { navController.navigate("history") }, modifier = Modifier.weight(1f))
                        ActionCard(title = if (isLoggedIn) "Account" else "Library", icon = if (isLoggedIn) R.drawable.person else R.drawable.library_music, onClick = { if (isLoggedIn) navController.navigate("account") else navController.navigate("library") }, modifier = Modifier.weight(1f))
                    }
                }

                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item { NavigationTitle(title = stringResource(R.string.from_the_community)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(playlists) { item ->
                                CommunityPlaylistCard(item = item, onClick = { navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}") }, onSongClick = { song -> playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())) })
                            }
                        }
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    item { NavigationTitle(title = stringResource(R.string.quick_picks)) }
                    item {
                        val carouselState = rememberCarouselState { minOf(picks.size, 10) }
                        HorizontalMultiBrowseCarousel(state = carouselState, preferredItemWidth = 320.dp, itemSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 16.dp)) { i ->
                            GlossyCarouselCard(song = picks[i], onClick = { playerConnection.playQueue(YouTubeQueue.radio(picks[i].toMediaMetadata())) }, navController = navController)
                        }
                    }
                }

                metroSpeedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = stringResource(R.string.speed_dial)) }
                    item { MetroSpeedDialSection(items = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                speedDialSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                    item { NavigationTitle(title = stringResource(R.string.speed_dial)) }
                    item { SpeedDialSection(speedDialSongs = songs, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item { NavigationTitle(title = stringResource(R.string.keep_listening)) }
                    item { KeepListeningSection(keepListening = items, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                }

                AccountPlaylistsContainer(viewModel = viewModel, accountName = accountName, accountImageUrl = url, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope)

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                    item { NavigationTitle(title = stringResource(R.string.forgotten_favorites)) }
                    item { ForgottenFavoritesSection(forgottenFavorites = favorites, mediaMetadata = mediaMetadata, isPlaying = isPlaying, horizontalLazyGridItemWidth = itemWidth, lazyGridState = rememberLazyGridState(), snapLayoutInfoProvider = SnapLayoutInfoProvider(rememberLazyGridState(), { l, i -> l * itemWidthFactor / 2f - i / 2f }), navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic) }
                }

                homePage?.sections?.forEach { section ->
                    item { HomePageSectionTitle(section = section, navController = navController) }
                    item { HomePageSectionContent(section = section, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, scope = scope) }
                }

                if (isLoading || homePage?.continuation != null) {
                    item { HomeLoadingShimmer() }
                }
            }

            
            if (pullRefreshState.distanceFraction > 0f || isRefreshing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                        .padding(top = 12.dp)
                        .size(44.dp)
                        .graphicsLayer {
                            rotationZ = if (isRefreshing) rotation else pullRefreshState.distanceFraction * 400f
                            scaleX = pullRefreshState.distanceFraction.coerceIn(0.7f, 1f)
                            scaleY = pullRefreshState.distanceFraction.coerceIn(0.7f, 1f)
                            alpha = pullRefreshState.distanceFraction.coerceIn(0f, 1f)
                        }
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
