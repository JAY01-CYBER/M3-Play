/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.extensions

import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

fun ExoPlayer.setOffloadEnabled(enabled: Boolean) {
    val candidates =
        listOf(
            "experimentalSetOffloadSchedulingEnabled",
            "setOffloadSchedulingEnabled",
            "setOffloadEnabled",
        )

    for (name in candidates) {
        try {
            val method = this::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
            method.invoke(this, enabled)
            return
        } catch (_: NoSuchMethodException) {
        } catch (t: Throwable) {
            Timber.tag("ExoPlayerExtensions").v(t, "$name reflection failed")
            return
        }
    }

    Timber.tag("ExoPlayerExtensions").v("No offload toggle method found")
}
