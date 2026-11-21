package com.j.m3play.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.j.m3play.BuildConfig
import com.j.m3play.MainActivity
import com.j.m3play.R
import com.j.m3play.constants.EnableUpdateNotificationKey
import com.j.m3play.constants.LastNotifiedVersionKey
import com.j.m3play.constants.LastUpdateCheckKey
import com.j.m3play.constants.UpdateChannel
import com.j.m3play.constants.UpdateChannelKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object UpdateNotificationManager {
    private const val CHANNEL_ID = "update_notification_channel"
    private const val NOTIFICATION_ID = 9999
    private const val WORK_NAME = "update_check_work"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.update_notification_channel_desc)
                }
            )
        }
    }

    fun schedulePeriodicUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            6, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPeriodicUpdateCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun checkForUpdates(context: Context) {
        scope.launch {
            try {
                val dataStore = context.dataStore
                val enabled = dataStore.data
                    .map { it[EnableUpdateNotificationKey] ?: false }
                    .first()

                if (!enabled) {
                    cancelPeriodicUpdateCheck(context)
                    return@launch
                }

                val channel = dataStore.data
                    .map {
                        it[UpdateChannelKey]?.let { value ->
                            runCatching { UpdateChannel.valueOf(value) }
                                .getOrDefault(UpdateChannel.STABLE)
                        } ?: UpdateChannel.STABLE
                    }
                    .first()

                if (channel == UpdateChannel.NIGHTLY) return@launch

                val lastCheck = dataStore.data
                    .map { it[LastUpdateCheckKey] ?: 0L }
                    .first()

                val now = System.currentTimeMillis()
                if (now - lastCheck < CHECK_INTERVAL_MS) return@launch

                dataStore.edit { it[LastUpdateCheckKey] = now }

                Updater.getLatestVersionName().onSuccess { latest ->
                    if (Updater.isUpdateAvailable(latest, BuildConfig.VERSION_NAME)) {
                        notifyIfNewVersion(context, latest)
                    }
                }
            } catch (_: Exception) {
                // Update checking is intentionally non-fatal.
            }
        }
    }

    suspend fun notifyIfNewVersion(context: Context, latestVersion: String) {
        if (!Updater.isUpdateAvailable(latestVersion, BuildConfig.VERSION_NAME)) return

        val dataStore = context.dataStore
        val lastNotified = dataStore.data
            .map { it[LastNotifiedVersionKey] ?: "" }
            .first()

        if (latestVersion == lastNotified) return

        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "settings/update")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(
                context.getString(R.string.update_notification_text, latestVersion)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        dataStore.edit { it[LastNotifiedVersionKey] = latestVersion }
    }

    fun cancelUpdateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
