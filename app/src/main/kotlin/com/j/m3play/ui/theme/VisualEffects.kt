/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.ui.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.utils.rememberPreference

val LocalBlurEnabled = staticCompositionLocalOf { false }

@Composable
internal fun rememberBlurEnabled(): Boolean {
    val context = LocalContext.current
    val disabled by rememberPreference(DisableBlurKey, true)
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    val lowRam = remember(context) { context.getSystemService(ActivityManager::class.java).isLowRamDevice }
    var powerSave by remember { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(context, power) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { powerSave = power.isPowerSaveMode }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return !disabled && !powerSave && !lowRam && Build.VERSION.SDK_INT >= 31
}

/** Apply bounded GPU blur only when the global preference and device allow it. */
@Composable
fun Modifier.appBlur(radius: Dp, edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle): Modifier =
    if (LocalBlurEnabled.current && radius > 0.dp) blur(radius.coerceAtMost(48.dp), edgeTreatment) else this
