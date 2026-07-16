package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.ListPreference
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.component.SwitchPreference
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
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(
        key = SmartTrimmerKey,
        defaultValue = false
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(0L) }
    var downloadCacheSize by remember { mutableStateOf(0L) }
    var canvasCacheSize by remember { mutableStateOf(CanvasArtworkPlaybackCache.size()) }

    val imageCacheProgress by animateFloatAsState(
        targetValue = if (imageDiskCache.maxSize > 0) {
            (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f)
        } else 0f,
        label = "imageCacheProgress",
    )
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(
        targetValue = if (maxSongCacheSizeBytes > 0) {
            (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f)
        } else 0f,
        label = "playerCacheProgress",
    )
    val canvasCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasCacheSize.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasCacheProgress",
    )

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
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
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
            playerCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize =
                withContext(Dispatchers.IO) {
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

    if (clearDownloads) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_all_downloads),
            onDismiss = { clearDownloads = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.keys.forEach { key ->
                        downloadCache.removeResource(key)
                    }
                }
                clearDownloads = false
            },
            onCancel = { clearDownloads = false },
            content = { Text(text = stringResource(R.string.clear_downloads_dialog)) }
        )
    }

    if (clearCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_song_cache),
            onDismiss = { clearCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                }
                clearCacheDialog = false
            },
            onCancel = { clearCacheDialog = false },
            content = { Text(text = stringResource(R.string.clear_song_cache_dialog)) }
        )
    }

    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_image_cache),
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                    com.j.m3play.utils.ArtworkStorage.clear(context)
                }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = { Text(text = stringResource(R.string.clear_image_cache_dialog)) }
        )
    }

    if (clearCanvasCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_canvas_cache),
            onDismiss = { clearCanvasCacheDialog = false },
            onConfirm = {
                CanvasArtworkPlaybackCache.clear()
                clearCanvasCacheDialog = false
            },
            onCancel = { clearCanvasCacheDialog = false },
            content = { Text(text = stringResource(R.string.clear_canvas_cache_dialog)) }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.storage),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item {
                Column {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.smart_trimmer)) },
                        description = stringResource(R.string.smart_trimmer_description),
                        checked = smartTrimmer && isSmartTrimmerAvailable,
                        onCheckedChange = onSmartTrimmerChange,
                        isEnabled = isSmartTrimmerAvailable,
                    )
                }
            }

            item {
                CacheSection(
                    icon = R.drawable.ic_download,
                    title = stringResource(R.string.downloaded_songs),
                    description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                    progress = null,
                    actions = {
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.clear_all_downloads)) },
                            onClick = { clearDownloads = true },
                        )
                    }
                )
            }

            item {
                CacheSection(
                    icon = R.drawable.ic_music,
                    title = stringResource(R.string.song_cache),
                    description = if (maxSongCacheSize == -1) {
                        stringResource(R.string.size_used, formatFileSize(playerCacheSize))
                    } else {
                        "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                    },
                    progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
                    actions = {
                        ListPreference(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            selectedValue = maxSongCacheSize,
                            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                            valueText = {
                                when (it) {
                                    0 -> stringResource(R.string.disable)
                                    -1 -> stringResource(R.string.unlimited)
                                    else -> formatFileSize(it * 1024 * 1024L)
                                }
                            },
                            onValueSelected = onMaxSongCacheSizeChange,
                        )
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.clear_song_cache)) },
                            onClick = { clearCacheDialog = true },
                        )
                    }
                )
            }

            item {
                CacheSection(
                    icon = R.drawable.image,
                    title = stringResource(R.string.image_cache),
                    description = if (maxImageCacheSize > 0) {
                        "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                    } else {
                        stringResource(R.string.disable)
                    },
                    progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
                    actions = {
                        ListPreference(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            selectedValue = maxImageCacheSize,
                            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                            valueText = {
                                when (it) {
                                    0 -> stringResource(R.string.disable)
                                    else -> formatFileSize(it * 1024 * 1024L)
                                }
                            },
                            onValueSelected = onMaxImageCacheSizeChange,
                        )
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.clear_image_cache)) },
                            onClick = { clearImageCacheDialog = true },
                        )
                    }
                )
            }

            item {
                CacheSection(
                    icon = R.drawable.motion_photos_on,
                    title = stringResource(R.string.canvas_cache),
                    description = if (maxCanvasCacheSize > 0) {
                        stringResource(
                            R.string.canvas_cache_usage,
                            stringResource(R.string.canvas_cache_items, canvasCacheSize),
                            stringResource(R.string.canvas_cache_items, maxCanvasCacheSize),
                        )
                    } else {
                        stringResource(R.string.disable)
                    },
                    progress = if (maxCanvasCacheSize > 0) canvasCacheProgress else null,
                    actions = {
                        ListPreference(
                            title = { Text(stringResource(R.string.max_cache_size)) },
                            selectedValue = maxCanvasCacheSize,
                            values = listOf(0, 64, 128, 256, 512, 1024),
                            valueText = {
                                when (it) {
                                    0 -> stringResource(R.string.disable)
                                    else -> stringResource(R.string.canvas_cache_items, it)
                                }
                            },
                            onValueSelected = onMaxCanvasCacheSizeChange,
                        )
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.clear_canvas_cache)) },
                            onClick = { clearCanvasCacheDialog = true },
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CacheSection(
    icon: Int,
    title: String,
    description: String,
    progress: Float?,
    actions: @Composable () -> Unit
) {
    Column(Modifier.padding(top = 24.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp).size(24.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (progress != null) {
            Spacer(Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.padding(8.dp))
        actions()
    }
}
