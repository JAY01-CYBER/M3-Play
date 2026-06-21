/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for premium music experience      │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V3     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.playlist

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.YouTubeListItem
import com.j.m3play.viewmodels.LocalPlaylistViewModel

@Composable
fun PlaylistSuggestionsSection(
    modifier: Modifier = Modifier,
    viewModel: LocalPlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: mutableStateOf(false)
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: mutableStateOf(null)
    
    val playlistSuggestions by viewModel.playlistSuggestions.collectAsState()
    val isLoading by viewModel.isLoadingSuggestions.collectAsState()
    
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var songToCheck by remember { mutableStateOf<SongItem?>(null) }
    
    val currentSuggestions = playlistSuggestions
    if (currentSuggestions == null && !isLoading) return
    if (currentSuggestions != null && currentSuggestions.items.isEmpty() && !isLoading) return

    if (showDuplicateDialog && songToCheck != null) {
        val song = songToCheck!!
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(onClick = { showDuplicateDialog = false; songToCheck = null }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val browseId = viewModel.playlist.value?.playlist?.browseId
                            viewModel.addSongToPlaylist(song, browseId)
                            val playlistName = viewModel.playlist.value?.playlist?.name
                            val message = if (playlistName != null) context.getString(R.string.added_to_playlist, playlistName) else context.getString(R.string.add_to_playlist)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        showDuplicateDialog = false; songToCheck = null
                    }
                ) { Text(stringResource(R.string.add_anyway)) }
            },
            onDismiss = { showDuplicateDialog = false; songToCheck = null }
        ) { Text(text = stringResource(R.string.duplicates_description_single)) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavigationTitle(
                title = stringResource(R.string.you_might_like),
                subtitle = currentSuggestions?.let { s -> if (s.totalQueries > 1) "${s.currentQueryIndex + 1} / ${s.totalQueries}" else null },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        currentSuggestions?.let { suggestions ->
            suggestions.items.forEach { item ->
                YouTubeListItem(
                    item = item, isActive = item.id == mediaMetadata?.id, isPlaying = isPlaying == true,
                    trailingContent = {
                        IconButton(
                            onClick = { 
                                val songItem = item as? SongItem
                                if (songItem != null) {
                                    songToCheck = songItem
                                    coroutineScope.launch {
                                        val isDuplicate = withContext(Dispatchers.IO) { database.playlistDuplicates(viewModel.playlistId, listOf(songItem.id)).isNotEmpty() }
                                        if (isDuplicate) showDuplicateDialog = true
                                        else {
                                            val browseId = viewModel.playlist.value?.playlist?.browseId
                                            if (viewModel.addSongToPlaylist(song = songItem, browseId = browseId)) {
                                                val pName = viewModel.playlist.value?.playlist?.name
                                                Toast.makeText(context, if (pName != null) context.getString(R.string.added_to_playlist, pName) else context.getString(R.string.add_to_playlist), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        ) { Icon(painterResource(R.drawable.playlist_add), null) }
                    },
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                        if (playerConnection == null) return@clickable
                        if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                        else if (item is SongItem) {
                            val songItems = suggestions.items.filterIsInstance<SongItem>()
                            val sIndex = songItems.indexOfFirst { it.id == item.id }
                            if (sIndex != -1) playerConnection.playQueue(ListQueue(title = context.getString(R.string.you_might_like), items = songItems.map { it.toMediaItem() }, startIndex = sIndex))
                        }
                    }
                )
            }
            
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else {
                    FilledTonalButton(onClick = { viewModel.resetAndLoadPlaylistSuggestions() }, shape = CircleShape) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.sync), null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.refresh_suggestions), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
