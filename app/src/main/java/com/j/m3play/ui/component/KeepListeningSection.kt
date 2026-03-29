package com.j.m3play.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j.m3play.db.entities.LocalItem

@Composable
fun KeepListeningSection(
    items: List<LocalItem>,
    itemHeight: androidx.compose.ui.unit.Dp,
    title: String = "Keep listening",
    itemContent: @Composable (LocalItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val rows = if (items.size > 6) 2 else 1

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HomeSectionTitle(title = title)

        LazyHorizontalGrid(
            state = rememberLazyGridState(),
            rows = GridCells.Fixed(rows),
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal)
                .asPaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * rows)
        ) {
            items(items) {
                itemContent(it)
            }
        }
    }
}
