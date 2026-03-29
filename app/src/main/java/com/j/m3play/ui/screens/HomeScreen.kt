@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)

package com.j.m3play.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.*
import com.j.m3play.constants.*
import com.j.m3play.db.entities.*
import com.j.m3play.extensions.*
import com.j.m3play.models.*
import com.j.m3play.playback.queues.*
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.*
import com.j.m3play.utils.*
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.*
import kotlin.random.Random

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {

    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 🔥 VIVI STYLE REFRESH FIXED
    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
            )
        }
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {

                // 🔹 QUICK PICKS TITLE
                quickPicks?.let { list ->

                    item {
                        NavigationTitle(
                            title = "Quick picks",
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = "Quick picks",
                                        items = list.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    // 🔹 SONG LIST
                    item {
                        LazyColumn {
                            items(list) { song ->

                                SongListItem(
                                    song = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(
                                                            song.toMediaMetadata()
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // 🔹 LOADING
                if (isLoading) {
                    item {
                        Text(
                            text = "Loading...",
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // 🔥 SHUFFLE FAB
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    scope.launch {
                        val item = quickPicks?.randomOrNull() ?: return@launch
                        playerConnection.playQueue(
                            YouTubeQueue.radio(item.toMediaMetadata())
                        )
                    }
                }
            )

            // 🔝 SCROLL TO TOP BUTTON
            val isScrolled by remember {
                derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
            }

            if (isScrolled) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
            }
        }
    }
}
