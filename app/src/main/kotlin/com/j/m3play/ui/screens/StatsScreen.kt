/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V3.2   │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.StatPeriod
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.Song
import com.j.m3play.db.entities.SongWithStats
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.ChoiceChipsRow
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.ItemThumbnail
import com.j.m3play.ui.component.LocalAlbumsGrid
import com.j.m3play.ui.component.LocalArtistsGrid
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.menu.AlbumMenu
import com.j.m3play.ui.menu.ArtistMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.joinByBullet
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val currentDate = LocalDateTime.now()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    val weeklyDates = if (currentDate != null && firstEvent != null) {
        generateSequence(currentDate) { it.minusWeeks(1) }
            .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
            .mapIndexed { index, date ->
                val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                val formatter = DateTimeFormatter.ofPattern("dd MMM")
                val text = if (date.month != endDate.month) {
                    "${formatter.format(date)} - ${formatter.format(endDate)}"
                } else {
                    "${date.dayOfMonth} - ${formatter.format(endDate)}"
                }
                Pair(index, text)
            }.toList()
    } else emptyList()

    val monthlyDates = if (currentDate != null && firstEvent != null) {
        generateSequence(currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)) { it.minusMonths(1) }
            .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.withDayOfMonth(1)) }
            .mapIndexed { index, date ->
                Pair(index, DateTimeFormatter.ofPattern("MMM").format(date))
            }.toList()
    } else emptyList()

    val yearlyDates = if (currentDate != null && firstEvent != null) {
        generateSequence(currentDate.plusYears(1).withDayOfYear(1).minusDays(1)) { it.minusYears(1) }
            .takeWhile { it.isAfter(firstEvent?.event?.timestamp) }
            .mapIndexed { index, date -> Pair(index, "${date.year}") }.toList()
    } else emptyList()

    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    
    // Using Material Theme Colors for perfect blend
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Base theme background
    ) {
        // Subtle thematic glow instead of hardcoded dark gradients
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.6f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(width * 0.1f, height * 0.2f), radius = width * 0.8f
                        )
                        val brush2 = Brush.radialGradient(
                            colors = listOf(secondaryColor.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(width * 0.9f, height * 0.4f), radius = width * 0.9f
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                        }
                    }
            )
        }
        
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        ) {
            // Options Chips
            item {
                ChoiceChipsRow(
                    chips = when (selectedOption) {
                        OptionStats.WEEKS -> weeklyDates
                        OptionStats.MONTHS -> monthlyDates
                        OptionStats.YEARS -> yearlyDates
                        OptionStats.CONTINUOUS -> {
                            listOf(
                                StatPeriod.WEEK_1.ordinal to pluralStringResource(R.plurals.n_week, 1, 1),
                                StatPeriod.MONTH_1.ordinal to pluralStringResource(R.plurals.n_month, 1, 1),
                                StatPeriod.MONTH_3.ordinal to pluralStringResource(R.plurals.n_month, 3, 3),
                                StatPeriod.MONTH_6.ordinal to pluralStringResource(R.plurals.n_month, 6, 6),
                                StatPeriod.YEAR_1.ordinal to pluralStringResource(R.plurals.n_year, 1, 1),
                                StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                            )
                        }
                    },
                    options = listOf(
                        OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                        OptionStats.WEEKS to stringResource(R.string.weeks),
                        OptionStats.MONTHS to stringResource(R.string.months),
                        OptionStats.YEARS to stringResource(R.string.years),
                    ),
                    selectedOption = selectedOption,
                    onSelectionChange = {
                        viewModel.selectedOption.value = it
                        viewModel.indexChips.value = 0
                    },
                    currentValue = indexChips,
                    onValueUpdate = { viewModel.indexChips.value = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Highlights Section
            if (mostPlayedArtists.isNotEmpty() || mostPlayedSongsStats.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Your Highlights", 
                        icon = Icons.Rounded.AutoAwesome, 
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val topArtist = mostPlayedArtists.firstOrNull()
                        if (topArtist != null) {
                            PremiumHighlightCard(
                                label = "Top Artist",
                                mainText = topArtist.artist.name,
                                subText = "${topArtist.songCount} songs played • ${makeTimeString(topArtist.timeListened?.toLong())}",
                                imageUrl = topArtist.artist.thumbnailUrl,
                                trailingIcon = Icons.Rounded.Star,
                                trailingBg = MaterialTheme.colorScheme.surfaceVariant,
                                trailingIconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { navController.navigate("artist/${topArtist.id}") }
                            )
                        }

                        val topSong = mostPlayedSongsStats.firstOrNull()
                        if (topSong != null) {
                            PremiumHighlightCard(
                                label = "Top Song",
                                mainText = topSong.title,
                                subText = "${topSong.songCountListened} plays • ${makeTimeString(topSong.timeListened)}",
                                imageUrl = topSong.thumbnailUrl,
                                trailingIcon = Icons.Rounded.PlayArrow,
                                trailingBg = MaterialTheme.colorScheme.primary,
                                trailingIconTint = MaterialTheme.colorScheme.onPrimary,
                                onClick = {
                                    if (topSong.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else {
                                        mostPlayedSongs.firstOrNull()?.let { entity ->
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    endpoint = WatchEndpoint(topSong.id),
                                                    preloadItem = entity.toMediaMetadata(),
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Listening Overview Section
            item(key = "listeningOverview") {
                if (mostPlayedArtists.isNotEmpty()) {
                    SectionHeader(title = "Listening Overview")
                    
                    // Pie Chart and Total Time
                    ArtistPieChart(
                        artists = mostPlayedArtists.take(5), 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // 3 Overview Pills (Songs, Artists, Albums) - FIXED LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Slightly reduced spacing to fit everything
                    ) {
                        OverviewStatPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.MusicNote,
                            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            count = mostPlayedSongsStats.size.toString(),
                            label = "Songs"
                        )
                        OverviewStatPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Headphones,
                            iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            iconTint = MaterialTheme.colorScheme.secondary,
                            count = mostPlayedArtists.size.toString(),
                            label = "Artists"
                        )
                        OverviewStatPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Album,
                            iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            count = mostPlayedAlbums.size.toString(),
                            label = "Albums"
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Top Songs
            if (mostPlayedSongsStats.isNotEmpty()) {
                item(key = "mostPlayedSongsHeader") {
                    SectionHeader(title = "Top Songs")
                }

                itemsIndexed(
                    items = mostPlayedSongsStats,
                    key = { _, song -> song.id },
                ) { index, song ->
                    PremiumSongRow(
                        index = index + 1,
                        song = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                endpoint = WatchEndpoint(song.id),
                                                preloadItem = mostPlayedSongs[index].toMediaMetadata(),
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = mostPlayedSongs[index],
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Top Artists Grid
            if (mostPlayedArtists.isNotEmpty()) {
                item(key = "mostPlayedArtists") {
                    SectionHeader(title = "Top Artists")
                }

                itemsIndexed(
                    items = mostPlayedArtists.chunked(2),
                    key = { _, rowArtists -> rowArtists.first().id },
                ) { _, rowArtists ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowArtists.forEach { artist ->
                            LocalArtistsGrid(
                                title = artist.artist.name,
                                subtitle = joinByBullet(
                                    pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                                    makeTimeString(artist.timeListened?.toLong()),
                                ),
                                thumbnailUrl = artist.artist.thumbnailUrl,
                                modifier = Modifier
                                    .weight(1f)
                                    .combinedClickable(
                                        onClick = { navController.navigate("artist/${artist.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = artist,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                        repeat(2 - rowArtists.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Top Albums
            if (mostPlayedAlbums.isNotEmpty()) {
                item(key = "mostPlayedAlbums") {
                    SectionHeader(title = "Top Albums")
                    LazyRow {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle = joinByBullet(
                                    pluralStringResource(R.plurals.n_time, album.songCountListened ?: 0, album.songCountListened ?: 0),
                                    makeTimeString(album.timeListened?.toLong()),
                                ),
                                thumbnailUrl = album.album.thumbnailUrl,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = album,
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
        }

        // Shuffle Extended FAB
        if (mostPlayedSongs.isNotEmpty()) {
            val isFabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = 16.dp)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Shuffle", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(painterResource(R.drawable.shuffle), contentDescription = null) },
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.most_played_songs),
                                items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled()
                            )
                        )
                    },
                    shape = RoundedCornerShape(50),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    expanded = isFabExpanded,
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(50)) 
                )
            }
        }

        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.stats),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(40.dp) 
                        .clip(CircleShape)
                        .clickable { navController.navigateUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back), 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(40.dp) 
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { navController.navigate("year_in_music") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.calendar_today), 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent, 
                scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
            )
        )
    }
}

// ------------------------------------------------------------------------
// Custom Ultra-Premium Components (Theme Friendly)
// ------------------------------------------------------------------------

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, iconTint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 6.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PremiumHighlightCard(
    label: String,
    mainText: String,
    subText: String,
    imageUrl: String?,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingBg: Color,
    trailingIconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = mainText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = trailingBg, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = trailingIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistPieChart(
    artists: List<Artist>,
    modifier: Modifier = Modifier
) {
    val totalTime = artists.sumOf { it.timeListened?.toLong() ?: 0L }
    if (totalTime == 0L) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                var startAngle = -90f
                val gapDegrees = 3f 

                artists.forEach { artist ->
                    val time = artist.timeListened?.toLong() ?: 0L
                    val sweepAngle = (time.toFloat() / totalTime) * 360f

                    if (sweepAngle > gapDegrees) {
                        AsyncImage(
                            model = artist.artist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(PieSliceShape(startAngle, sweepAngle, gapDegrees))
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            Column {
                Text(
                    text = "Total Time\nListened",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                val timeString = makeTimeString(totalTime)
                val parts = timeString.split(" ")
                
                if (parts.size >= 2) {
                    Text(
                        text = "${parts[0]} ${parts[1]}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (parts.size >= 3) {
                        Text(
                            text = parts[2],
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Fixed Layout for Pills to stop text wrapping
@Composable
fun OverviewStatPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    count: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) { // Weight 1f is the magic fix for wrapping!
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PremiumSongRow(
    index: Int,
    song: SongWithStats,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemThumbnail(
            thumbnailUrl = song.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$index. ${song.title}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = joinByBullet(
                    pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                    makeTimeString(song.timeListened),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { /* Menus are handled in combinedClickable */ },
            onLongClick = { /* Menus are handled in combinedClickable */ }
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun PieSliceShape(startAngle: Float, sweepAngle: Float, gap: Float): GenericShape {
    return GenericShape { size, _ ->
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        val actualStart = startAngle + (gap / 2f)
        val actualSweep = (sweepAngle - gap).coerceAtLeast(0.1f)
        
        moveTo(center.x, center.y)
        val startRad = Math.toRadians(actualStart.toDouble())
        lineTo(
            (center.x + radius * cos(startRad)).toFloat(),
            (center.y + radius * sin(startRad)).toFloat()
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(center = center, radius = radius),
            startAngleDegrees = actualStart,
            sweepAngleDegrees = actualSweep,
            forceMoveTo = false
        )
        lineTo(center.x, center.y)
        close()
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }
