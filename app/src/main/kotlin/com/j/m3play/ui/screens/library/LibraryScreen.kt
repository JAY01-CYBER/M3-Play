package com.j.m3play.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
                        val newTags =
                            if (tag.id in selectedTagIds) selectedTagIds - tag.id
                            else selectedTagIds + tag.id

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
    val surfaceColor = MaterialTheme.colorScheme.surface


    val infinite = rememberInfiniteTransition(label = "")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Box(modifier = Modifier.fillMaxSize()) {

        
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {

                        val w = size.width
                        val h = size.height

                        drawRect(
                            brush = Brush.radialGradient(
                                listOf(color1.copy(0.15f), Color.Transparent),
                                center = Offset(w * 0.2f + shift, h * 0.2f),
                                radius = w * 0.6f
                            )
                        )

                        drawRect(
                            brush = Brush.radialGradient(
                                listOf(color2.copy(0.13f), Color.Transparent),
                                center = Offset(w * 0.8f - shift, h * 0.3f),
                                radius = w * 0.7f
                            )
                        )

                        drawRect(
                            brush = Brush.radialGradient(
                                listOf(color3.copy(0.12f), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.8f
                            )
                        )

                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = 0.35f),
                                    surfaceColor
                                )
                            )
                        )
                    }
            )
        }

        
        AnimatedContent(
            targetState = filterType,
            transitionSpec = {
                fadeIn(tween(300)) + slideInVertically { it / 8 } togetherWith
                        fadeOut(tween(200))
            },
            label = ""
        ) { state ->

            when (state) {

                LibraryFilter.LIBRARY ->
                    LibraryMixScreen(navController, filterContent)

                LibraryFilter.PLAYLISTS ->
                    LibraryPlaylistsScreen(navController, filterContent)

                LibraryFilter.SONGS ->
                    LibrarySongsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.LIBRARY }
                    )

                LibraryFilter.ALBUMS ->
                    LibraryAlbumsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.LIBRARY }
                    )

                LibraryFilter.ARTISTS ->
                    LibraryArtistsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.LIBRARY }
                    )
            }
        }
    }
}
