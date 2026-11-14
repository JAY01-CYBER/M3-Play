package com.j.m3play.ui.screens.settings

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.R
import com.j.m3play.constants.ShowSpotifyPlaylistsKey
import com.j.m3play.spotify.SpotifyAccountViewModel
import com.j.m3play.spotify.SpotifyAuth
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLoginScreen(
    navController: NavController,
    viewModel: SpotifyAccountViewModel = hiltViewModel()
) {
    val spotifyState by viewModel.uiState.collectAsState()
    val (showPlaylists, onShowPlaylistsChange) = rememberPreference(ShowSpotifyPlaylistsKey, true)
    var showWebViewSheet by remember { mutableStateOf(false) }

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (spotifyState.isAuthenticated) {
                // CONNECTED STATE
                Material3SettingsGroup(
                    title = "Account",
                    items = listOf(
                        Material3SettingsItem(
                            leadingContent = {
                                AsyncImage(
                                    model = spotifyState.accountAvatarUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(42.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            title = { Text(spotifyState.accountName.ifBlank { "Spotify User" }) },
                            description = { Text("Connected via Spotify") },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = { viewModel.logout() },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Logout", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    )
                )

                Material3SettingsGroup(
                    title = "Options",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.library_music),
                            title = { Text("Show Spotify Playlists") },
                            description = { Text("Display your playlists in the Library") },
                            trailingContent = {
                                Switch(
                                    checked = showPlaylists,
                                    onCheckedChange = onShowPlaylistsChange,
                                    thumbContent = {
                                        Icon(painterResource(if (showPlaylists) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize))
                                    }
                                )
                            },
                            onClick = { onShowPlaylistsChange(!showPlaylists) }
                        )
                    )
                )
            } else {
                // LOGIN STATE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            painterResource(R.drawable.library_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Connect your Spotify Account",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Login securely through Spotify to access your playlists. Your credentials are not stored in this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Button(
                            onClick = { showWebViewSheet = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !spotifyState.isLoading,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer, contentColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            if (spotifyState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                Text("Login to Spotify", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWebViewSheet) {
        SpotifyLoginSheet(
            onDismiss = { showWebViewSheet = false },
            onCookiesCaptured = { spDc, spKey ->
                showWebViewSheet = false
                viewModel.connectWithCookies(spDc, spKey)
            }
        )
    }
}
// Keep your SpotifyLoginSheet and helper functions exactly as they are below this...
