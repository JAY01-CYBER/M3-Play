/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.utils

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import android.view.View
import android.view.PixelCopy
import androidx.core.view.drawToBitmap
import com.j.m3play.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ComposeToImage {

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        val config = bitmap.config
        if (config != Bitmap.Config.HARDWARE && config != null) return bitmap
        return runCatching { bitmap.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: bitmap
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private suspend fun pixelCopyViewBitmap(view: View): Bitmap? {
        if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) return null
        val activity = view.context.findActivity() ?: return null
        val window = activity.window ?: return null

        val location = IntArray(2)
        view.getLocationInWindow(location)
        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val copyResult =
            suspendCancellableCoroutine { cont ->
                PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { result -> cont.resume(result) },
                    Handler(Looper.getMainLooper()),
                )
            }
        return if (copyResult == PixelCopy.SUCCESS) bitmap else null
    }

    suspend fun captureViewBitmap(
        view: View,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        backgroundColor: Int? = null,
    ): Bitmap {
        val fallbackBitmap = runCatching {
            view.drawToBitmap()
        }.getOrElse {
            val w = view.width.coerceAtLeast(1)
            val h = view.height.coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                backgroundColor?.let { Canvas(bmp).drawColor(it) }
            }
        }

        val original =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pixelCopyViewBitmap(view) ?: fallbackBitmap
            } else {
                fallbackBitmap
            }
        val needsScale =
            (targetWidth != null && targetWidth > 0 && targetWidth != original.width) ||
            (targetHeight != null && targetHeight > 0 && targetHeight != original.height)
        val base = if (needsScale) {
            val safeOriginal = ensureSoftwareBitmap(original)
            val tw = targetWidth ?: original.width
            val th = targetHeight ?: (original.height * tw / original.width)
            ensureSoftwareBitmap(Bitmap.createScaledBitmap(safeOriginal, tw, th, true))
        } else {
            ensureSoftwareBitmap(original)
        }
        if (backgroundColor != null) {
            val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawColor(backgroundColor)
            c.drawBitmap(base, 0f, 0f, null)
            return out
        }
        return base
    }

    fun cropBitmap(source: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        val safeSource = ensureSoftwareBitmap(source)
        val safeLeft = left.coerceIn(0, safeSource.width.coerceAtLeast(1) - 1)
        val safeTop = top.coerceIn(0, safeSource.height.coerceAtLeast(1) - 1)
        val safeWidth = width.coerceIn(1, safeSource.width - safeLeft)
        val safeHeight = height.coerceIn(1, safeSource.height - safeTop)
        return ensureSoftwareBitmap(Bitmap.createBitmap(safeSource, safeLeft, safeTop, safeWidth, safeHeight))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        glassStyle: com.j.m3play.ui.component.LyricsGlassStyle? = null,
        aspectRatio: Float = 1f, 
        textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Center, 
        customBlur: Int? = null, 
        showWatermark: Boolean = true, 
        showTrackInfo: Boolean = true, 
        textScale: Float = 1f, 
        customDarkness: Float? = null,
        fontStyle: Int = 0, // NAYA: 0=Modern, 1=Serif, 2=Monospace, 3=Cursive
        bgMode: Int = 0,    //  NAYA: 0=Glass, 1=Gradient
        textGlow: Boolean = false, //  NAYA: Glow
        showBarcode: Boolean = true //  NAYA: Barcode
    ): Bitmap = withContext(Dispatchers.Default) {
        val style = glassStyle ?: com.j.m3play.ui.component.LyricsGlassStyle.FrostedDark
        val baseSize = minOf(width, height)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val activeDarkness = customDarkness ?: style.backgroundDimAlpha
        val activeBlur = customBlur ?: style.cloudyRadius

        val mainTextColor = textColor
            ?: style.textColor.let {
                ((it.alpha * 255).toInt() shl 24) or
                ((it.red * 255).toInt() shl 16) or
                ((it.green * 255).toInt() shl 8) or
                (it.blue * 255).toInt()
            }
        val secondaryTxtColor = secondaryTextColor
            ?: style.secondaryTextColor.let {
                ((it.alpha * 255).toInt() shl 24) or
                ((it.red * 255).toInt() shl 16) or
                ((it.green * 255).toInt() shl 8) or
                (it.blue * 255).toInt()
            }

        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(800) 
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.image?.toBitmap()
            } catch (_: Exception) {}
        }

        val outerCornerRadius = baseSize * 0.06f

        val artPath = Path().apply {
            addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), outerCornerRadius, outerCornerRadius, Path.Direction.CW)
        }

        // 🔥 Background Rendering (Glass vs Gradient)
        if (bgMode == 1) {
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    style.surfaceTint.toArgb(),
                    android.graphics.Color.BLACK,
                    style.overlayColor.toArgb()
                ),
                null, Shader.TileMode.CLAMP
            )
            val gradPaint = Paint().apply { shader = gradient; isAntiAlias = true }
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), outerCornerRadius, outerCornerRadius, gradPaint)
        } else {
            val scaledArt = if (coverArtBitmap != null) {
                val scale = maxOf(width.toFloat() / coverArtBitmap.width, height.toFloat() / coverArtBitmap.height)
                val scaledW = (coverArtBitmap.width * scale).toInt()
                val scaledH = (coverArtBitmap.height * scale).toInt()
                val scaled = ensureSoftwareBitmap(Bitmap.createScaledBitmap(coverArtBitmap, scaledW, scaledH, true))
                val x = ((scaledW - width) / 2).coerceAtLeast(0)
                val y = ((scaledH - height) / 2).coerceAtLeast(0)
                cropBitmap(scaled, x, y, width, height)
            } else null
            
            if (scaledArt != null) {
                canvas.withClip(artPath) { drawBitmap(scaledArt, 0f, 0f, null) }
            } else {
                val bgPaint = Paint().apply { color = backgroundColor ?: 0xFF121212.toInt(); isAntiAlias = true }
                canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), outerCornerRadius, outerCornerRadius, bgPaint)
            }
        }

        val dimPaint = Paint().apply { color = android.graphics.Color.argb((activeDarkness * 255).toInt(), 0, 0, 0); isAntiAlias = true }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), outerCornerRadius, outerCornerRadius, dimPaint)

        val glassMargin = baseSize * 0.04f
        val glassLeft = glassMargin
        val glassTop = glassMargin
        val glassRight = width - glassMargin
        val glassBottom = height - glassMargin
        val glassWidth = glassRight - glassLeft
        val glassHeight = glassBottom - glassTop
        val glassCornerRadius = baseSize * 0.05f

        val glassRect = RectF(glassLeft, glassTop, glassRight, glassBottom)
        val glassPath = Path().apply { addRoundRect(glassRect, glassCornerRadius, glassCornerRadius, Path.Direction.CW) }

        if (bgMode == 0 && coverArtBitmap != null) {
            val scale = maxOf(width.toFloat() / coverArtBitmap.width, height.toFloat() / coverArtBitmap.height)
            val scaled = ensureSoftwareBitmap(Bitmap.createScaledBitmap(coverArtBitmap, (coverArtBitmap.width * scale).toInt(), (coverArtBitmap.height * scale).toInt(), true))
            val cropLeft = glassLeft.toInt().coerceIn(0, width - 1)
            val cropTop = glassTop.toInt().coerceIn(0, height - 1)
            val cropWidth = glassWidth.toInt().coerceIn(1, width - cropLeft)
            val cropHeight = glassHeight.toInt().coerceIn(1, height - cropTop)
            
            val glassCrop = Bitmap.createBitmap(scaled, ((scaled.width - width)/2 + cropLeft).coerceAtLeast(0), ((scaled.height - height)/2 + cropTop).coerceAtLeast(0), cropWidth, cropHeight)
            val frostedCrop = scaleBlur(glassCrop, activeBlur.coerceAtLeast(1))
            canvas.withClip(glassPath) { drawBitmap(frostedCrop, glassLeft, glassTop, null) }
        }

        val glassBgPaint = Paint().apply {
            color = style.surfaceTint.let { android.graphics.Color.argb((style.surfaceAlpha * 255).toInt(), (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, glassBgPaint)

        val overlayPaint = Paint().apply {
            color = style.overlayColor.let { android.graphics.Color.argb((style.overlayAlpha * 255).toInt(), (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, overlayPaint)

        val borderPaint = Paint().apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = android.graphics.Color.argb(25, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, borderPaint)

        val contentPadding = baseSize * 0.08f
        val contentLeft = glassLeft + contentPadding
        val contentTop = glassTop + contentPadding
        val contentRight = glassRight - contentPadding

        val imageCornerRadius = baseSize * 0.035f
        val coverSize = baseSize * 0.16f

        var textBlockHeight = 0f

        if (showTrackInfo && coverArtBitmap != null) {
            val rect = RectF(contentLeft, contentTop, contentLeft + coverSize, contentTop + coverSize)
            val path = Path().apply { addRoundRect(rect, imageCornerRadius, imageCornerRadius, Path.Direction.CW) }
            canvas.withClip(path) { drawBitmap(coverArtBitmap, null, rect, null) }
            val artBorderPaint = Paint().apply {
                this.style = Paint.Style.STROKE; strokeWidth = 1f; color = android.graphics.Color.argb(38, 255, 255, 255); isAntiAlias = true
            }
            canvas.drawRoundRect(rect, imageCornerRadius, imageCornerRadius, artBorderPaint)

            val titlePaint = TextPaint().apply { color = mainTextColor; textSize = baseSize * 0.038f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; letterSpacing = -0.02f }
            val artistPaint = TextPaint().apply { color = secondaryTxtColor; textSize = baseSize * 0.028f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }

            val textMaxWidth = (contentRight - contentLeft - coverSize - baseSize * 0.04f).toInt()
            val textStartX = contentLeft + coverSize + baseSize * 0.04f

            val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(1).build()
            val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(1).build()

            val imageCenter = contentTop + coverSize / 2f
            textBlockHeight = titleLayout.height + artistLayout.height + 6f
            val textBlockY = imageCenter - textBlockHeight / 2f

            canvas.withTranslation(textStartX, textBlockY) {
                titleLayout.draw(this)
                translate(0f, titleLayout.height.toFloat() + 6f)
                artistLayout.draw(this)
            }
        }

    
        val customTypeface = when(fontStyle) {
            1 -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            2 -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            3 -> Typeface.create("cursive", Typeface.BOLD)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            typeface = customTypeface // Applied Font
            isAntiAlias = true
            letterSpacing = -0.01f
        }

        
        if (textGlow) {
            lyricsPaint.setShadowLayer(25f, 0f, 0f, mainTextColor)
        }

        val androidTextAlign = when (textAlign) {
            androidx.compose.ui.text.style.TextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
            androidx.compose.ui.text.style.TextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val lyricsMaxWidth = (glassWidth * 0.85f).toInt()
        val logoBlockHeight = if (showWatermark) (baseSize * 0.08f).toInt() else 0
        
        val lyricsTop = if (showTrackInfo) glassTop + (glassHeight * 0.22f).coerceAtLeast(coverSize + contentPadding) else glassTop + contentPadding
        val lyricsBottom = glassBottom - (logoBlockHeight + contentPadding)
        val availableLyricsHeight = lyricsBottom - lyricsTop

        var lyricsTextSize = baseSize * 0.055f * textScale
        var lyricsLayout: StaticLayout
        do {
            lyricsPaint.textSize = lyricsTextSize
            lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, lyricsMaxWidth)
                .setAlignment(androidTextAlign)
                .setIncludePad(false)
                .setLineSpacing(8f, 1.35f)
                .setMaxLines(14)
                .build()
            
            if (lyricsLayout.height > availableLyricsHeight && lyricsTextSize > (22f * textScale)) {
                lyricsTextSize -= 2f
            } else { break }
        } while (lyricsTextSize > (22f * textScale))

        val lyricsYOffset = lyricsTop + (availableLyricsHeight - lyricsLayout.height) / 2f
        canvas.withTranslation(glassLeft + (glassWidth - lyricsMaxWidth) / 2f, lyricsYOffset) { lyricsLayout.draw(this) }

        if (showWatermark) {
            AppLogo(context = context, canvas = canvas, canvasWidth = width, canvasHeight = height, padding = glassLeft + contentPadding, bottomPadding = glassBottom - contentPadding, circleColor = secondaryTxtColor, logoTint = if (style.isDark) 0xDD000000.toInt() else 0xE6FFFFFF.toInt(), textColor = secondaryTxtColor, showBarcode = showBarcode)
        }

        return@withContext bitmap
    }

    private fun scaleBlur(source: Bitmap, strength: Int): Bitmap {
        val safe = ensureSoftwareBitmap(source)
        val factor = (1f / strength.coerceAtLeast(1)).coerceAtLeast(0.02f)
        val smallW = (safe.width * factor).toInt().coerceAtLeast(1)
        val smallH = (safe.height * factor).toInt().coerceAtLeast(1)
        val downscaled = Bitmap.createScaledBitmap(safe, smallW, smallH, true)
        return Bitmap.createScaledBitmap(downscaled, safe.width, safe.height, true)
    }

    private fun AppLogo(
        context: Context, canvas: Canvas, canvasWidth: Int, canvasHeight: Int, padding: Float, bottomPadding: Float = canvasHeight - padding, circleColor: Int, logoTint: Int, textColor: Int, showBarcode: Boolean = true // 🔥 Barcode Added
    ) {
        val baseSize = minOf(canvasWidth, canvasHeight).toFloat()
        val logoSize = (baseSize * 0.045f).toInt()

        val rawLogo = context.getDrawable(R.drawable.ic_app_logo)?.toBitmap(logoSize, logoSize)
        val logo = rawLogo?.let { source ->
            val colored = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvasLogo = Canvas(colored)
            val paint = Paint().apply { colorFilter = PorterDuffColorFilter(logoTint, PorterDuff.Mode.SRC_IN); isAntiAlias = true }
            canvasLogo.drawBitmap(source, 0f, 0f, paint)
            colored
        }

        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply { color = textColor; textSize = baseSize * 0.028f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAntiAlias = true; letterSpacing = 0.02f }

        val circleRadius = logoSize * 0.55f
        val circleX = padding + circleRadius
        val circleY = bottomPadding - circleRadius
        val logoX = circleX - logoSize / 2f
        val logoY = circleY - logoSize / 2f
        val textX = padding + circleRadius * 2 + 10f
        val textY = circleY + appNamePaint.textSize * 0.3f

        val circlePaint = Paint().apply { color = circleColor; isAntiAlias = true; style = Paint.Style.FILL }
        canvas.drawCircle(circleX, circleY, circleRadius, circlePaint)
        logo?.let { canvas.drawBitmap(it, logoX, logoY, null) }
        canvas.drawText(appName, textX, textY, appNamePaint)

        
        if (showBarcode) {
            val wavePaint = Paint().apply {
                color = textColor
                strokeWidth = baseSize * 0.006f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            val waveScales = listOf(0.4f, 0.8f, 0.5f, 1f, 0.6f, 0.3f, 0.9f, 0.5f)
            val waveStartX = textX + appNamePaint.measureText(appName) + 30f
            var currentX = waveStartX
            waveScales.forEach { scale ->
                val h = logoSize * scale * 0.8f
                val topY = circleY - h / 2f
                val botY = circleY + h / 2f
                canvas.drawLine(currentX, topY, currentX, botY, wavePaint)
                currentX += baseSize * 0.012f
            }
        }
    }
}
