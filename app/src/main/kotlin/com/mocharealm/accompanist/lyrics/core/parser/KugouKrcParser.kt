package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.stripEnclosingParentheses
import com.mocharealm.accompanist.lyrics.core.utils.KugouKrcMetadataDecoder

object KugouKrcParser : ILyricsParser {
    override fun canParse(content: String): Boolean {
        val lineTimeRegex = """^\[\d+,\d+\]""".toRegex()
        val wordTimeRegex = """<\d+,\d+,\d+>.{1}""".toRegex()

        return content.lineSequence()
            .map { it.trim() }
            .any { line ->
                lineTimeRegex.containsMatchIn(line) && wordTimeRegex.containsMatchIn(line)
            }
    }

    private val KRC_LINE_REGEX = Regex("""^\[(\d+),(\d+)\](.*)$""")
    private val SYLLABLE_REGEX = Regex("""<(\d+),(\d+),\d+>""")
    private val BG_LINE_REGEX = Regex("""^\[bg:(.*)\](.*)$""")
    private const val LANGUAGE_TAG_START = "[language:"

    override fun parse(lines: List<String>): SyncedLyrics = parseInternal(lines.asSequence())
    override fun parse(content: String): SyncedLyrics = parseInternal(content.lineSequence())

    private fun parseInternal(rawLinesSequence: Sequence<String>): SyncedLyrics {
        val rawLines = rawLinesSequence.toList()

        val languageLine = rawLines.firstOrNull { it.trim().startsWith(LANGUAGE_TAG_START) }
        val metadata = KugouKrcMetadataDecoder.decode(languageLine)

        val resultLines = mutableListOf<KaraokeLine>()

        var currentRoleState = KaraokeAlignment.Start
        var lyricLineIndex = 0
        var lastLineStartTime = -1

        for (raw in rawLines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(LANGUAGE_TAG_START)) continue

            if (line.startsWith("[bg:")) {
                parseBackgroundLine(line)?.let { bgLine ->
                    if (resultLines.isNotEmpty()) {
                        val last = resultLines.last()
                        if (last is KaraokeLine.MainKaraokeLine) {
                            resultLines[resultLines.size - 1] = last.copy(
                                accompanimentLines = (last.accompanimentLines
                                    ?: emptyList()) + bgLine
                            )
                        } else {
                            resultLines.add(bgLine)
                        }
                    } else {
                        resultLines.add(bgLine)
                    }
                }
                continue
            }

            val match = KRC_LINE_REGEX.find(line) ?: continue

            var lineStart = match.groupValues[1].toInt()
            if (lastLineStartTime != -1 && lineStart <= lastLineStartTime) {
                lineStart = lastLineStartTime + 3
            }
            lastLineStartTime = lineStart

            val contentPart = match.groupValues[3]
            val rawSyllables = parseSyllablesAndMergeColons(contentPart, lineStart)

            val syllablesWithPhonetics =
                injectPhonetics(rawSyllables, metadata.phonetics, lyricLineIndex)

            val (alignment, finalSyllables, nextState) = determineRole(
                syllablesWithPhonetics,
                currentRoleState
            )
            currentRoleState = nextState

            val translation =
                metadata.translations.getOrNull(lyricLineIndex)?.takeIf { it.isNotBlank() }

            if (finalSyllables.isNotEmpty()) {
                resultLines.add(
                    KaraokeLine.MainKaraokeLine(
                        syllables = finalSyllables,
                        translation = translation,
                        alignment = alignment,
                        start = finalSyllables.first().start,
                        end = finalSyllables.last().end
                    )
                )
            }
            lyricLineIndex++
        }

        return SyncedLyrics(resultLines)
    }

    private fun parseBackgroundLine(line: String): KaraokeLine.AccompanimentKaraokeLine? {
        val m = BG_LINE_REGEX.find(line) ?: return null
        val content = m.groupValues[1]
        // Background vocals may be wrapped in parentheses as a marker; drop them.
        val syllables = parseSyllablesAndMergeColons(content, 0).stripEnclosingParentheses()
        if (syllables.isEmpty()) return null

        return KaraokeLine.AccompanimentKaraokeLine(
            syllables = syllables,
            translation = null,
            alignment = KaraokeAlignment.Unspecified,
            start = syllables.first().start,
            end = syllables.last().end
        )
    }

    private fun injectPhonetics(
        syllables: List<KaraokeSyllable>,
        allPhonetics: List<List<String>>,
        lineIndex: Int
    ): List<KaraokeSyllable> {
        val linePhonetics = allPhonetics.getOrNull(lineIndex)
        return if (linePhonetics != null && linePhonetics.size == syllables.size) {
            syllables.mapIndexed { i, s -> s.copy(phonetic = linePhonetics[i]) }
        } else {
            syllables
        }
    }

    private fun parseSyllablesAndMergeColons(
        content: String,
        baseStartTime: Int
    ): List<KaraokeSyllable> {
        data class TempToken(val offset: Int, val duration: Int, val text: String)

        val tokens = mutableListOf<TempToken>()

        var cursor = 0
        while (cursor < content.length) {
            val m = SYLLABLE_REGEX.find(content, cursor) ?: break
            val offset = m.groupValues[1].toIntOrNull() ?: 0
            val duration = m.groupValues[2].toIntOrNull() ?: 0

            val textStart = m.range.last + 1
            val nextMatch = SYLLABLE_REGEX.find(content, textStart)
            val textEnd = nextMatch?.range?.first ?: content.length

            if (textStart > textEnd) break

            val text = content.substring(textStart, textEnd)
            tokens.add(TempToken(offset, duration, text))
            cursor = textEnd
        }

        if (tokens.isEmpty()) return emptyList()

        val mergedSyllables = mutableListOf<KaraokeSyllable>()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i]
            val next = tokens.getOrNull(i + 1)

            if (next != null && (next.text == "：" || next.text == ":")) {
                val s = baseStartTime + current.offset
                val e = s + current.duration + next.duration
                mergedSyllables.add(KaraokeSyllable(current.text + next.text, s, e))
                i += 2
            } else {
                val s = baseStartTime + current.offset
                val e = s + current.duration
                mergedSyllables.add(KaraokeSyllable(current.text, s, e))
                i++
            }
        }
        return mergedSyllables
    }

    private fun determineRole(
        syllables: List<KaraokeSyllable>,
        currentState: KaraokeAlignment
    ): Triple<KaraokeAlignment, List<KaraokeSyllable>, KaraokeAlignment> {
        if (syllables.isEmpty()) return Triple(
            KaraokeAlignment.Unspecified,
            syllables,
            currentState
        )

        val rawText = syllables.joinToString("") { it.content }
        val hasMarker = rawText.startsWith("：") || rawText.startsWith(":") ||
                rawText.endsWith("：") || rawText.endsWith(":")

        if (hasMarker) {
            val newState =
                if (currentState == KaraokeAlignment.Start) KaraokeAlignment.End else KaraokeAlignment.Start
            return Triple(newState, syllables, newState)
        }

        return Triple(currentState, syllables, currentState)
    }
}