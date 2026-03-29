package com.j.m3play.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.j.m3play.LocalDatabase
import com.j.m3play.db.entities.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuickPicksSection(
    title: String = "Quick picks",
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onPlayAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val database = LocalDatabase.current

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HomeSectionTitle(
            title = title,
            actionText = if (onPlayAllClick != null) "Play all" else null,
            onActionClick = onPlayAllClick
        )

        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = songs,
                key = { _, it -> it.id }
            ) { index, originalSong ->
                val song by database.song(originalSong.id).collectAsState(initial = originalSong)

                val alpha = remember { Animatable(0f) }
                val offset = remember { Animatable(20f) }

                LaunchedEffect(Unit) {
                    delay(index * 30L)
                    launch { alpha.animateTo(1f, tween(250)) }
                    launch { offset.animateTo(0f, tween(250)) }
                }

                M3PlayQuickPickCard(
                    song = song!!,
                    subtitle = song!!.artists.joinToString { it.name },
                    onClick = { onSongClick(song!!) },
                    onMenuClick = { onSongMenuClick(song!!) },
                    modifier = Modifier
                        .width(180.dp)
                        .graphicsLayer {
                            this.alpha = alpha.value
                            translationY = offset.value
                            scaleX = 0.96f + (alpha.value * 0.04f)
                            scaleY = 0.96f + (alpha.value * 0.04f)
                        }
                )
            }
        }
    }
}
