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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.SearchHistory
import com.j.m3play.ui.screens.search.suggestions.SuggestionsTabContent
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
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

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) } // 1 = Suggestions (Apple Music)
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

    // Simplified M3-Play navigation without YouTubeUrlParser dependency
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
                                // Replaced globe_search with language icon available in M3-Play
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
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Explore") }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Suggestions") }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Albums") }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    0 -> ExploreTabPlaceholder(contentPadding = tabPadding)
                    1 -> SuggestionsTabContent(navController = navController, contentPadding = tabPadding)
                    2 -> AlbumsTabPlaceholder(contentPadding = tabPadding)
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

// Safely isolated dummy placeholders to avoid M3-Play's model compilation errors
@Composable
fun ExploreTabPlaceholder(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        Text("Explore Content", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun AlbumsTabPlaceholder(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        Text("Albums Content", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
