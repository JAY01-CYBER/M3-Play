package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.ShowSpotifyPlaylistKey
import com.j.m3play.constants.SpotifyConnectedKey
import com.j.m3play.constants.SpotifyTokenKey
import com.j.m3play.constants.SpotifyUserNameKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.utils.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isConnected by context.dataStore.data.map { it[SpotifyConnectedKey] ?: false }.collectAsState(initial = false)
    val userName by context.dataStore.data.map { it[SpotifyUserNameKey] ?: "" }.collectAsState(initial = "")
    val showPlaylist by context.dataStore.data.map { it[ShowSpotifyPlaylistKey] ?: true }.collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotify Integration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Material3SettingsGroup(
                    title = "External Service",
                    items = buildList {
                        if (!isConnected) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.library_music),
                                title = { Text("Connect Spotify") },
                                description = { Text("Spotify is not connected") },
                                onClick = { navController.navigate("spotify_login") },
                                showChevron = true
                            ))
                        } else {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.library_music),
                                title = { Text("Connected as $userName") },
                                description = { Text("Spotify account linked") },
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.library_music),
                                title = { Text("Show Playlist") },
                                description = { Text("Show Spotify playlists in Library.") },
                                trailingContent = {
                                    Switch(
                                        checked = showPlaylist,
                                        onCheckedChange = { isChecked ->
                                            coroutineScope.launch {
                                                context.dataStore.edit { it[ShowSpotifyPlaylistKey] = isChecked }
                                            }
                                        },
                                        thumbContent = { Icon(painterResource(if (showPlaylist) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        context.dataStore.edit { it[ShowSpotifyPlaylistKey] = !showPlaylist }
                                    }
                                }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.close),
                                title = { Text("Log out") },
                                onClick = {
                                    coroutineScope.launch {
                                        context.dataStore.edit {
                                            it[SpotifyConnectedKey] = false
                                            it[SpotifyTokenKey] = ""
                                            it[SpotifyUserNameKey] = ""
                                        }
                                        com.j.m3play.spotify.Spotify.accessToken = null
                                    }
                                }
                            ))
                        }
                    }
                )
            }
        }
    }
}
