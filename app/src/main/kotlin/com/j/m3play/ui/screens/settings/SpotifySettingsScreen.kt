package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.j.m3play.R
import com.j.m3play.constants.ShowSpotifyPlaylistKey
import com.j.m3play.constants.SpotifyConnectedKey
import com.j.m3play.constants.SpotifyTokenKey
import com.j.m3play.constants.SpotifyUserNameKey
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
                title = { Text("Spotify Integration") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "External Service",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp, start = 16.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column {
                    if (!isConnected) {
                        ListItem(
                            headlineContent = { Text("Connect Spotify") },
                            supportingContent = { Text("Spotify is not connected") },
                            leadingContent = { 
                                Icon(painterResource(R.drawable.library_music), contentDescription = null, modifier = Modifier.size(24.dp)) 
                            },
                            modifier = Modifier.clickable { 
                                navController.navigate("spotify_login") 
                            }
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Connected as $userName") },
                            supportingContent = { Text("Spotify account linked") },
                            leadingContent = { 
                                Icon(painterResource(R.drawable.library_music), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFF1DB954)) 
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text("Show Playlist") },
                            supportingContent = { Text("Show Spotify playlists in Library.") },
                            leadingContent = { Icon(painterResource(R.drawable.library_music), contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = showPlaylist,
                                    onCheckedChange = { isChecked ->
                                        coroutineScope.launch {
                                            context.dataStore.edit { it[ShowSpotifyPlaylistKey] = isChecked }
                                        }
                                    }
                                )
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text("Log out") },
                            leadingContent = { Icon(painterResource(R.drawable.close), contentDescription = null) },
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    context.dataStore.edit {
                                        it[SpotifyConnectedKey] = false
                                        it[SpotifyTokenKey] = ""
                                        it[SpotifyUserNameKey] = ""
                                    }
                                    com.j.m3play.spotify.Spotify.accessToken = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
