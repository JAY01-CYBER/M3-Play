package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.ui.component.M3PlaySlider
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.EnableLastFMScrobblingKey
import com.j.m3play.constants.LastFMSessionKey
import com.j.m3play.constants.LastFMUsernameKey
import com.j.m3play.constants.LastFMUseNowPlaying
import com.j.m3play.constants.ScrobbleDelayPercentKey
import com.j.m3play.constants.ScrobbleMinSongDurationKey
import com.j.m3play.constants.ScrobbleDelaySecondsKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.reportException
import com.j.m3play.lastfm.LastFM
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFMSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val coroutineScope = rememberCoroutineScope()

    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var lastfmSession by rememberPreference(LastFMSessionKey, "")

    val isLoggedIn = remember(lastfmSession) { lastfmSession.isNotEmpty() }

    val (useNowPlaying, onUseNowPlayingChange) = rememberPreference(key = LastFMUseNowPlaying, defaultValue = false)
    val (lastfmScrobbling, onlastfmScrobblingChange) = rememberPreference(key = EnableLastFMScrobblingKey, defaultValue = false)
    val (scrobbleDelayPercent, onScrobbleDelayPercentChange) = rememberPreference(ScrobbleDelayPercentKey, defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
    val (minTrackDuration, onMinTrackDurationChange) = rememberPreference(ScrobbleMinSongDurationKey, defaultValue = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
    val (scrobbleDelaySeconds, onScrobbleDelaySecondsChange) = rememberPreference(ScrobbleDelaySecondsKey, defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)

    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }

    if (showLoginDialog) {
        var tempUsername by rememberSaveable { mutableStateOf("") }
        var tempPassword by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onDismissRequest = { 
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginError = null
                }
            },
            title = { Text(stringResource(R.string.login), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it; loginError = null },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it; loginError = null },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    loginError?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (isLoggingIn) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text(text = stringResource(R.string.logging_in), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUsername.isBlank() || tempPassword.isBlank()) { loginError = "Please enter username and password"; return@TextButton }
                        if (!LastFM.isInitialized()) { loginError = "Last.fm API key not configured"; return@TextButton }
                        isLoggingIn = true; loginError = null
                        coroutineScope.launch(Dispatchers.IO) {
                            LastFM.getMobileSession(tempUsername, tempPassword)
                                .onSuccess { auth ->
                                    withContext(Dispatchers.Main) {
                                        lastfmUsername = auth.session.name
                                        lastfmSession = auth.session.key
                                        LastFM.sessionKey = auth.session.key
                                        isLoggingIn = false; showLoginDialog = false; loginError = null
                                    }
                                }
                                .onFailure { exception ->
                                    withContext(Dispatchers.Main) {
                                        isLoggingIn = false
                                        loginError = exception.message ?: "Login failed"
                                        reportException(exception)
                                    }
                                }
                        }
                    },
                    enabled = !isLoggingIn && tempUsername.isNotBlank() && tempPassword.isNotBlank()
                ) { Text(stringResource(R.string.login), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false; loginError = null }, enabled = !isLoggingIn) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lastfm_integration), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                    title = stringResource(R.string.account),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.token),
                            title = {
                                Text(
                                    text = if (isLoggedIn) lastfmUsername else stringResource(R.string.not_logged_in),
                                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                                )
                            },
                            trailingContent = {
                                if (isLoggedIn) {
                                    OutlinedButton(onClick = {
                                        lastfmSession = ""
                                        lastfmUsername = ""
                                        LastFM.sessionKey = null
                                    }) { Text(stringResource(R.string.action_logout)) }
                                } else {
                                    OutlinedButton(onClick = { showLoginDialog = true }) { Text(stringResource(R.string.action_login)) }
                                }
                            }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.options),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.enable_scrobbling)) },
                            trailingContent = { Switch(checked = lastfmScrobbling, onCheckedChange = onlastfmScrobblingChange, enabled = isLoggedIn, thumbContent = { Icon(painterResource(if (lastfmScrobbling) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { if (isLoggedIn) onlastfmScrobblingChange(!lastfmScrobbling) }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.lastfm_now_playing)) },
                            trailingContent = { Switch(checked = useNowPlaying, onCheckedChange = onUseNowPlayingChange, enabled = isLoggedIn && lastfmScrobbling, thumbContent = { Icon(painterResource(if (useNowPlaying) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { if (isLoggedIn && lastfmScrobbling) onUseNowPlayingChange(!useNowPlaying) }
                        )
                    )
                )
            }

            item {
                var showMinTrackDurationDialog by rememberSaveable { mutableStateOf(false) }
                if (showMinTrackDurationDialog) {
                    var tempMinTrackDuration by remember { mutableIntStateOf(minTrackDuration) }
                    AlertDialog(
                        shape = RoundedCornerShape(32.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onDismissRequest = { showMinTrackDurationDialog = false },
                        title = { Text(stringResource(R.string.scrobble_min_track_duration)) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Text(text = "${tempMinTrackDuration}s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                                M3PlaySlider(value = tempMinTrackDuration.toFloat(), onValueChange = { tempMinTrackDuration = it.toInt() }, valueRange = 10f..60f, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = { TextButton(onClick = { onMinTrackDurationChange(tempMinTrackDuration); showMinTrackDurationDialog = false }) { Text(stringResource(android.R.string.ok)) } },
                    )
                }

                var showScrobbleDelayPercentDialog by rememberSaveable { mutableStateOf(false) }
                if (showScrobbleDelayPercentDialog) {
                    var tempScrobbleDelayPercent by remember { mutableFloatStateOf(scrobbleDelayPercent) }
                    AlertDialog(
                        shape = RoundedCornerShape(32.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onDismissRequest = { showScrobbleDelayPercentDialog = false },
                        title = { Text(stringResource(R.string.scrobble_delay_percent)) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Text(text = "${(tempScrobbleDelayPercent * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                                M3PlaySlider(value = tempScrobbleDelayPercent, onValueChange = { tempScrobbleDelayPercent = it }, valueRange = 0.3f..0.95f, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = { TextButton(onClick = { onScrobbleDelayPercentChange(tempScrobbleDelayPercent); showScrobbleDelayPercentDialog = false }) { Text(stringResource(android.R.string.ok)) } },
                    )
                }

                var showScrobbleDelaySecondsDialog by rememberSaveable { mutableStateOf(false) }
                if (showScrobbleDelaySecondsDialog) {
                    var tempScrobbleDelaySeconds by remember { mutableIntStateOf(scrobbleDelaySeconds) }
                    AlertDialog(
                        shape = RoundedCornerShape(32.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onDismissRequest = { showScrobbleDelaySecondsDialog = false },
                        title = { Text(stringResource(R.string.scrobble_delay_minutes)) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Text(text = "${tempScrobbleDelaySeconds}s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                                M3PlaySlider(value = tempScrobbleDelaySeconds.toFloat(), onValueChange = { tempScrobbleDelaySeconds = it.toInt() }, valueRange = 30f..360f, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = { TextButton(onClick = { onScrobbleDelaySecondsChange(tempScrobbleDelaySeconds); showScrobbleDelaySecondsDialog = false }) { Text(stringResource(android.R.string.ok)) } },
                    )
                }

                Material3SettingsGroup(
                    title = stringResource(R.string.scrobbling_configuration),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.scrobble_min_track_duration)) },
                            description = { Text("${minTrackDuration}s") },
                            onClick = { showMinTrackDurationDialog = true }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.scrobble_delay_percent)) },
                            description = { Text("${(scrobbleDelayPercent * 100).roundToInt()}%") },
                            onClick = { showScrobbleDelayPercentDialog = true }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.scrobble_delay_minutes)) },
                            description = { Text("${scrobbleDelaySeconds}s") },
                            onClick = { showScrobbleDelaySecondsDialog = true }
                        )
                    )
                )
            }
        }
    }
}
