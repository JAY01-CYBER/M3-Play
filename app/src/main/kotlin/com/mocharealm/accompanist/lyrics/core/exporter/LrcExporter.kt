package com.mocharealm.accompanist.lyrics.core.exporter

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.contentToString
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.mapper.toSyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.toTimeFormattedString

/**
 * Exporter for the standard LRC format.
 *
 * It converts [SyncedLyrics] back into a string representation in the standard LRC format without syllables and background lines.
 * - Supports ID3 tags ([ti:...], [ar:...]).
 * - Supports standard line timestamps [mm:ss.xx].
 * - Supports translations as separate lines with the same timestamp.
 */
object LrcExporter : ILyricsExporter {
    override fun export(lyrics: SyncedLyrics): String {
        if (lyrics.lines.isEmpty()) return ""

        val builder = StringBuilder()

        if (lyrics.title.isNotBlank()) {
            builder.appendLine("[ti:${lyrics.title}]")
        }
        if (!lyrics.artists.isNullOrEmpty() && lyrics.artists.all { it.name.isNotBlank() }) {
            builder.appendLine(
                "[ar:${lyrics.artists.joinToString("/") { it.name }}]"
            )
        }

        lyrics.lines.forEach { line ->
            val normalizedLine = when (line) {
                is KaraokeLine -> line.toSyncedLine()
                is SyncedLine -> line
                else -> return@forEach
            }

            val timeTag = "[${normalizedLine.start.toTimeFormattedString()}]"

            builder.appendLine("${timeTag}${normalizedLine.content}")
            normalizedLine.translation?.let { builder.appendLine("${timeTag}${it}") }
        }

        return builder.toString()
    }
}