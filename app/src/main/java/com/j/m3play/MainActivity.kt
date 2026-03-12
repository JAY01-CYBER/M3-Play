package com.j.m3play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.j.m3play.data.Song
import com.j.m3play.player.PlayerViewModel
import com.j.m3play.ui.components.M3BottomNav
import com.j.m3play.ui.components.RhythmMiniPlayer
import com.j.m3play.ui.screens.FullPlayerScreen
import com.j.m3play.ui.screens.HomeScreen
import com.j.m3play.ui.screens.LibraryScreen
import com.j.m3play.ui.screens.SearchScreen
import com.j.m3play.ui.theme.M3PlayTheme

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    private val demoSongs = listOf(
        Song(
            title = "Big Buck Bunny Theme",
            artist = "Blender Foundation",
            thumbnailUrl = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        Song(
            title = "SoundHelix Track 2",
            artist = "SoundHelix",
            thumbnailUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=500",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            M3PlayTheme {
                var isPlayerExpanded by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableIntStateOf(0) }
                val playerState by playerViewModel.uiState.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            bottomBar = {
                                Column {
                                    if (!isPlayerExpanded && playerState.currentSong != null) {
                                        RhythmMiniPlayer(
                                            state = playerState,
                                            onClick = { isPlayerExpanded = true },
                                            onPlayPause = playerViewModel::togglePlayPause,
                                            onNext = playerViewModel::playNext
                                        )
                                    }
                                    if (!isPlayerExpanded) {
                                        M3BottomNav(
                                            selectedItem = selectedTab,
                                            onTabSelected = { selectedTab = it }
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                when (selectedTab) {
                                    0 -> HomeScreen(songs = demoSongs, onSongClicked = {
                                        playerViewModel.setQueue(demoSongs, it)
                                        isPlayerExpanded = true
                                    })
                                    1 -> SearchScreen()
                                    else -> LibraryScreen()
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            FullPlayerScreen(
                                state = playerState,
                                onMinimize = { isPlayerExpanded = false },
                                onPlayPause = playerViewModel::togglePlayPause,
                                onNext = playerViewModel::playNext,
                                onPrevious = playerViewModel::playPrevious,
                                onSeek = playerViewModel::seekTo
                            )
                        }
                    }
                }
            }
        }
    }
}
