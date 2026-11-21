package com.j.m3play.ui.screens.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.Song
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.my.kizzy.rpc.KizzyRPC
import kotlinx.coroutines.*
import timber.log.Timber

enum class ActivitySource { ARTIST, ALBUM, SONG, APP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)

    LaunchedEffect(discordToken) {
        if (discordToken.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) { KizzyRPC.getUserInfo(discordToken) }.onSuccess {
                    discordUsername = it.username
                    discordName = it.name
                }
            } catch (e: Exception) { Timber.tag("DiscordSettings").w(e, "getUserInfo failed") }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(EnableDiscordRPCKey, true)

    LaunchedEffect(discordToken, discordRPC) {
        if (!discordRPC || discordToken.isBlank()) { DiscordPresenceManager.stop() }
    }

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showActivityStatusDialog by remember { mutableStateOf(false) }
    var showPlatformDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    
    val activityStatusOptions = listOf("online", "dnd", "idle", "streaming")
    val (activityStatusSelection, onActivityStatusSelectionChange) = rememberPreference(DiscordPresenceStatusKey, "online")
    
    val platformOptions = listOf("android", "desktop", "web")
    val (platformSelection, onPlatformSelectionChange) = rememberPreference(DiscordActivityPlatformKey, "desktop")
    
    val intervalOptions = listOf("20s", "50s", "1m", "5m", "Disabled")
    val (intervalSelection, onIntervalSelectionChange) = rememberPreference(stringPreferencesKey("discordPresenceIntervalPreset"), "20s")

    val (nameSource, onNameSourceChange) = rememberEnumPreference(DiscordActivityNameKey, ActivitySource.APP)
    val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(DiscordActivityDetailsKey, ActivitySource.SONG)
    val (stateSource, onStateSourceChange) = rememberEnumPreference(DiscordActivityStateKey, ActivitySource.ARTIST)
    val (activityType, onActivityTypeChange) = rememberPreference(DiscordActivityTypeKey, "LISTENING")
    var showActivityTypeDialog by remember { mutableStateOf(false) }

    var showNameSourceDialog by remember { mutableStateOf(false) }
    var showDetailsSourceDialog by remember { mutableStateOf(false) }
    var showStateSourceDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.logout_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    discordName = ""; discordToken = ""; discordUsername = ""
                    showLogoutConfirm = false
                }) { Text(stringResource(R.string.logout_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.logout_confirm_no)) }
            }
        )
    }

    if (showActivityStatusDialog) {
        EnumDialog(
            onDismiss = { showActivityStatusDialog = false },
            onSelect = { onActivityStatusSelectionChange(it); showActivityStatusDialog = false },
            title = stringResource(R.string.activity_status),
            current = activityStatusSelection,
            values = activityStatusOptions,
            valueText = { when(it) { "online" -> "Online"; "dnd" -> "Do Not Disturb"; "idle" -> "Idle"; "streaming" -> "Streaming"; else -> it } }
        )
    }

    if (showPlatformDialog) {
        EnumDialog(
            onDismiss = { showPlatformDialog = false },
            onSelect = { onPlatformSelectionChange(it); showPlatformDialog = false },
            title = stringResource(R.string.platform_status),
            current = platformSelection,
            values = platformOptions,
            valueText = { it.replaceFirstChar { char -> char.titlecase() } }
        )
    }

    if (showIntervalDialog) {
        EnumDialog(
            onDismiss = { showIntervalDialog = false },
            onSelect = { onIntervalSelectionChange(it); showIntervalDialog = false },
            title = stringResource(R.string.update_interval),
            current = intervalSelection,
            values = intervalOptions,
            valueText = { it }
        )
    }

    if (showActivityTypeDialog) {
        EnumDialog(
            onDismiss = { showActivityTypeDialog = false },
            onSelect = { onActivityTypeChange(it); showActivityTypeDialog = false },
            title = stringResource(R.string.discord_activity_type),
            current = activityType,
            values = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING"),
            valueText = { it }
        )
    }

    if (showNameSourceDialog) {
        EnumDialog(
            onDismiss = { showNameSourceDialog = false },
            onSelect = { onNameSourceChange(it); showNameSourceDialog = false },
            title = stringResource(R.string.discord_activity_name),
            current = nameSource,
            values = ActivitySource.values().toList(),
            valueText = { it.name }
        )
    }

    if (showDetailsSourceDialog) {
        EnumDialog(
            onDismiss = { showDetailsSourceDialog = false },
            onSelect = { onDetailsSourceChange(it); showDetailsSourceDialog = false },
            title = stringResource(R.string.discord_activity_details),
            current = detailsSource,
            values = ActivitySource.values().toList(),
            valueText = { it.name }
        )
    }
    
    if (showStateSourceDialog) {
        EnumDialog(
            onDismiss = { showStateSourceDialog = false },
            onSelect = { onStateSourceChange(it); showStateSourceDialog = false },
            title = stringResource(R.string.discord_activity_state),
            current = stateSource,
            values = ActivitySource.values().toList(),
            valueText = { it.name }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.discord_integration), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) { Icon(painterResource(R.drawable.more_vert), null) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.experiment_settings)) },
                            onClick = { menuExpanded = false; navController.navigate("settings/discord/experimental") },
                            leadingIcon = { Icon(painterResource(R.drawable.experiment), null) }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            if (!infoDismissed) {
                item {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(painterResource(R.drawable.info), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                            Text(stringResource(R.string.discord_information), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))
                            TextButton(onClick = { infoDismissed = true }, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                                Text(stringResource(R.string.dismiss), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.account),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.discord),
                            title = { Text(if (isLoggedIn) discordName else stringResource(R.string.not_logged_in)) },
                            description = if (discordUsername.isNotEmpty()) { { Text("@$discordUsername") } } else null,
                            trailingContent = {
                                if (isLoggedIn) OutlinedButton(onClick = { showLogoutConfirm = true }) { Text(stringResource(R.string.action_logout)) }
                                else OutlinedButton(onClick = { navController.navigate("settings/discord/login") }) { Text(stringResource(R.string.action_login)) }
                            }
                        )
                    )
                )
            }

            item {
                var isRefreshing by remember { mutableStateOf(false) }
                var showWhenPaused by rememberPreference(DiscordShowWhenPausedKey, false)
                
                Material3SettingsGroup(
                    title = stringResource(R.string.options),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.discord),
                            title = { Text(stringResource(R.string.enable_discord_rpc)) },
                            trailingContent = { Switch(checked = discordRPC, onCheckedChange = onDiscordRPCChange, enabled = isLoggedIn) },
                            onClick = { if (isLoggedIn) onDiscordRPCChange(!discordRPC) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = { Text(stringResource(R.string.refresh)) },
                            description = { Text(stringResource(R.string.description_refresh)) },
                            enabled = discordRPC,
                            trailingContent = {
                                if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                else OutlinedButton(
                                    enabled = discordRPC,
                                    onClick = {
                                        // Handled natively by discord service
                                    }
                                ) { Text(stringResource(R.string.refresh)) }
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.status),
                            title = { Text(stringResource(R.string.activity_status)) },
                            description = { Text(when(activityStatusSelection) { "online" -> "Online"; "dnd" -> "Do Not Disturb"; "idle" -> "Idle"; "streaming" -> "Streaming"; else -> activityStatusSelection }) },
                            enabled = discordRPC,
                            onClick = { showActivityStatusDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.desktop_windows),
                            title = { Text(stringResource(R.string.platform_status)) },
                            description = { Text(platformSelection.replaceFirstChar { it.titlecase() }) },
                            enabled = discordRPC,
                            onClick = { showPlatformDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.timer),
                            title = { Text(stringResource(R.string.update_interval)) },
                            description = { Text(intervalSelection) },
                            enabled = discordRPC,
                            onClick = { showIntervalDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_pause_white),
                            title = { Text(stringResource(R.string.discord_show_when_paused)) },
                            description = { Text(stringResource(R.string.discord_show_when_paused_desc)) },
                            trailingContent = { Switch(checked = showWhenPaused, onCheckedChange = { showWhenPaused = it }) },
                            enabled = discordRPC,
                            onClick = { showWhenPaused = !showWhenPaused }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.preview),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.discord_activity_name)) },
                            description = { Text(nameSource.name) },
                            onClick = { showNameSourceDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.discord_activity_details)) },
                            description = { Text(detailsSource.name) },
                            onClick = { showDetailsSourceDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.discord_activity_state)) },
                            description = { Text(stateSource.name) },
                            onClick = { showStateSourceDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.discord),
                            title = { Text(stringResource(R.string.discord_activity_type)) },
                            description = { Text(activityType) },
                            onClick = { showActivityTypeDialog = true }
                        )
                    )
                )
            }

            item {
                val (largeImageType) = rememberPreference(DiscordLargeImageTypeKey, "thumbnail")
                val (largeImageCustomUrl) = rememberPreference(DiscordLargeImageCustomUrlKey, "")
                val (smallImageType) = rememberPreference(DiscordSmallImageTypeKey, "artist")
                val (smallImageCustomUrl) = rememberPreference(DiscordSmallImageCustomUrlKey, "")
                
                val playerIsPlayingForPreview = playerConnection.player.playWhenReady && playbackState == STATE_READY
                val (button1Enabled) = rememberPreference(DiscordActivityButton1EnabledKey, true)
                val (button2Enabled) = rememberPreference(DiscordActivityButton2EnabledKey, true)

                // SAFE STATIC PLACEHOLDERS using the inner `song.song` entity
                val currentTitle = song?.song?.title ?: "Playing Music"
                val currentArtist = song?.artists?.joinToString { it.name } ?: "Unknown Artist"
                val currentAlbum = song?.song?.albumName ?: song?.album?.title ?: "Unknown Album"
                val currentDuration = (song?.song?.duration?.toLong() ?: 0L) * 1000L
                val currentThumbnail = song?.song?.thumbnailUrl

                RichPresence(
                    songThumbnailUrl = currentThumbnail,
                    currentPlaybackTimeMillis = playerConnection.player.currentPosition.coerceAtLeast(0L),
                    songTitle = currentTitle,
                    songArtist = currentArtist,
                    songAlbum = currentAlbum,
                    songDuration = currentDuration,
                    nameSource = nameSource,
                    detailsSource = detailsSource,
                    stateSource = stateSource,
                    activityType = activityType,
                    largeImageType = largeImageType,
                    largeImageCustomUrl = largeImageCustomUrl,
                    smallImageType = smallImageType,
                    smallImageCustomUrl = smallImageCustomUrl,
                    button1Enabled = button1Enabled,
                    button2Enabled = button2Enabled,
                    isPlaying = playerIsPlayingForPreview
                )
            }
        }
    }
}

@Composable
fun RichPresence(
    songThumbnailUrl: String?,
    currentPlaybackTimeMillis: Long,
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    songDuration: Long,
    nameSource: ActivitySource,
    detailsSource: ActivitySource,
    stateSource: ActivitySource,
    activityType: String,
    largeImageType: String,
    largeImageCustomUrl: String,
    smallImageType: String,
    smallImageCustomUrl: String,
    button1Enabled: Boolean,
    button2Enabled: Boolean,
    isPlaying: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.discord),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rich Presence Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    if (songThumbnailUrl != null) {
                        AsyncImage(
                            model = songThumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (songThumbnailUrl != null && smallImageType == "artist") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nameSource == ActivitySource.APP) stringResource(R.string.app_name) else songTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (detailsSource == ActivitySource.SONG) songTitle else songArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (stateSource == ActivitySource.ARTIST) songArtist else songAlbum,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SongProgressBar(
                        progress = if (songDuration > 0) currentPlaybackTimeMillis.toFloat() / songDuration.toFloat() else 0f,
                        currentTime = makeTimeString(currentPlaybackTimeMillis),
                        totalTime = makeTimeString(songDuration)
                    )
                }
            }
            
            if (button1Enabled || button2Enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (button1Enabled) {
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("Button 1", fontSize = 12.sp) }
                    }
                    if (button2Enabled) {
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("Button 2", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun SongProgressBar(progress: Float, currentTime: String, totalTime: String) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = currentTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = totalTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
