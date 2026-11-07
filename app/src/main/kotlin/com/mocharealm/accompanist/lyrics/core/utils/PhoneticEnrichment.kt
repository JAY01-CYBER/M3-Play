package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.copy
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/** Add missing captions without changing source text, timing, metadata or supplied phonetics. */
fun SyncedLyrics.withPhonetics(provider: PhoneticProvider): SyncedLyrics {
    fun process(source: KaraokeLine): KaraokeLine {
        val populated = if (!source.phonetic.isNullOrBlank() || source.syllables.any { !it.phonetic.isNullOrBlank() }) source
        else {
            val text = source.syllables.joinToString("") { it.content }
            if (text.isBlank()) source
            else {
                var offset = 0
                val ranges = source.syllables.map { syllable ->
                    PhoneticTextRange(offset, offset + syllable.content.length).also { offset = it.end }
                }
                val hints = ArrayList<PhoneticLanguageHint>()
                source.syllables.forEachIndexed { index, syllable ->
                    val tag = syllable.languageTag ?: source.languageTag
                    val range = ranges[index]
                    if (tag != null && range.end > range.start) {
                        val previous = hints.lastOrNull()
                        if (previous?.languageTag == tag && previous.range.end == range.start)
                            hints[hints.lastIndex] = previous.copy(range = PhoneticTextRange(previous.range.start, range.end))
                        else hints.add(PhoneticLanguageHint(range, tag))
                    }
                }
                val result = provider.resolve(PhoneticRequest(text, hints, ranges))
                require(result.projections.isEmpty() || result.projections.size == ranges.size)
                if (result.projections.size == ranges.size && result.projections.all { it.aligned })
                    source.copy(syllables = source.syllables.mapIndexed { index, syllable ->
                        val projection = result.projections[index]
                        syllable.copy(phonetic = projection.phonetic,
                            phoneticSeparatorBefore = if (projection.phonetic.isNullOrBlank()) "" else projection.separatorBefore)
                    })
                else source.copy(phonetic = result.phonetic)
            }
        }
        return if (populated is KaraokeLine.MainKaraokeLine) populated.copy(
            accompanimentLines = populated.accompanimentLines?.map { process(it) as KaraokeLine.AccompanimentKaraokeLine },
        ) else populated
    }
    return copy(lines = lines.map { line ->
        when (line) {
            is KaraokeLine -> process(line)
            is SyncedLine -> if (!line.phonetic.isNullOrBlank() || line.content.isBlank()) line else {
                val hints = line.languageTag?.let {
                    listOf(PhoneticLanguageHint(PhoneticTextRange(0, line.content.length), it))
                }.orEmpty()
                line.copy(phonetic = provider.resolve(PhoneticRequest(line.content, hints)).phonetic)
            }
            else -> line
        }
    })
}
