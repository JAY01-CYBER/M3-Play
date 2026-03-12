package com.j.m3play.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.designsystem.ElevationOverlays

@Composable
fun M3BottomNav(selectedItem: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        modifier = Modifier.height(74.dp),
        containerColor = ElevationOverlays.surface(0.6f),
        tonalElevation = 0.dp,
    ) {
        val items = listOf("Home", "Search", "Library")
        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic)
        items.forEachIndexed { index, item ->
            val tint = animateColorAsState(
                if (selectedItem == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabTint",
            )
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item, tint = tint.value) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { onTabSelected(index) },
            )
        }
    }
}
