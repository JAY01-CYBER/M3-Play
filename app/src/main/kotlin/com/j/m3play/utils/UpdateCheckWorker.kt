/*
 * M3Play Utility Module
 *
 * Background GitHub update checker
 */

package com.j.m3play.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.j.m3play.BuildConfig
import com.j.m3play.constants.EnableUpdateNotificationKey
import com.j.m3play.constants.UpdateChannel
import com.j.m3play.constants.UpdateChannelKey

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            val dataStore = applicationContext.dataStore

            val enabled =
                dataStore.data
                    .map {
                        it[EnableUpdateNotificationKey] ?: false
                    }
                    .first()

            if (!enabled) {
                return Result.success()
            }

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
                return Result.success()
            }

            val latestVersion =
                Updater.getLatestVersionName()
                    .getOrNull()
                    ?: return Result.retry()

            /*
             * IMPORTANT:
             * Do NOT use "latest != installed".
             * That would treat 3.1.0 as an update for 3.2.0.
             */
            if (
                Updater.isUpdateAvailable(
                    remoteVersion = latestVersion,
                    installedVersion = BuildConfig.VERSION_NAME,
                )
            ) {
                UpdateNotificationManager.notifyIfNewVersion(
                    applicationContext,
                    latestVersion,
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
}
