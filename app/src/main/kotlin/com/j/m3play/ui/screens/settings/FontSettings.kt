/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.j.m3play.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.AppFontKey
import com.j.m3play.constants.CustomFontFileKey
import com.j.m3play.constants.UseSystemFontKey
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.theme.AppFont
import com.j.m3play.ui.theme.importAppFont
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontSettings(navController: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val (font, onFontChange) = rememberPreference(AppFontKey, "")
    val customFile by rememberPreference(CustomFontFileKey, "")
    val useSystemFont by rememberPreference(UseSystemFontKey, false)
    
    var importing by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                importing = true
                try {
                    val file = importAppFont(context, uri)
                    var previousFile: String? = null
                    context.dataStore.edit {
                        previousFile = it[CustomFontFileKey]
                        it[CustomFontFileKey] = file
                        it[AppFontKey] = AppFont.CUSTOM.name
                    }
                    withContext(Dispatchers.IO) {
                        previousFile?.takeIf { it != file && File(it).name == it }?.let {
                            File(context.filesDir, "fonts/$it").delete()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Toast.makeText(context, R.string.font_import_failed, Toast.LENGTH_LONG).show()
                } finally {
                    importing = false
                }
            }
        }
    }

    val currentFont = font.ifEmpty { if (useSystemFont) AppFont.SYSTEM.name else AppFont.GOOGLE_SANS.name }
    val fontOptions = AppFont.entries.filter { it != AppFont.CUSTOM || customFile.isNotBlank() }.map { it.name }

    if (showFontDialog) {
        EnumDialog(
            onDismiss = { showFontDialog = false },
            onSelect = { onFontChange(it); showFontDialog = false },
            title = stringResource(R.string.app_font),
            current = currentFont,
            values = fontOptions,
            valueText = { AppFont.valueOf(it).label }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_font), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                        }
                    }
                },
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
                Material3SettingsGroup(
                    title = "Typography",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.app_font)) },
                            description = { Text(AppFont.valueOf(currentFont).label) },
                            onClick = { showFontDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.import_font)) },
                            description = { Text(stringResource(R.string.import_font_description)) },
                            enabled = !importing,
                            trailingContent = if (importing) {
                                { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = { launcher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream")) }
                        )
                    )
                )
            }
        }
    }
}
