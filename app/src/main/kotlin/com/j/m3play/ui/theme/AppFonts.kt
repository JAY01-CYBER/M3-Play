/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.j.m3play.R
import com.j.m3play.constants.AppFontKey
import com.j.m3play.constants.CustomFontFileKey
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal enum class AppFont(val label: String) {
    SYSTEM("System"), POPPINS("Poppins"), GOOGLE_SANS("Google Sans"),
    SF_PRO("SF Pro Display"), SERIF("Serif"), CUSTOM("Imported font")
}

@Composable
internal fun rememberAppFontFamily(useSystemFont: Boolean): FontFamily {
    val context = LocalContext.current
    val choice by rememberPreference(AppFontKey, "")
    val customFile by rememberPreference(CustomFontFileKey, "")
    val selected = AppFont.entries.firstOrNull { it.name == choice }
        ?: if (useSystemFont) AppFont.SYSTEM else AppFont.GOOGLE_SANS
    val custom by produceState<Typeface?>(null, selected, customFile) {
        value = if (selected == AppFont.CUSTOM && customFile.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    require(File(customFile).name == customFile)
                    Typeface.createFromFile(File(context.filesDir, "fonts/$customFile"))
                }.getOrNull()
            }
        } else null
    }
    return when (selected) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.POPPINS -> FontFamily(Font(R.font.poppins))
        AppFont.GOOGLE_SANS -> FontFamily(
            Font(R.font.google_sans_regular),
            Font(R.font.google_sans_medium, FontWeight.Medium),
            Font(R.font.google_sans_bold, FontWeight.Bold),
        )
        AppFont.SF_PRO -> FontFamily(Font(R.font.sfprodisplaybold, FontWeight.Bold))
        AppFont.SERIF -> FontFamily.Serif
        AppFont.CUSTOM -> custom?.let { FontFamily(it) } ?: FontFamily.Default
    }
}

/** Copy a validated font into private storage; no persistent access to a user's document is needed. */
internal suspend fun importAppFont(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val directory = File(context.filesDir, "fonts").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.ttf")
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open this font" }
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 8 * 1024 * 1024) { "Choose a font smaller than 8 MB" }
                    output.write(buffer, 0, count)
                }
                require(total > 0) { "The font file is empty" }
            }
        }
        Typeface.createFromFile(file)
        file.name
    } catch (error: Exception) {
        file.delete()
        throw error
    }
}
