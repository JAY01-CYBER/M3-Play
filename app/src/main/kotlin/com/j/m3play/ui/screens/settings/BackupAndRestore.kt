package com.j.m3play.ui.screens.settings

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.ShowSpotifyPlaylistsKey
import com.j.m3play.db.entities.Song
import com.j.m3play.spotify.SpotifyAuth
import com.j.m3play.spotify.SpotifyAccountUiState
import com.j.m3play.spotify.SpotifyAccountViewModel
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.component.PreferenceGroup
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.ui.menu.AddToPlaylistDialogOnline
import com.j.m3play.ui.menu.LoadingScreen
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.BackupRestoreViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CSV_MIME_TYPES = arrayOf(
    "text/csv", "text/x-csv", "text/comma-separated-values", "text/x-comma-separated-values",
    "application/csv", "application/x-csv", "application/vnd.ms-excel", "text/plain", "text/*", "application/octet-stream"
)

private val SpotifyAccountIconSize = 44.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
    spotifyAccountViewModel: SpotifyAccountViewModel = hiltViewModel(),
) {
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable { mutableStateOf(false) }
    var isProgressStarted by rememberSaveable { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressPercentage by rememberSaveable { mutableIntStateOf(0) }
    var showSpotifyLogin by rememberSaveable { mutableStateOf(false) }

    val backupRestoreProgress by viewModel.backupRestoreProgress.collectAsStateWithLifecycle()
    val spotifyState by spotifyAccountViewModel.uiState.collectAsStateWithLifecycle()
    val (showSpotifyPlaylists, onShowSpotifyPlaylistsChange) = rememberPreference(ShowSpotifyPlaylistsKey, false)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) { viewModel.backup(context, uri) }
    }
    
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { viewModel.restore(context, uri) }
    }
    
    val importPlaylistFromCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = viewModel.importPlaylistFromCsv(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) { showChoosePlaylistDialogOnline = true }
        }
    }
    
    val importM3uLauncherOnline = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) { showChoosePlaylistDialogOnline = true }
        }
    }

    LaunchedEffect(spotifyState.isAuthenticated) {
        if (spotifyState.isAuthenticated) { showSpotifyLogin = false }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroup(title = "Internal Service") {
            PreferenceEntry(
                title = { Text(stringResource(R.string.action_backup)) },
                description = "Export library, settings, and account details.",
                icon = { Icon(painterResource(R.drawable.backup), null) },
                onClick = {
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    backupLauncher.launch("${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
                },
            )

            PreferenceEntry(
                title = { Text(stringResource(R.string.action_restore)) },
                description = "Restore selected data from a previous backup.",
                icon = { Icon(painterResource(R.drawable.restore), null) },
                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream")) },
            )

            PreferenceEntry(
                title = { Text("Import a \"m3u\" playlists") },
                description = "M3U audio playlist",
                icon = { Icon(painterResource(R.drawable.playlist_import), null) },
                onClick = { importM3uLauncherOnline.launch(arrayOf("audio/*")) },
            )

            PreferenceEntry(
                title = { Text("Import a \"csv\" playlists") },
                description = "CSV playlist file",
                icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                onClick = { importPlaylistFromCsv.launch(CSV_MIME_TYPES) },
            )
        }

        PreferenceGroup(title = "External Service") {
            SpotifyAccountPreferences(
                state = spotifyState,
                showPlaylists = showSpotifyPlaylists,
                onConnectClick = { showSpotifyLogin = true },
                onShowPlaylistsChange = onShowSpotifyPlaylistsChange,
                onReloadClick = spotifyAccountViewModel::reloadPlaylists,
                onLogoutClick = { spotifyAccountViewModel.logout() },
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.backup_restore)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    )

    if (showSpotifyLogin) {
        SpotifyLoginSheet(
            onDismiss = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                showSpotifyLogin = false
                spotifyAccountViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
            },
        )
    }

    spotifyState.errorMessage?.let { error ->
        SpotifyErrorDialog(message = error, onDismiss = spotifyAccountViewModel::dismissError)
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { isProgressStarted = it },
        onPercentageChange = { progressPercentage = it },
        onStatusChange = { progressStatus = it },
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10_000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    LoadingScreen(
        isVisible = backupRestoreProgress != null || isProgressStarted,
        value = backupRestoreProgress?.percent ?: progressPercentage,
        title = backupRestoreProgress?.title,
        stepText = backupRestoreProgress?.step ?: progressStatus,
        indeterminate = backupRestoreProgress?.indeterminate ?: false,
    )
}

@Composable
private fun SpotifyAccountPreferences(
    state: SpotifyAccountUiState,
    showPlaylists: Boolean,
    onConnectClick: () -> Unit,
    onShowPlaylistsChange: (Boolean) -> Unit,
    onReloadClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    if (!state.isAuthenticated) {
        PreferenceEntry(
            title = { Text("Connect Spotify") },
            description = "Spotify is not connected",
            icon = { Icon(painterResource(R.drawable.spotify_icon), null) },
            trailingContent = {
                AnimatedVisibility(visible = state.isLoading) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                }
            },
            onClick = onConnectClick,
            isEnabled = !state.isLoading,
        )
        return
    }

    PreferenceEntry(
        title = {
            Text(
                text = if (state.accountName.isNotBlank()) "Connected as ${state.accountName}" else "Spotify Account",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        description = when {
            state.isLoading -> "Loading library..."
            state.playlistCount > 0 -> "${state.playlistCount} playlists available"
            else -> "No playlists found"
        },
        icon = { SpotifyAccountIcon(avatarUrl = state.accountAvatarUrl) },
        trailingContent = {
            AnimatedVisibility(visible = state.isLoading) {
                CircularWavyProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
            }
        },
        isEnabled = false,
    )

    SwitchPreference(
        title = { Text("Show Playlist") },
        description = "Show Spotify playlists in Library.",
        icon = { Icon(painterResource(R.drawable.spotify_icon), null) },
        checked = showPlaylists,
        onCheckedChange = onShowPlaylistsChange,
        isEnabled = !state.isLoading,
    )

    PreferenceEntry(
        title = { Text("Reload Playlist") },
        description = "Refresh Spotify playlists from your account.",
        icon = { Icon(painterResource(R.drawable.sync), null) },
        onClick = onReloadClick,
        isEnabled = !state.isLoading,
    )

    PreferenceEntry(
        title = { Text(stringResource(R.string.action_logout)) },
        icon = { Icon(painterResource(R.drawable.logout), null) },
        onClick = onLogoutClick,
        isEnabled = !state.isLoading,
    )
}

@Composable
private fun SpotifyAccountIcon(avatarUrl: String?) {
    val context = LocalContext.current
    val requestSize = with(LocalDensity.current) { SpotifyAccountIconSize.roundToPx() }
    val accountIcon = painterResource(R.drawable.spotify_icon)
    val imageRequest = remember(context, avatarUrl, requestSize) {
        avatarUrl?.takeIf(String::isNotBlank)?.let { ImageRequest.Builder(context).data(it).size(requestSize).build() }
    }

    Box(
        modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageRequest != null) {
            AsyncImage(model = imageRequest, contentDescription = null, placeholder = accountIcon, error = accountIcon, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(painter = accountIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Spotify Login", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Waiting for Spotify login", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large),
                factory = { context ->
                    WebView(context).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                        webViewClient = object : WebViewClient() {
                            private fun captureCookies(url: String?): Boolean {
                                if (captured) return true
                                val cookies = readSpotifyCookies(cookieManager, url)
                                val spDc = cookies["sp_dc"].orEmpty()
                                if (spDc.isBlank()) return false
                                captured = true
                                cookieManager.flush()
                                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                                return true
                            }
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = captureCookies(request.url?.toString())
                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) { captureCookies(url) }
                            override fun onPageFinished(view: WebView, url: String?) { captureCookies(url) }
                        }
                        webView = this
                        cookieManager.removeAllCookies(null)
                        cookieManager.flush()
                        loadUrl(SpotifyAuth.LOGIN_URL)
                    }
                },
                update = { view -> webView = view },
            )
        }
    }
}

private fun readSpotifyCookies(cookieManager: CookieManager, currentUrl: String?): Map<String, String> {
    val urls = linkedSetOf("https://open.spotify.com", "https://accounts.spotify.com", "https://spotify.com")
    currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
    val cookies = linkedMapOf<String, String>()
    cookieManager.flush()
    urls.forEach { url ->
        cookieManager.getCookie(url)?.split(";")?.map(String::trim)?.filter(String::isNotBlank)?.forEach { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@forEach
            val key = part.substring(0, separator).trim()
            val value = part.substring(separator + 1).trim()
            if (key.isNotBlank()) { cookies[key] = value }
        }
    }
    return cookies
}

private fun String.toSpotifyCookieOrigin(): String? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
    val scheme = uri.scheme?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) } ?: "https"
    return "$scheme://$host"
}

@Composable
private fun SpotifyErrorDialog(message: String, onDismiss: () -> Unit) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.import_failed)) },
        buttons = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(android.R.string.ok)) } },
    ) { Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
