/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Lyrics quality scoring is intentionally source-agnostic: providers return raw lyrics,
 * then this evaluator prefers useful, well-timed, internally consistent results.
 */
package com.j.m3play.lyrics

internal object LyricsQualityEvaluator {
    data class Assessment(
        val score: Int,
        val isWordSynced: Boolean,
        val isLineSynced: Boolean,
        val lineCount: Int,
        val timedEndMs: Long?,
    )

    fun assess(rawLyrics: String, durationSeconds: Int): Assessment {
        val lyrics = rawLyrics
            .replace("\uFEFF", "")
            .replace(INVISIBLE_CHARS, "")
            .trim()

        if (lyrics.isBlank()) return Assessment(Int.MIN_VALUE, false, false, 0, null)

        val ttml = LyricsUtils.isTtml(lyrics)
        val qrc = LyricsUtils.isQrc(lyrics)
        val parsed = runCatching {
            if (ttml) LyricsUtils.parseTtml(lyrics, durationSeconds)
            else LyricsUtils.parseLyrics(lyrics)
        }.getOrDefault(emptyList())
        val usefulLines = parsed.filter { it.text.isNotBlank() }
        if ((ttml || qrc) && usefulLines.isEmpty()) {
            return Assessment(Int.MIN_VALUE, false, false, 0, null)
        }
        val lineCount = if (usefulLines.isNotEmpty()) usefulLines.size else
            lyrics.lineSequence().count { line ->
                val cleaned = line.replace(LRC_TIMESTAMPS, "").trim()
                cleaned.isNotEmpty() && !cleaned.startsWith("<tt", ignoreCase = true)
            }

        val wordSynced = usefulLines.any { line ->
            val words = line.words.orEmpty().filter { it.text.isNotBlank() && it.endTime > it.startTime }
            words.size >= 2 && words.map { it.startTime }.distinct().size >= 2
        }
        val lineSynced = ttml || qrc || LyricsUtils.TIME_REGEX.containsMatchIn(lyrics)
        val normalizedLines = (if (usefulLines.isNotEmpty()) usefulLines.map { it.text } else
            lyrics.lineSequence().map { it.replace(LRC_TIMESTAMPS, "").trim() }.filter { it.isNotBlank() }.toList())
            .map(::normalizeLine)
            .filter { it.isNotEmpty() }

        val distinctRatio = if (normalizedLines.isEmpty()) 0.0
            else normalizedLines.distinct().size.toDouble() / normalizedLines.size
        val charCount = normalizedLines.sumOf { it.length }
        val timedEndMs = usefulLines.maxOfOrNull { line ->
            maxOf(
                line.time,
                line.words.orEmpty().maxOfOrNull { (it.endTime * 1000.0).toLong() } ?: line.time,
            )
        }
        val durationMs = durationSeconds.takeIf { it > 0 }?.toLong()?.times(1000L)

        var score = 0
        score += when {
            wordSynced -> 100
            lineSynced -> 48
            else -> 0
        }
        score += lineCount.coerceIn(0, 45) * 2
        score += (charCount / 45).coerceIn(0, 24)
        score += (distinctRatio * 18).toInt()
        if (lineCount < 3) score -= 24
        if (charCount < 24) score -= 18
        if (distinctRatio < 0.35 && lineCount >= 6) score -= 22

        if (durationMs != null && timedEndMs != null && timedEndMs > 0L) {
            val allowedTail = 15_000L
            if (timedEndMs > durationMs + allowedTail) score -= 65
            if (timedEndMs < durationMs / 5L && lineCount >= 5) score -= 18
            if (timedEndMs > durationMs * 3L) score -= 80
        }

        // Reject payloads that are mostly markup or metadata rather than lyric text.
        if (normalizedLines.isEmpty()) score -= 100
        return Assessment(score, wordSynced, lineSynced, lineCount, timedEndMs)
    }

    private fun normalizeLine(line: String): String =
        line.lowercase()
            .replace(NORMALIZE_PUNCTUATION, "")
            .replace(WHITESPACE, " ")
            .trim()

    private val LRC_TIMESTAMPS = Regex("""\[[0-9]{1,3}:[0-9]{2}(?:[.:][0-9]{1,3})?]""")
    private val INVISIBLE_CHARS = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    private val NORMALIZE_PUNCTUATION = Regex("""[^\p{L}\p{N}\s]""")
    private val WHITESPACE = Regex("""\s+""")
}
