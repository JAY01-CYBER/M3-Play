/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.ChipSortTypeKey
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.LibraryFilter
import com.j.m3play.constants.PlaylistTagsFilterKey
import com.j.m3play.constants.ShowTagsInLibraryKey
import com.j.m3play.ui.component.TagsFilterChips
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, true)

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filtersList = remember {
        listOf(
            LibraryFilter.LIBRARY to R.string.filter_library to R.drawable.library_music,
            LibraryFilter.PLAYLISTS to R.string.filter_playlists to R.drawable.queue_music,
            LibraryFilter.SONGS to R.string.filter_songs to R.drawable.music_note,
            LibraryFilter.ARTISTS to R.string.filter_artists to R.drawable.person,
            LibraryFilter.ALBUMS to R.string.filter_albums to R.drawable.album
        )
    }

    val titlesList = remember {
        listOf(
            "Library" to "Everything you love",
            "Playlists" to "All your playlists, organized for you",
            "Songs" to "All your songs, organized for you",
            "Artists" to "All your artists, in one place",
            "Albums" to "All your albums, beautifully organized"
        )
    }

    val initialPageIndex = remember {
        val index = filtersList.indexOfFirst { it.first.first == filterType }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(initialPage = initialPageIndex) { filtersList.size }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(pagerState.currentPage) {
        val currentFilter = filtersList[pagerState.currentPage].first.first
        if (filterType != currentFilter) {
            filterType = currentFilter
        }
        lazyListState.animateScrollToItem(pagerState.currentPage)
    }

    val density = LocalDensity.current
    var titleHeightPx by remember { mutableFloatStateOf(with(density) { 80.dp.toPx() }) }
    var chipsHeightPx by remember { mutableFloatStateOf(with(density) { 60.dp.toPx() }) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val previousOffset = headerOffsetPx
                val newOffset = (headerOffsetPx + delta).coerceIn(-titleHeightPx, 0f)
                headerOffsetPx = newOffset
                val consumed = newOffset - previousOffset
                return Offset(0f, consumed)
            }
        }
    }

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    val topPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        drawRect(Brush.radialGradient(listOf(color1.copy(alpha = 0.38f), color1.copy(alpha = 0.24f), color1.copy(alpha = 0.14f), color1.copy(alpha = 0.06f), Color.Transparent), center = Offset(width * 0.15f, height * 0.1f), radius = width * 0.55f))
                        drawRect(Brush.radialGradient(listOf(color2.copy(alpha = 0.34f), color2.copy(alpha = 0.2f), color2.copy(alpha = 0.11f), color2.copy(alpha = 0.05f), Color.Transparent), center = Offset(width * 0.85f, height * 0.2f), radius = width * 0.65f))
                        drawRect(Brush.radialGradient(listOf(color3.copy(alpha = 0.3f), color3.copy(alpha = 0.17f), color3.copy(alpha = 0.09f), color3.copy(alpha = 0.04f), Color.Transparent), center = Offset(width * 0.3f, height * 0.45f), radius = width * 0.6f))
                        drawRect(Brush.radialGradient(listOf(color4.copy(alpha = 0.26f), color4.copy(alpha = 0.14f), color4.copy(alpha = 0.08f), color4.copy(alpha = 0.03f), Color.Transparent), center = Offset(width * 0.7f, height * 0.5f), radius = width * 0.7f))
                        drawRect(Brush.radialGradient(listOf(color5.copy(alpha = 0.22f), color5.copy(alpha = 0.12f), color5.copy(alpha = 0.06f), color5.copy(alpha = 0.02f), Color.Transparent), center = Offset(width * 0.5f, height * 0.75f), radius = width * 0.8f))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.22f), surfaceColor.copy(alpha = 0.55f), surfaceColor), startY = height * 0.4f, endY = height))
                    }
            ) {}
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val screenTopPadding = with(density) { (titleHeightPx + chipsHeightPx).toDp() } + topPadding
            val contentPadding = PaddingValues(top = screenTopPadding)

            when (filtersList[page].first.first) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(navController, contentPadding)
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, contentPadding)
                LibraryFilter.SONGS -> LibrarySongsScreen(navController, contentPadding)
                LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, contentPadding)
                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, contentPadding)
            }
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                .fillMaxWidth()
                .background(surfaceColor.copy(alpha = 0.90f))
                .padding(top = topPadding)
        ) {
            val currentTitle = titlesList[pagerState.currentPage].first
            val currentSubtitle = titlesList[pagerState.currentPage].second

            Column(
                modifier = Modifier
                    .onSizeChanged { titleHeightPx = it.height.toFloat() }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(currentTitle, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                Text(currentSubtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
                modifier = Modifier.onSizeChanged { chipsHeightPx = it.height.toFloat() }
            ) {
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filtersList) { index, filterItem ->
                        val (filterPair, iconRes) = filterItem
                        val isSelected = pagerState.currentPage == index
                        
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Icon(painterResource(iconRes), null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(filterPair.second), color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }

                if (showTagsInLibrary) {
                    TagsFilterChips(
                        database = database,
                        selectedTags = selectedTagIds,
                        onTagToggle = { tag ->
                            val newTags = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                            onSelectedTagsFilterChange(newTags.joinToString(","))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp) 
                    )
                }
            }
        }
    }
}
