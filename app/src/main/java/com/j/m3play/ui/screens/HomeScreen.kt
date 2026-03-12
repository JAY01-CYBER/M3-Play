package com.j.m3play.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Rhythm jaisa 2-column grid
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items((1..10).toList()) {
            SongCard(title = "Song Title", artist = "Artist Name")
        }
    }
}

@Composable
fun SongCard(title: String, artist: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.aspectRatio(1f)
        ) {
            // Image yahan aayegi
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
