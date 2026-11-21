package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel

interface PhoneticProvider {
    val phoneticLevel: PhoneticLevel
    fun getPhonetic(string: String): String

    /** Language-aware overload. Existing providers keep working through this default. */
    fun getPhonetic(string: String, languageTag: String?): String = getPhonetic(string)

    /** Resolve full context once, then project only proven source alignments.
     * The default keeps existing providers working; contextual providers override this method.
     */
    fun resolve(request: PhoneticRequest): PhoneticResponse {
        val tag = request.hints.singleOrNull()?.takeIf {
            it.range.start == 0 && it.range.end == request.text.length
        }?.languageTag
        return PhoneticResponse(
            getPhonetic(request.text, tag),
            if (phoneticLevel == PhoneticLevel.SYLLABLE) request.projectionRanges.map { range ->
                val language = request.hints.firstOrNull { range.start >= it.range.start && range.end <= it.range.end }?.languageTag
                PhoneticProjection(getPhonetic(request.text.substring(range.start, range.end), language).ifBlank { null })
            } else emptyList(),
        )
    }
}

/** Half-open UTF-16 offsets into the original, unmodified text. */
data class PhoneticTextRange(val start: Int, val end: Int) {
    init { require(start >= 0 && end >= start) }
}
data class PhoneticLanguageHint(val range: PhoneticTextRange, val languageTag: String)
data class PhoneticRequest(
    val text: String,
    val hints: List<PhoneticLanguageHint> = emptyList(),
    val projectionRanges: List<PhoneticTextRange> = emptyList(),
) {
    init {
        fun valid(range: PhoneticTextRange) = range.end <= text.length &&
            listOf(range.start, range.end).all { at ->
                !(at > 0 && at < text.length && text[at].isLowSurrogate() && text[at - 1].isHighSurrogate())
            }
        require(projectionRanges.all(::valid))
        val sorted = hints.sortedBy { it.range.start }
        require(sorted.all { valid(it.range) && it.range.end > it.range.start })
        require(sorted.zipWithNext().all { (a, b) -> a.range.end <= b.range.start })
    }
}
data class PhoneticResponse(
    /** Null means the text needs no generated pronunciation caption. */
    val phonetic: String?,
    /** Same order as request ranges; empty means this provider supports line captions only. */
    val projections: List<PhoneticProjection> = emptyList(),
)
data class PhoneticProjection(
    /** Null with aligned=true deliberately omits a caption, e.g. for Latin source text. */
    val phonetic: String?,
    val aligned: Boolean = true,
    /** Preserves a formatting boundary without putting whitespace in the caption itself. */
    val separatorBefore: String = "",
)
