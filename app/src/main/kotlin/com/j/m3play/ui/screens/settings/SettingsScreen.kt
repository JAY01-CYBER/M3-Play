/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * [span_0](start_span)│  Signature: M3PLAY::UI::EXPRESSIVE::V1[span_0](end_span)   │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.j.m3play.BuildConfig
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.TopSearch
import com.j.m3play.ui.utils.backToMain
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
    val listState = rememberLazyListState()

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        [span_1](start_span)Manifest.permission.READ_EXTERNAL_STORAGE[span_1](end_span)
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
            [span_2](start_span)ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED[span_2](end_span)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isStorageGranted = result[storagePermission] == true ||
        [span_3](start_span)isStorageGranted[span_3](end_span)
        if (notificationPermission != null) {
            isNotificationGranted = result[notificationPermission] == true ||
            [span_4](start_span)isNotificationGranted[span_4](end_span)
        }
    }

    val shouldShowPermissionHint = !isStorageGranted ||
    [span_5](start_span)!isNotificationGranted[span_5](end_span)
    val hasUpdate = !Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)

    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    // consolidated lists of all settings groups and internal items
    val finalSettingsGroups = buildFinalSettingsGroups(navController, context, latestVersionName, hasUpdate, resetSearch)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.text.trim()
    [span_6](start_span)val showSearchBar = isSearching || queryText.isNotBlank()[span_6](end_span)

    // filter only the new consolidated list of settings groups
    val filteredGroups = filterSettingsGroups(finalSettingsGroups, queryText)
    val filteredInternalItems = filterInternalItems(internalItems, queryText)

    val hasSearchResults by remember(
        filteredGroups,
        filteredInternalItems,
    ) {
        derivedStateOf {
            filteredGroups.isNotEmpty() ||
            [span_7](start_span)filteredInternalItems.isNotEmpty()[span_7](end_span)
        }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) {
        SettingsGroup(
            title = stringResource(R.string.internal_subcategory_settings),
            items = filteredInternalItems,
        )
    } else null

    val contentState = SettingsContentState(
        groups = if (queryText.isBlank()) finalSettingsGroups else filteredGroups,
        internalGroup = if (queryText.isNotBlank()) internalGroup else null,
        showPermissionBanner = shouldShowPermissionHint,
        showUpdateBanner = hasUpdate,
        isSearchActive = queryText.isNotBlank(),
        hasSearchResults = hasSearchResults,
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) {
                    add(notificationPermission)
                }
            [span_8](start_span)}
            if (toRequest.isNotEmpty()) {
                permissionLauncher.launch(toRequest.toTypedArray())
            }
        },
    )

    Scaffold(
        topBar = {
            if (!showSearchBar) {
                LargeTopAppBar([span_8](end_span)
                    title = {
                        Text(
                            [span_9](start_span)text = stringResource(R.string.settings),[span_9](end_span)
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        [span_10](start_span)IconButton([span_10](end_span)
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                        ) {
                            [span_11](start_span)Icon([span_11](end_span)
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            [span_12](start_span))
                        }
                    },
                    actions = {
                        IconButton([span_12](end_span)
                            onClick = { isSearching = true },
                            onLongClick = {},
                        ) {
                            [span_13](start_span)Icon([span_13](end_span)
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                            [span_14](start_span))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors([span_14](end_span)
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showSearchBar) {
                // Now passes state and listState to AdaptiveSettingsLayout
                [span_15](start_span)AdaptiveSettingsLayout([span_15](end_span)
                    state = contentState,
                    listState = listState,
                    topPadding = innerPadding.calculateTopPadding(),
                    modifier = Modifier.fillMaxSize(),
                [span_16](start_span))
            }

            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn(tween(durationMillis = 220)),
                exit = fadeOut(tween(durationMillis = 160)),
            ) {
                TopSearch([span_16](end_span)
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { focusManager.clearFocus() },
                    [span_17](start_span)active = showSearchBar,[span_17](end_span)
                    onActiveChange = { active ->
                        if (active) {
                            isSearching = true
                        [span_18](start_span)} else {[span_18](end_span)
                            resetSearch()
                        }
                    },
                    [span_19](start_span)placeholder = { Text(text = stringResource(R.string.search)) },[span_19](end_span)
                    leadingIcon = {
                        IconButton(
                            onClick = { resetSearch() },
                            [span_20](start_span)onLongClick = {[span_20](end_span)
                                if (queryText.isBlank()) {
                                    navController.backToMain()
                                [span_21](start_span)}
                            },
                        ) {
                            Icon([span_21](end_span)
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            )
                        [span_22](start_span)}
                    },
                    trailingIcon = {
                        Row {
                            if (query.text.isNotBlank()) {[span_22](end_span)
                                IconButton(
                                    onClick = { query = TextFieldValue() },
                                    [span_23](start_span)onLongClick = {},[span_23](end_span)
                                ) {
                                    Icon(
                                        [span_24](start_span)painter = painterResource(R.drawable.close),[span_24](end_span)
                                        contentDescription = null,
                                    [span_25](start_span))
                                }
                            }
                        }[span_25](end_span)
                    },
                    focusRequester = focusRequester,
                ) {
                    val searchState = contentState.copy(
                        [span_26](start_span)isSearchActive = true,[span_26](end_span)
                    )
                    AdaptiveSettingsLayout(
                        state = searchState,
                        [span_27](start_span)modifier = Modifier.fillMaxWidth(),[span_27](end_span)
                    )
                }
            }
        }
    }
}

// Function to build a consolidated list of settings groups in a Pixel phone style
fun buildFinalSettingsGroups(
    navController: NavController,
    context: android.content.Context,
    latestVersionName: String,
    hasUpdate: Boolean,
    resetSearch: () -> Unit
): List<SettingsGroup> {
    return listOf(
        // Quick Access - formerly grid actions, now consolidated linear list tiles
        SettingsGroup(
            title = "QUICK ACCESS",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.appearance, // use icon from photo
                    title = "Appearance",
                    subtitle = "Theme, scaling",
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/appearance")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.player, // use icon from photo
                    title = "Player and audio",
                    subtitle = "Audio quality, playback controls",
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/audio")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.storage, // use icon from photo
                    title = "Storage",
                    subtitle = "Cache, data usage",
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/storage")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.privacy, // use icon from photo
                    title = "Privacy",
                    subtitle = "Listen history, tracking",
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/privacy")
                    }
                )
            )
        ),
        
        // Integrations section, consolidated from separate buttons
        SettingsGroup(
            title = "INTEGRATIONS",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.discord, // Use discord icon from photo
                    title = "Discord RPC",
                    subtitle = "Sync with Discord",
                    onClick = {
                        resetSearch()
                        // navigate to discord integration screen
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.integration, // Puzzle icon from photo
                    title = "Integrations",
                    subtitle = "Manage external integrations",
                    onClick = {
                        resetSearch()
                        // navigate to integration screen
                    }
                )
            )
        ),

        // User Interface, keeping the original photo text but consolidated
        SettingsGroup(
            title = "USER INTERFACE",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.appearance, // use icon from photo
                    title = "Appearance",
                    [span_28](start_span)subtitle = "Dark theme", // Keep text from photo[span_28](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/appearance")
                    }
                )
            )
        ),
        
        // Player & Content, keeping the original photo text
        SettingsGroup(
            title = "PLAYER & CONTENT",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.player, // use icon from photo
                    title = "Player and audio",
                    [span_29](start_span)subtitle = "Audio quality", // Keep text from photo[span_29](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/audio")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.content, // use icon from photo
                    title = "Content",
                    [span_30](start_span)subtitle = "Default content language", // Keep text from photo[span_30](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/content")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.po_token, // use icon from photo
                    title = "PO Token Generation",
                    [span_31](start_span)subtitle = "Generate and manage web tokens", // Keep text from photo[span_31](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/po-token")
                    }
                )
            )
        ),
        
        // Privacy & Security, keeping the original photo text
        SettingsGroup(
            title = "PRIVACY & SECURITY",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.privacy, // use icon from photo
                    title = "Privacy",
                    [span_32](start_span)subtitle = "Pause listen history", // Keep text from photo[span_32](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/privacy")
                    }
                )
            )
        ),

        // Storage & Data, keeping the original photo text
        SettingsGroup(
            title = "STORAGE & DATA",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.storage, // use icon from photo
                    title = "Storage",
                    [span_33](start_span)subtitle = "Cache", // Keep text from photo[span_33](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/storage")
                    }
                ),
                SettingsItem(
                    iconRes = R.drawable.backup, // use icon from photo
                    title = "Backup and restore",
                    [span_34](start_span)subtitle = "Backup", // Keep text from photo[span_34](end_span)
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/backup")
                    }
                )
            )
        ),

        // About - Formerly the top large card, now a standard entry at the end of the list
        SettingsGroup(
            title = "ABOUT M3PLAY",
            items = buildList {
                add(SettingsItem(
                    [span_35](start_span)iconRes = R.drawable.about, // Music icon from photo[span_35](end_span)
                    title = "About",
                    subtitle = "M3Play Version",
                    onClick = {
                        resetSearch()
                        navController.navigate("settings/about")
                    }
                ))
                add(SettingsItem(
                    iconRes = R.drawable.update,
                    title = if (hasUpdate) "Update Available" else "Check for Updates",
                    subtitle = if (hasUpdate) "Version: $latestVersionName" else "App is up to date",
                    onClick = {
                        resetSearch()
                        if (hasUpdate) {
                            navController.navigate("settings/update")
                        } else {
                            // check for updates logic
                        }
                    }
                ))
            }
        ),

        // Example new group to show the flexibility and 'expressive' design
        SettingsGroup(
            title = "EXPRESSIVE EXPERIENCE",
            items = listOf(
                SettingsItem(
                    iconRes = R.drawable.expressive,
                    title = "Expressive Icons",
                    [span_36](start_span)subtitle = "Enable handcrafted icons[span_36](end_span)",
                    onClick = {
                        // expressive setting
                    }
                )
            )
        )
    )
}

// Data models for the new organized structure
data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

// Updated AdaptiveSettingsLayout to render the consolidated linear list
@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .padding(top = topPadding)
            .fillMaxSize(),
    ) {
        // Permissions banner at the top
        if (state.showPermissionBanner) {
            item {
                PermissionBannerCard(onClick = state.onRequestPermission)
            }
        }
        
        // Update banner at the top
        if (state.showUpdateBanner && !state.isSearchActive) {
            item {
                UpdateBannerCard(onClick = {}) // onUpdateClick handled by specific list item now
            }
        }

        // Iterate through consolidated groups and render their items as linear list tiles
        for (group in state.groups) {
            item {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp))
            }
            items(group.items) { item ->
                SettingsListItemCard(
                    iconRes = item.iconRes,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        // Render Internal items if present, keeping the style consistent
        if (state.internalGroup != null) {
             item {
                Text(
                    text = state.internalGroup.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp))
            }
            items(state.internalGroup.items) { item ->
                 SettingsListItemCard(
                    iconRes = item.iconRes,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Custom card for each linear list item in the new structure
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListItemCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // subtle expressive card color
        shape = MaterialTheme.shapes.medium, // expressive rounded corners
        tonalElevation = 1.dp, // slightly raised card look
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // standard pixel phone style list item: icon -> title/subtitle -> arrow
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Icon(
                painterResource(R.drawable.arrow_forward), // forward arrow as is standard
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Simplified data structure definitions, keeping the context in mind
data class SettingsContentState(
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup? = null,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
    val onRequestPermission: () -> Unit,
)

// Simplified stub functions, assume filtering logic exists elsewhere
fun filterSettingsGroups(groups: List<SettingsGroup>, query: String): List<SettingsGroup> = groups
fun filterInternalItems(items: List<SettingsItem>, query: String): List<SettingsItem> = items
fun buildInternalItems(navController: NavController, resetSearch: () -> Unit): List<SettingsItem> = emptyList()

@Composable
fun PermissionBannerCard(onClick: () -> Unit) { /* generic banner card impl */ }
@Composable
fun UpdateBannerCard(onClick: () -> Unit) { /* generic update card impl */ }
