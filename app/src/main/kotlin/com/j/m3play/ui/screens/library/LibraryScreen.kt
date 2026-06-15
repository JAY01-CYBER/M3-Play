package com.j.m3play.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.j.m3play.ui.component.TagsFilterChips
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.extensions.bounceClick

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

    val filterContent = @Composable {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filters = listOf(
                    LibraryFilter.LIBRARY to R.string.filter_library to R.drawable.library_music,
                    LibraryFilter.PLAYLISTS to R.string.filter_playlists to R.drawable.queue_music,
                    LibraryFilter.SONGS to R.string.filter_songs to R.drawable.music_note,
                    LibraryFilter.ARTISTS to R.string.filter_artists to R.drawable.person,
                    LibraryFilter.ALBUMS to R.string.filter_albums to R.drawable.album
                )

                filters.forEach { (filterPair, iconRes) ->
                    val (type, stringRes) = filterPair
                    val isSelected = filterType == type

                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .bounceClick { filterType = type }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(stringRes),
                                fontWeight = FontWeight.Bold
                            )
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

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.6f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        
                        drawRect(Brush.radialGradient(listOf(color1.copy(alpha = 0.15f), Color.Transparent), center = Offset(width * 0.2f, height * 0.1f), radius = width * 0.8f))
                        drawRect(Brush.radialGradient(listOf(color2.copy(alpha = 0.12f), Color.Transparent), center = Offset(width * 0.8f, height * 0.2f), radius = width * 0.7f))
                        drawRect(Brush.radialGradient(listOf(color3.copy(alpha = 0.1f), Color.Transparent), center = Offset(width * 0.5f, height * 0.4f), radius = width * 0.9f))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, surfaceColor.copy(alpha = 0.8f), surfaceColor), startY = height * 0.3f, endY = height))
                    }
            ) {}
        }

        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, filterContent)
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, filterContent)
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, filterContent)
        }
    }
}
