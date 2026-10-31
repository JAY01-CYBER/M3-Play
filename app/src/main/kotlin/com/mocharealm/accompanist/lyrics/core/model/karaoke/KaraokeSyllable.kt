package com.mocharealm.accompanist.lyrics.core.model.karaoke


data class KaraokeSyllable(
    val content: String,
    val start: Int,
    val end: Int,
    val phonetic: String? = null,
    /** BCP-47 language tag used when generating a fallback phonetic. */
    val languageTag: String? = null,
    /** Formatter-owned boundary before this caption when adjacent timed fragments are combined.
     * Empty by default for legacy word fragments, such as "to" + "night".
     */
    val phoneticSeparatorBefore: String = "",
) {
    val duration = end - start

    init {
        require(end >= start)
    }

    fun progress(current: Int): Float {
        return when {
            current < start -> 0f
            current in start..end -> (current - start).toFloat() / duration
            current > end -> 1f
            else -> 0f
        }.coerceIn(0f, 1f)
    }
}
