/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.search.suggestions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

val SuggestionRegionSlugToName = mapOf(
    "in" to "India",
    "us" to "United States",
    "gb" to "United Kingdom",
    "ca" to "Canada",
    "au" to "Australia",
    "jp" to "Japan",
    "kr" to "South Korea",
    "br" to "Brazil",
    "fr" to "France",
    "de" to "Germany"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionRegionSheet(
    currentRegionSlug: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Select Region",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
            }
            
            val regions = SuggestionRegionSlugToName.toList()
            
            itemsIndexed(regions) { index, (slug, name) ->
                val selected = slug == currentRegionSlug
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val top by animateDpAsState(
                    if (isPressed) 36.dp else if (regions.size == 1 || index == 0) 20.dp else 4.dp, 
                    label = "top"
                )
                val bottom by animateDpAsState(
                    if (isPressed) 36.dp else if (regions.size == 1 || index == regions.size - 1) 20.dp else 4.dp, 
                    label = "bottom"
                )

                ListItem(
                    headlineContent = { Text(name) },
                    leadingContent = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                    } else null,
                    colors = if (selected) ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        leadingIconColor = MaterialTheme.colorScheme.primary
                    ) else ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clip(if (selected) CircleShape else RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                        .clickable(
                            onClick = { 
                                onRegionSelected(slug)
                                onDismiss()
                            },
                            interactionSource = interactionSource,
                            indication = null
                        )
                )
            }
        }
    }
}
