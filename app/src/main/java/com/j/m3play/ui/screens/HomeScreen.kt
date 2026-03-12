package com.j.m3play.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j.m3play.data.Song

@Composable
fun HomeScreen(modifier: Modifier = Modifier, songs: List<Song>, onSongClicked: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs.indices.toList()) { index ->
            SongCard(title = songs[index].title, artist = songs[index].artist, onClick = { onSongClicked(index) })
        }
    }
}

@Composable
private fun SongCard(title: String, artist: String, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Card(onClick = onClick, modifier = Modifier.aspectRatio(1f)) {}
        Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(text = artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
