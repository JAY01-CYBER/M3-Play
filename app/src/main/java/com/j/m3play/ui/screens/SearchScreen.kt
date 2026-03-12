package com.j.m3play.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j.m3play.data.Song
import com.j.m3play.ui.designsystem.FrostedLayer
import com.j.m3play.ui.designsystem.MotionSpecs
import com.j.m3play.ui.designsystem.ShapeTokens

@Composable
fun SearchScreen(songs: List<Song>, onSongClick: (Song) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val results = songs.filter {
        it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FrostedLayer(modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.control) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, artists...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = ShapeTokens.control,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                ),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(results, key = { _, it -> it.id }) { index, song ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = MotionSpecs.listEnterTween) +
                        slideInHorizontally(initialOffsetX = { it / 4 + index * 8 }),
                ) {
                    FrostedLayer(modifier = Modifier.fillMaxWidth(), shape = ShapeTokens.control) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { onSongClick(song) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(song.title, modifier = Modifier.weight(1f))
                            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
