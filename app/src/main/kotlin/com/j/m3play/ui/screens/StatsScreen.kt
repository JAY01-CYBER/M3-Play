/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
import com.j.m3play.ui.component.HideOnScrollFAB
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.ItemThumbnail
import com.j.m3play.ui.component.ListItem
import com.j.m3play.ui.component.LocalAlbumsGrid
import com.j.m3play.ui.component.LocalArtistsGrid
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.menu.AlbumMenu
import com.j.m3play.ui.menu.ArtistMenu
import com.j.m3play.ui.menu.SongMenu
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.joinByBullet
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
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
                val startDateFormatted = formatter.format(date)
                val endDateFormatted = formatter.format(endDate)
                val startMonth = date.month
                val endMonth = endDate.month
                val startYear = date.year
                val endYear = endDate.year

                val text = when {
                    startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                    startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                    else -> "${date.dayOfMonth} - $endDateFormatted"
                }
                Pair(index, text)
            }.toList()
    } else emptyList()

    val monthlyDates = if (currentDate != null && firstEvent != null) {
        generateSequence(currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)) { it.minusMonths(1) }
            .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.withDayOfMonth(1)) }
            .mapIndexed { index, date ->
                val formatter = DateTimeFormatter.ofPattern("MMM")
                val formattedDate = formatter.format(date)
                val text = if (date.year != currentDate.year) "$formattedDate ${date.year}" else formattedDate
                Pair(index, text)
            }.toList()
    } else emptyList()

    val yearlyDates = if (currentDate != null && firstEvent != null) {
        generateSequence(currentDate.plusYears(1).withDayOfYear(1).minusDays(1)) { it.minusYears(1) }
            .takeWhile { it.isAfter(firstEvent?.event?.timestamp) }
            .mapIndexed { index, date -> Pair(index, "${date.year}") }.toList()
    } else emptyList()

    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    
    // Enhanced Premium Colors for Background
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxSize()) {
        // Modern Background
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.85f) // Extended slightly for immersive feel
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(
                            colors = listOf(color1.copy(alpha = 0.45f), color1.copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(width * 0.2f, height * 0.1f), radius = width * 0.7f
                        )
                        val brush2 = Brush.radialGradient(
                            colors = listOf(color2.copy(alpha = 0.4f), color2.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(width * 0.8f, height * 0.25f), radius = width * 0.75f
                        )
                        val overlayBrush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.6f), surfaceColor),
                            startY = height * 0.3f, endY = height
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = overlayBrush)
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
            // Segmented Chips Options (Top)
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
            }

            // HighLights Section - Unified into a single Premium Card
            item {
                StatsHighlightsSection(
                    topArtist = mostPlayedArtists.firstOrNull(),
                    topSong = mostPlayedSongsStats.firstOrNull(),
                    topSongEntity = mostPlayedSongs.firstOrNull(),
                    navController = navController,
                )
            }

            // Artist Pie Chart with Neon Arc
            item(key = "artistPieChart") {
                if (mostPlayedArtists.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(24.dp))
                    ArtistPieChart(
                        artists = mostPlayedArtists.take(5), 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem()
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }

            item(key = "mostPlayedSongsHeader") {
                NavigationTitle(
                    title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                    modifier = Modifier.animateItem(),
                )
            }

            itemsIndexed(
                items = mostPlayedSongsStats,
                key = { _, song -> song.id },
            ) { index, song ->
                ListItem(
                    title = "${index + 1}. ${song.title}",
                    subtitle = joinByBullet(
                        pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                        makeTimeString(song.timeListened),
                    ),
                    thumbnailContent = {
                        ItemThumbnail(
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            shape = RoundedCornerShape(8.dp), 
                            modifier = Modifier.size(56.dp)
                        )
                    },
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
                            },
                        )
                        .animateItem()
                )
            }

            item(key = "mostPlayedArtists") {
                NavigationTitle(
                    title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                    modifier = Modifier.animateItem(),
                )
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

            item(key = "mostPlayedAlbums") {
                NavigationTitle(
                    title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                    modifier = Modifier.animateItem(),
                )
                if (mostPlayedAlbums.isNotEmpty()) {
                    LazyRow {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle = joinByBullet(
                                    pluralStringResource(R.plurals.n_time, album.songCountListened!!, album.songCountListened),
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

        // Shuffle FAB
        if (mostPlayedSongs.isNotEmpty()) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled()
                        )
                    )
                }
            )
        }

        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.stats)) },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate("year_in_music") }, onLongClick = { }) {
                    Icon(painterResource(R.drawable.calendar_today), contentDescription = stringResource(R.string.year_in_music))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
        )
    }
}

// ------------------------------------------------------------------------
// Custom Redesigned Components
// ------------------------------------------------------------------------

@Composable
fun ArtistPieChart(
    artists: List<Artist>,
    modifier: Modifier = Modifier
) {
    val totalTime = artists.sumOf { it.timeListened?.toLong() ?: 0L }
    if (totalTime == 0L) return
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Neon Progress Ring + Pie Chart
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Neon Arc Indicator
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    startAngle = -90f,
                    sweepAngle = 260f, // Partial wrap for aesthetic look
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner Pie (Scaled down slightly to fit inside the ring)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                var startAngle = -90f
                artists.forEach { artist ->
                    val time = artist.timeListened?.toLong() ?: 0L
                    val sweepAngle = (time.toFloat() / totalTime) * 360f

                    if (sweepAngle > 1f) {
                        AsyncImage(
                            model = artist.artist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(PieSliceShape(startAngle, sweepAngle))
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }

        // Big Typography for Stats
        Column {
            Text(
                text = "Total Time Listened",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            // Splitting hours/mins for typographic hierarchy
            val timeString = makeTimeString(totalTime)
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun PieSliceShape(startAngle: Float, sweepAngle: Float): GenericShape {
    return GenericShape { size, _ ->
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        moveTo(center.x, center.y)
        val startRad = Math.toRadians(startAngle.toDouble())
        lineTo(
            (center.x + radius * cos(startRad)).toFloat(),
            (center.y + radius * sin(startRad)).toFloat()
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(center = center, radius = radius),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )
        lineTo(center.x, center.y)
        close()
    }
}

@Composable
fun StatsHighlightsSection(
    topArtist: Artist?,
    topSong: SongWithStats?,
    topSongEntity: Song?,
    navController: NavController
) {
    if (topArtist == null && topSong == null) return

    // Unified Premium Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) // Glassmorphism base
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Your Music Highlights",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (topArtist != null) {
                StatsHighlightItemRow(
                    label = "Top Artist",
                    mainText = topArtist.artist.name,
                    subText = "${topArtist.songCount} songs • ${makeTimeString(topArtist.timeListened?.toLong())}",
                    imageUrl = topArtist.artist.thumbnailUrl,
                    onClick = { navController.navigate("artist/${topArtist.id}") }
                )
            }

            if (topSong != null && topSongEntity != null) {
                StatsHighlightItemRow(
                    label = "Top Song",
                    mainText = topSong.title,
                    subText = "${topSong.songCountListened} plays • ${makeTimeString(topSong.timeListened)}",
                    imageUrl = topSong.thumbnailUrl,
                    onClick = { /* Handle click */ }
                )
            }
        }
    }
}

@Composable
fun StatsHighlightItemRow(
    label: String,
    mainText: String,
    subText: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = mainText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }
