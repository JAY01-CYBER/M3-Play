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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.j.m3play.data.DataUpdater
import com.j.m3play.data.SampleCatalog
import com.j.m3play.data.YoutubeRepository
import com.j.m3play.model.Song
import com.j.m3play.ui.components.M3BottomNav
import com.j.m3play.ui.components.RhythmMiniPlayer
import com.j.m3play.ui.screens.FullPlayerScreen
import com.j.m3play.ui.screens.HomeScreen
import com.j.m3play.ui.screens.LibraryScreen
import com.j.m3play.ui.screens.SearchScreen
import com.j.m3play.ui.theme.M3PlayTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3PlayTheme {
                val repository = remember { YoutubeRepository() }
                val dataUpdater = remember { DataUpdater() }

                var catalog by remember { mutableStateOf(SampleCatalog.trendingSongs) }
                var isPlayerExpanded by remember { mutableStateOf(false) }
                var currentSong by remember { mutableStateOf<Song?>(catalog.firstOrNull()) }
                var selectedTab by remember { mutableIntStateOf(0) }
                var isPlaying by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(30_000)
                        dataUpdater.refresh()
                        catalog = dataUpdater.catalog.value
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            bottomBar = {
                                Column {
                                    if (!isPlayerExpanded) {
                                        currentSong?.let { song ->
                                            RhythmMiniPlayer(
                                                song = song,
                                                isPlaying = isPlaying,
                                                onClick = { isPlayerExpanded = true },
                                                onPlayPause = { isPlaying = !isPlaying }
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
                            Box(modifier = Modifier.padding(padding)) {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        songs = catalog,
                                        onSongSelected = {
                                            currentSong = it
                                            isPlaying = true
                                            isPlayerExpanded = true
                                        }
                                    )

                                    1 -> SearchScreen(
                                        repository = repository,
                                        onSongSelected = {
                                            currentSong = it
                                            isPlaying = true
                                            isPlayerExpanded = true
                                        }
                                    )

                                    else -> LibraryScreen(currentSong = currentSong)
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            currentSong?.let { song ->
                                FullPlayerScreen(
                                    song = song,
                                    isPlaying = isPlaying,
                                    onMinimize = { isPlayerExpanded = false },
                                    onPlayPause = { isPlaying = !isPlaying }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
