package com.j.m3play.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.j.m3play.data.Song
import com.j.m3play.ui.designsystem.FrostedLayer
import com.j.m3play.ui.designsystem.MotionSpecs
import com.j.m3play.ui.designsystem.ShapeTokens

@Composable
fun HomeScreen(modifier: Modifier = Modifier, songs: List<Song>, onSongClick: (Song) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(songs, key = { _, it -> it.id }) { index, song ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = MotionSpecs.listEnterTween) +
                    slideInVertically(
                        initialOffsetY = { it / 3 + index * 10 },
                        animationSpec = tween(380, easing = MotionSpecs.EasingStandard),
                    ),
            ) {
                SongCard(
                    title = song.title,
                    artist = song.artist,
                    imageUrl = song.thumbnailUrl,
                    onClick = { onSongClick(song) },
                )
            }
        }
    }
}

@Composable
private fun SongCard(title: String, artist: String, imageUrl: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        FrostedLayer(
            modifier = Modifier.aspectRatio(1f).clip(ShapeTokens.card),
            shape = ShapeTokens.card,
        ) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(ShapeTokens.card))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Text(text = artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
