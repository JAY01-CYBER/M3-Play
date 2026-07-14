package com.j.m3play.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j.m3play.db.entities.LyricsEntity
import com.j.m3play.db.entities.SongEntity
import com.j.m3play.lyrics.LyricsTranslationHelper
import com.j.m3play.models.MediaMetadata
import com.j.m3play.utils.rememberPreference
import com.j.m3play.constants.*

@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    songProvider: () -> SongEntity? = { null },
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    onShowOffsetDialog: () -> Unit = {}
) {
    var respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    var showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)
    val hasTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { LyricsTranslationHelper.triggerManualTranslation() }.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Lyrics Translation", modifier = Modifier.weight(1f))
                        Switch(checked = hasTranslations, onCheckedChange = { if (it) LyricsTranslationHelper.triggerManualTranslation() else LyricsTranslationHelper.triggerClearTranslations() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { respectAgentPositioning = !respectAgentPositioning }.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Respect Agent Positioning", modifier = Modifier.weight(1f))
                        Switch(checked = respectAgentPositioning, onCheckedChange = { respectAgentPositioning = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showIntervalIndicator = !showIntervalIndicator }.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Interval Indicator", modifier = Modifier.weight(1f))
                        Switch(checked = showIntervalIndicator, onCheckedChange = { showIntervalIndicator = it })
                    }
                }
            }
        }
    }
}
