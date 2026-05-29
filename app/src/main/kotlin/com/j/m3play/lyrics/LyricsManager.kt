package com.j.m3play.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LyricsManager {

    // MAIN ENGINE
    suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int
    ): Result<String> = withContext(Dispatchers.IO) {

        // SMART YOULYPLUS
        val youly = SmartYouLyPlus.getBestLyrics(
            title, artist, album, duration, id
        )
        if (youly.isSuccess) return@withContext clean(youly)

        // SIMPMUSIC
        val simp = SimpMusicLyricsProvider.getLyrics(
            id, title, artist, album, duration
        )
        if (simp.isSuccess) return@withContext clean(simp)

        // YOUTUBE SUBTITLE
        val yt = YouTubeSubtitleLyricsProvider.getLyrics(
            id, title, artist, album, duration
        )
        if (yt.isSuccess) return@withContext clean(yt)

        Result.failure(Exception("No lyrics found"))
    }

    // TRANSLATION SUPPORT
    suspend fun getLyricsWithTranslation(
        context: Context,
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        targetLang: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {

        val base = getLyrics(context, id, title, artist, album, duration)
        if (base.isFailure) return@withContext base

        val original = base.getOrNull() ?: return@withContext base

        val detected = LyricsTranslator.detectLanguage(original)

        if (detected == targetLang) {
            return@withContext Result.success(original)
        }

        val translated = LyricsTranslator.translate(original, targetLang)

        val finalText = """
            🌐 Translated ($targetLang)

            $translated

            ─────────────

            🎵 Original ($detected)

            $original
        """.trimIndent()

        Result.success(finalText)
    }

    // 🔹 CLEAN RESULT
    private fun clean(result: Result<String>): Result<String> {
        val text = result.getOrNull() ?: return result

        val cleaned = text
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return Result.success(cleaned)
    }
}
