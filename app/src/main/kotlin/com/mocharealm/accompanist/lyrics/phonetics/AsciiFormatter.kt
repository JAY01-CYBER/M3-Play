package com.mocharealm.accompanist.lyrics.phonetics

enum class ToneStyle { NONE, NUMBERS }
enum class UmlautStyle { V, U_COLON }
enum class PassthroughStyle { ASCII, OMIT }
data class FormatOptions(
    val tones: ToneStyle = ToneStyle.NONE,
    val umlaut: UmlautStyle = UmlautStyle.V,
    val japanese: JapaneseRomanization = JapaneseRomanization.HEPBURN,
    val separator: String = " ",
    val unknownMarker: String = "_",
    val requireResolved: Boolean = false,
    /** Caption consumers may omit text that already needs no pronunciation, including Latin. */
    val passthrough: PassthroughStyle = PassthroughStyle.ASCII,
) {
    init { require(separator.all { it.code < 128 } && unknownMarker.isNotEmpty() && unknownMarker.all { it.code in 32..126 }) }
}
data class FormattedSpan(val sourceRange: TextRange, val latin: String, val resolution: Resolution)
data class FormattedResult(val latin: String, val spans: List<FormattedSpan>)
/** A proven projection plus the formatter's boundary before it; empty captions carry no boundary. */
data class FormattedProjection(val latin: String, val separatorBefore: String = "")

/** Pure formatting of already resolved readings. This class never reads a dictionary. */
class AsciiFormatter(val options: FormatOptions = FormatOptions(), formatters: List<PronunciationFormatter> = emptyList()) {
    private val formatters = formatters.associateBy { it.notation }
    init { require(this.formatters.size == formatters.size) { "Duplicate notation formatter" } }
    fun format(pronunciation: Pronunciation): String = if (pronunciation.notation != null) {
        val output = formatters[pronunciation.notation]?.format(pronunciation, options) ?: options.unknownMarker
        require(output.all { it.code < 128 }) { "A pronunciation formatter must produce Latin ASCII" }
        output
    } else if (pronunciation.locale == ReadingLocale.JAPANESE)
        KanaRomanizer.format(pronunciation.units.joinToString("") { it.value }, options.japanese, options.unknownMarker)
    else pronunciation.units.joinToString(options.separator) { unit ->
        val letters = if (options.umlaut == UmlautStyle.U_COLON) unit.value.replace("v", "u:") else unit.value
        letters + if (options.tones == ToneStyle.NUMBERS) unit.tone?.toString().orEmpty() else ""
    }

    fun format(result: ParseResult): FormattedResult {
        fun japaneseReading(span: PhoneticSpan?): String? = span?.selected?.pronunciation?.takeIf {
            it.locale == ReadingLocale.JAPANESE && it.notation == null &&
                (!options.requireResolved || span.resolution == Resolution.RESOLVED)
        }?.units?.joinToString("") { it.value }
        val joinedKana = BooleanArray(result.spans.size)
        val spans = ArrayList<FormattedSpan>(result.spans.size)
        result.spans.forEachIndexed { index, span ->
            val reading = japaneseReading(span)
            val firstKana = reading?.firstOrNull()
            val lastKana = reading?.lastOrNull()
            val previous = result.spans.getOrNull(index - 1)
            val precedingVowel = if ((firstKana == 'ー' || firstKana == 'ｰ') && japaneseReading(previous) != null &&
                previous?.range?.end == span.range.start) spans.lastOrNull()?.latin?.lastOrNull()?.takeIf { it in "aeiou" } else null
            val next = result.spans.getOrNull(index + 1)
            val following = japaneseReading(next)?.takeIf { span.range.end == next?.range?.start }
            val connects = (lastKana == 'っ' || lastKana == 'ッ' || lastKana == 'ｯ') && following != null &&
                KanaRomanizer.sokuonPrefix(following, options.japanese) != null
            val contextualN = options.separator.isEmpty() && (lastKana == 'ん' || lastKana == 'ン' || lastKana == 'ﾝ') && following != null
            if (connects) joinedKana[index + 1] = true
            if (precedingVowel != null) joinedKana[index] = true
            val latin = when {
                span.resolution == Resolution.PASSTHROUGH -> if (options.passthrough == PassthroughStyle.OMIT) "" else asciiLiteral(span.range.textIn(result.text))
                connects || contextualN || precedingVowel != null -> KanaRomanizer.format(reading!!, options.japanese,
                    options.unknownMarker, if (connects || contextualN) following.orEmpty() else "", precedingVowel)
                span.selected != null && (!options.requireResolved || span.resolution == Resolution.RESOLVED) -> format(span.selected.pronunciation)
                else -> options.unknownMarker
            }
            spans.add(FormattedSpan(span.range, latin, span.resolution))
        }
        val out = StringBuilder()
        for ((index, span) in spans.withIndex()) {
            if (!joinedKana[index] && out.isNotEmpty() && out.last().isLetterOrDigit() && span.latin.firstOrNull()?.isLetterOrDigit() == true) out.append(options.separator)
            out.append(span.latin)
        }
        return FormattedResult(out.toString(), spans)
    }

    /** Exact projection only. Null means an indivisible word reading crosses the requested range.
     * In particular, Japanese word readings are NEVER guessed character by character.
     */
    fun formatRange(result: ParseResult, range: TextRange): String? {
        require(range.end <= result.text.length && result.text.isScalarBoundary(range.start) && result.text.isScalarBoundary(range.end))
        val projected = ArrayList<PhoneticSpan>()
        for (span in result.spans) {
            if (span.range.end <= range.start || span.range.start >= range.end) continue
            val intersection = TextRange(maxOf(span.range.start, range.start), minOf(span.range.end, range.end))
            if (intersection == span.range) { projected.add(span); continue }
            if (span.resolution == Resolution.PASSTHROUGH) { projected.add(span.copy(range = intersection)); continue }
            val selected = span.selected ?: return null
            val units = selected.pronunciation.units
            if (units.any { it.sourceRange == null }) return null
            val subset = units.filter { it.sourceRange!!.start >= intersection.start && it.sourceRange.end <= intersection.end }
            if (subset.isEmpty() || subset.first().sourceRange!!.start != intersection.start || subset.last().sourceRange!!.end != intersection.end) return null
            projected.add(span.copy(range = intersection, selected = selected.copy(pronunciation = selected.pronunciation.copy(units = subset))))
        }
        return format(result.copy(spans = projected)).latin
    }

    /** Project a complete, ordered partition of the original text without losing formatting boundaries.
     * Null means the fragments cannot reproduce the whole caption exactly (including Japanese
     * contextual Romanization); the consumer must use the whole caption instead of guessing.
     * This checks already proven source alignments and never assigns a reading by string matching.
     */
    fun formatRanges(result: ParseResult, ranges: List<TextRange>): List<FormattedProjection>? {
        var sourceOffset = 0
        for (range in ranges) {
            require(range.start == sourceOffset && range.end <= result.text.length)
            sourceOffset = range.end
        }
        require(sourceOffset == result.text.length)
        val captions = ranges.map { formatRange(result, it) ?: return null }
        val whole = format(result).latin
        val projections = ArrayList<FormattedProjection>(ranges.size)
        var offset = 0
        for (caption in captions) {
            if (caption.isEmpty()) {
                projections.add(FormattedProjection(""))
                continue
            }
            val separator = if (offset > 0 && options.separator.isNotEmpty() &&
                whole.startsWith(options.separator + caption, offset)) options.separator else ""
            if (!whole.startsWith(caption, offset + separator.length)) return null
            offset += separator.length + caption.length
            projections.add(FormattedProjection(caption, separator))
        }
        return projections.takeIf { offset == whole.length }
    }

    private fun asciiLiteral(text: String): String = buildString {
        var at = 0
        while (at < text.length) {
            val cp = text.scalarAt(at)
            append(when {
                cp < 128 -> cp.toChar().toString()
                cp in 0xFF01..0xFF5E -> (cp - 0xFEE0).toChar().toString()
                else -> when (cp) {
                    0x3000 -> " "; 0x3001 -> ","; 0x3002 -> "."; 0x2018, 0x2019 -> "'"
                    0x201C, 0x201D, 0x300C, 0x300D -> "\""; 0x2026 -> "..."; 0x2014 -> "-"
                    0xFF5E, 0x301C -> "~"
                    else -> options.unknownMarker
                }
            }); at = text.nextScalar(at)
        }
    }
}
