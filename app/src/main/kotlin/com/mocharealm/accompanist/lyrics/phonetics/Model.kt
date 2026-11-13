package com.mocharealm.accompanist.lyrics.phonetics

/** Offsets into the ORIGINAL string, in UTF-16 code units; end is exclusive. */
data class TextRange(val start: Int, val end: Int) {
    init { require(start >= 0 && end >= start) }
    fun textIn(text: String): String = text.substring(start, end)
}

enum class ReadingLocale(val languageTag: String, val packName: String) {
    MANDARIN_CN("zh-CN", "zh-CN.lpd"), MANDARIN_TW("zh-TW", "zh-TW.lpd"),
    CANTONESE_HK("yue-HK", "yue-HK.lpd"), JAPANESE("ja-JP", "ja-JP.lpd");

    companion object {
        fun fromLanguageTag(tag: String): ReadingLocale? {
            val parts = tag.replace('_', '-').lowercase().split('-')
            return when (parts.first()) {
                "ja" -> JAPANESE
                "yue" -> CANTONESE_HK
                "zh", "cmn" -> when {
                    "hk" in parts -> CANTONESE_HK
                    "tw" in parts || "hant" in parts -> MANDARIN_TW
                    else -> MANDARIN_CN
                }
                else -> null
            }
        }
    }
}

/** A canonical Pinyin/Jyutping syllable WITHOUT its tone digit, or Japanese kana. */
data class ReadingUnit(val value: String, val tone: Int? = null, val sourceRange: TextRange? = null) {
    init { require(value.isNotEmpty() && (tone == null || tone in 1..6)) }
}
data class Pronunciation(
    val locale: ReadingLocale?,
    val units: List<ReadingUnit>,
    /** Extension readings retain native notation until a separate formatter is applied. */
    val notation: String? = null,
    val languageTag: String = locale?.languageTag ?: "und",
) {
    init {
        require(units.isNotEmpty())
        if (notation != null) {
            require(notation.isNotBlank() && locale == null && languageTag.isNotBlank() && languageTag != "und")
            require(units.all { it.tone == null })
        } else if (locale != ReadingLocale.JAPANESE) {
            require(locale != null)
            require(units.all { unit -> unit.value.all { it in 'a'..'z' || it == '^' } })
            require(locale == ReadingLocale.CANTONESE_HK || units.all { it.tone == null || it.tone in 1..5 })
        } else require(units.all { it.tone == null })
    }
}
data class ReadingCandidate(
    val pronunciation: Pronunciation,
    val sourceIds: List<String>,
    /** Difference from the best complete sentence cost; never a probability. */
    val costDelta: Long = 0,
    /** Complete sentence cost difference with weak preferences removed; not a probability. */
    val uncertaintyCostDelta: Long = costDelta,
    val selectionRules: List<String> = emptyList(),
)
enum class Resolution { RESOLVED, AMBIGUOUS, UNKNOWN, PASSTHROUGH, LANGUAGE_AMBIGUOUS }
data class PhoneticSpan(
    val range: TextRange,
    val locale: ReadingLocale?,
    val resolution: Resolution,
    val selected: ReadingCandidate? = null,
    /** Includes less likely readings as well as unresolved readings. */
    val candidates: List<ReadingCandidate> = emptyList(),
    val reason: String? = null,
)
/** Also retains tied segmentations whose boundaries differ from the selected path. */
data class ReadingAmbiguity(val range: TextRange, val alternatives: List<PhoneticSpan>)
data class LanguageAlternative(val range: TextRange, val locale: ReadingLocale, val spans: List<PhoneticSpan>, val ambiguities: List<ReadingAmbiguity> = emptyList())
data class ParseResult(
    val text: String,
    val spans: List<PhoneticSpan>,
    val ambiguities: List<ReadingAmbiguity> = emptyList(),
    val languageAlternatives: List<LanguageAlternative> = emptyList(),
)
data class LanguageHint(val range: TextRange, val languageTag: String)
/** Explicit caller pronunciation; ranges form lexical boundaries and retain main-text positions. */
data class ReadingOverride(val range: TextRange, val pronunciation: Pronunciation, val sourceId: String = "caller") {
    init { require(sourceId.isNotBlank()) }
}
data class ParseOptions(
    /** Null avoids guessing the language of Han-only runs. */
    val defaultLocale: ReadingLocale? = null,
    val hints: List<LanguageHint> = emptyList(),
    val ambiguityCostWindow: Long = 100,
    val overrides: List<ReadingOverride> = emptyList(),
) { init { require(ambiguityCostWindow >= 0) } }

/** Public extension point: a language may use its own tokenizer, index and resolver. */
interface LanguageResolver {
    val locale: ReadingLocale
    fun resolve(text: String, range: TextRange, options: ParseOptions, workspace: ParseWorkspace): LanguageResult
}
data class LanguageResult(
    val spans: List<PhoneticSpan>,
    val ambiguities: List<ReadingAmbiguity> = emptyList(),
    val languageAlternatives: List<LanguageAlternative> = emptyList(),
)

/** Immutable, thread-safe random access. Implementations must retain ownership of their storage. */
interface ByteSource {
    val size: Int
    fun byteAt(offset: Int): Int
}
class ArrayByteSource(bytes: ByteArray) : ByteSource {
    private val data = bytes.copyOf()
    override val size: Int get() = data.size
    override fun byteAt(offset: Int): Int = data[offset].toInt() and 255
}
fun interface PackLoader { fun load(packName: String): ByteSource }

internal fun String.nextScalar(at: Int): Int =
    if (at + 1 < length && this[at].isHighSurrogate() && this[at + 1].isLowSurrogate()) at + 2 else at + 1
internal fun String.scalarAt(at: Int): Int = if (nextScalar(at) == at + 2)
    0x10000 + ((this[at].code - 0xD800) shl 10) + this[at + 1].code - 0xDC00 else this[at].code
internal fun String.isScalarBoundary(at: Int): Boolean = at in 0..length &&
    !(at > 0 && at < length && this[at].isLowSurrogate() && this[at - 1].isHighSurrogate())
internal fun isHan(cp: Int): Boolean = cp in 0x3400..0x4DBF || cp in 0x4E00..0x9FFF ||
    cp in 0xF900..0xFAFF || cp in 0x20000..0x33479
internal fun isKana(cp: Int): Boolean = cp in 0x3041..0x3096 || cp in 0x30A1..0x30FA ||
    cp in 0xFF66..0xFF9F || cp in 0x3099..0x309C
internal fun isReadingScript(cp: Int): Boolean = isHan(cp) || isKana(cp) || cp == 0x30FC || cp == 0x3005 || cp == 0x3007 || cp in 0x309D..0x309E
