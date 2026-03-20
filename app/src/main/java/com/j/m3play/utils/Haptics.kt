package com.j.m3play.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object Haptics {

    fun click(haptic: HapticFeedback? = null, context: Context? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            ?: context?.let { vibrate(it, 12L, 80) }
    }

    fun tick(haptic: HapticFeedback? = null, context: Context? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            ?: context?.let { vibrate(it, 8L, 60) }
    }

    fun longPress(haptic: HapticFeedback? = null, context: Context? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            ?: context?.let { vibrate(it, 35L, 180) }
    }

    fun success(context: Context) {
        waveform(
            context = context,
            timings = longArrayOf(0, 20, 30, 35),
            amplitudes = intArrayOf(0, 120, 0, 180)
        )
    }

    fun error(context: Context) {
        waveform(
            context = context,
            timings = longArrayOf(0, 35, 40, 35, 40, 45),
            amplitudes = intArrayOf(0, 220, 0, 180, 0, 255)
        )
    }

    private fun waveform(
        context: Context,
        timings: LongArray,
        amplitudes: IntArray
    ) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings.sum())
        }
    }

    private fun vibrate(
        context: Context,
        duration: Long,
        amplitude: Int
    ) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    amplitude.coerceIn(1, 255)
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
