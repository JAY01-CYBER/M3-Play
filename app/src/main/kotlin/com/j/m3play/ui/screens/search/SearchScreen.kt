/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.SearchHistory
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.ui.screens.search.suggestions.SuggestionsTabContent
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.ExploreViewModel
import com.j.m3play.viewmodels.MoodAndGenresViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val playerConnection = LocalPlayerConnection.current

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) } 
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showSearchContent by remember { mutableStateOf(false) }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            kotlinx.coroutines.delay(100)
            showSearchContent = true
        } else {
            showSearchContent = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    val searchBarHorizontalPadding by animateDpAsState(targetValue = if (searchActive) 0.dp else 16.dp, animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing), label = "")
    val searchBarTopPadding by animateDpAsState(targetValue = if (searchActive) 0.dp else 8.dp, animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing), label = "")

    val onSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                focusManager.clearFocus()
                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) { database.query { insert(SearchHistory(query = searchQuery)) } }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            Column(modifier = Modifier.background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)) {
                
                // Greeting Header Using Dynamic Theme Colors
                AnimatedVisibility(visible = !searchActive) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Good evening", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                            IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Rounded.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "What do you want to play?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }

                SearchBar(
                    query = query.text,
                    onQueryChange = { query = TextFieldValue(it) },
                    onSearch = { onSearch(it); searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = {
                        Text(
                            text = "Search YouTube Music...",
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (searchActive) { searchActive = false; query = TextFieldValue("") } else { searchActive = true }
                        }) {
                            Icon(painter = painterResource(if (searchActive) R.drawable.arrow_back else R.drawable.search), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.text.isNotEmpty()) {
                                IconButton(onClick = { query = TextFieldValue("") }) { Icon(painter = painterResource(R.drawable.close), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                            } else if (!searchActive) {
                                IconButton(onClick = { }) { Icon(painter = painterResource(R.drawable.mic), contentDescription = "Voice Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = if (pureBlack) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                        dividerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = searchBarHorizontalPadding).padding(top = searchBarTopPadding)
                ) {
                    if (showSearchContent) {
                        when (searchSource) {
                            SearchSource.LOCAL -> LocalSearchScreen(query = query.text, navController = navController, onDismiss = { searchActive = false }, pureBlack = pureBlack)
                            SearchSource.ONLINE -> OnlineSearchScreen(query = query.text, onQueryChange = { query = it }, navController = navController, onSearch = { onSearch(it); searchActive = false }, onDismiss = { searchActive = false }, pureBlack = pureBlack)
                        }
                    }
                }

                // Custom Pill Chips using dynamic Material Theme Colors
                AnimatedVisibility(visible = !searchActive, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val tabs = listOf(
                            Triple("Explore", Icons.Rounded.Explore, 0),
                            Triple("Suggestions", Icons.Rounded.Star, 1),
                            Triple("Albums", Icons.Rounded.Album, 2)
                        )
                        
                        items(tabs.size) { index ->
                            val isSelected = selectedTabIndex == index
                            val item = tabs[index]
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedTabIndex = index }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.second, 
                                    contentDescription = null, 
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.first, 
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize()) {
            if (!searchActive) {
                val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                val tabPadding = PaddingValues(bottom = bottomPadding + 80.dp)
                
                when (selectedTabIndex) {
                    0 -> ExploreTabContent(navController = navController, contentPadding = tabPadding)
                    1 -> SuggestionsTabContent(navController = navController, contentPadding = tabPadding)
                    2 -> AlbumsTabContent(navController = navController, contentPadding = tabPadding)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

// Keep original old functions for these tabs
@Composable
fun ExploreTabContent(navController: NavController, viewModel: MoodAndGenresViewModel = hiltViewModel(), contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()
    
    if (moodAndGenresList == null) { 
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            // Changed loading to Wavy Circular style
            CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, color = MaterialTheme.colorScheme.primary) 
        } 
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 6.dp, top = 12.dp, end = 6.dp, bottom = 12.dp + contentPadding.calculateBottomPadding()),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = moodAndGenresList!!) { item ->
                Box(
                    contentAlignment = Alignment.CenterStart, 
                    modifier = Modifier.padding(6.dp).height(64.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}") }
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun AlbumsTabContent(navController: NavController, viewModel: ExploreViewModel = hiltViewModel(), contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val coroutineScope = rememberCoroutineScope()
    
    val explorePage by viewModel.explorePage.collectAsState()
    val newReleaseAlbums = explorePage?.newReleaseAlbums

    if (newReleaseAlbums == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, color = MaterialTheme.colorScheme.primary) 
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + contentPadding.calculateBottomPadding()),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = newReleaseAlbums, key = { it.id }) { album ->
                YouTubeGridItem(
                    item = album, isActive = mediaMetadata?.album?.id == album.id, isPlaying = isPlaying, coroutineScope = coroutineScope, fillMaxWidth = true, 
                    modifier = Modifier.combinedClickable(
                        onClick = { navController.navigate("album/${album.id}") }, 
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss) } }
                    )
                )
            }
        }
    }
}
