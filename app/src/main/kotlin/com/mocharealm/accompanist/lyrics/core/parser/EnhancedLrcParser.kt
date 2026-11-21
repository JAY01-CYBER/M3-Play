package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.contentToString
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.stripEnclosingParentheses
import com.mocharealm.accompanist.lyrics.core.utils.LrcMetadataHelper
import com.mocharealm.accompanist.lyrics.core.utils.parseAsTime
import kotlin.math.abs

/**
 * A parser for Enhanced LRC files.
 *
 * Enhanced LRC extends the standard LRC format with support for syllable-level timing (Karaoke),
 * multiple singers/voices, and background vocals.
 *
 * Recommended format:
 * ```
 * [00:12.34]<00:12.34>Hel<00:12.60>lo <00:12.90>World
 * [bg:<00:12.34>Back<00:12.60>ground<00:12.90>]
 * ```
 *
 * Also supports bad karaoke format with [] brackets:
 * ```
 * [00:12.34][00:12.34]Hel[00:12.60]lo [00:12.90]World
 * [bg:[00:12.34]Back[00:12.60]ground[00:12.90]]
 * ```
 */
object EnhancedLrcParser : ILyricsParser {
    override fun canParse(content: String): Boolean {
        val hasLineTimestamp = content.contains("""\[\d{2}:\d{2}\.\d{2,3}\]""".toRegex())
        return hasLineTimestamp
    }

    private val voiceParser = Regex("^(v\\d+)\\s*:\\s*(.*)")
    private val tagRegex = Regex("""\[(.*?)\]""")
    private val timestampPattern = Regex("""\d+([:.]\d+)+""")
    // Precompiled once — these were previously recompiled on every parsed line.
    private val angleSyllableRegex = Regex("""<([^>]+)>([^<]*)""")
    private val squareSyllableRegex = Regex("""\[([^\]]+)\]([^\[]*)""")

    private fun isTimestamp(s: String): Boolean {
        return timestampPattern.matches(s.trim())
    }

    private fun proceduralParseSyllables(content: String, bracketType: BracketType = BracketType.ANGLE): List<KaraokeSyllable> {
        if (content.isBlank()) return emptyList()

        val syllables = mutableListOf<KaraokeSyllable>()

        val syllableRegex = when (bracketType) {
            BracketType.ANGLE -> angleSyllableRegex
            BracketType.SQUARE -> squareSyllableRegex
        }

        for (match in syllableRegex.findAll(content)) {
            val tsPart = match.groupValues[1].trim()
            val text = match.groupValues[2]

            if (isTimestamp(tsPart)) {
                val time = runCatching { tsPart.parseAsTime() }.getOrNull()
                if (time != null) {
                    syllables.add(KaraokeSyllable(text, time, time))
                }
            }
        }

        return if (syllables.isEmpty()) emptyList() else syllables.rearrangeTime()
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines).filter { it.isNotBlank() }

        val rawData = lyricsLines.flatMap { line -> parseLine(line) }
            .combineRawWithTranslation()
            .rearrangeAccompanimentAlignment()
            .rearrangeUncheckedLineTime()

        val data = mutableListOf<ISyncedLine>()
        rawData.forEach { line ->
            if (line is KaraokeLine.AccompanimentKaraokeLine && data.isNotEmpty()) {
                val last = data.last()
                if (last is KaraokeLine.MainKaraokeLine) {
                    val updated = last.copy(
                        accompanimentLines = (last.accompanimentLines ?: emptyList()) + line
                    )
                    data[data.size - 1] = updated
                } else {
                    data.add(line)
                }
            } else {
                data.add(line)
            }
        }

        val attributes = LrcMetadataHelper.parse(lines)
        return SyncedLyrics(
            lines = data,
            title = attributes.title ?: "",
            artists = attributes.artist?.let { artistStr ->
                artistStr.split("/").map { part ->
                    val segments = part.split(":", limit = 2)
                    if (segments.size == 2) com.mocharealm.accompanist.lyrics.core.model.Artist(segments[0], segments[1])
                    else com.mocharealm.accompanist.lyrics.core.model.Artist("Main", part)
                }
            } ?: emptyList()
        )
    }

    private fun parseLine(string: String): List<ISyncedLine> {
        if (string.isBlank()) return emptyList()

        val matches = tagRegex.findAll(string).toList()
        if (matches.isEmpty()) return emptyList()

        var lastEnd = 0
        val leadingTags = mutableListOf<MatchResult>()
        for (match in matches) {
            val prefix = string.substring(lastEnd, match.range.first)
            if (prefix.isBlank()) {
                leadingTags.add(match)
                lastEnd = match.range.last + 1
            } else break
        }

        if (leadingTags.isEmpty()) return emptyList()

        val content = if (lastEnd < string.length) string.substring(lastEnd).trim() else ""
        val results = mutableListOf<ISyncedLine>()
        val timestamps = mutableListOf<Int>()
        var bgTag: String? = null

        for (match in leadingTags) {
            val tagContentRaw = match.groupValues[1].trim()
            if (tagContentRaw.startsWith("bg:")) {
                bgTag = tagContentRaw.substring(3).trim()
            } else if (isTimestamp(tagContentRaw)) {
                runCatching { tagContentRaw.parseAsTime() }.getOrNull()?.let { timestamps.add(it) }
            }
        }

        // 检测内容中使用的括号类型
        val bracketType = detectBracketType(content, bgTag)

        // Background vocals may be wrapped in parentheses as a marker; drop them.
        val bgSyllables =
            (bgTag?.let { proceduralParseSyllables(it, bracketType) } ?: emptyList())
                .stripEnclosingParentheses()
        val mainSyllables = if (timestamps.isNotEmpty() && content.isNotBlank()) {
            proceduralParseSyllables(content, bracketType)
        } else emptyList()

        val voiceMatch = voiceParser.find(content)
        val alignment = when (voiceMatch?.groupValues?.get(1)) {
            "v1" -> KaraokeAlignment.Start
            "v2" -> KaraokeAlignment.End
            else -> KaraokeAlignment.Unspecified
        }
        val textContent = voiceMatch?.groupValues?.get(2)?.trim() ?: content

        val firstTimestamp = timestamps.firstOrNull() ?: 0
        val isRelative = mainSyllables.firstOrNull()?.start?.let { it < firstTimestamp } ?: false
        val bgIsRelative = bgSyllables.firstOrNull()?.start?.let { it < firstTimestamp } ?: false

        if (timestamps.isNotEmpty()) {
            for (startTime in timestamps) {
                if (mainSyllables.isNotEmpty()) {
                    val offset = if (isRelative) startTime else startTime - firstTimestamp
                    val shifted = mainSyllables.map { it.copy(start = it.start + offset, end = it.end + offset) }
                    results.add(KaraokeLine.MainKaraokeLine(
                        syllables = shifted,
                        translation = null,
                        alignment = alignment,
                        start = shifted.first().start,
                        end = shifted.last().end
                    ))
                } else if (textContent.isNotBlank()) {
                    // For typical lines without enhanced syllable parts
                    results.add(SyncedLine(
                        content = textContent,
                        translation = null,
                        start = startTime,
                        end = startTime
                    ))
                }

                if (bgSyllables.isNotEmpty()) {
                    val bgOffset = if (bgIsRelative) startTime else startTime - firstTimestamp
                    val shifted = bgSyllables.map { it.copy(start = it.start + bgOffset, end = it.end + bgOffset) }
                    results.add(KaraokeLine.AccompanimentKaraokeLine(
                        syllables = shifted,
                        translation = null,
                        alignment = KaraokeAlignment.Unspecified,
                        start = shifted.first().start,
                        end = shifted.last().end
                    ))
                }
            }
        } else if (bgSyllables.isNotEmpty()) {
            results.add(KaraokeLine.AccompanimentKaraokeLine(
                syllables = bgSyllables,
                translation = null,
                alignment = KaraokeAlignment.Unspecified,
                start = bgSyllables.first().start,
                end = bgSyllables.last().end
            ))
        }

        return results
    }

    /**
     * 检测内容中使用的括号类型
     */
    private fun detectBracketType(content: String?, bgTag: String?): BracketType {
        // 检查主内容
        if (content != null) {
            if (content.contains(Regex("<\\d+"))) {
                return BracketType.ANGLE
            }
            if (content.contains(Regex("\\[\\d+"))) {
                return BracketType.SQUARE
            }
        }

        // 检查bg标签
        if (bgTag != null) {
            if (bgTag.contains(Regex("<\\d+"))) {
                return BracketType.ANGLE
            }
            if (bgTag.contains(Regex("\\[\\d+"))) {
                return BracketType.SQUARE
            }
        }

        // 默认使用尖括号
        return BracketType.ANGLE
    }

    private fun List<KaraokeSyllable>.rearrangeTime(): List<KaraokeSyllable> {
        if (this.isEmpty()) return emptyList()
        val list = mutableListOf<KaraokeSyllable>()
        for (i in 0 until this.size - 1) {
            list.add(this[i].copy(end = this[i + 1].start))
        }
        val last = this.last()
        if (last.content.isNotEmpty()) {
            list.add(last)
        }
        return list
    }

    private fun List<ISyncedLine>.combineRawWithTranslation(): List<ISyncedLine> {
        val list = ArrayList<ISyncedLine>()
        val usedIndices = mutableSetOf<Int>()

        for (i in this.indices) {
            if (i in usedIndices) continue
            val line = this[i]
            val contentStr = when (line) {
                is KaraokeLine -> line.syllables.contentToString().trim()
                is SyncedLine -> line.content.trim()
                else -> ""
            }

            var translationFound = false
            for (j in i + 1 until this.size) {
                if (j in usedIndices) continue
                val nextLine = this[j]

                // 兼容逻辑：类型相同，或者当 AccompanimentLine 的翻译未带 bg 标签而被识别为 MainLine 或 SyncedLine 时
                val isCompatibleType = (line::class == nextLine::class) ||
                        (line is KaraokeLine.AccompanimentKaraokeLine && nextLine is KaraokeLine.MainKaraokeLine) ||
                        nextLine is SyncedLine

                if (isCompatibleType && abs(line.start - nextLine.start) <= 150) {
                    val nextContent = when (nextLine) {
                        is KaraokeLine -> nextLine.syllables.contentToString().trim()
                        is SyncedLine -> nextLine.content.trim()
                        else -> ""
                    }
                    if (contentStr != nextContent && contentStr.isNotEmpty()) {
                        val updated = when (line) {
                            is KaraokeLine.MainKaraokeLine -> line.copy(translation = nextContent)
                            is KaraokeLine.AccompanimentKaraokeLine -> line.copy(translation = nextContent)
                            is SyncedLine -> line.copy(translation = nextContent)
                            else -> line
                        }
                        list.add(updated)
                        usedIndices.add(i)
                        usedIndices.add(j)
                        translationFound = true
                        break
                    }
                }
            }

            if (!translationFound) {
                list.add(line)
                usedIndices.add(i)
            }
        }
        return list
    }

    private fun List<ISyncedLine>.rearrangeAccompanimentAlignment(): List<ISyncedLine> {
        var lastAlignment = KaraokeAlignment.Unspecified
        return this.map { line ->
            if (line is KaraokeLine.AccompanimentKaraokeLine) {
                if (line.alignment == lastAlignment) line else line.copy(alignment = lastAlignment)
            } else if (line is KaraokeLine) {
                lastAlignment = line.alignment
                line
            } else {
                lastAlignment = KaraokeAlignment.Unspecified
                line
            }
        }
    }

    private fun List<ISyncedLine>.rearrangeUncheckedLineTime(): List<ISyncedLine> =
        this.mapIndexed { index, line ->
            if (line is SyncedLine) {
                val end = this.getOrNull(index + 1)?.start ?: Int.MAX_VALUE
                line.copy(end = end)
            } else {
                line
            }
        }

    private enum class BracketType {
        ANGLE,    // < >
        SQUARE    // [ ]
    }
}