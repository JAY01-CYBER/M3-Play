package com.mocharealm.accompanist.lyrics.core.exporter

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.toTimeFormattedString

object TTMLExporter : ILyricsExporter {
    override fun export(lyrics: SyncedLyrics): String {
        if (lyrics.lines.isEmpty()) return ""

        val builder = StringBuilder()
        val hasAlignmentData = lyrics.lines.run {
            var hasStart = false
            var hasEnd = false

            if (lyrics.lines.isEmpty()) {
                false
            }

            for (line in lyrics.lines) {
                if (line is KaraokeLine) {
                    when (line.alignment) {
                        KaraokeAlignment.Start -> hasStart = true
                        KaraokeAlignment.End -> hasEnd = true
                        else -> {}
                    }
                }
                if (hasStart && hasEnd) {
                    break
                }
            }
            hasStart && hasEnd
        }

        builder.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        builder.appendLine("""<tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" itunes:timing="Word">""")

        builder.appendLine("  <head>")
        if (hasAlignmentData) {
            builder.appendLine("    <metadata>")
            builder.appendLine("""      <ttm:agent type="person" xml:id="v1"/>""")
            builder.appendLine("""      <ttm:agent type="person" xml:id="v2"/>""")
            builder.appendLine("    </metadata>")
        }
        builder.appendLine("  </head>")

        val totalDuration = lyrics.lines.maxOfOrNull { it.end } ?: 0
        builder.appendLine("""  <body dur="${totalDuration.toTimeFormattedString()}">""")

        val firstLineTime = lyrics.lines.first().start
        builder.appendLine("""    <div begin="${firstLineTime.toTimeFormattedString()}" end="${totalDuration.toTimeFormattedString()}">""")

        lyrics.lines.forEach { line ->
            when (line) {
                is KaraokeLine.MainKaraokeLine -> appendPElement(builder, line)
                is SyncedLine -> appendPElement(builder, line)
            }
        }

        builder.appendLine("    </div>")
        builder.appendLine("  </body>")
        builder.appendLine("</tt>")

        return builder.toString()
    }

    private fun appendPElement(builder: StringBuilder, line: KaraokeLine.MainKaraokeLine) {
        val agent = when (line.alignment) {
            KaraokeAlignment.Start -> """ ttm:agent="v1""""
            KaraokeAlignment.End -> """ ttm:agent="v2""""
            else -> ""
        }
        val pTag = """<p begin="${line.start.toTimeFormattedString()}" end="${line.end.toTimeFormattedString()}"$agent>"""
        builder.append("      $pTag")

        fun buildContent(syllables: List<KaraokeSyllable>, translation: String?) {
            syllables.forEach { syllable ->
                val content = syllable.content
                val trimmedContent = content.trim()
                val escapedContent = trimmedContent
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

                builder.append("""<span begin="${syllable.start.toTimeFormattedString()}" end="${syllable.end.toTimeFormattedString()}">$escapedContent</span>""")
                if (content.endsWith(' ')) {
                    builder.append(" ")
                }
            }
            if (translation != null) {
                builder.append("""<span ttm:role="x-translation" xml:lang="zh-CN">${translation.trim()}</span>""")
            }
        }

        buildContent(line.syllables, line.translation)
        
        line.accompanimentLines?.forEach { bgLine ->
            builder.append("""<span ttm:role="x-bg" begin="${bgLine.start.toTimeFormattedString()}" end="${bgLine.end.toTimeFormattedString()}">""")
            buildContent(bgLine.syllables, bgLine.translation)
            builder.append("""</span>""")
        }
        
        builder.appendLine("</p>")
    }

    private fun appendPElement(builder: StringBuilder, line: SyncedLine) {
        val pTag = """<p begin="${line.start.toTimeFormattedString()}" end="${line.end.toTimeFormattedString()}">"""
        builder.append("      $pTag")

        val escapedContent = line.content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            
        builder.append(escapedContent)

        if (line.translation != null) {
            val escapedTranslation = line.translation
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            builder.append("""<span ttm:role="x-translation" xml:lang="zh-CN">${escapedTranslation.trim()}</span>""")
        }
        
        builder.appendLine("</p>")
    }
}