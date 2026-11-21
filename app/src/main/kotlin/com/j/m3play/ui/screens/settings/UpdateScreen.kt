package com.j.m3play.ui.screens.settings

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.j.m3play.BuildConfig
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.EnableUpdateNotificationKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.UpdateNotificationManager
import com.j.m3play.utils.Updater
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (notificationsEnabled, setNotificationsEnabled) = rememberPreference(EnableUpdateNotificationKey, false)

    var latestVersion by remember { mutableStateOf<String?>(null) }
    var latestRelease by remember { mutableStateOf<com.j.m3play.utils.ReleaseInfo?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setNotificationsEnabled(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    fun checkForUpdate() {
        if (checking) return
        checking = true
        checkError = null
        scope.launch {
            val result = Updater.getLatestReleaseInfo()
            result.onSuccess { release ->
                latestRelease = release
                latestVersion = release.tagName.removePrefix("v").ifBlank { release.name }
            }.onFailure {
                latestRelease = null
                latestVersion = null
                checkError = it.message ?: "Unable to check for updates"
            }
            checking = false
        }
    }

    fun downloadUpdate() {
        val release = latestRelease ?: return
        val remoteVersion = latestVersion ?: return
        if (!Updater.isUpdateAvailable(remoteVersion, BuildConfig.VERSION_NAME)) return
        val url = Updater.getDownloadUrl(release) ?: run { checkError = "No APK asset found for this device."; return }
        
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("M3Play $remoteVersion")
            .setDescription("Downloading M3Play update")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(1) 
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "M3Play-update.apk")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        downloading = true
    }

    LaunchedEffect(Unit) { checkForUpdate() }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(text = "Enable Notifications", fontWeight = FontWeight.Bold) },
            text = { Text("M3Play can periodically check GitHub for stable releases and notify you when a newer version exists.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            setNotificationsEnabled(true)
                            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                        }
                    }
                ) { Text("Enable") }
            },
            dismissButton = { TextButton(onClick = { showNotificationDialog = false }) { Text("Cancel") } }
        )
    }

    val hasUpdate = latestVersion?.let { Updater.isUpdateAvailable(it, BuildConfig.VERSION_NAME) } == true

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Updates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val bottomInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInsets + 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.surfaceContainerHigh))).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                    Icon(painter = painterResource(R.drawable.update), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Current Version", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    when {
                                        checking -> "Checking GitHub releases…"
                                        checkError != null -> checkError!!
                                        hasUpdate -> "Update available: $latestVersion"
                                        latestVersion != null -> "You're up to date"
                                        else -> "Checking…"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (hasUpdate) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(onClick = { checkForUpdate() }, enabled = !checking, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
                    Text(if (checking) "Checking…" else "Check for updates", fontWeight = FontWeight.Bold)
                }
            }

            if (hasUpdate) {
                item {
                    Button(onClick = { downloadUpdate() }, enabled = !downloading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
                        Text(if (downloading) "Download started" else "Download update", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = "Notification Settings",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = { Text("Enable Update Notification") },
                            description = { Text(if (notificationsEnabled) "Stable GitHub update checks are enabled" else "Disabled") },
                            trailingContent = {
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) { showNotificationDialog = true } 
                                        else { setNotificationsEnabled(false); UpdateNotificationManager.cancelPeriodicUpdateCheck(context) }
                                    },
                                    thumbContent = { Icon(painterResource(if (notificationsEnabled) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                )
                            },
                            onClick = {
                                if (!notificationsEnabled) showNotificationDialog = true
                                else { setNotificationsEnabled(false); UpdateNotificationManager.cancelPeriodicUpdateCheck(context) }
                            }
                        )
                    )
                )
            }

            item {
                Button(
                    onClick = { navController.navigate("settings/changelog") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(painterResource(R.drawable.update), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("View Changelog", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
