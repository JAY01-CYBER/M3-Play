package com.j.m3play.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.j.m3play.lyrics.LyricsTranslationHelper
import com.j.m3play.constants.LyricsPosition

@Composable
internal fun LyricsTranslationHeader(
    status: LyricsTranslationHelper.TranslationStatus,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = status !is LyricsTranslationHelper.TranslationStatus.Idle, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        when (status) {
            is LyricsTranslationHelper.TranslationStatus.Translating -> {
                TranslationCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Translating lyrics...", style = MaterialTheme.typography.labelMedium)
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Error -> {
                TranslationCard(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Text(status.message, style = MaterialTheme.typography.labelMedium)
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Success -> {
                TranslationCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                    Text("Lyrics translated", style = MaterialTheme.typography.labelMedium)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun TranslationCard(containerColor: Color, contentColor: Color, content: @Composable RowScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun LyricsActionOverlay(isAutoScrollEnabled: Boolean, isSynced: Boolean, isSelectionModeActive: Boolean, anySelected: Boolean, onSyncClick: () -> Unit, onCancelSelection: () -> Unit, onShareSelection: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(bottom = 16.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            FilledTonalButton(onClick = onSyncClick) {
                Text("Auto Scroll")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsShareDialog(txt: String, title: String, arts: String, songId: String, onDismiss: () -> Unit, onShareAsImage: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card { Column(Modifier.padding(20.dp)) { Text("Share Lyrics"); Button(onClick = onShareAsImage) { Text("Share as Image") }; Button(onClick = onDismiss) { Text("Cancel") } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsColorPickerDialog(
    txt: String, title: String, arts: String, thumbnailUrl: String?, lyricsTextPosition: LyricsPosition,
    onDismiss: () -> Unit, onShare: (backgroundColor: Color, textColor: Color, secondaryTextColor: Color, style: Int) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card { Column(Modifier.padding(20.dp)) { Text("Color Picker"); Button(onClick = { onShare(Color.Black, Color.White, Color.Gray, 0) }) { Text("Share") }; Button(onClick = onDismiss) { Text("Cancel") } } }
    }
}
