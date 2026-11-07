package com.mocharealm.accompanist.lyrics.phonetics

/** Ordered script dispatch and reading policy, independent of UI layout and Romanization.
 * Profiles resolve complete contiguous runs rather than individual timed lyric fragments.
 * Implementations must be safe to share; mutable scratch belongs in the supplied workspace.
 */
interface PhoneticProfile {
    fun matches(codePoint: Int): Boolean
    /** Optional contextual dispatch, e.g. digits belonging to a Japanese counter. */
    fun matches(text: String, index: Int, options: ParseOptions): Boolean = matches(text.scalarAt(index))
    fun resolve(text: String, range: TextRange, options: ParseOptions, workspace: ParseWorkspace): LanguageResult
}

/** Tags, kana and the configured region select a contextual Han/kana resolver. */
class CjkPhoneticProfile(
    resolvers: List<LanguageResolver>,
    private val defaultLocale: ReadingLocale? = null,
) : PhoneticProfile {
    private val languages = resolvers.associateBy { it.locale }
    init { require(languages.size == resolvers.size) { "Duplicate locale resolver" } }
    override fun matches(codePoint: Int): Boolean = isReadingScript(codePoint)
    override fun matches(text: String, index: Int, options: ParseOptions): Boolean {
        if (matches(text.scalarAt(index))) return true
        if (!JapaneseNumbers.isDigit(text[index]) && text[index] != '.' && text[index] != '．') return false
        val hint = options.hints.firstOrNull { index >= it.range.start && index < it.range.end }
        val from = hint?.range?.start ?: options.hints.lastOrNull { it.range.end <= index }?.range?.end ?: 0
        val limit = hint?.range?.end ?: options.hints.firstOrNull { it.range.start > index }?.range?.start ?: text.length
        var start = index
        while (start > from && (JapaneseNumbers.isDigit(text[start - 1]) || text[start - 1] in ".．")) start--
        if (start > from && text[start - 1] in "负負") start--
        var end = index
        while (end < limit && (isReadingScript(text.scalarAt(end)) || JapaneseNumbers.isDigit(text[end]) || text[end] in ".．")) end = text.nextScalar(end)
        val locale = if (hint != null) ReadingLocale.fromLanguageTag(hint.languageTag)
            else if (text.substring(start, end).any { isKana(it.code) }) ReadingLocale.JAPANESE else options.defaultLocale ?: defaultLocale
        return when (locale) {
            ReadingLocale.JAPANESE -> JapaneseNumbers.match(text, start, limit) != null
            ReadingLocale.MANDARIN_CN, ReadingLocale.MANDARIN_TW, ReadingLocale.CANTONESE_HK -> ChineseNumbers.match(text, start, limit)?.let {
                index < it.digitsEnd && (it.counter.isNotEmpty() || hint != null ||
                    text.any { ch -> isReadingScript(ch.code) } || text.none { ch -> ch in 'a'..'z' || ch in 'A'..'Z' })
            } == true
            else -> false
        }
    }

    override fun resolve(text: String, range: TextRange, options: ParseOptions, workspace: ParseWorkspace): LanguageResult {
        val hint = options.hints.firstOrNull { range.start >= it.range.start && range.end <= it.range.end }
        val hasKana = range.textIn(text).any { isKana(it.code) || it == 'ー' }
        val locale = if (hint != null) ReadingLocale.fromLanguageTag(hint.languageTag)
            else if (hasKana) ReadingLocale.JAPANESE else options.defaultLocale ?: defaultLocale
        if (hint != null && locale == null) return LanguageResult(listOf(
            PhoneticSpan(range, null, Resolution.UNKNOWN, reason = "Unsupported language tag: ${hint.languageTag}"),
        ))
        if (locale != null) return languages[locale]?.resolve(text, range, options, workspace)
            ?: LanguageResult(listOf(PhoneticSpan(range, locale, Resolution.UNKNOWN, reason = "Language pack not installed")))
        return LanguageResult(
            listOf(PhoneticSpan(range, null, Resolution.LANGUAGE_AMBIGUOUS, reason = "Han script does not identify a spoken language")),
            languageAlternatives = languages.map { (possible, resolver) ->
                val result = resolver.resolve(text, range, options, workspace)
                LanguageAlternative(range, possible, result.spans, result.ambiguities)
            },
        )
    }
}

/** Pure extension formatting: it must not parse or query language data. */
interface PronunciationFormatter {
    val notation: String
    fun format(pronunciation: Pronunciation, options: FormatOptions): String
}

/** Script classification for optional profiles; no Korean dictionary or parser is bundled. */
fun isHangulCodePoint(codePoint: Int): Boolean = codePoint in 0x1100..0x11FF ||
    codePoint in 0x3130..0x318F || codePoint in 0xA960..0xA97F ||
    codePoint in 0xAC00..0xD7A3 || codePoint in 0xD7B0..0xD7FF || codePoint in 0xFFA0..0xFFDC
