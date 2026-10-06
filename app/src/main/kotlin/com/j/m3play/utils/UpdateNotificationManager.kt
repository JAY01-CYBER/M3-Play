/*
 * M3Play Utility Module
 *
 * GitHub update notification manager
 */

package com.j.m3play.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.j.m3play.BuildConfig
import com.j.m3play.MainActivity
import com.j.m3play.R
import com.j.m3play.constants.EnableUpdateNotificationKey
import com.j.m3play.constants.LastNotifiedVersionKey
import com.j.m3play.constants.LastUpdateCheckKey
import com.j.m3play.constants.UpdateChannel
import com.j.m3play.constants.UpdateChannelKey
import java.util.concurrent.TimeUnit

object UpdateNotificationManager {

    private const val CHANNEL_ID = "update_notification_channel"
    private const val NOTIFICATION_ID = 9999
    private const val WORK_NAME = "update_check_work"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(
                        R.string.update_notification_channel_name
                    ),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description =
                        context.getString(
                            R.string.update_notification_channel_desc
                        )
                }

            context.getSystemService(
                NotificationManager::class.java
            )?.createNotificationChannel(channel)
        }
    }

    fun schedulePeriodicUpdateCheck(
        context: Context,
    ) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(true)
                .build()

        val request =
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                6,
                TimeUnit.HOURS,
                30,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    fun cancelPeriodicUpdateCheck(
        context: Context,
    ) {
        WorkManager
            .getInstance(context)
            .cancelUniqueWork(WORK_NAME)
    }

    fun checkForUpdates(
        context: Context,
    ) {
        scope.launch {
            try {
                val dataStore = context.dataStore

                val enabled =
                    dataStore.data
                        .map {
                            it[EnableUpdateNotificationKey] ?: false
                        }
                        .first()

                if (!enabled) {
                    cancelPeriodicUpdateCheck(context)
                    return@launch
                }

                schedulePeriodicUpdateCheck(context)

                val updateChannel =
                    dataStore.data
                        .map {
                            it[UpdateChannelKey]?.let { value ->
                                runCatching {
                                    UpdateChannel.valueOf(value)
                                }.getOrDefault(
                                    UpdateChannel.STABLE
                                )
                            } ?: UpdateChannel.STABLE
                        }
                        .first()

                if (updateChannel == UpdateChannel.NIGHTLY) {
                    return@launch
                }

                val lastCheck =
                    dataStore.data
                        .map {
                            it[LastUpdateCheckKey] ?: 0L
                        }
                        .first()

                val now = System.currentTimeMillis()

                if (
                    now - lastCheck <
                    CHECK_INTERVAL_MS
                ) {
                    return@launch
                }

                dataStore.edit {
                    it[LastUpdateCheckKey] = now
                }

                val latestVersion =
                    Updater.getLatestVersionName()
                        .getOrNull()
                        ?: return@launch

                if (
                    Updater.isUpdateAvailable(
                        remoteVersion = latestVersion,
                        installedVersion = BuildConfig.VERSION_NAME,
                    )
                ) {
                    notifyIfNewVersion(
                        context,
                        latestVersion,
                    )
                }
            } catch (_: Exception) {
                // Background updater must never crash the app.
            }
        }
    }

    suspend fun notifyIfNewVersion(
        context: Context,
        latestVersion: String,
    ) {
        try {
            /*
             * Never notify about an older or equal release.
             */
            if (
                !Updater.isUpdateAvailable(
                    remoteVersion = latestVersion,
                    installedVersion = BuildConfig.VERSION_NAME,
                )
            ) {
                return
            }

            val dataStore = context.dataStore

            val lastNotified =
                dataStore.data
                    .map {
                        it[LastNotifiedVersionKey] ?: ""
                    }
                    .first()

            if (latestVersion == lastNotified) {
                return
            }

            showUpdateNotification(
                context,
                latestVersion,
            )

            dataStore.edit {
                it[LastNotifiedVersionKey] = latestVersion
            }
        } catch (_: Exception) {
            // Silently fail.
        }
    }

    private fun showUpdateNotification(
        context: Context,
        newVersion: String,
    ) {
        createNotificationChannel(context)

        val openAppIntent =
            Intent(
                context,
                MainActivity::class.java,
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                putExtra(
                    "navigate_to",
                    "settings/update",
                )
            }

        val openAppPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        /*
         * Uses the real release asset names:
         * M3Play.apk / app-arm64-release.apk / etc.
         */
        val downloadIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    Updater.getLatestDownloadUrl()
                ),
            )

        val downloadPendingIntent =
            PendingIntent.getActivity(
                context,
                1,
                downloadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID,
            )
                .setSmallIcon(R.drawable.small_icon)
                .setContentTitle(
                    context.getString(
                        R.string.update_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.update_notification_text,
                        newVersion,
                    )
                )
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .setContentIntent(
                    openAppPendingIntent
                )
                .setAutoCancel(true)
                .addAction(
                    R.drawable.download,
                    context.getString(R.string.download),
                    downloadPendingIntent,
                )
                .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(
                    NOTIFICATION_ID,
                    notification,
                )
        } catch (_: SecurityException) {
            // Android 13+ notification permission not granted.
        }
    }

    fun cancelUpdateNotification(
        context: Context,
    ) {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_ID)
    }
}
