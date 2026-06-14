/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.R
import com.j.m3play.constants.ChipSortTypeKey
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.LibraryFilter
import com.j.m3play.constants.PlaylistTagsFilterKey
import com.j.m3play.constants.ShowTagsInLibraryKey
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.TagsFilterChips
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false) // Default set to false for Premium look

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filterContent = @Composable {
        Column {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ChipsRow(
                    chips = listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    ),
                    currentValue = filterType,
                    onValueUpdate = {
                        filterType = if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                    },
                    icons = mapOf(
                        LibraryFilter.PLAYLISTS to R.drawable.queue_music,
                        LibraryFilter.SONGS to R.drawable.music_note,
                        LibraryFilter.ALBUMS to R.drawable.album,
                        LibraryFilter.ARTISTS to R.drawable.person,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            if (showTagsInLibrary) {
                TagsFilterChips(
                    database = database,
                    selectedTags = selectedTagIds,
                    onTagToggle = { tag ->
                        val newTags = if (tag.id in selectedTagIds) {
                            selectedTagIds - tag.id
                        } else {
                            selectedTagIds + tag.id
                        }
                        onSelectedTagsFilterChange(newTags.joinToString(","))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
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
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.28f),
                                color1.copy(alpha = 0.16f),
                                color1.copy(alpha = 0.08f),
                                color1.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.15f, height * 0.1f),
                            radius = width * 0.55f
                        )
                    )
                    
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.26f),
                                color2.copy(alpha = 0.14f),
                                color2.copy(alpha = 0.07f),
                                color2.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.85f, height * 0.2f),
                            radius = width * 0.65f
                        )
                    )
                    
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.24f),
                                color3.copy(alpha = 0.12f),
                                color3.copy(alpha = 0.06f),
                                color3.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.3f, height * 0.45f),
                            radius = width * 0.6f
                        )
                    )
                    
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color4.copy(alpha = 0.20f),
                                color4.copy(alpha = 0.10f),
                                color4.copy(alpha = 0.05f),
                                color4.copy(alpha = 0.01f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.7f, height * 0.5f),
                            radius = width * 0.7f
                        )
                    )
                    
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color5.copy(alpha = 0.18f),
                                color5.copy(alpha = 0.08f),
                                color5.copy(alpha = 0.04f),
                                color5.copy(alpha = 0.01f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.75f),
                            radius = width * 0.8f
                        )
                    )
                    
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.4f),
                                surfaceColor.copy(alpha = 0.8f),
                                surfaceColor
                            ),
                            startY = height * 0.3f,
                            endY = height
                        )
                    )
                }
            ) {}
        }

        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, { filterType = LibraryFilter.LIBRARY })
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, { filterType = LibraryFilter.LIBRARY })
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, { filterType = LibraryFilter.LIBRARY })
        }
    }
}
