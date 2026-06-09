package com.j.m3play.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.j.m3play.BuildConfig
import com.j.m3play.R

// टॉप 4 ग्रिड आइटम्स के लिए
@Composable
fun buildQuickSettings(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsItem> = listOf(
    SettingsItem(
        icon = painterResource(R.drawable.palette),
        title = stringResource(R.string.appearance),
        subtitle = "Dark theme",
        accentColor = Color(0xFF8B5CF6), // Purple accent like image
        keywords = listOf("theme", "palette", "dark"),
        onClick = { resetSearch(); navController.navigate("settings/appearance") },
    ),
    SettingsItem(
        icon = painterResource(R.drawable.play),
        title = "Player & Audio",
        subtitle = "Audio quality",
        accentColor = Color(0xFFEC4899), // Pink accent
        keywords = listOf("audio", "playback"),
        onClick = { resetSearch(); navController.navigate("settings/player") },
    ),
    SettingsItem(
        icon = painterResource(R.drawable.language),
        title = "Content",
        subtitle = "Language & region",
        accentColor = Color(0xFF3B82F6), // Blue accent
        keywords = listOf("language", "content"),
        onClick = { resetSearch(); navController.navigate("settings/content") },
    ),
    SettingsItem(
        icon = painterResource(R.drawable.security),
        title = "Privacy",
        subtitle = "Privacy & history",
        accentColor = Color(0xFF10B981), // Green accent
        keywords = listOf("privacy", "history"),
        onClick = { resetSearch(); navController.navigate("settings/privacy") },
    )
)

// नीचे वाले मेन लिस्ट कार्ड्स के लिए
@Composable
fun buildSettingsGroups(
    navController: NavController,
    isAndroid12OrLater: Boolean,
    hasUpdate: Boolean,
    context: Context,
    resetSearch: () -> Unit,
): List<SettingsGroup> = buildList {

    add(
        SettingsGroup(
            title = "Player & Content",
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = "Player & Audio",
                    subtitle = "Audio quality, equalizer & playback",
                    accentColor = Color(0xFF8B5CF6),
                    onClick = { resetSearch(); navController.navigate("settings/player") },
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.language), // Use any lyrics icon you have
                    title = "Lyrics",
                    subtitle = "Lyrics appearance & behavior",
                    accentColor = Color(0xFF3B82F6),
                    onClick = { resetSearch(); navController.navigate("settings/lyrics") },
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.storage), // Use download icon
                    title = "Downloads",
                    subtitle = "Download quality & storage",
                    accentColor = Color(0xFFF59E0B), // Orange/Yellow
                    onClick = { resetSearch(); navController.navigate("settings/downloads") },
                ),
            )
        )
    )

    add(
        SettingsGroup(
            title = "Privacy & Data",
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = "Privacy",
                    subtitle = "History, privacy & data controls",
                    accentColor = Color(0xFF10B981),
                    onClick = { resetSearch(); navController.navigate("settings/privacy") },
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = "Storage & Data",
                    subtitle = "Cache, storage & data usage",
                    accentColor = Color(0xFF3B82F6),
                    onClick = { resetSearch(); navController.navigate("settings/storage") },
                ),
            )
        )
    )

    add(
        SettingsGroup(
            title = "More",
            items = buildList {
                add(
                    SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = "About",
                        subtitle = "App version & information",
                        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { resetSearch(); navController.navigate("settings/about") },
                    )
                )
                if (hasUpdate) {
                    add(
                        SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = "Updates",
                            subtitle = "New version available",
                            accentColor = Color(0xFF10B981),
                            onClick = { resetSearch(); navController.navigate("settings/update") },
                        )
                    )
                }
            }
        )
    )
}

@Composable
fun buildInternalItems(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsItem> = emptyList() // इसे आप अपनी ज़रूरत के हिसाब से भर सकते हैं
