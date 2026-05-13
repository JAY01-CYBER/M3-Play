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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.component.CircularWavyProgressIndicator
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
    
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) } // Default: Suggestions
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
        topBar = {
            Column(modifier = Modifier.background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)) {
                SearchBar(
                    query = query.text,
                    onQueryChange = { query = TextFieldValue(it) },
                    onSearch = { onSearch(it); searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = {
                        Text(
                            text = stringResource(if (searchSource == SearchSource.LOCAL) R.string.search_library else R.string.search_yt_music),
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp)
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
                            }
                            IconButton(onClick = { searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE }) {
                                Icon(painter = painterResource(if (searchSource == SearchSource.LOCAL) R.drawable.library_music else R.drawable.language), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(containerColor = if (pureBlack) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = searchBarHorizontalPadding).padding(top = searchBarTopPadding)
                ) {
                    if (showSearchContent) {
                        when (searchSource) {
                            SearchSource.LOCAL -> LocalSearchScreen(query = query.text, navController = navController, onDismiss = { searchActive = false }, pureBlack = pureBlack)
                            SearchSource.ONLINE -> OnlineSearchScreen(query = query.text, onQueryChange = { query = it }, navController = navController, onSearch = { onSearch(it); searchActive = false }, onDismiss = { searchActive = false }, pureBlack = pureBlack)
                        }
                    }
                }

                AnimatedVisibility(visible = !searchActive, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        indicator = {
                            Box(modifier = Modifier.tabIndicatorOffset(selectedTabIndex).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                Box(modifier = Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    ) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text(stringResource(R.string.tab_explore)) }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text(stringResource(R.string.tab_Suggestions)) }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text(stringResource(R.string.tab_album)) }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun ExploreTabContent(
    navController: NavController, 
    viewModel: MoodAndGenresViewModel = hiltViewModel(), 
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    [span_8](start_span)val moodAndGenresList by viewModel.moodAndGenres.collectAsState() //[span_8](end_span)
    
    if (moodAndGenresList == null) { 
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            CircularWavyProgressIndicator() 
        } 
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            [span_9](start_span)// Hum section define nahi kar rahe kyunki ViewModel direct items de raha hai[span_9](end_span)
            val rows = moodAndGenresList!!.chunked(2)
            items(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    row.forEach { item ->
                        Box(
                            contentAlignment = Alignment.CenterStart, 
                            modifier = Modifier
                                .weight(1f)
                                .padding(6.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                [span_10](start_span).clickable { navController.navigate("youtube_browse/${item.browseId}?params=${item.params}") } //[span_10](end_span)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(text = item.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AlbumsTabContent(
    navController: NavController, 
    viewModel: ExploreViewModel = hiltViewModel(), 
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val coroutineScope = rememberCoroutineScope()
    
    [span_11](start_span)val explorePage by viewModel.explorePage.collectAsState() //[span_11](end_span)
    [span_12](start_span)val newReleaseAlbums = explorePage?.newReleaseAlbums //[span_12](end_span)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    if (newReleaseAlbums == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            CircularWavyProgressIndicator() 
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 12.dp, 
                top = 12.dp, 
                end = 12.dp, 
                bottom = 12.dp + contentPadding.calculateBottomPadding()
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = newReleaseAlbums!!, key = { it.id }) { album ->
                YouTubeGridItem(
                    item = album, 
                    isActive = mediaMetadata?.album?.id == album.id, 
                    isPlaying = isPlaying, 
                    coroutineScope = coroutineScope, 
                    fillMaxWidth = true, 
                    modifier = Modifier.combinedClickable(
                        onClick = { navController.navigate("album/${album.id}") }, 
                        onLongClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss) } 
                        }
                    )
                )
            }
        }
    }
}
