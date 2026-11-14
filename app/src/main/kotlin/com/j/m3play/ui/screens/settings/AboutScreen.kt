package com.j.m3play.ui.screens.settings

import android.content.pm.PackageManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown" } 
        catch (e: PackageManager.NameNotFoundException) { "Unknown" }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "about_logo")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000), RepeatMode.Reverse),
        label = "about_logo_scale",
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("About", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "Learn more about M3Play", 
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- HEADER ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(160.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)).scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.ic_app_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "M3Play", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "v$versionName 🚀",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape).padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Crafting smooth, expressive and\npremium music experiences \uD83C\uDFA7",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- ALL LISTS (Glossy Unified UI) ---
            Material3SettingsGroup(
                title = "Developer",
                items = listOf(
                    Material3SettingsItem(
                        leadingContent = { AsyncImage(model = "https://github.com/JAY01-CYBER.png", contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop) },
                        title = { Text("Jay Chaudhary ⚡") },
                        description = { Text("Creator of M3Play • Android Developer") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/JAY01-CYBER") }
                    )
                )
            )

            Material3SettingsGroup(
                title = "Connect",
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.github),
                        title = { Text("GitHub") },
                        description = { Text("View source code") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/JAY01-CYBER") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.telegram),
                        title = { Text("Telegram") },
                        description = { Text("Updates & news") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://t.me/M3Play_updates") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.alternate_email),
                        title = { Text("Discord") },
                        description = { Text("Chat with community") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://discord.gg/zdbAuRpVt") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.website),
                        title = { Text("Website") },
                        description = { Text("m3play.app") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://jay01-cyber.github.io/M3Play-Website/") }
                    )
                )
            )

            Material3SettingsGroup(
                title = "The Team",
                items = listOf(
                    Material3SettingsItem(
                        leadingContent = { AsyncImage(model = "https://github.com/pr13260.png", contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop) },
                        title = { Text("Prince Raj") },
                        description = { Text("Contributor") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/pr13260") }
                    ),
                    Material3SettingsItem(
                        leadingContent = { AsyncImage(model = "https://github.com/vivizzz007.png", contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop) },
                        title = { Text("vivi") },
                        description = { Text("Contributor") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/vivizzz007") }
                    ),
                    Material3SettingsItem(
                        leadingContent = { AsyncImage(model = "https://github.com/fluxx-pro.png", contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop) },
                        title = { Text("Jay Chaudhary") },
                        description = { Text("Contributor") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/fluxx-pro") }
                    )
                )
            )

            Material3SettingsGroup(
                title = "Special Helpers",
                items = listOf(
                    Material3SettingsItem(
                        leadingContent = { AsyncImage(model = "https://github.com/m4trxx.png", contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop) },
                        title = { Text("M4TRX") },
                        description = { Text("Special Helper") },
                        showChevron = true,
                        onClick = { uriHandler.openUri("https://github.com/m4trxx") }
                    )
                )
            )

            Material3SettingsGroup(
                title = "Credits & Open-Source",
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.ic_app_logo),
                        title = { Text("ArchiveTune") },
                        description = { Text("Base project and foundation") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.ic_app_logo),
                        title = { Text("Lyrics & metadata services") },
                        description = { Text("Used for lyrics, track information") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.ic_app_logo),
                        title = { Text("Open-source community") },
                        description = { Text("Thanks to all upstream contributors") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
