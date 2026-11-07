package com.mocharealm.accompanist.lyrics.phonetics

private const val HALF_KANA = "ヲァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン"
private val voiced = mapOf('か' to 'が', 'き' to 'ぎ', 'く' to 'ぐ', 'け' to 'げ', 'こ' to 'ご',
    'さ' to 'ざ', 'し' to 'じ', 'す' to 'ず', 'せ' to 'ぜ', 'そ' to 'ぞ',
    'た' to 'だ', 'ち' to 'ぢ', 'つ' to 'づ', 'て' to 'で', 'と' to 'ど',
    'は' to 'ば', 'ひ' to 'び', 'ふ' to 'ぶ', 'へ' to 'べ', 'ほ' to 'ぼ', 'う' to 'ゔ')
private val semiVoiced = mapOf('は' to 'ぱ', 'ひ' to 'ぴ', 'ふ' to 'ぷ', 'へ' to 'ぺ', 'ほ' to 'ぽ')

/** Normalization affects ONLY readings. Original UTF-16 positions remain untouched. */
internal fun normalizeKana(input: String): String? {
    val out = StringBuilder()
    for (raw in input) {
        val wide = if (raw.code in 0xFF66..0xFF9D) HALF_KANA[raw.code - 0xFF66] else raw
        val ch = if (wide in 'ァ'..'ヶ' || wide in "ヽヾ") (wide.code - 0x60).toChar() else wide
        when (ch) {
            '\uFF9E', '\u3099', '\u309B', '\uFF9F', '\u309A', '\u309C' -> {
                if (out.isEmpty()) return null
                val table = if (ch in "\uFF9F\u309A\u309C") semiVoiced else voiced
                val replacement = table[out.last()] ?: return null
                out[out.length - 1] = replacement
            }
            'ゝ', 'ゞ' -> {
                if (out.isEmpty()) return null
                val previous = out.last()
                out.append(if (ch == 'ゞ') voiced[previous] ?: previous else previous)
            }
            else -> if (ch in 'ぁ'..'ゖ' || ch == 'ー') out.append(ch) else return null
        }
    }
    return out.toString()
}

enum class JapaneseRomanization { HEPBURN, KUNREI }

internal object KanaRomanizer {
    private val mono = buildMap<Char, String> {
        fun row(kana: String, latin: String) { kana.toList().zip(latin.split(' ')).forEach { (k, r) -> put(k, r) } }
        row("あいうえお", "a i u e o"); row("かきくけこ", "ka ki ku ke ko")
        row("がぎぐげご", "ga gi gu ge go"); row("さしすせそ", "sa shi su se so")
        row("ざじずぜぞ", "za ji zu ze zo"); row("たちつてと", "ta chi tsu te to")
        row("だぢづでど", "da ji zu de do"); row("なにぬねの", "na ni nu ne no")
        row("はひふへほ", "ha hi fu he ho"); row("ばびぶべぼ", "ba bi bu be bo")
        row("ぱぴぷぺぽ", "pa pi pu pe po"); row("まみむめも", "ma mi mu me mo")
        row("やゆよ", "ya yu yo"); row("らりるれろ", "ra ri ru re ro")
        row("わゐゑをん", "wa wi we o n"); row("ぁぃぅぇぉゃゅょゎゕゖゔ", "a i u e o ya yu yo wa ka ke vu")
    }
    private val pairs = buildMap<String, String> {
        for ((kana, stem) in listOf('き' to "ky", 'ぎ' to "gy", 'し' to "sh", 'じ' to "j", 'ち' to "ch", 'ぢ' to "j",
            'に' to "ny", 'ひ' to "hy", 'び' to "by", 'ぴ' to "py", 'み' to "my", 'り' to "ry")) {
            for ((small, vowel) in listOf('ゃ' to "a", 'ゅ' to "u", 'ょ' to "o")) put("$kana$small", stem + vowel)
        }
        putAll(mapOf("しぇ" to "she", "じぇ" to "je", "ちぇ" to "che", "てぃ" to "ti", "でぃ" to "di",
            "とぅ" to "tu", "どぅ" to "du", "てゅ" to "tyu", "でゅ" to "dyu", "つぁ" to "tsa", "つぃ" to "tsi",
            "つぇ" to "tse", "つぉ" to "tso", "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo", "ふゅ" to "fyu",
            "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo", "ゔゅ" to "vyu",
            "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo", "いぇ" to "ye", "くぁ" to "kwa", "ぐぁ" to "gwa"))
    }
    private fun mora(kana: String, at: Int, style: JapaneseRomanization): Pair<String, Int>? {
        val pair = if (at + 1 < kana.length) pairs[kana.substring(at, at + 2)] else null
        var value = pair ?: mono[kana[at]] ?: return null
        if (style == JapaneseRomanization.KUNREI) value = when {
            value.startsWith("sh") -> "sy" + value.drop(2)
            value.startsWith("ch") -> "ty" + value.drop(2)
            value.startsWith("j") -> "zy" + value.drop(1)
            value == "tsu" -> "tu"
            value == "fu" -> "hu"
            else -> value
        }.let { if (it == "syi") "si" else if (it == "tyi") "ti" else if (it == "zyi") "zi" else it }
        return value to if (pair == null) 1 else 2
    }
    internal fun sokuonPrefix(reading: String, style: JapaneseRomanization): String? {
        val kana = normalizeKana(reading)?.takeIf { it.isNotEmpty() } ?: return null
        return sokuonPrefix(kana, 0, style)
    }
    private fun sokuonPrefix(kana: String, at: Int, style: JapaneseRomanization): String? {
        val next = mora(kana, at, style)?.first ?: return null
        return when {
            next.first() in "aeioun" -> null
            next.startsWith("ch") -> "t"
            else -> next.first().toString()
        }
    }
    fun format(reading: String, style: JapaneseRomanization, unknown: String, followingReading: String = "", precedingVowel: Char? = null): String {
        val kana = normalizeKana(reading) ?: return unknown
        val out = StringBuilder(); var at = 0
        while (at < kana.length) {
            when (kana[at]) {
                'っ' -> {
                    val prefix = if (at + 1 < kana.length) sokuonPrefix(kana, at + 1, style)
                        else sokuonPrefix(followingReading, style)
                    out.append(prefix ?: unknown); at++
                }
                'ー' -> {
                    val previous = out.lastOrNull() ?: precedingVowel
                    out.append(previous?.takeIf { it in "aeiou" }?.toString() ?: unknown); at++
                }
                'ん' -> {
                    val next = if (at + 1 < kana.length) mora(kana, at + 1, style)?.first else
                        normalizeKana(followingReading)?.takeIf { it.isNotEmpty() }?.let { mora(it, 0, style)?.first }
                    out.append(if (next?.firstOrNull()?.let { it in "aeiouy" } == true) "n'" else "n"); at++
                }
                else -> {
                    val item = mora(kana, at, style)
                    out.append(item?.first ?: unknown); at += item?.second ?: 1
                }
            }
        }
        return out.toString()
    }
}
