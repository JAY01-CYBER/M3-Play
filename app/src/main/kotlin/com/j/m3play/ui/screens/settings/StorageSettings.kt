package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.MaxCanvasCacheSizeKey
import com.j.m3play.constants.MaxImageCacheSizeKey
import com.j.m3play.constants.MaxSongCacheSizeKey
import com.j.m3play.constants.SmartTrimmerKey
import com.j.m3play.extensions.directorySizeBytes
import com.j.m3play.extensions.tryOrNull
import com.j.m3play.ui.component.ActionPromptDialog
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.player.CanvasArtworkPlaybackCache
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.formatFileSize
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return
    
    val downloadCacheDir = remember { context.filesDir.resolve("download") }
    val playerCacheDir = remember { context.filesDir.resolve("exoplayer") }

    val coroutineScope = rememberCoroutineScope()
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(SmartTrimmerKey, false)
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(MaxImageCacheSizeKey, 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(MaxSongCacheSizeKey, 1024)
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(MaxCanvasCacheSizeKey, 256)
    
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(0L) }
    var downloadCacheSize by remember { mutableStateOf(0L) }
    var canvasCacheSize by remember { mutableStateOf(CanvasArtworkPlaybackCache.size()) }

    var showMaxSongCacheDialog by remember { mutableStateOf(false) }
    var showMaxImageCacheDialog by remember { mutableStateOf(false) }
    var showMaxCanvasCacheDialog by remember { mutableStateOf(false) }

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) {
        if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false)
    }

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
                com.j.m3play.utils.ArtworkStorage.clear(context)
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key -> playerCache.removeResource(key) }
            }
        }
    }
    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
        if (maxCanvasCacheSize == 0) {
            CanvasArtworkPlaybackCache.clear()
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache, playerCacheDir) {
        while (isActive) {
            delay(500)
            playerCacheSize = withContext(Dispatchers.IO) {
                val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
            }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize = withContext(Dispatchers.IO) {
                val cacheSpace = tryOrNull { downloadCache.cacheSpace } ?: 0L
                if (cacheSpace == 0L) downloadCacheDir.directorySizeBytes() else cacheSpace
            }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            canvasCacheSize = CanvasArtworkPlaybackCache.size()
        }
    }

    // --- DIALOGS ---
    if (showMaxSongCacheDialog) {
        EnumDialog(
            onDismiss = { showMaxSongCacheDialog = false },
            onSelect = { onMaxSongCacheSizeChange(it); showMaxSongCacheDialog = false },
            title = stringResource(R.string.max_cache_size),
            current = maxSongCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
            valueText = {
                when (it) {
                    0 -> stringResource(R.string.disable)
                    -1 -> stringResource(R.string.unlimited)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            }
        )
    }

    if (showMaxImageCacheDialog) {
        EnumDialog(
            onDismiss = { showMaxImageCacheDialog = false },
            onSelect = { onMaxImageCacheSizeChange(it); showMaxImageCacheDialog = false },
            title = stringResource(R.string.max_cache_size),
            current = maxImageCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
            valueText = {
                when (it) {
                    0 -> stringResource(R.string.disable)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            }
        )
    }

    if (showMaxCanvasCacheDialog) {
        EnumDialog(
            onDismiss = { showMaxCanvasCacheDialog = false },
            onSelect = { onMaxCanvasCacheSizeChange(it); showMaxCanvasCacheDialog = false },
            title = stringResource(R.string.max_cache_size),
            current = maxCanvasCacheSize,
            values = listOf(0, 64, 128, 256, 512, 1024),
            valueText = {
                when (it) {
                    0 -> stringResource(R.string.disable)
                    else -> stringResource(R.string.canvas_cache_items, it)
                }
            }
        )
    }

    // Prompt Dialogs...
    // (Omitted unchanged ActionPromptDialogs for brevity. Keep your existing ones here.)

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.smart_trimmer),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.manage_search),
                            title = { Text(stringResource(R.string.smart_trimmer)) },
                            description = { Text(stringResource(R.string.smart_trimmer_description)) },
                            trailingContent = { Switch(checked = smartTrimmer && isSmartTrimmerAvailable, onCheckedChange = onSmartTrimmerChange, enabled = isSmartTrimmerAvailable) },
                            onClick = { if (isSmartTrimmerAvailable) onSmartTrimmerChange(!smartTrimmer) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.storage),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_download),
                            title = { Text(stringResource(R.string.downloaded_songs)) },
                            description = { Text(stringResource(R.string.size_used, formatFileSize(downloadCacheSize))) },
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.clear_all),
                            title = { Text(stringResource(R.string.clear_all_downloads)) },
                            onClick = { clearDownloads = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.song_cache),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_music),
                            title = { Text(stringResource(R.string.song_cache)) },
                            description = { 
                                Text(
                                    if (maxSongCacheSize == -1) stringResource(R.string.size_used, formatFileSize(playerCacheSize))
                                    else "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                                )
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            description = { 
                                Text(
                                    when (maxSongCacheSize) {
                                        0 -> stringResource(R.string.disable)
                                        -1 -> stringResource(R.string.unlimited)
                                        else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                                    }
                                )
                            },
                            onClick = { showMaxSongCacheDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.clear_all),
                            title = { Text(stringResource(R.string.clear_song_cache)) },
                            onClick = { clearCacheDialog = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.image_cache),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.image),
                            title = { Text(stringResource(R.string.image_cache)) },
                            description = {
                                Text(
                                    if (maxImageCacheSize > 0) "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                                    else stringResource(R.string.disable)
                                )
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            description = { 
                                Text(
                                    if (maxImageCacheSize == 0) stringResource(R.string.disable)
                                    else formatFileSize(maxImageCacheSize * 1024 * 1024L)
                                )
                            },
                            onClick = { showMaxImageCacheDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.clear_all),
                            title = { Text(stringResource(R.string.clear_image_cache)) },
                            onClick = { clearImageCacheDialog = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Canvas Video Cache",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.motion_photos_on),
                            title = { Text("Canvas Cache") },
                            description = {
                                Text(
                                    if (maxCanvasCacheSize > 0) stringResource(R.string.canvas_cache_usage, stringResource(R.string.canvas_cache_items, canvasCacheSize), stringResource(R.string.canvas_cache_items, maxCanvasCacheSize))
                                    else stringResource(R.string.disable)
                                )
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            description = { 
                                Text(
                                    if (maxCanvasCacheSize == 0) stringResource(R.string.disable)
                                    else stringResource(R.string.canvas_cache_items, maxCanvasCacheSize)
                                )
                            },
                            onClick = { showMaxCanvasCacheDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.clear_all),
                            title = { Text("Clear Canvas Video Cache") },
                            onClick = { clearCanvasCacheDialog = true }
                        )
                    )
                )
            }
        }
    }
}
