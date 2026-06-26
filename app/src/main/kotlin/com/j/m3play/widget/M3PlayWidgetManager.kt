package com.j.m3play.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.j.m3play.MainActivity
import com.j.m3play.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3PlayWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context).crossfade(false).build()
    }

    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null

    suspend fun updateWidget(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val albumArt: Bitmap?
        if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null) {
            albumArt = cachedAlbumArt
        } else {
            albumArt = artworkUri?.let { loadAlbumArt(it, 300) }
            cachedArtworkUri = artworkUri
            cachedAlbumArt = albumArt
        }

        val componentName = ComponentName(context, M3PlayWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (widgetIds.isNotEmpty()) {
            widgetIds.forEach { widgetId ->
                val views = createYtMusicRemoteViews(title, artist, albumArt, isPlaying)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun createYtMusicRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_yt_music)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 24f)
            views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.default_album_art)
        }

        // Apne icons R.drawable me ensure karein
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_root_container, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause_container, getActionIntent(M3PlayWidgetReceiver.ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_next_button, getActionIntent(M3PlayWidgetReceiver.ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_prev_button, getActionIntent(M3PlayWidgetReceiver.ACTION_PREVIOUS))

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(size, size)
                    .allowHardware(false)
                    .crossfade(300)
                    .build()
                imageLoader.execute(request).image?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, paint)
        
        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }
        return output
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(context, M3PlayWidgetReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
