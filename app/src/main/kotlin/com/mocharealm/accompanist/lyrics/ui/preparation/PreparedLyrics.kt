package com.mocharealm.accompanist.lyrics.ui.preparation

class PreparedLyrics internal constructor(val lines: List<PreparedLine?>) {
    internal val allLines = buildList {
        val seen = mutableSetOf<PreparedLine>()
        fun append(line: PreparedLine) {
            if (!seen.add(line)) return
            add(line)
            line.before.forEach(::append)
            line.after.forEach(::append)
        }
        lines.filterNotNull().forEach(::append)
    }
    internal val embeddedLines = allLines.flatMap { it.before + it.after }.toSet()
    internal val rows = allLines.flatMap { it.rows }.filter { it.animated }

    private val fontLayouts =
        buildSet {
                for (line in allLines) {
                    line.translation?.let(::add)
                    line.phonetic?.let(::add)
                    for (run in line.runs) for (group in run.groups) for (unit in group.units) {
                        add(unit.text.layout)
                        unit.phonetic?.let(::add)
                    }
                }
            }
            .toList()

    internal fun hasStaleFonts(): Boolean {
        for (index in fontLayouts.indices) {
            if (fontLayouts[index].multiParagraph.intrinsics.hasStaleResolvedFonts) return true
        }
        return false
    }
}
