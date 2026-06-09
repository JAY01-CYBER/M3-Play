package com.j.m3play.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import com.j.m3play.BuildConfig
import com.j.m3play.utils.Updater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val listState = rememberLazyListState()

    var query by remember { mutableStateOf("") }

    val hasUpdate = !Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)

    val resetSearch: () -> Unit = {
        query = ""
        focusManager.clearFocus()
    }

    val quickSettings = buildQuickSettings(navController, resetSearch)
    val settingsGroups = buildSettingsGroups(navController, isAndroid12OrLater, hasUpdate, context, resetSearch)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.trim()
    val isSearchActive = queryText.isNotBlank()

    val filteredQuickSettings = filterSettingsItems(quickSettings, queryText)
    val filteredGroups = filterSettingsGroups(settingsGroups, queryText)
    val filteredInternalItems = filterSettingsItems(internalItems, queryText)

    val hasSearchResults by remember(filteredQuickSettings, filteredGroups, filteredInternalItems) {
        derivedStateOf {
            filteredQuickSettings.isNotEmpty() || filteredGroups.isNotEmpty() || filteredInternalItems.isNotEmpty()
        }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) {
        SettingsGroup(title = "Internal Settings", items = filteredInternalItems)
    } else null

    val contentState = SettingsContentState(
        quickSettings = if (isSearchActive) filteredQuickSettings else quickSettings,
        groups = if (isSearchActive) filteredGroups else settingsGroups,
        internalGroup = if (isSearchActive) internalGroup else null,
        isSearchActive = isSearchActive,
        hasSearchResults = hasSearchResults,
    )

    // Scaffold without TopAppBar because everything is inline now!
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AdaptiveSettingsLayout(
                state = contentState,
                query = query,
                onQueryChange = { query = it },
                listState = listState,
                topPadding = innerPadding.calculateTopPadding(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
