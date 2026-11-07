/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Enhanced lyrics renderer adapted to M3Play's existing lyrics/player pipeline.
 * The rendering/animation engine is vendored from the supplied Accompanist Lyrics UI
 * reference used by ArchiveTune; M3Play remains responsible for playback, providers,
 * parsing and timing.
 */
package com.j.m3play.ui.component

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.constants.LyricsClickKey
import com.j.m3play.constants.LyricsRomanizeJapaneseKey
import com.j.m3play.constants.LyricsRomanizeKoreanKey
import com.j.m3play.constants.LyricsTextSizeKey
import com.j.m3play.constants.PlayerBackgroundStyle
import com.j.m3play.constants.PlayerBackgroundStyleKey
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.lyrics.LyricsEntry
import com.j.m3play.lyrics.M3PlayPhoneticProvider
import com.j.m3play.lyrics.WordTimestamp
import com.j.m3play.lyrics.LyricsUtils.findCurrentLineIndex
import com.j.m3play.lyrics.LyricsUtils.isChinese
import com.j.m3play.lyrics.LyricsUtils.isJapanese
import com.j.m3play.lyrics.LyricsUtils.isKorean
import com.j.m3play.lyrics.LyricsUtils.isTtml
import com.j.m3play.lyrics.LyricsUtils.isQrc
import com.j.m3play.lyrics.LyricsUtils.isSyncedLyrics
import com.j.m3play.lyrics.LyricsUtils.parseLyrics
import com.j.m3play.lyrics.LyricsUtils.parseTtml
import com.j.m3play.lyrics.LyricsUtils.romanizeJapanese
import com.j.m3play.lyrics.LyricsUtils.romanizeKorean
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.utils.withPhonetics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.list.rememberLyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val DRIFT_FORWARD_MS = 250L
private const val DRIFT_BACKWARD_MS = 250L
private const val DRIFT_CORRECTION = 0.08
private const val MAX_FRAME_CORRECTION_MS = 2.0

@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackParameters by playerConnection.playbackParameters.collectAsState()

    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 34f)
    val (romanizeJapanesePref) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKoreanPref) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.onBackground
    } else Color.White

    val lyricsText = currentLyrics?.lyrics
    if (lyricsText == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ShimmerHost { repeat(6) { TextPlaceholder() } }
        }
        return
    }
    if (lyricsText == LYRICS_NOT_FOUND) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lyrics not found", color = textColor.copy(alpha = 0.6f))
        }
        return
    }

    val isTtmlFormat = remember(lyricsText) { isTtml(lyricsText) }
    val isQrcFormat = remember(lyricsText) { isQrc(lyricsText) }
    val parsedEntries = remember(lyricsText, currentLyrics?.provider) {
        val parsed = when {
            isTtmlFormat -> parseTtml(lyricsText)
            lyricsText.startsWith("[") || isQrcFormat -> parseLyrics(lyricsText)
            else -> lyricsText.lines()
                .filter { it.isNotBlank() }
                .map { LyricsEntry(time = -1L, text = it.trim()) }
        }
        parsed
    }

    val isSynced = isSyncedLyrics(lyricsText) && parsedEntries.any { it.time >= 0L }
    if (!isSynced) {
        PlainLyricsFallback(parsedEntries, textColor, lyricsTextSize, modifier)
        return
    }

    // Preserve genuine provider timing. Fabricating syllables from line timestamps
    // kept a line highlighted until the next line even after its vocal ended.
    // Preserve real word timings only; fabricated word timings cause false karaoke highlights.
    val entriesWithWords = parsedEntries
    LaunchedEffect(entriesWithWords, romanizeJapanesePref, romanizeKoreanPref) {
        if (romanizeJapanesePref || romanizeKoreanPref) {
            entriesWithWords.forEach { entry ->
                if (entry.text.isNotBlank() && entry.romanizedTextFlow.value == null) {
                    val value = when {
                        romanizeJapanesePref && isJapanese(entry.text) -> romanizeJapanese(entry.text)
                        romanizeKoreanPref && isKorean(entry.text) -> romanizeKorean(entry.text)
                        else -> null
                    }
                    if (value != null) entry.romanizedTextFlow.value = value
                }
            }
        }
    }

    val isWordSynced = entriesWithWords.any { !it.words.isNullOrEmpty() }
    val syncedLyrics = remember(entriesWithWords, isWordSynced) {
        val source = buildArchiveSyncedLyrics(entriesWithWords, isWordSynced)
        // Preserve provider/TTML captions; generate only missing phonetic captions.
        runCatching { source.withPhonetics(M3PlayPhoneticProvider) }.getOrDefault(source)
    }
    if (syncedLyrics.lines.isEmpty()) {
        PlainLyricsFallback(entriesWithWords, textColor, lyricsTextSize, modifier)
        return
    }

    var playbackPositionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    LaunchedEffect(player, playbackParameters.speed, sliderPositionProvider, entriesWithWords) {
        var smoothed = player.currentPosition.coerceAtLeast(0L).toDouble()
        var previousFrame = 0L
        while (isActive) {
            val slider = sliderPositionProvider()
            val raw = (slider ?: player.currentPosition).coerceAtLeast(0L)
            if (slider != null || !player.isPlaying) {
                smoothed = raw.toDouble()
                previousFrame = 0L
                playbackPositionMs = raw
                if (slider == null) delay(80L) else withFrameNanos { }
                continue
            }
            val frame = withFrameNanos { it }
            if (previousFrame == 0L) {
                previousFrame = frame
                smoothed = raw.toDouble()
            } else {
                val elapsed = (frame - previousFrame).coerceAtLeast(0L) / 1_000_000.0
                previousFrame = frame
                smoothed += elapsed * playbackParameters.speed.toDouble()
                val drift = raw - smoothed
                if (kotlin.math.abs(drift) > DRIFT_FORWARD_MS || kotlin.math.abs(drift) > DRIFT_BACKWARD_MS) {
                    smoothed = raw.toDouble()
                } else {
                    smoothed += (drift * DRIFT_CORRECTION).coerceIn(-MAX_FRAME_CORRECTION_MS, MAX_FRAME_CORRECTION_MS)
                }
                playbackPositionMs = smoothed.roundToLong().coerceAtLeast(0L)
            }
        }
    }

    key(mediaMetadata?.id, lyricsText.hashCode()) {
        val listState = rememberLyricsLazyListState()
        KaraokeLyricsView(
            listState = listState,
            lyrics = syncedLyrics,
            currentPosition = { playbackPositionMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt() },
            onLineClicked = { line ->
                if (lyricsClick && line.start > 0) player.seekTo(line.start.toLong())
            },
            onLinePressed = { },
            textColor = textColor,
            normalLineTextStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = lyricsTextSize.sp,
                fontWeight = FontWeight.Bold,
                textMotion = TextMotion.Animated,
            ),
            accompanimentLineTextStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = (lyricsTextSize * 0.82f).sp,
                fontWeight = FontWeight.Bold,
                textMotion = TextMotion.Animated,
            ),
            phoneticTextStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (lyricsTextSize * 0.55f).sp,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
            useBlurEffect = true,
            showTranslation = true,
            showPhonetic = true,
            itemSpacing = 16.dp,
            scrollAnimationSpec = tween(650, easing = FastOutSlowInEasing),
            autoScrollResumeDelayMillis = 2500L,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PlainLyricsFallback(
    entries: List<LyricsEntry>,
    color: Color,
    textSize: Float,
    modifier: Modifier,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(entries.size) { index ->
            val entry = entries[index]
            val rom by entry.romanizedTextFlow.collectAsState()
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    entry.text,
                    color = color,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (!rom.isNullOrBlank()) {
                    Text(rom!!, color = color.copy(alpha = 0.55f), fontSize = (textSize * 0.55f).sp)
                }
            }
        }
    }
}

private fun prepareWordTiming(entries: List<LyricsEntry>): List<LyricsEntry> =
    entries.mapIndexed { index, entry ->
        if (entry.words != null || entry.time < 0L || entry.text.isBlank()) return@mapIndexed entry
        val next = entries.getOrNull(index + 1)?.time ?: (entry.time + 5000L)
        val duration = (next - entry.time).coerceAtLeast(500L)
        val cjk = isJapanese(entry.text) || isChinese(entry.text) || isKorean(entry.text)
        val tokens = if (cjk) {
            entry.text.flatMap { ch ->
                if (ch.isWhitespace()) listOf(ch.toString()) else listOf(ch.toString())
            }
        } else entry.text.split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return@mapIndexed entry
        val total = tokens.sumOf { it.length }.coerceAtLeast(1)
        var elapsed = 0.0
        val words = tokens.mapIndexed { i, token ->
            val dur = duration * token.length.toDouble() / total
            val start = entry.time / 1000.0 + elapsed / 1000.0
            val end = start + dur / 1000.0
            elapsed += dur
            WordTimestamp(if (!cjk && i < tokens.lastIndex) "$token " else token, start, end)
        }
        entry.copy(words = words)
    }

private fun String?.isSecondarySingerAgent(): Boolean {
    val normalized = this?.trim()?.lowercase()?.replace(Regex("[^a-z0-9]"), "").orEmpty()
    if (normalized.isBlank()) return false
    if (normalized in setOf("v1", "1", "singer1", "voice1", "agent1", "main", "primary")) return false
    return normalized in setOf("v2", "2", "singer2", "voice2", "agent2", "secondary", "backing", "background") ||
        Regex("^(v|agent|singer|voice)0*2$").matches(normalized) ||
        normalized.contains("background") ||
        normalized.contains("backing") ||
        normalized.contains("secondary")
}

private fun buildArchiveSyncedLyrics(entries: List<LyricsEntry>, wordSynced: Boolean): SyncedLyrics {
    val lines = entries.mapIndexedNotNull { index, entry ->
        if (entry.time < 0L || entry.text.isBlank()) return@mapIndexedNotNull null
        if (wordSynced && !entry.words.isNullOrEmpty()) {
            val main = entry.words.filterNot(WordTimestamp::isBackground)
            val bg = entry.words.filter(WordTimestamp::isBackground)
            val words = if (main.isNotEmpty()) main else entry.words
            val syllables = words.map { it.toSyllable() }
            if (syllables.isEmpty()) return@mapIndexedNotNull null
            val start = entry.time.toInt().coerceAtLeast(0)
            val nextStart = entries.getOrNull(index + 1)?.time?.toInt()
            // For karaoke lines, the final word's end time controls when the line
            // stops being active. Do not keep the whole line active until the next
            // lyric's start; that causes the previous line to remain bright in gaps.
            val finalWordEnd = syllables.maxOf(KaraokeSyllable::end)
            val end = maxOf(start + 1, finalWordEnd)
            // Keep secondary/alternate vocalist lines aligned from the opposite side.
            val alignment = if (entry.agent.isSecondarySingerAgent()) KaraokeAlignment.End else KaraokeAlignment.Start
            val accompaniment = if (bg.isNotEmpty()) {
                val bs = bg.map { it.toSyllable() }
                listOf(KaraokeLine.AccompanimentKaraokeLine(bs, null, alignment, bs.minOf(KaraokeSyllable::start), bs.maxOf(KaraokeSyllable::end)))
            } else null
            KaraokeLine.MainKaraokeLine(
                syllables,
                entry.providerTranslationText,
                alignment,
                start,
                end,
                entry.romanizedTextFlow.value,
                accompaniment,
            )
        } else {
            val next = entries.getOrNull(index + 1)?.time ?: (entry.time + 5000L)
            SyncedLine(
                entry.text,
                entry.providerTranslationText,
                entry.time.toInt().coerceAtLeast(0),
                maxOf(entry.time.toInt() + 1, next.toInt()),
                languageTag = inferLyricsLanguageTag(entry.text),
            )
        }
    }
    return SyncedLyrics(lines)
}

private fun WordTimestamp.toSyllable(): KaraokeSyllable =
    KaraokeSyllable(
        content = text,
        start = (startTime * 1000.0).roundToInt().coerceAtLeast(0),
        end = (endTime * 1000.0).roundToInt().coerceAtLeast((startTime * 1000.0).roundToInt() + 1),
        languageTag = inferLyricsLanguageTag(text),
    )

private fun inferLyricsLanguageTag(text: String): String? = when {
    isJapanese(text) -> "ja"
    isKorean(text) -> "ko"
    isChinese(text) -> {
        val locale = java.util.Locale.getDefault()
        if (locale.country.equals("TW", ignoreCase = true) || locale.country.equals("HK", ignoreCase = true)) "zh-TW"
        else "zh-CN"
    }
    else -> null
}

