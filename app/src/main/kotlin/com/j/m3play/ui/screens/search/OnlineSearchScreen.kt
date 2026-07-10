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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
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
import coil.compose.AsyncImage
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.SearchHistory
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.YouTubeGridItem
import com.j.m3play.ui.menu.YouTubeAlbumMenu
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.ExploreViewModel
import com.j.m3play.viewmodels.MoodAndGenresViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder


val CustomBgColor = Color(0xFF0A0A0A)
val CustomSurfaceColor = Color(0xFF222222)
val CustomAccentColor = Color(0xFFFFD2B4)

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
            Column(modifier = Modifier.background(if (pureBlack) Color.Black else CustomBgColor)) {
                
                // Greeting Header (Only visible when search is NOT active)
                AnimatedVisibility(visible = !searchActive) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Good evening", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                            IconButton(onClick = { /* Handle Notifications */ }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Rounded.Notifications, contentDescription = "Notifications", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "What do you want to play?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = CustomAccentColor)
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
                            style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (searchActive) { searchActive = false; query = TextFieldValue("") } else { searchActive = true }
                        }) {
                            Icon(
                                painter = painterResource(if (searchActive) R.drawable.arrow_back else R.drawable.search), 
                                contentDescription = null, 
                                tint = if (searchActive) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.text.isNotEmpty()) {
                                IconButton(onClick = { query = TextFieldValue("") }) { Icon(painter = painterResource(R.drawable.close), contentDescription = null, tint = Color.White) }
                            } else if (!searchActive) {
                                // Mic icon when not typing
                                IconButton(onClick = { /* Voice Search */ }) { Icon(painter = painterResource(R.drawable.mic), contentDescription = "Voice Search", tint = Color.White.copy(alpha = 0.7f)) }
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = CustomSurfaceColor,
                        dividerColor = Color.Transparent,
                        inputFieldColors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CustomAccentColor
                        )
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

                // Custom Chips Row (Replaces TabRow)
                AnimatedVisibility(visible = !searchActive, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val tabs = listOf(
                            Triple("Explore", R.drawable.explore, 0),
                            Triple("Suggestions", R.drawable.sparkles, 1), // Assuming you have a sparkle icon
                            Triple("Albums", R.drawable.album, 2)
                        )
                        
                        items(tabs.size) { index ->
                            val isSelected = selectedTabIndex == index
                            val item = tabs[index]
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) CustomAccentColor else CustomSurfaceColor)
                                    .clickable { selectedTabIndex = index }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = item.second),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.first,
                                    color = if (isSelected) Color.Black else Color.White,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (pureBlack) Color.Black else CustomBgColor
    ) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize()) {
            if (!searchActive) {
                val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                val tabPadding = PaddingValues(bottom = bottomPadding + 80.dp)
                
                when (selectedTabIndex) {
                    0 -> ExploreTabContent(navController = navController, contentPadding = tabPadding)
                    1 -> NumberedSuggestionsList(navController = navController, contentPadding = tabPadding) // New UI
                    2 -> AlbumsTabContent(navController = navController, contentPadding = tabPadding)
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// SUGGESTIONS TAB 
// -----------------------------------------------------------------
@Composable
fun NumberedSuggestionsList(
    navController: NavController,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = contentPadding.calculateBottomPadding()),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Apple Music Top 100", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(CustomSurfaceColor).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "See all", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Placeholder items for the design (Replace with actual data)
        items(5) { index ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 10.dp).background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number Index
                Text(
                    text = "${index + 1}", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.width(32.dp)
                )
                
                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Song Title ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Artist Name", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "#${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CustomAccentColor)
                }
                
                // Square Image on Right
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(CustomSurfaceColor)
                ) {
                    // AsyncImage here
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}


