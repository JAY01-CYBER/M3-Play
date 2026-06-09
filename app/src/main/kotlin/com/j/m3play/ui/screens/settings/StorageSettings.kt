package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(key = SmartTrimmerKey, defaultValue = false)
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(key = MaxImageCacheSizeKey, defaultValue = 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(key = MaxSongCacheSizeKey, defaultValue = 1024)
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(key = MaxCanvasCacheSizeKey, defaultValue = 256)
    
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(0L) }
    var downloadCacheSize by remember { mutableStateOf(0L) }
    var canvasCacheSize by remember { mutableStateOf(CanvasArtworkPlaybackCache.size()) }

    val imageCacheProgress by animateFloatAsState(targetValue = if (imageDiskCache.maxSize > 0) (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f) else 0f)
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(targetValue = if (maxSongCacheSizeBytes > 0) (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f) else 0f)
    val canvasCacheProgress by animateFloatAsState(targetValue = if (maxCanvasCacheSize > 0) (canvasCacheSize.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f) else 0f)

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) { if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false) }

    // Dialog Handlers (Kept minimal for copy-paste)
    if (clearDownloads) ActionPromptDialog(title = stringResource(R.string.clear_all_downloads), onDismiss = { clearDownloads = false }, onConfirm = { coroutineScope.launch(Dispatchers.IO) { downloadCache.keys.forEach { downloadCache.removeResource(it) } }; clearDownloads = false }, onCancel = { clearDownloads = false }, content = { Text(text = stringResource(R.string.clear_downloads_dialog)) })
    if (clearCacheDialog) ActionPromptDialog(title = stringResource(R.string.clear_song_cache), onDismiss = { clearCacheDialog = false }, onConfirm = { coroutineScope.launch(Dispatchers.IO) { playerCache.keys.forEach { playerCache.removeResource(it) } }; clearCacheDialog = false }, onCancel = { clearCacheDialog = false }, content = { Text(text = stringResource(R.string.clear_song_cache_dialog)) })
    if (clearImageCacheDialog) ActionPromptDialog(title = stringResource(R.string.clear_image_cache), onDismiss = { clearImageCacheDialog = false }, onConfirm = { coroutineScope.launch(Dispatchers.IO) { imageDiskCache.clear(); com.j.m3play.utils.ArtworkStorage.clear(context) }; clearImageCacheDialog = false }, onCancel = { clearImageCacheDialog = false }, content = { Text(text = stringResource(R.string.clear_image_cache_dialog)) })
    if (clearCanvasCacheDialog) ActionPromptDialog(title = stringResource(R.string.clear_canvas_cache), onDismiss = { clearCanvasCacheDialog = false }, onConfirm = { CanvasArtworkPlaybackCache.clear(); clearCanvasCacheDialog = false }, onCancel = { clearCanvasCacheDialog = false }, content = { Text(text = stringResource(R.string.clear_canvas_cache_dialog)) })

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
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
                CacheCard(
                    icon = R.drawable.ic_download,
                    title = stringResource(R.string.downloaded_songs),
                    description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                    progress = null,
                    actions = { PreferenceEntry(title = { Text(stringResource(R.string.clear_all_downloads)) }, onClick = { clearDownloads = true }) }
                )
            }

            item {
                CacheCard(
                    icon = R.drawable.ic_music,
                    title = stringResource(R.string.song_cache),
                    description = if (maxSongCacheSize == -1) stringResource(R.string.size_used, formatFileSize(playerCacheSize)) else "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}",
                    progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
                    actions = {
                        ListPreference(title = { Text(stringResource(R.string.max_cache_size)) }, selectedValue = maxSongCacheSize, values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1), valueText = { when (it) { 0 -> stringResource(R.string.disable); -1 -> stringResource(R.string.unlimited); else -> formatFileSize(it * 1024 * 1024L) } }, onValueSelected = onMaxSongCacheSizeChange)
                        PreferenceEntry(title = { Text(stringResource(R.string.clear_song_cache)) }, onClick = { clearCacheDialog = true })
                    }
                )
            }

            item {
                CacheCard(
                    icon = R.drawable.image,
                    title = stringResource(R.string.image_cache),
                    description = if (maxImageCacheSize > 0) "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}" else stringResource(R.string.disable),
                    progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
                    actions = {
                        ListPreference(title = { Text(stringResource(R.string.max_cache_size)) }, selectedValue = maxImageCacheSize, values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192), valueText = { when (it) { 0 -> stringResource(R.string.disable); else -> formatFileSize(it * 1024 * 1024L) } }, onValueSelected = onMaxImageCacheSizeChange)
                        PreferenceEntry(title = { Text(stringResource(R.string.clear_image_cache)) }, onClick = { clearImageCacheDialog = true })
                    }
                )
            }
            
            // ... Canvas Cache Card item same logic
        }
    }
}
// CacheCard method remains the same as in your original file.
