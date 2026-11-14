package com.j.m3play.ui.screens.settings

import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.common.Player
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.GlobalLog
import com.j.m3play.utils.LogEntry
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettings(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val (showDevDebug, onShowDevDebugChange) = rememberPreference(key = booleanPreferencesKey("dev_show_discord_debug"), defaultValue = false)
    val (showNerdStats, onShowNerdStatsChange) = rememberPreference(key = booleanPreferencesKey("dev_show_nerd_stats"), defaultValue = false)
    val (showCodecOnPlayer, onShowCodecOnPlayerChange) = rememberPreference(key = booleanPreferencesKey("show_codec_on_player"), defaultValue = false)

    val playerConnection = LocalPlayerConnection.current

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.experiment_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Material3SettingsGroup(
                title = stringResource(R.string.experimental_features),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.discord),
                        title = { Text(stringResource(R.string.show_discord_debug_ui)) },
                        description = { Text(stringResource(R.string.enable_discord_debug_lines)) },
                        trailingContent = {
                            Switch(
                                checked = showDevDebug,
                                onCheckedChange = onShowDevDebugChange,
                                thumbContent = { Icon(painterResource(if (showDevDebug) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            )
                        },
                        onClick = { onShowDevDebugChange(!showDevDebug) }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.stats),
                        title = { Text(stringResource(R.string.show_nerd_stats)) },
                        description = { Text(stringResource(R.string.description_show_nerd_stats)) },
                        trailingContent = {
                            Switch(
                                checked = showNerdStats,
                                onCheckedChange = onShowNerdStatsChange,
                                thumbContent = { Icon(painterResource(if (showNerdStats) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            )
                        },
                        onClick = { onShowNerdStatsChange(!showNerdStats) }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.graphic_eq),
                        title = { Text(stringResource(R.string.display_codec_on_player)) },
                        description = { Text(stringResource(R.string.description_display_codec_on_player)) },
                        trailingContent = {
                            Switch(
                                checked = showCodecOnPlayer,
                                onCheckedChange = onShowCodecOnPlayerChange,
                                thumbContent = { Icon(painterResource(if (showCodecOnPlayer) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            )
                        },
                        onClick = { onShowCodecOnPlayerChange(!showCodecOnPlayer) }
                    )
                )
            )

            AnimatedVisibility(visible = showDevDebug, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DiscordDebugSection() // Keep your custom Discord section as is
                }
            }

            AnimatedVisibility(visible = showNerdStats && playerConnection != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NerdStatsSection(playerConnection = playerConnection) // Keep your NerdStats as is
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Ensure you paste the existing `DiscordDebugSection`, `LogViewerPanel`, `NerdStatsSection`, etc. from your original DebugSettings.kt below this point.
