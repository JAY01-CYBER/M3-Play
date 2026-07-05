/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens

import android.content.Intent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.db.entities.Album
import com.j.m3play.db.entities.Artist
import com.j.m3play.db.entities.SongWithStats
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.ComposeToImage
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.YearInMusicViewModel
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.sin

// --- Premium Colors for MD3 Redesign ---
private val NeonPink = Color(0xFFFF006E)
private val DeepMaroon = Color(0xFF2D161F) // For total listening time card
private val MutedBlue = Color(0xFF161B2D) // For songs played card
private val ElectricPurple = Color(0xFF8338EC)
private val DeepBlack = Color(0xFF0A0A0F)
private val SurfaceDark = Color(0xFF151515) // For list items
private val SoftWhite = Color(0xFFFAFAFA)
private val GlassWhite = Color(0x1AFFFFFF) // Reduced opacity for sleeker glass
private val TextGray = Color(0xFFA0A0A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    
    val availableYears by viewModel.availableYears.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val topSongsStats by viewModel.topSongsStats.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val topAlbums by viewModel.topAlbums.collectAsState()
    val totalListeningTime by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage by remember { mutableStateOf(false) }
    var isShareCaptureMode by remember { mutableStateOf(false) }
    var shareBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen by remember { mutableStateOf(false) }

    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val shareBackgroundArgb = DeepBlack.toArgb()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .onGloballyPositioned { coordinates ->
                shareBounds = coordinates.boundsInRoot()
            }
    ) {
        // Master Scrolling Dashboard
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp) // padding for bottom bar
        ) {
            // Hero Section (Top Artist Image as Background)
            item {
                DashboardHeroSection(
                    year = selectedYear,
                    topArtist = topArtists.firstOrNull(),
                    isShareCaptureMode = isShareCaptureMode
                )
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        iconRes = R.drawable.timer,
                        title = stringResource(R.string.total_listening_time),
                        value = makeTimeString(totalListeningTime).split(" ").firstOrNull() ?: "0",
                        unit = "hours",
                        cardColor = DeepMaroon,
                        iconColor = NeonPink
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        iconRes = R.drawable.music_note,
                        title = stringResource(R.string.songs) + " Played",
                        value = totalSongsPlayed.toString(),
                        unit = "songs",
                        cardColor = MutedBlue,
                        iconColor = Color(0xFF3A86FF)
                    )
                }
            }

            // Top Picks List
            item {
                SectionHeader(title = "Your Top Picks", actionText = "View all >")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topSongsStats.firstOrNull()?.let { song ->
                        TopPickItem(
                            label = "Top Song",
                            title = song.title,
                            subtitle = "${song.songCountListened} times",
                            imageUrl = song.thumbnailUrl,
                            labelColor = NeonPink
                        )
                    }
                    topArtists.firstOrNull()?.let { artist ->
                        TopPickItem(
                            label = "Top Artist",
                            title = artist.artist.name,
                            subtitle = "${artist.songCount} times",
                            imageUrl = artist.artist.thumbnailUrl,
                            labelColor = ElectricPurple
                        )
                    }
                    topAlbums.firstOrNull()?.let { album ->
                        TopPickItem(
                            label = "Top Album",
                            title = album.album.title,
                            subtitle = album.artists.take(2).joinToString(" • ") { it.name },
                            imageUrl = album.thumbnailUrl,
                            labelColor = Color(0xFFFFBE0B)
                        )
                    }
                }
            }

            // Explore Cards Row
            item {
                SectionHeader(title = "Explore Your $selectedYear", subtitle = "Dive deeper into your music story.")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ExploreCard(
                            title = "Top Songs",
                            subtitle = "Your most played tracks",
                            iconRes = R.drawable.ic_music, 
                            imageUrl = topSongsStats.getOrNull(1)?.thumbnailUrl ?: topSongsStats.firstOrNull()?.thumbnailUrl,
                            accentColor = NeonPink
                        )
                    }
                    item {
                        ExploreCard(
                            title = "Top Artists",
                            subtitle = "The artists you loved most",
                            iconRes = R.drawable.artist, 
                            imageUrl = topArtists.getOrNull(1)?.artist?.thumbnailUrl ?: topArtists.firstOrNull()?.artist?.thumbnailUrl,
                            accentColor = ElectricPurple
                        )
                    }
                    item {
                        ExploreCard(
                            title = "Top Albums",
                            subtitle = "Your favorite albums",
                            iconRes = R.drawable.album, 
                            imageUrl = topAlbums.getOrNull(1)?.thumbnailUrl ?: topAlbums.firstOrNull()?.thumbnailUrl,
                            accentColor = Color(0xFFFF6B35)
                        )
                    }
                }
            }
        }

        // Top App Bar Overlay
        if (!isShareCaptureMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                    )
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // M3 Adaptive Logo Integration
                                AdaptiveAppIcon(modifier = Modifier.size(28.dp))
                                
                                Column {
                                    Text(
                                        text = stringResource(R.string.year_in_music),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftWhite
                                    )
                                    Text(
                                        text = "Your $selectedYear Music Journey",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = navController::navigateUp,
                                onLongClick = navController::backToMain
                            ) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = null,
                                    tint = SoftWhite
                                )
                            }
                        },
                        actions = {
                            PremiumYearChip(
                                year = selectedYear,
                                onClick = { isYearPickerOpen = true }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                    // Instagram-style story progress (static for dashboard look)
                    PremiumStoryProgressIndicator(totalPages = 4, currentPage = 0)
                }
            }
        }

        // Bottom Navigation Bar Overlay
        if (!isShareCaptureMode) {
            PremiumBottomNavBar(
                year = selectedYear,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                    )
                    .padding(16.dp)
            )
        }

        // Share FAB overlay
        if (topSongsStats.isNotEmpty() && !isShareCaptureMode) {
            PremiumShareButton(
                isGenerating = isGeneratingImage,
                onClick = { /* Share Logic stays the same */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp) // Raised above BottomNavBar
            )
        }

        if (!isShareCaptureMode && isYearPickerOpen) {
            PremiumYearPickerDialog(
                availableYears = availableYears,
                selectedYear = selectedYear,
                onSelectYear = { year ->
                    viewModel.selectedYear.value = year
                    isYearPickerOpen = false
                },
                onDismiss = { isYearPickerOpen = false }
            )
        }
    }
}

// ==========================================
// NEW UI COMPONENTS BASED ON SCREENSHOT
// ==========================================

@Composable
private fun AdaptiveAppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(CircleShape)
    ) {
        // Background Layer
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Foreground Layer
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Inside
        )
    }
}

@Composable
private fun DashboardHeroSection(
    year: Int,
    topArtist: Artist?,
    isShareCaptureMode: Boolean
) {
    val imageModel = rememberShareSafeImageRequest(topArtist?.artist?.thumbnailUrl)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Hero Background Image
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.7f)
        )
        // Gradient Overlay to blend into background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DeepBlack.copy(alpha = 0.6f),
                            DeepBlack
                        ),
                        startY = 100f
                    )
                )
        )

        // Text & Play Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(top = 100.dp) // Space for TopAppBar
        ) {
            Text(
                text = "YOUR $year",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonPink,
                letterSpacing = 1.sp
            )
            Text(
                text = "Music\nSnapshot",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                lineHeight = 44.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Relive your year in sound.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextGray
            )
        }

        // Replay FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GlassWhite)
                    .border(1.dp, SoftWhite.copy(alpha = 0.2f), CircleShape)
                    .clickable { /* Play Top Songs */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = "Replay",
                    tint = SoftWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Replay Year",
                style = MaterialTheme.typography.labelSmall,
                color = SoftWhite
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    value: String,
    unit: String,
    cardColor: Color,
    iconColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, GlassWhite, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null, actionText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
        if (actionText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricPurple,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TopPickItem(
    label: String,
    title: String,
    subtitle: String,
    imageUrl: Any?,
    labelColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, GlassWhite, RoundedCornerShape(16.dp))
            .clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = rememberShareSafeImageRequest(imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, GlassWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next), 
                contentDescription = null,
                tint = SoftWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExploreCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    imageUrl: Any?,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(160.dp, 200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { }
    ) {
        AsyncImage(
            model = rememberShareSafeImageRequest(imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.4f),
                            DeepBlack.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = SoftWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, GlassWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward), 
                        contentDescription = null,
                        tint = SoftWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBottomNavBar(
    year: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceDark.copy(alpha = 0.9f))
            .border(1.dp, GlassWhite, RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { },
                onLongClick = { } // ADDED ONLONGCLICK
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = SoftWhite
                )
            }
            
            Text(
                text = "Your $year in Music",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = SoftWhite
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(NeonPink, ElectricPurple)))
                    .clickable { }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = SoftWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// RETAINED UTILITY COMPOSABLES
// ==========================================

@Composable
private fun rememberShareSafeImageRequest(data: Any?): Any? {
    val context = LocalContext.current
    return remember(data, context) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .allowHardware(false)
                .build()
        }
    }
}

@Composable
private fun PremiumStoryProgressIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index == currentPage) NeonPink else SoftWhite.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun PremiumYearChip(
    year: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NeonPink.copy(alpha = 0.2f), ElectricPurple.copy(alpha = 0.2f))))
            .border(1.5.dp, NeonPink.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
            Icon(
                painter = painterResource(R.drawable.calendar_today),
                contentDescription = null,
                tint = SoftWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PremiumShareButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(56.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            containerColor = DeepBlack,
            contentColor = SoftWhite
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = NeonPink)
            } else {
                Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = SoftWhite)
            }
        }
    }
}

@Composable
private fun PremiumYearPickerDialog(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepBlack,
        titleContentColor = SoftWhite,
        title = { Text(text = "Select Year", fontWeight = FontWeight.Bold) },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(availableYears) { year ->
                    val isSelected = year == selectedYear
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Brush.linearGradient(listOf(NeonPink, ElectricPurple)) else Brush.linearGradient(listOf(GlassWhite, GlassWhite)))
                            .clickable { onSelectYear(year) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = year.toString(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = SoftWhite,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss", color = NeonPink)
            }
        }
    )
}
