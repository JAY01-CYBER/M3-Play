package com.j.m3play.ui.screens.search

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showSearchContent by remember { mutableStateOf(false) }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            kotlinx.coroutines.delay(100)
            showSearchContent = true
        } else {
            showSearchContent = false
            keyboardController?.hide()
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
                                Icon(painter = painterResource(if (searchSource == SearchSource.LOCAL) R.drawable.library_music else R.drawable.search), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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
            }
        },
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize()) {
            if (!searchActive && searchSource == SearchSource.ONLINE) {
                val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                SuggestionsTabContent(navController = navController, contentPadding = PaddingValues(bottom = bottomPadding))
            }
        }
    }
}
