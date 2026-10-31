package com.mocharealm.accompanist.lyrics.ui.internal.text

import com.mocharealm.accompanist.lyrics.ui.profile.*

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

/** Classify a code point once, independently of the number/order of configured profiles. */
internal fun builtinProfileKind(codePoint: Int): Int {
    return when (codePoint) {
        in 0x41..0x5A,
        in 0x61..0x7A,
        in 0x00C0..0x024F,
        in 0x1E00..0x1EFF -> 9
        in 0x0600..0x06FF,
        in 0x0750..0x077F,
        in 0x0870..0x089F,
        in 0x08A0..0x08FF,
        in 0xFB50..0xFDFF,
        in 0xFE70..0xFEFF -> 0
        in 0x0590..0x05FF,
        in 0xFB1D..0xFB4F -> 1
        in 0x0900..0x0DFF,
        in 0x1CD0..0x1CFF,
        in 0xA8E0..0xA8FF -> 2
        in 0x0E00..0x0EFF,
        in 0x1000..0x109F,
        in 0x1780..0x17FF,
        in 0x19E0..0x19FF,
        in 0xA9E0..0xA9FF,
        in 0xAA60..0xAA7F -> 3
        in 0x0F00..0x0FFF -> 4
        in 0x1100..0x11FF,
        in 0xA960..0xA97F,
        in 0xD7B0..0xD7FF -> 5
        in 0x0400..0x052F,
        in 0x2DE0..0x2DFF,
        in 0xA640..0xA69F -> 6
        in 0x0370..0x03FF,
        in 0x1F00..0x1FFF -> 7
        in 0x20000..0x323AF -> 8
        else ->
            if (
                codePoint <= 0xFFFF &&
                    with(codePoint.toChar()) { isCjk() || isJapanese() || isKorean() }
            )
                8
            else 10
    }
}

/** Resolve precedence once. Jamo overlaps CJK; fallback may be explicitly ordered first. */
internal class BuiltinProfileDispatch(profiles: List<LyricsProfile>) {
    private val ranks = IntArray(DefaultLyricsProfiles.size) { Int.MAX_VALUE }
    private val selected: Array<LyricsProfile>
    private val basicJamo: LyricsProfile

    init {
        for (index in profiles.indices) {
            val kind = DefaultLyricsProfiles.indexOf(profiles[index])
            if (kind >= 0 && ranks[kind] == Int.MAX_VALUE) ranks[kind] = index
        }
        selected =
            Array(ranks.size) { kind ->
                if (ranks[kind] < ranks[10]) DefaultLyricsProfiles[kind] else FallbackProfile
            }
        val jamoKind = if (ranks[5] < ranks[8]) 5 else 8
        basicJamo = selected[jamoKind]
    }

    fun profile(codePoint: Int): LyricsProfile =
        if (codePoint in 0x1100..0x11FF) basicJamo else selected[builtinProfileKind(codePoint)]
}

/** One pass emits already-resolved runs; no per-character syllables or second matching pass. */
internal fun resolveBuiltinProfiles(
    syllables: List<KaraokeSyllable>,
    profiles: List<LyricsProfile>,
): List<ResolvedProfileRun> {
    val dispatch = BuiltinProfileDispatch(profiles)
    val result = mutableListOf<ResolvedProfileRun>()
    var pending = mutableListOf<KaraokeSyllable>()
    var current: LyricsProfile? = null
    fun append(source: KaraokeSyllable, selected: LyricsProfile?) {
        if (selected != null && current != null && selected !== current) {
            result.add(ResolvedProfileRun(current!!, current!!.groups(pending)))
            pending = mutableListOf()
        }
        if (selected != null) current = selected
        pending.add(source)
    }
    for (source in syllables) {
        val text = source.content
        var begin = 0
        var index = 0
        var previous: LyricsProfile? = null
        fun emit(end: Int) {
            val part =
                if (begin == 0 && end == text.length) source
                else
                    source.copy(
                        content = text.substring(begin, end),
                        start =
                            source.start +
                                ((source.end.toLong() - source.start) * begin / text.length)
                                    .toInt(),
                        end =
                            source.start +
                                ((source.end.toLong() - source.start) * end / text.length).toInt(),
                        phonetic = if (begin == 0) source.phonetic else null,
                    )
            append(part, previous)
        }
        while (index < text.length) {
            val start = index++
            val value = text[start]
            val codePoint =
                if (
                    value.isHighSurrogate() && index < text.length && text[index].isLowSurrogate()
                ) {
                    0x10000 + ((value.code - 0xD800) shl 10) + text[index++].code - 0xDC00
                } else value.code
            val category = value.category
            if (
                value.isWhitespace() ||
                    value.isProfilePunctuation() ||
                    category == CharCategory.NON_SPACING_MARK ||
                    category == CharCategory.COMBINING_SPACING_MARK ||
                    category == CharCategory.ENCLOSING_MARK ||
                    category == CharCategory.FORMAT
            )
                continue
            val selected = dispatch.profile(codePoint)
            if (previous != null && selected !== previous) {
                emit(start)
                begin = start
            }
            previous = selected
        }
        emit(text.length)
    }
    if (pending.isNotEmpty()) {
        val profile = current ?: FallbackProfile
        result.add(ResolvedProfileRun(profile, profile.groups(pending)))
    }
    return result
}
