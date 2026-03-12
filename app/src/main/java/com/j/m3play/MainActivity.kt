package com.j.m3play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.j.m3play.data.Song
import com.j.m3play.ui.components.M3BottomNav
import com.j.m3play.ui.components.RhythmMiniPlayer
import com.j.m3play.ui.screens.FullPlayerScreen
import com.j.m3play.ui.screens.HomeScreen
import com.j.m3play.ui.screens.LibraryScreen
import com.j.m3play.ui.screens.SearchScreen
import com.j.m3play.ui.theme.M3PlayTheme

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
