/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import com.j.m3play.R
import com.j.m3play.constants.AppFontKey
import com.j.m3play.constants.CustomFontFileKey
import com.j.m3play.constants.UseSystemFontKey
import com.j.m3play.ui.component.ListPreference
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.theme.AppFont
import com.j.m3play.ui.theme.importAppFont
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun FontSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (font, onFontChange) = rememberPreference(AppFontKey, "")
    val customFile by rememberPreference(CustomFontFileKey, "")
    val useSystemFont by rememberPreference(UseSystemFontKey, false)
    var importing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                val file = importAppFont(context, uri)
                var previousFile: String? = null
                context.dataStore.edit {
                    previousFile = it[CustomFontFileKey]
                    it[CustomFontFileKey] = file
                    it[AppFontKey] = AppFont.CUSTOM.name
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    previousFile?.takeIf { it != file && java.io.File(it).name == it }?.let {
                        java.io.File(context.filesDir, "fonts/$it").delete()
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
    ListPreference(
        title = { Text(stringResource(R.string.app_font)) },
        icon = { Icon(painterResource(R.drawable.text_fields), null) },
        selectedValue = font.ifEmpty { if (useSystemFont) AppFont.SYSTEM.name else AppFont.GOOGLE_SANS.name },
        values = AppFont.entries.filter { it != AppFont.CUSTOM || customFile.isNotBlank() }.map { it.name },
        valueText = { AppFont.valueOf(it).label },
        onValueSelected = onFontChange,
    )
    PreferenceEntry(
        title = { Text(stringResource(R.string.import_font)) },
        description = stringResource(R.string.import_font_description),
        icon = { Icon(painterResource(R.drawable.text_fields), null) },
        isEnabled = !importing,
        onClick = { launcher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream")) },
    )
}
