package com.j.m3play.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.j.m3play.playback.MusicService

class M3VinylWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MusicService::class.java).apply { action = ACTION_UPDATE_WIDGET }
        try { context.startService(intent) } catch (e: Exception) {}
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.j.m3play.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.j.m3play.widget.NEXT"
        const val ACTION_PREVIOUS = "com.j.m3play.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.j.m3play.widget.UPDATE_WIDGET"
    }
}
