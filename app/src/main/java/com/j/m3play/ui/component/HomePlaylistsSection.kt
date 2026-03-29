package com.j.m3play.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.innertube.models.YTItem

@Composable
fun HomePlaylistsSection(
    title: String,
    items: List<YTItem>,
    itemContent: @Composable (YTItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HomeSectionTitle(title = title)

        LazyRow(
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal)
                .asPaddingValues()
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}
