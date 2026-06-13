package com.j.m3play.spotify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit,
    onImportReady: (List<String>) -> Unit 
) {
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    // Apni banayi hui repository ko call karenge
    val repository = remember { SpotifyRepository() }

    AlertDialog(
        onDismissRequest = { 
            // Agar loading nahi ho rahi, tabhi bahar click karke band hone dein
            if (!isLoading) onDismiss() 
        },
        title = { Text("Import Spotify Playlist") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Apni public Spotify playlist ka link yahan paste karein:")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Spotify Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading // Loading ke time disable kar dein
                )
                
                // Agar gaane fetch ho rahe hain toh loader dikhayein
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        isLoading = true
                        // Background me API call chalayenge
                        coroutineScope.launch {
                            val tracks = repository.fetchPlaylistTracks(url)
                            isLoading = false
                            onImportReady(tracks) // Data wapas UI ko bhejenge
                            onDismiss() // Dialog band kar denge
                        }
                    }
                },
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
