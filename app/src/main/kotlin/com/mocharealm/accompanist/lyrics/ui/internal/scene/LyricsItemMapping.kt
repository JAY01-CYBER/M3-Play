package com.mocharealm.accompanist.lyrics.ui.internal.scene

import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLyrics

/** Source indices belong to the timeline; displayed indices belong to the lazy list. */
internal class LyricsItemMapping(prepared: PreparedLyrics) {
    val sourceIndices = prepared.lines.indices.filter {
        prepared.lines[it] !in prepared.embeddedLines
    }
    val lines = sourceIndices.map { prepared.lines[it] }
    private val sourceToItem: IntArray

    init {
        val owners = mutableMapOf<PreparedLine, Int>()
        fun register(line: PreparedLine, item: Int) {
            owners[line] = item
            line.before.forEach { register(it, item) }
            line.after.forEach { register(it, item) }
        }
        sourceIndices.forEachIndexed { item, source ->
            prepared.lines[source]?.let { register(it, item) }
        }
        sourceToItem = IntArray(prepared.lines.size) { -1 }
        sourceIndices.forEachIndexed { item, source -> sourceToItem[source] = item }
        prepared.lines.forEachIndexed { source, line ->
            owners[line]?.let { sourceToItem[source] = it }
        }
    }

    fun itemIndex(sourceIndex: Int): Int = sourceToItem.getOrElse(sourceIndex) { -1 }
}
