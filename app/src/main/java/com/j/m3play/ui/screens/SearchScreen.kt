package com.j.m3play.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.j.m3play.data.YoutubeRepository
import com.j.m3play.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    repository: YoutubeRepository,
    onSongSelected: (Song) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        results = withContext(Dispatchers.Default) { repository.searchSongs(searchQuery) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs or artists") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(results, key = { it.id }) { song ->
                SongListItem(song = song, onClick = { onSongSelected(song) })
            }
        }
    }
}

@Composable
fun SongListItem(song: Song, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = { Text(song.artist) },
        overlineContent = { Text(song.durationLabel, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    )
}
