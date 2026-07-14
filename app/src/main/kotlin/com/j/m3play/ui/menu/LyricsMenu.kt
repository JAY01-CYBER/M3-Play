package com.j.m3play.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j.m3play.R
import com.j.m3play.db.entities.LyricsEntity
import com.j.m3play.db.entities.SongEntity
import com.j.m3play.lyrics.LyricsResyncHelper
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
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    var respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    var showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)

    val hasTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsStateWithLifecycle()
    var isChecked by remember { mutableStateOf(songProvider()?.romanizeLyrics ?: true) }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    
                    // Option 1: AI Translation
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { LyricsTranslationHelper.triggerManualTranslation() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.translate), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("AI Lyrics Translation", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = hasTranslations, onCheckedChange = { if (it) LyricsTranslationHelper.triggerManualTranslation() else LyricsTranslationHelper.triggerClearTranslations() })
                    }

                    // Option 2: Respect Agent
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { respectAgentPositioning = !respectAgentPositioning }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.lyrics), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Respect Agent Positioning", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = respectAgentPositioning, onCheckedChange = { respectAgentPositioning = it })
                    }

                    // Option 3: Interval Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showIntervalIndicator = !showIntervalIndicator }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.lyrics), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Show Interval Indicator", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = showIntervalIndicator, onCheckedChange = { showIntervalIndicator = it })
                    }

                    // Option 4: Romanize
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isChecked = !isChecked }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.language_korean_latin), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Romanize Current Track", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = isChecked, onCheckedChange = { isChecked = it })
                    }
                }
            }
        }
    }
}
