package com.j.m3play.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import com.j.m3play.playback.MusicService

class M3VinylWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.j.m3play.widget.ACTION_PLAY_PAUSE"
        const val ACTION_LIKE = "com.j.m3play.widget.ACTION_LIKE" // Yahan Like action add kiya
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MusicService::class.java).apply { action = "UPDATE_WIDGET" }
        try { context.startService(intent) } catch (e: Exception) {}
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val action = intent.action
        // Agar Play ya Like press hua hai toh MusicService ko bhejo
        if (action == ACTION_PLAY_PAUSE || action == ACTION_LIKE) {
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                this.action = action
                putExtras(intent)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
