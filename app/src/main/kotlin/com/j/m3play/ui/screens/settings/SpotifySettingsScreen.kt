package com.j.m3play.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.j.m3play.R
import com.j.m3play.constants.SpotifyConnectedKey
import com.j.m3play.constants.SpotifyTokenKey
import com.j.m3play.spotify.SpotifyRepository
import com.j.m3play.utils.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Automatically preference se data fetch karega
    val isConnected by context.dataStore.data
        .map { it[SpotifyConnectedKey] ?: false }
        .collectAsState(initial = false)

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Spotify ka Icon (Agar custom icon daalna hai toh name change kar lein)
            Icon(
                painter = painterResource(R.drawable.library_music), 
                contentDescription = "Spotify",
                modifier = Modifier.size(100.dp),
                tint = Color(0xFF1DB954)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isConnected) {
                Text(
                    text = "Account Connected!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1DB954)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aapki Spotify playlists M3Play ke sath sync ho rahi hain.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            context.dataStore.edit { it[SpotifyConnectedKey] = false }
                            context.dataStore.edit { it.remove(SpotifyTokenKey) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect Account")
                }
            } else {
                Text(
                    text = "Connect to Spotify",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apni Spotify playlists ko direct M3Play mein laane ke liye apne account se login karein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        // Repository call karke browser me kholna
                        val repo = SpotifyRepository()
                        val loginUrl = repo.getLoginUrl()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) {
                    Text("Login with Spotify", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
