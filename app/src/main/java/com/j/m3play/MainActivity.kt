package com.j.m3play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.j.m3play.ui.theme.M3PlayTheme
import com.j.m3play.ui.components.*
import com.j.m3play.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3PlayTheme {
                // 1. App State Management
                var isPlayerExpanded by remember { mutableStateOf(false) }
                var currentSong by remember { mutableStateOf<Song?>(null) }
                var selectedTab by remember { mutableIntStateOf(0) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 2. Main Content (Scaffold)
                        Scaffold(
                            bottomBar = {
                                Column {
                                    // Sirf tab dikhao jab player full screen na ho
                                    if (!isPlayerExpanded) {
                                        currentSong?.let { song ->
                                            RhythmMiniPlayer(
                                                songName = song.title,
                                                artist = song.artist,
                                                onClick = { isPlayerExpanded = true },
                                                onPlayPause = { /* Play/Pause Logic */ }
                                            )
                                        }
                                        M3BottomNav(
                                            selectedItem = selectedTab,
                                            onTabSelected = { selectedTab = it }
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            // 3. Navigation Logic
                            Box(modifier = Modifier.padding(padding)) {
                                when (selectedTab) {
                                    0 -> HomeScreen()
                                    1 -> SearchScreen()
                                    2 -> LibraryScreen()
                                }
                            }
                        }

                        // 4. Rhythm Style Full Screen Player Transition
                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            currentSong?.let { song ->
                                FullPlayerScreen(
                                    song = song,
                                    onMinimize = { isPlayerExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
