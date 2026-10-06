package com.j.m3play.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (id == -1L) return

        val apk = runCatching { Updater.prepareDownloadedApk(context, id) }.getOrNull() ?: return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            apk,
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("M3Play update", uri)
        }

        runCatching { context.startActivity(installIntent) }
    }
}
