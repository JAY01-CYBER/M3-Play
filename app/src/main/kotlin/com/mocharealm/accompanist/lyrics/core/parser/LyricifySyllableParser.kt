package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.stripEnclosingParentheses
import com.mocharealm.accompanist.lyrics.core.utils.LrcMetadataHelper
import com.mocharealm.accompanist.lyrics.core.utils.isDigitsOnly

/**
 * A parser for lyrics in the Lyricify Syllable format.
 *
 * More information about Lyricify Syllable format can be found [here](https://github.com/WXRIW/Lyricify-App/blob/main/docs/Lyricify%204/Lyrics.md#lyricify-syllable-%E6%A0%BC%E5%BC%8F%E8%A7%84%E8%8C%83).
 */
object LyricifySyllableParser: ILyricsParser {
    override fun canParse(content: String): Boolean {
        val detector = """[a-zA-Z]+\s*\(\d+,\d+\)""".toRegex()
        return content.contains(detector)
    }

    private val syllableRegex = Regex("(.*?)\\((\\d+),(\\d+)\\)")
    private val attributeRegex = Regex("\\[(\\d+)\\]")



    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = mutableListOf<KaraokeLine>()
        lyricsLines.forEach { line ->
            if (line.isNotBlank()) {
                val parsed = parseLine(line)
                // 逻辑保持一致，但注意 data.last() 的安全性
                if (parsed is KaraokeLine.AccompanimentKaraokeLine && data.isNotEmpty()) {
                    val last = data.last()
                    if (last is KaraokeLine.MainKaraokeLine) {
                        data[data.size - 1] = last.copy(
                            accompanimentLines = (last.accompanimentLines ?: emptyList()) + parsed
                        )
                    } else {
                        data.add(parsed)
                    }
                } else {
                    data.add(parsed)
                }
            }
        }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(line: String): KaraokeLine {
        val real: String
        var isAccompaniment = false
        val alignment: KaraokeAlignment

        if (line.contains("]") && line.contains("[") && (line.indexOf("]") - line.indexOf("[") == 2)) {
            real = line.substring(line.indexOf(']') + 1)
            val attribute = attributeRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

            if (attribute !in 0..5) {
                isAccompaniment = true
            }
            alignment = if (attribute == 2 || attribute == 5 || attribute == 8) {
                KaraokeAlignment.End
            } else {
                KaraokeAlignment.Start
            }
        } else {
            real = line
            isAccompaniment = false
            alignment = KaraokeAlignment.Start
        }

        val data = syllableRegex.findAll(real)
        val syllables = data.map { matched ->
            val result = matched.groupValues
            if (result.size >= 4 && result[2].isDigitsOnly() && result[3].isDigitsOnly()) {
                val start = result[2].toInt()
                KaraokeSyllable(
                    content = result[1],
                    start = start,
                    end = start + result[3].toInt()
                )
            } else {
                KaraokeSyllable("Error", 0, 0)
            }
        }.toList()

        val startTime = syllables.firstOrNull()?.start ?: 0
        val endTime = syllables.lastOrNull()?.end ?: 0

        return if (isAccompaniment) {
            // Background vocals are wrapped in parentheses as a marker; drop them.
            KaraokeLine.AccompanimentKaraokeLine(
                syllables.stripEnclosingParentheses(), null, alignment, startTime, endTime
            )
        } else {
            KaraokeLine.MainKaraokeLine(syllables, null, alignment, startTime, endTime)
        }
    }
}