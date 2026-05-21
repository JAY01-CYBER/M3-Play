/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.Song
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.component.PreferenceGroupTitle
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.ui.component.ListItem
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.TranslatorLanguages
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import com.my.kizzy.rpc.KizzyRPC
import timber.log.Timber
import com.j.m3play.utils.DiscordRPC
import com.j.m3play.utils.getPresenceIntervalMillis
import kotlinx.coroutines.*
import com.j.m3play.utils.ArtworkStorage

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
    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    val lastRpcStartTime = DiscordPresenceManager.lastRpcStartTime
    val lastRpcEndTime = DiscordPresenceManager.lastRpcEndTime
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    KizzyRPC.getUserInfo(token)
                }.onSuccess {
                    discordUsername = it.username
                    discordName = it.name
                }
            } catch (e: Exception) {
                Timber.tag("DiscordSettings").w(e, "getUserInfo failed")
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    LaunchedEffect(discordToken, discordRPC) {
        if (discordRPC && discordToken.isNotBlank()) {
            Timber.tag("DiscordSettings").d("RPC enabled with token, MusicService will handle start")
        } else {
            Timber.tag("DiscordSettings").d("RPC disabled or no token, stopping manager")
            DiscordPresenceManager.stop()
        }
    }

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        AnimatedVisibility(visible = !infoDismissed) {
            Card(
                shape = RoundedCornerShape(32.dp), // Premium Large Radius
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.discord_information),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    TextButton(
                        onClick = { infoDismissed = true },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.dismiss), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.account))

    var showLogoutConfirm by remember { mutableStateOf(false) }

    PreferenceEntry(
            title = {
                Text(
                    text = if (isLoggedIn) discordName else stringResource(R.string.not_logged_in),
                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                )
            },
            description = if (discordUsername.isNotEmpty()) "@$discordUsername" else null,
            icon = { Icon(painterResource(R.drawable.discord), null) },
            trailingContent = {
                if (isLoggedIn) {
                        OutlinedButton(onClick = { showLogoutConfirm = true }) { Text(stringResource(R.string.action_logout)) }
                    } else {
                    OutlinedButton(onClick = {
                        navController.navigate("settings/discord/login")
                    }) { Text(stringResource(R.string.action_login)) }
                }
            },
        )

            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text(stringResource(R.string.logout_confirm_title)) },
                    text = { Text(stringResource(R.string.logout_confirm_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            discordName = ""
                            discordToken = ""
                            discordUsername = ""
                            showLogoutConfirm = false
                        }) { Text(stringResource(R.string.logout_confirm_yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.logout_confirm_no)) }
                    }
                )
            }

        PreferenceGroupTitle(title = stringResource(R.string.options))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_discord_rpc)) },
            checked = discordRPC,
            onCheckedChange = onDiscordRPCChange,
            isEnabled = isLoggedIn,
        )

        val imageOptions = listOf("thumbnail", "artist", "appicon", "custom")
        val smallImageOptions = listOf("thumbnail", "artist", "appicon", "custom", "dontshow")

        val (largeImageType, onLargeImageTypeChange) = rememberPreference(
            key = DiscordLargeImageTypeKey,
            defaultValue = "thumbnail"
     )
        val (largeImageCustomUrl, onLargeImageCustomUrlChange) = rememberPreference(
            key = DiscordLargeImageCustomUrlKey,
            defaultValue = ""
     )
        val (smallImageType, onSmallImageTypeChange) = rememberPreference(
            key = DiscordSmallImageTypeKey,
            defaultValue = "artist"
     )
        val (smallImageCustomUrl, onSmallImageCustomUrlChange) = rememberPreference(
            key = DiscordSmallImageCustomUrlKey,
            defaultValue = ""
     )

        LaunchedEffect(largeImageType, smallImageType) {
            ArtworkStorage.removeBySongId(context, song?.song?.id ?: return@LaunchedEffect)
        }

        var isRefreshing by remember { mutableStateOf(false) }

        PreferenceEntry(
        title = { Text(stringResource(R.string.refresh)) },
        description = stringResource(R.string.description_refresh),
        icon = { Icon(painterResource(R.drawable.update), null) },
        isEnabled = discordRPC,
        trailingContent = {
           if (isRefreshing) {
                CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
        } else {
            OutlinedButton(
                enabled = discordRPC,
                onClick = {
                    coroutineScope.launch {
                       isRefreshing = true
                       val start = System.currentTimeMillis()

                       val success = DiscordPresenceManager.updatePresence(
                           context = context,
                           token = discordToken,
                           song = song,
                           positionMs = playerConnection.player.currentPosition,
                           isPaused = !playerConnection.player.isPlaying,
                       )
                       isRefreshing = false
                        withContext(Dispatchers.Main) {
                            if (success) {
                                snackbarHostState.showSnackbar("Refreshed!")
                            } else {
                                snackbarHostState.showSnackbar("Refresh failed")
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
)

        val activityStatus = listOf("online", "dnd", "idle", "streaming")
        val (activityStatusSelection, onActivityStatusSelectionChange) = rememberPreference(
            key = DiscordPresenceStatusKey,
            defaultValue = "online"
        )

        var activityStatusExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = activityStatusExpanded, onExpandedChange = { activityStatusExpanded = it }) {
            TextField(
                value = when (activityStatusSelection) {
                    "online" -> "Online"
                    "dnd" -> "Do Not Disturb"
                    "idle" -> "Idle"
                    "streaming" -> "Streaming"
                    else -> "Online"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.activity_status)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityStatusExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .pointerInput(Unit) { detectTapGestures { activityStatusExpanded = true } },
                leadingIcon = { Icon(painterResource(R.drawable.status), null) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            ExposedDropdownMenu(expanded = activityStatusExpanded, onDismissRequest = { activityStatusExpanded = false }) {
                activityStatus.forEach { opt ->
                    val display = when (opt) {
                        "online" -> "Online"
                        "dnd" -> "Do Not Disturb"
                        "idle" -> "Idle"
                        "streaming" -> "Streaming"
                        else -> opt
                    }
                    DropdownMenuItem(text = { Text(display) }, onClick = {
                        onActivityStatusSelectionChange(opt)
                        activityStatusExpanded = false
                    })
                }
            }
        }

        val platformOptions = listOf("android", "desktop", "web")
        val (platformSelection, onPlatformSelectionChange) = rememberPreference(
            key = DiscordActivityPlatformKey,
            defaultValue = "desktop"
        )

        var platformExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = platformExpanded, onExpandedChange = { platformExpanded = it }) {
            TextField(
                value = platformSelection.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.platform_status)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .pointerInput(Unit) { detectTapGestures { platformExpanded = true } },
                leadingIcon = { Icon(painterResource(R.drawable.desktop_windows), null) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            ExposedDropdownMenu(expanded = platformExpanded, onDismissRequest = { platformExpanded = false }) {
                platformOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }, onClick = {
                        onPlatformSelectionChange(opt)
                        platformExpanded = false
                    })
                }
            }
        }

       val intervalOptions = listOf("20s", "50s", "1m", "5m", "Custom", "Disabled")
       val (intervalSelection, onIntervalSelectionChange) = rememberPreference(
           key = stringPreferencesKey("discordPresenceIntervalPreset"),
           defaultValue = "20s"
        )

        var intervalExpanded by remember { mutableStateOf(false) }

ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = it }) {
    TextField(
        value = intervalSelection,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.update_interval)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .pointerInput(Unit) { detectTapGestures { intervalExpanded = true } },
        leadingIcon = { Icon(painterResource(R.drawable.timer), null) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
    ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
        intervalOptions.forEach { opt ->
            DropdownMenuItem(text = { Text(opt) }, onClick = {
                onIntervalSelectionChange(opt)
                intervalExpanded = false
            })
        }
    }
}

if (intervalSelection == "Custom") {
    val (customValue, onCustomValueChange) = rememberPreference(
        key = DiscordPresenceIntervalValueKey,
        defaultValue = 30
    )
    val (customUnit, onCustomUnitChange) = rememberPreference(
        key = DiscordPresenceIntervalUnitKey,
        defaultValue = "S"
    )

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = customValue.toString(),
            onValueChange = { text ->
                val number = text.toIntOrNull()
                if (number != null) {
                    if (customUnit == "S" && number < 30) {
                        onCustomValueChange(30)
                    } else {
                        onCustomValueChange(number)
                    }
                }
            },
            label = { Text("Value") },
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        var unitExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
            OutlinedTextField(
                value = when (customUnit) {
                    "S" -> "Seconds"
                    "M" -> "Minutes"
                    "H" -> "Hours"
                    else -> "Seconds"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .weight(1f)
                    .pointerInput(Unit) { detectTapGestures { unitExpanded = true } },
                shape = RoundedCornerShape(16.dp)
            )
            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                listOf("S" to "Seconds", "M" to "Minutes", "H" to "Hours").forEach { (code, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        if (code == "S" && customValue < 30) {
                            onCustomValueChange(30)
                        }
                        onCustomUnitChange(code)
                        unitExpanded = false
                    })
                }
            }
        }
    }
}

        PreferenceGroupTitle(title = stringResource(R.string.preview))

        val (nameSource, onNameSourceChange) = rememberEnumPreference(
            key = DiscordActivityNameKey, defaultValue = ActivitySource.APP
        )
        val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(
            key = DiscordActivityDetailsKey, defaultValue = ActivitySource.SONG
        )
        val (stateSource, onStateSourceChange) = rememberEnumPreference(
            key = DiscordActivityStateKey, defaultValue = ActivitySource.ARTIST
        )

        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_name),
            iconRes = R.drawable.text_fields,
            selected = nameSource,
            onChange = onNameSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_details),
            iconRes = R.drawable.text_fields,
            selected = detailsSource,
            onChange = onDetailsSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_state),
            iconRes = R.drawable.text_fields,
            selected = stateSource,
            onChange = onStateSourceChange
        )

        val (button1Label, onButton1LabelChange) = rememberPreference(
            key = DiscordActivityButton1LabelKey,
            defaultValue = "Listen on YouTube Music"
        )
        val (button1Enabled, onButton1EnabledChange) = rememberPreference(
            key = DiscordActivityButton1EnabledKey,
            defaultValue = true
        )
        val (button2Label, onButton2LabelChange) = rememberPreference(
            key = DiscordActivityButton2LabelKey,
            defaultValue = "Go to M3Play"
        )
        val (button2Enabled, onButton2EnabledChange) = rememberPreference(
            key = DiscordActivityButton2EnabledKey,
            defaultValue = true
        )

        val (activityType, onActivityTypeChange) = rememberPreference(
            key = DiscordActivityTypeKey,
            defaultValue = "LISTENING"
        )
        val activityOptions = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")

        var showWhenPaused by rememberPreference(
        key = DiscordShowWhenPausedKey,
        defaultValue = false
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.discord_show_when_paused)) },
            description = stringResource(R.string.discord_show_when_paused_desc),
            icon = { Icon(painterResource(R.drawable.ic_pause_white), null) },
            checked = showWhenPaused,
            onCheckedChange = { showWhenPaused = it }
        )

        var activityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            TextField(
                value = activityType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.discord_activity_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .pointerInput(Unit) { detectTapGestures { activityExpanded = true } }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                leadingIcon = { Icon(painterResource(R.drawable.discord), null) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                activityOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        onActivityTypeChange(opt)
                        activityExpanded = false
                    })
                }
            }
        }

    PreferenceGroupTitle(title = stringResource(R.string.discord_image_options))

        val largeTextOptions = listOf("song", "artist", "album", "app", "custom", "dontshow")

        val (largeTextSource, onLargeTextSourceChange) = rememberPreference(
            key = DiscordLargeTextSourceKey,
            defaultValue = "album"
     )
        val (largeTextCustom, onLargeTextCustomChange) = rememberPreference(
            key = DiscordLargeTextCustomKey,
            defaultValue = ""
     )

var largeImageExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = largeImageExpanded, onExpandedChange = { largeImageExpanded = it }) {
    TextField(
        value = largeImageType,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.large_image)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeImageExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { largeImageExpanded = true } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        leadingIcon = { Icon(painterResource(R.drawable.image), null) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
    ExposedDropdownMenu(expanded = largeImageExpanded, onDismissRequest = { largeImageExpanded = false }) {
        imageOptions.forEach { opt ->
            val display = when (opt) {
                "appicon" -> "App Icon"
                else -> opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            DropdownMenuItem(text = { Text(display) }, onClick = {
                onLargeImageTypeChange(opt)
                largeImageExpanded = false
            })
        }
    }
}
if (largeImageType == "custom") {
    EditablePreference(
        title = stringResource(R.string.large_image_custom_url),
        iconRes = R.drawable.link,
        value = largeImageCustomUrl,
        defaultValue = "",
        onValueChange = onLargeImageCustomUrlChange,
    )
}

var largeTextExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = largeTextExpanded, onExpandedChange = { largeTextExpanded = it }) {
    TextField(
        value = largeTextSource,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.large_text)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeTextExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { largeTextExpanded = true } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        leadingIcon = { Icon(painterResource(R.drawable.text_fields), null) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
    ExposedDropdownMenu(expanded = largeTextExpanded, onDismissRequest = { largeTextExpanded = false }) {
        largeTextOptions.forEach { opt ->
            val display = when (opt) {
                "song" -> "Song name"
                "artist" -> "Artist name"
                "album" -> "Album name"
                "app" -> "App name"
                "custom" -> "Custom text"
                "dontshow" -> "Don't show"
                else -> opt
            }
            DropdownMenuItem(
                text = { Text(display) },
                onClick = {
                    onLargeTextSourceChange(opt)
                    largeTextExpanded = false
                }
            )
        }
    }
}

if (largeTextSource == "custom") {
    EditablePreference(
        title = stringResource(R.string.custom_large_text),
        iconRes = R.drawable.text_fields,
        value = largeTextCustom,
        defaultValue = "",
        onValueChange = onLargeTextCustomChange
    )
}

var smallImageExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = smallImageExpanded, onExpandedChange = { smallImageExpanded = it }) {
    TextField(
        value = smallImageType,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.small_image)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = smallImageExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { smallImageExpanded = true } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        leadingIcon = { Icon(painterResource(R.drawable.image), null) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
    ExposedDropdownMenu(expanded = smallImageExpanded, onDismissRequest = { smallImageExpanded = false }) {
        smallImageOptions.forEach { opt ->
            val display = when (opt) {
                "appicon" -> "App Icon"
                "dontshow" -> "Don't show"
                else -> opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            DropdownMenuItem(text = { Text(display) }, onClick = {
                onSmallImageTypeChange(opt)
                smallImageExpanded = false
            })
        }
    }
}
if (smallImageType == "custom") {
    EditablePreference(
        title = stringResource(R.string.small_image_custom_url),
        iconRes = R.drawable.link,
        value = smallImageCustomUrl,
        defaultValue = "",
        onValueChange = onSmallImageCustomUrlChange,
    )
}

    val playerIsPlayingForPreview = playerConnection.player.playWhenReady && playbackState == STATE_READY

    RichPresence(
        song,
        currentPlaybackTimeMillis = playerConnection.player.currentPosition,
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
        isPlaying = playerConnection.player.isPlaying
    )
    Spacer(Modifier.height(32.dp))
}

    TopAppBar(
        title = { Text(stringResource(R.string.discord_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        actions = {
            var threeDotMenuExpanded by remember { mutableStateOf(false) }

            IconButton(onClick = { threeDotMenuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = threeDotMenuExpanded,
                onDismissRequest = { threeDotMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.experiment_settings)) },
                    onClick = {
                        threeDotMenuExpanded = false
                        navController.navigate("settings/discord/experimental")
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.experiment),
                            contentDescription = null
                        )
                    }
                )
            }
        }
      )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySourceDropdown(
    title: String,
    iconRes: Int,
    selected: ActivitySource,
    onChange: (ActivitySource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        TextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(painterResource(iconRes), null) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ActivitySource.values().forEach { source ->
                DropdownMenuItem(
                    text = { Text(source.name) },
                    onClick = {
                        onChange(source)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EditablePreference(
    title: String,
    iconRes: Int,
    value: String,
    defaultValue: String,
    onValueChange: (String) -> Unit,
    description: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    PreferenceEntry(
        title = { Text(title) },
        description = description ?: if (value.isEmpty()) defaultValue else value,
        icon = { Icon(painterResource(iconRes), null) },
        trailingContent = {
            TextButton(onClick = { showDialog = true }) { Text("Edit") }
        }
    )
    if (showDialog) {
        var text by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(if (text.isBlank()) "" else text)
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            title = { Text("Edit $title") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(defaultValue) },
                    singleLine = true,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        )
    }
}

@Composable
fun RichPresence(
    song: Song?,
    currentPlaybackTimeMillis: Long = 0L,
    nameSource: ActivitySource = ActivitySource.APP,
    detailsSource: ActivitySource = ActivitySource.SONG,
    stateSource: ActivitySource = ActivitySource.ARTIST,
    activityType: String = "LISTENING",
    largeImageType: String = "thumbnail",
    largeImageCustomUrl: String = "",
    smallImageType: String = "artist",
    smallImageCustomUrl: String = "",
    button1Enabled: Boolean = true,
    button2Enabled: Boolean = true,
    isPlaying: Boolean = false,
) {
    val context = LocalContext.current

    fun resolveUrl(source: String, song: Song?, custom: String): String? {
    return when (source.lowercase()) {
        "songurl" -> song?.id?.let { "https://music.youtube.com/watch?v=$it" }
        "artisturl" -> song?.artists?.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
        "albumurl" -> song?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        "custom" -> if (custom.isNotBlank()) custom else null
        else -> null
    }
   }

   val (button1Label) = rememberPreference(DiscordActivityButton1LabelKey, "Listen on YouTube Music")
   val (button2Label) = rememberPreference(DiscordActivityButton2LabelKey, "Go to M3Play")
   val (button1UrlSource) = rememberPreference(DiscordActivityButton1UrlSourceKey, "songurl")
   val (button1CustomUrl) = rememberPreference(DiscordActivityButton1CustomUrlKey, "")
   val (button2UrlSource) = rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
   val (button2CustomUrl) = rememberPreference(DiscordActivityButton2CustomUrlKey, "https://github.com/JAY01-CYBER/M3-Play")
   val (largeTextSource) = rememberPreference(DiscordLargeTextSourceKey, "album")
   val (largeTextCustom) = rememberPreference(DiscordLargeTextCustomKey, "")

    val previewLargeText = when (largeTextSource) {
    "song" -> song?.song?.title ?: "Song name"
    "artist" -> song?.artists?.firstOrNull()?.name ?: "Artist"
    "album" -> song?.song?.albumName ?: song?.album?.title ?: "Album"
    "app" -> stringResource(R.string.app_name)
    "custom" -> largeTextCustom.ifBlank { "Custom text" }
    "dontshow" -> null
    else -> song?.song?.albumName ?: song?.album?.title
    }
    val resolvedButton1Url = resolveUrl(button1UrlSource, song, button1CustomUrl)
    val resolvedButton2Url = resolveUrl(button2UrlSource, song, button2CustomUrl)
    val activityVerb = when (activityType.uppercase()) {
    "PLAYING" -> "Playing"
    "LISTENING" -> "Listening to"
    "WATCHING" -> "Watching"
    "STREAMING" -> "Streaming"
    "COMPETING" -> "Competing in"
    else -> activityType.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
       }
    }

    val previewTitle = when (nameSource) {
    ActivitySource.ARTIST -> "$activityVerb ${song?.artists?.firstOrNull()?.name ?: "Artist"}"
    ActivitySource.ALBUM -> "$activityVerb ${song?.album?.title ?: song?.song?.albumName ?: "Album"}"
    ActivitySource.SONG -> "$activityVerb ${song?.song?.title ?: "Song"}"
    ActivitySource.APP -> "$activityVerb M3Play"
   }

    PreferenceEntry(
        title = {
            Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh, // Premium Color
                shape = RoundedCornerShape(32.dp), // Premium Radius
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp), // More breathing room
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = previewTitle,
                        style = MaterialTheme.typography.titleMedium, // Better typography
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(108.dp)) {
                            AsyncImage(
                                model = when (largeImageType) {
                                    "thumbnail" -> song?.song?.thumbnailUrl
                                    "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl
                                    "appicon" -> "https://raw.githubusercontent.com/JAY01-CYBER/M3-Play/main/fastlane/metadata/android/en-US/images/icon.png"
                                    "custom" -> largeImageCustomUrl.ifBlank { song?.song?.thumbnailUrl }
                                    else -> song?.song?.thumbnailUrl
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(20.dp)) // Pixel Squircle look
                                    .align(Alignment.TopStart)
                                    .run {
                                        if (song == null) border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            RoundedCornerShape(20.dp)
                                        ) else this
                                    },
                            )
                            val songThumb = song?.song?.thumbnailUrl
                            val artistThumb = song?.artists?.firstOrNull()?.thumbnailUrl

                            val smallModel = when (smallImageType.lowercase()) {
                                "thumbnail" -> songThumb
                                "artist" -> artistThumb
                                "appicon" -> "https://raw.githubusercontent.com/JAY01-CYBER/M3-Play/main/fastlane/metadata/android/en-US/images/icon.png"
                                "custom" -> smallImageCustomUrl.takeIf { it.isNotBlank() } ?: songThumb
                                "dontshow", "none" -> null
                                else -> artistThumb
                            }
                            smallModel?.let {
                                Box(
                                    modifier = Modifier
                                        .border(3.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                        .padding(2.dp)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    AsyncImage(
                                        model = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp).clip(CircleShape),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        ) {
                            Text(
                                text = song?.song?.title ?: "Song Title",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val previewState = when (stateSource) {
                                ActivitySource.ARTIST -> song?.artists?.joinToString { it.name } ?: "Artist"
                                ActivitySource.ALBUM -> song?.song?.albumName ?: song?.album?.title ?: song?.song?.title ?: "Unknown Album"
                                ActivitySource.SONG -> song?.song?.title ?: "Song"
                                ActivitySource.APP -> stringResource(R.string.app_name)
                            }

                            Text(
                                text = previewState,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            previewLargeText?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (song != null) {
                                SongProgressBar(
                                    currentTimeMillis = currentPlaybackTimeMillis,
                                    durationMillis = song.song.duration * 1000L,
                                    isPlaying = isPlaying,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedVisibility(visible = button1Enabled && button1Label.isNotBlank()) {
                        FilledTonalButton(
                            enabled = !resolvedButton1Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton1Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                         }
                     },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(button1Label.ifBlank { "Listen on YouTube Music" }, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (button1Enabled && button1Label.isNotBlank() && button2Enabled && button2Label.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    AnimatedVisibility(visible = button2Enabled && button2Label.isNotBlank()) {
                        FilledTonalButton(
                            enabled = !resolvedButton2Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton2Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        }
                     },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(button2Label.ifBlank { "View Album" }, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SongProgressBar(
    currentTimeMillis: Long,
    durationMillis: Long,
    isPlaying: Boolean = false
) {
    var displayedTime by remember { mutableStateOf(currentTimeMillis) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(500)
                displayedTime += 500
                if (displayedTime >= durationMillis) {
                    displayedTime = durationMillis
                    break
                }
            }
        }
    }

    val progress = if (durationMillis > 0) {
        displayedTime.toFloat() / durationMillis
    } else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                text = makeTimeString(displayedTime),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = makeTimeString(durationMillis),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
