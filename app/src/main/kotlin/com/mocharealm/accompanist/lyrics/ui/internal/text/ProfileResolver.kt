package com.mocharealm.accompanist.lyrics.ui.internal.text

import com.mocharealm.accompanist.lyrics.ui.profile.*

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.internal.text.*

internal data class ResolvedProfileRun(
    val profile: LyricsProfile,
    val groups: List<List<KaraokeSyllable>>,
)

internal fun resolveProfiles(
    syllables: List<KaraokeSyllable>,
    profiles: List<LyricsProfile>,
    leadingWhitespace: Boolean = false,
): List<ResolvedProfileRun> {
    if (profiles.all { it in DefaultLyricsProfiles }) {
        val runs = resolveBuiltinProfiles(syllables, profiles)
        return if (leadingWhitespace) assignLeadingWhitespace(runs) else runs
    }
    val result = mutableListOf<ResolvedProfileRun>()
    var pending = mutableListOf<KaraokeSyllable>()
    var profile: LyricsProfile? = null
    for (source in syllables) for (syllable in splitProfileSyllable(source, profiles)) {
        val neutral = syllable.content.isBlank() || syllable.content.isPunctuation()
        val selected =
            if (neutral) profile
            else profiles.firstOrNull { it.matches(syllable) } ?: FallbackProfile
        if (selected != null && profile != null && selected !== profile) {
            result.add(ResolvedProfileRun(profile, profile.groups(pending)))
            pending = mutableListOf()
        }
        if (selected != null) profile = selected
        pending.add(syllable)
    }
    if (pending.isNotEmpty()) {
        val finalProfile = profile ?: FallbackProfile
        result.add(ResolvedProfileRun(finalProfile, finalProfile.groups(pending)))
    }
    return if (leadingWhitespace) assignLeadingWhitespace(result) else result
}

/** Assign separators to the following group before shaping, including across profile boundaries. */
private fun assignLeadingWhitespace(runs: List<ResolvedProfileRun>): List<ResolvedProfileRun> {
    var pending = emptyList<KaraokeSyllable>()
    return runs
        .mapIndexed { runIndex, run ->
            ResolvedProfileRun(
                run.profile,
                buildList {
                    for ((groupIndex, source) in run.groups.withIndex()) {
                        // Terminal whitespace has no following group: retain its source and timing.
                        val last = runIndex == runs.lastIndex && groupIndex == run.groups.lastIndex
                        var boundary = source.size
                        if (!last)
                            while (
                                boundary > 0 && source[boundary - 1].content.isBlank()
                            ) boundary--
                        val body = mutableListOf<KaraokeSyllable>()
                        body.addAll(pending)
                        pending = emptyList()
                        for (i in 0 until boundary) body.add(source[i])
                        val tail = mutableListOf<KaraokeSyllable>()
                        if (!last && body.size > 0 && boundary > 0) {
                            val syllable = source[boundary - 1]
                            val split = syllable.content.trimEnd().length
                            if (split < syllable.content.length) {
                                val time =
                                    syllable.start +
                                        ((syllable.end.toLong() - syllable.start) * split /
                                                syllable.content.length)
                                            .toInt()
                                body[body.lastIndex] =
                                    syllable.copy(
                                        content = syllable.content.substring(0, split),
                                        end = time,
                                    )
                                tail.add(
                                    syllable.copy(
                                        content = syllable.content.substring(split),
                                        start = time,
                                        phonetic = null,
                                    )
                                )
                            }
                        }
                        for (i in boundary until source.size) tail.add(source[i])
                        if (body.isNotEmpty()) add(body)
                        pending = tail
                    }
                },
            )
        }
        .filter { it.groups.isNotEmpty() }
}

/** Split mixed source syllables once; neutral text stays with its neighbouring script. */
private fun splitProfileSyllable(
    source: KaraokeSyllable,
    profiles: List<LyricsProfile>,
): List<KaraokeSyllable> {
    val override = profiles.firstOrNull { it.matches(source) }
    if (override != null && override !in DefaultLyricsProfiles) return listOf(source)
    val parts = mutableListOf<KaraokeSyllable>()
    var begin = 0
    var index = 0
    var previous: LyricsProfile? = null
    while (index < source.content.length) {
        val start = index++
        if (
            source.content[start].isHighSurrogate() &&
                index < source.content.length &&
                source.content[index].isLowSurrogate()
        )
            index++
        val value = source.content[start]
        val neutral =
            value.isWhitespace() ||
                value.isProfilePunctuation() ||
                value.category == CharCategory.NON_SPACING_MARK ||
                value.category == CharCategory.COMBINING_SPACING_MARK ||
                value.category == CharCategory.ENCLOSING_MARK ||
                value.category == CharCategory.FORMAT
        val selected =
            if (neutral) previous
            else {
                val probe = source.copy(content = source.content.substring(start, index))
                profiles.firstOrNull { it.matches(probe) } ?: FallbackProfile
            }
        if (selected != null && previous != null && selected !== previous) {
            parts.add(
                source.copy(
                    content = source.content.substring(begin, start),
                    start =
                        source.start +
                            ((source.end.toLong() - source.start) * begin / source.content.length)
                                .toInt(),
                    end =
                        source.start +
                            ((source.end.toLong() - source.start) * start / source.content.length)
                                .toInt(),
                    phonetic = if (begin == 0) source.phonetic else null,
                )
            )
            begin = start
        }
        if (selected != null) previous = selected
    }
    if (begin == 0) return listOf(source)
    parts.add(
        source.copy(
            content = source.content.substring(begin),
            start =
                source.start +
                    ((source.end.toLong() - source.start) * begin / source.content.length).toInt(),
            phonetic = null,
        )
    )
    return parts
}
