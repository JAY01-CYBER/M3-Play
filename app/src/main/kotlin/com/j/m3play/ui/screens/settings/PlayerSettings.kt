package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.ActionPromptDialog
import com.j.m3play.ui.component.ArtistSeparatorsDialog
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.M3PlaySlider
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.component.TagsManagementDialog
import com.j.m3play.ui.component.TextFieldDialog
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (playerStreamClient, onPlayerStreamClientChange) = rememberEnumPreference(PlayerStreamClientKey, defaultValue = PlayerStreamClient.ANDROID_VR)
    val (networkMetered, onNetworkMeteredChange) = rememberPreference(NetworkMeteredKey, defaultValue = true)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(PermanentShuffleKey, defaultValue = false)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = true)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(AudioOffload, defaultValue = false)
    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(SeekExtraSeconds, defaultValue = false)
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(AutoDownloadOnLikeKey, defaultValue = false)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)
    val (pauseOnDeviceMute, onPauseOnDeviceMuteChange) = rememberPreference(PauseOnDeviceMuteKey, defaultValue = false)
    val (autoStartOnBluetooth, onAutoStartOnBluetoothChange) = rememberPreference(AutoStartOnBluetoothKey, defaultValue = false)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (historyDuration, onHistoryDurationChange) = rememberPreference(HistoryDuration, defaultValue = 30f)
    val (audioCrossfadeSeconds, onAudioCrossfadeSecondsChange) = rememberPreference(AudioCrossfadeDurationKey, defaultValue = 0)
    val (artistSeparators, onArtistSeparatorsChange) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (externalDownloaderEnabled, onExternalDownloaderEnabledChange) = rememberPreference(ExternalDownloaderEnabledKey, defaultValue = false)
    val (externalDownloaderPackage, onExternalDownloaderPackageChange) = rememberPreference(ExternalDownloaderPackageKey, defaultValue = "")
    val (wakelockEnabled, onWakelockChange) = rememberPreference(WakelockKey, defaultValue = false)

    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showPlayerStreamClientDialog by remember { mutableStateOf(false) }
    var showHistoryDurationDialog by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
    var showArtistSeparatorsDialog by remember { mutableStateOf(false) }
    var showTagsManagementDialog by remember { mutableStateOf(false) }
    var showExternalDownloaderPackageDialog by remember { mutableStateOf(false) }
    
    val database = LocalDatabase.current

    // --- DIALOGS ---
    if (showAudioQualityDialog) {
        EnumDialog(
            onDismiss = { showAudioQualityDialog = false },
            onSelect = { onAudioQualityChange(it); showAudioQualityDialog = false },
            title = stringResource(R.string.audio_quality),
            current = audioQuality,
            values = AudioQuality.values().toList(),
            valueText = {
                when (it) {
                    AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_max)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )
    }

    if (showPlayerStreamClientDialog) {
        EnumDialog(
            onDismiss = { showPlayerStreamClientDialog = false },
            onSelect = { onPlayerStreamClientChange(it); showPlayerStreamClientDialog = false },
            title = stringResource(R.string.player_stream_client),
            current = playerStreamClient,
            values = listOf(PlayerStreamClient.ANDROID_VR, PlayerStreamClient.WEB_REMIX),
            valueText = {
                when (it) {
                    PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr)
                    else -> stringResource(R.string.player_stream_client_web_remix)
                }
            },
            valueDescription = {
                when (it) {
                    PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr_desc)
                    else -> stringResource(R.string.player_stream_client_web_remix_desc)
                }
            }
        )
    }

    if (showHistoryDurationDialog) {
        var sliderValue by remember { mutableFloatStateOf(historyDuration) }
        ActionPromptDialog(
            titleBar = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Text(text = stringResource(R.string.history_duration), style = MaterialTheme.typography.headlineSmall) } },
            onDismiss = { showHistoryDurationDialog = false },
            onConfirm = { onHistoryDurationChange(sliderValue); showHistoryDurationDialog = false },
            onCancel = { sliderValue = historyDuration; showHistoryDurationDialog = false },
            onReset = { sliderValue = 30f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = pluralStringResource(R.plurals.seconds, sliderValue.roundToInt(), sliderValue.roundToInt()), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    M3PlaySlider(value = sliderValue, onValueChange = { sliderValue = it }, valueRange = 15f..60f, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }

    if (showCrossfadeDialog) {
        var sliderValue by remember { mutableFloatStateOf(audioCrossfadeSeconds.toFloat()) }
        ActionPromptDialog(
            titleBar = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Text(text = stringResource(R.string.audio_crossfade_dialog_title), style = MaterialTheme.typography.headlineSmall) } },
            onDismiss = { showCrossfadeDialog = false },
            onConfirm = { onAudioCrossfadeSecondsChange(sliderValue.roundToInt().coerceIn(0, 10)); showCrossfadeDialog = false },
            onCancel = { sliderValue = audioCrossfadeSeconds.toFloat(); showCrossfadeDialog = false },
            onReset = { sliderValue = 0f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(0, 10)
                    Text(text = if (rounded == 0) stringResource(R.string.dark_theme_off) else pluralStringResource(R.plurals.seconds, rounded, rounded), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(text = stringResource(R.string.audio_crossfade_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    M3PlaySlider(value = sliderValue, onValueChange = { sliderValue = it.coerceIn(0f, 10f) }, valueRange = 0f..10f, steps = 9, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }

    if (showArtistSeparatorsDialog) {
        ArtistSeparatorsDialog(
            currentSeparators = artistSeparators,
            onDismiss = { showArtistSeparatorsDialog = false },
            onSave = { newSeparators ->
                onArtistSeparatorsChange(newSeparators)
                showArtistSeparatorsDialog = false
            }
        )
    }

    if (showTagsManagementDialog) {
        TagsManagementDialog(
            database = database,
            onDismiss = { showTagsManagementDialog = false }
        )
    }

    if (showExternalDownloaderPackageDialog) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(externalDownloaderPackage),
            onDone = { pkg ->
                onExternalDownloaderPackageChange(pkg)
                showExternalDownloaderPackageDialog = false
            },
            onDismiss = { showExternalDownloaderPackageDialog = false },
            singleLine = true,
            maxLines = 1,
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_and_audio), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) { Icon(painterResource(R.drawable.arrow_back), null) }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.player),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.audio_quality)) },
                            description = {
                                Text(
                                    when (audioQuality) {
                                        AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_max)
                                        AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                        AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                        AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                                    }
                                )
                            },
                            onClick = { showAudioQualityDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.integration),
                            title = { Text(stringResource(R.string.player_stream_client)) },
                            description = {
                                Text(
                                    when (playerStreamClient) {
                                        PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr)
                                        else -> stringResource(R.string.player_stream_client_web_remix)
                                    }
                                )
                            },
                            onClick = { showPlayerStreamClientDialog = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Playback & Audio",
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.android_cell),
                            title = { Text(stringResource(R.string.network_metered_title)) },
                            description = { Text(stringResource(R.string.network_metered_description)) },
                            trailingContent = { Switch(checked = networkMetered, onCheckedChange = onNetworkMeteredChange, thumbContent = { Icon(painterResource(if (networkMetered) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onNetworkMeteredChange(!networkMetered) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.history),
                            title = { Text(stringResource(R.string.history_duration)) },
                            description = { Text(historyDuration.roundToInt().toString()) },
                            onClick = { showHistoryDurationDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.audio_crossfade_title)) },
                            description = { Text(if (audioCrossfadeSeconds <= 0) stringResource(R.string.dark_theme_off) else pluralStringResource(R.plurals.seconds, audioCrossfadeSeconds, audioCrossfadeSeconds)) },
                            enabled = !audioOffload,
                            onClick = { showCrossfadeDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.fast_forward),
                            title = { Text(stringResource(R.string.skip_silence)) },
                            enabled = !audioOffload,
                            trailingContent = { Switch(checked = skipSilence, onCheckedChange = onSkipSilenceChange, enabled = !audioOffload, thumbContent = { Icon(painterResource(if (skipSilence) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { if (!audioOffload) onSkipSilenceChange(!skipSilence) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.volume_up),
                            title = { Text(stringResource(R.string.audio_normalization)) },
                            trailingContent = { Switch(checked = audioNormalization, onCheckedChange = onAudioNormalizationChange, thumbContent = { Icon(painterResource(if (audioNormalization) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onAudioNormalizationChange(!audioNormalization) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.audio_offload)) },
                            description = { Text(stringResource(R.string.audio_offload_desc)) },
                            trailingContent = { Switch(checked = audioOffload, onCheckedChange = { onAudioOffloadChange(it); if (it) onSkipSilenceChange(false) }, thumbContent = { Icon(painterResource(if (audioOffload) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { val enabled = !audioOffload; onAudioOffloadChange(enabled); if (enabled) onSkipSilenceChange(false) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.arrow_forward),
                            title = { Text(stringResource(R.string.seek_seconds_addup)) },
                            description = { Text(stringResource(R.string.seek_seconds_addup_description)) },
                            trailingContent = { Switch(checked = seekExtraSeconds, onCheckedChange = onSeekExtraSeconds, thumbContent = { Icon(painterResource(if (seekExtraSeconds) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onSeekExtraSeconds(!seekExtraSeconds) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.volume_off),
                            title = { Text(stringResource(R.string.pause_on_device_mute)) },
                            description = { Text(stringResource(R.string.pause_on_device_mute_desc)) },
                            trailingContent = { Switch(checked = pauseOnDeviceMute, onCheckedChange = onPauseOnDeviceMuteChange, thumbContent = { Icon(painterResource(if (pauseOnDeviceMute) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onPauseOnDeviceMuteChange(!pauseOnDeviceMute) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.bluetooth),
                            title = { Text(stringResource(R.string.auto_start_on_bluetooth)) },
                            description = { Text(stringResource(R.string.auto_start_on_bluetooth_desc)) },
                            trailingContent = { Switch(checked = autoStartOnBluetooth, onCheckedChange = onAutoStartOnBluetoothChange, thumbContent = { Icon(painterResource(if (autoStartOnBluetooth) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onAutoStartOnBluetoothChange(!autoStartOnBluetooth) }
                        ))
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.queue),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.queue_music),
                            title = { Text(stringResource(R.string.persistent_queue)) },
                            description = { Text(stringResource(R.string.persistent_queue_desc)) },
                            trailingContent = { Switch(checked = persistentQueue, onCheckedChange = onPersistentQueueChange, thumbContent = { Icon(painterResource(if (persistentQueue) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onPersistentQueueChange(!persistentQueue) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.shuffle),
                            title = { Text(stringResource(R.string.permanent_shuffle)) },
                            description = { Text(stringResource(R.string.permanent_shuffle_desc)) },
                            trailingContent = { Switch(checked = permanentShuffle, onCheckedChange = onPermanentShuffleChange, thumbContent = { Icon(painterResource(if (permanentShuffle) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onPermanentShuffleChange(!permanentShuffle) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.download),
                            title = { Text(stringResource(R.string.auto_download_on_like)) },
                            description = { Text(stringResource(R.string.auto_download_on_like_desc)) },
                            trailingContent = { Switch(checked = autoDownloadOnLike, onCheckedChange = onAutoDownloadOnLikeChange, thumbContent = { Icon(painterResource(if (autoDownloadOnLike) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onAutoDownloadOnLikeChange(!autoDownloadOnLike) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.skip_next),
                            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                            description = { Text(stringResource(R.string.auto_skip_next_on_error_desc)) },
                            trailingContent = { Switch(checked = autoSkipNextOnError, onCheckedChange = onAutoSkipNextOnErrorChange, thumbContent = { Icon(painterResource(if (autoSkipNextOnError) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onAutoSkipNextOnErrorChange(!autoSkipNextOnError) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.misc),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.clear_all),
                            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                            trailingContent = { Switch(checked = stopMusicOnTaskClear, onCheckedChange = onStopMusicOnTaskClearChange, thumbContent = { Icon(painterResource(if (stopMusicOnTaskClear) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.bolt),
                            title = { Text(stringResource(R.string.wakelock)) },
                            description = { Text(stringResource(R.string.wakelock_desc)) },
                            trailingContent = { Switch(checked = wakelockEnabled, onCheckedChange = onWakelockChange, thumbContent = { Icon(painterResource(if (wakelockEnabled) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onWakelockChange(!wakelockEnabled) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.artist),
                            title = { Text(stringResource(R.string.artist_separators)) },
                            description = { Text(artistSeparators.map { "\"$it\"" }.joinToString("  ")) },
                            onClick = { showArtistSeparatorsDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.style),
                            title = { Text(stringResource(R.string.manage_playlist_tags)) },
                            description = { Text(stringResource(R.string.manage_playlist_tags_desc)) },
                            onClick = { showTagsManagementDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.download),
                            title = { Text(stringResource(R.string.external_downloader)) },
                            description = { Text(stringResource(R.string.external_downloader_desc)) },
                            trailingContent = { Switch(checked = externalDownloaderEnabled, onCheckedChange = onExternalDownloaderEnabledChange, thumbContent = { Icon(painterResource(if (externalDownloaderEnabled) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onExternalDownloaderEnabledChange(!externalDownloaderEnabled) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.integration),
                            title = { Text(stringResource(R.string.external_downloader_package)) },
                            description = { Text(externalDownloaderPackage.ifEmpty { stringResource(R.string.external_downloader_package_desc) }) },
                            enabled = externalDownloaderEnabled,
                            onClick = { showExternalDownloaderPackageDialog = true }
                        )
                    )
                )
            }
        }
    }
}
