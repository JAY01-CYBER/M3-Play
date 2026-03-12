package com.j.m3play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.j.m3play.data.sampleSongs
import com.j.m3play.ui.components.M3BottomNav
import com.j.m3play.ui.components.RhythmMiniPlayer
import com.j.m3play.ui.designsystem.MotionSpecs
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
                var isPlayerExpanded by remember { mutableStateOf(false) }
                var currentSong by remember { mutableStateOf(sampleSongs.first()) }
                var selectedTab by remember { mutableIntStateOf(0) }
                var isPlaying by remember { mutableStateOf(false) }
                var fakeProgress by remember { mutableFloatStateOf(0.35f) }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            bottomBar = {
                                Column {
                                    if (!isPlayerExpanded) {
                                        RhythmMiniPlayer(
                                            song = currentSong,
                                            isPlaying = isPlaying,
                                            onClick = { isPlayerExpanded = true },
                                            onPlayPause = {
                                                isPlaying = !isPlaying
                                                fakeProgress = (fakeProgress + 0.1f) % 1f
                                            },
                                        )
                                        M3BottomNav(
                                            selectedItem = selectedTab,
                                            onTabSelected = { selectedTab = it },
                                        )
                                    }
                                }
                            },
                        ) { padding ->
                            AnimatedContent(
                                targetState = selectedTab,
                                modifier = Modifier.padding(padding),
                                transitionSpec = {
                                    (fadeIn(animationSpec = MotionSpecs.mediumTween) +
                                        slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(400, easing = MotionSpecs.EasingStandard))) togetherWith
                                        (fadeOut(animationSpec = MotionSpecs.mediumTween))
                                },
                                label = "tabs",
                            ) { tab ->
                                when (tab) {
                                    0 -> HomeScreen(songs = sampleSongs, onSongClick = { currentSong = it; isPlayerExpanded = true })
                                    1 -> SearchScreen(songs = sampleSongs, onSongClick = { currentSong = it; isPlayerExpanded = true })
                                    else -> LibraryScreen()
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(450, easing = MotionSpecs.EasingEmphasized),
                            ),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        ) {
                            FullPlayerScreen(
                                song = currentSong,
                                isPlaying = isPlaying,
                                progress = fakeProgress,
                                onMinimize = { isPlayerExpanded = false },
                                onPlayPause = {
                                    isPlaying = !isPlaying
                                    fakeProgress = (fakeProgress + 0.08f) % 1f
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
