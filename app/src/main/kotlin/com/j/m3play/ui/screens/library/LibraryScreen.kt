package com.j.m3play.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import com.j.m3play.constants.*
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.TagsFilterChips
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {

    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) =
        rememberPreference(PlaylistTagsFilterKey, "")

    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filterContent = @Composable {
        Column {

            // 🔥 FILTER CHIPS (clean spacing)
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType = if (filterType == it) LibraryFilter.LIBRARY else it
                },
                icons = mapOf(
                    LibraryFilter.PLAYLISTS to R.drawable.queue_music,
                    LibraryFilter.SONGS to R.drawable.music_note,
                    LibraryFilter.ALBUMS to R.drawable.album,
                    LibraryFilter.ARTISTS to R.drawable.person,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

        // 🔥 CLEAN GRADIENT (TOP ONLY + SUBTLE)
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp) // ✅ LIMITED HEIGHT
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {

                        val w = size.width
                        val h = size.height

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color1.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.2f, h * 0.2f),
                                radius = w * 0.6f
                            )
                        )

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color2.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.8f, h * 0.25f),
                                radius = w * 0.7f
                            )
                        )

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color3.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.8f
                            )
                        )

                        // Smooth fade
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = 0.4f),
                                    surfaceColor
                                )
                            )
                        )
                    }
            )
        }

        // 🔥 CONTENT
        when (filterType) {
            LibraryFilter.LIBRARY ->
                LibraryMixScreen(navController, filterContent)

            LibraryFilter.PLAYLISTS ->
                LibraryPlaylistsScreen(navController, filterContent)

            LibraryFilter.SONGS ->
                LibrarySongsScreen(navController) {
                    filterType = LibraryFilter.LIBRARY
                }

            LibraryFilter.ALBUMS ->
                LibraryAlbumsScreen(navController) {
                    filterType = LibraryFilter.LIBRARY
                }

            LibraryFilter.ARTISTS ->
                LibraryArtistsScreen(navController) {
                    filterType = LibraryFilter.LIBRARY
                }
        }
    }
}
