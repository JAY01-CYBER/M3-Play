package com.mocharealm.accompanist.lyrics.phonetics

/** Original Mandarin/Cantonese number grammar, keeping lexical tones (no speech sandhi).
 * Integer quantities up to 16 digits and decimals up to 8 fractional digits.
 * Years read digit by digit; month/day/hour values read as cardinals.
 */
internal object ChineseNumbers {
    data class Match(val digits: String, val fraction: String, val negative: Boolean, val digitsEnd: Int, val end: Int, val counter: String)
    private object Tables {
        val counters = listOf("分钟", "分鐘", "小时", "小時", "公斤", "千米", "年", "月", "日", "号", "號",
            "人", "分", "时", "時", "秒", "次", "名", "岁", "歲", "本", "张", "張", "个", "個", "元", "块", "塊",
            "点", "點", "天", "米", "首", "遍", "辆", "輛", "条", "條", "位", "只", "隻", "册", "冊", "碗", "杯", "朵", "颗", "顆")
        val mandarin = listOf("ling2", "yi1", "er4", "san1", "si4", "wu3", "liu4", "qi1", "ba1", "jiu3")
        val cantonese = listOf("ling4", "jat1", "ji6", "saam1", "sei3", "ng5", "luk6", "cat1", "baat3", "gau2")
    }
    private const val HAN_DIGITS = "零一二三四五六七八九"
    fun isStart(ch: Char) = JapaneseNumbers.isDigit(ch) || ch in "负負零〇一二三四五六七八九十百千万萬亿億兆两兩"
    private fun normalized(ch: Char): Char = if (ch in '０'..'９') ('0'.code + (ch - '０')).toChar() else ch
    fun match(text: String, start: Int, limit: Int): Match? {
        if (start >= limit || !isStart(text[start])) return null
        if (start > 0 && (JapaneseNumbers.isDigit(text[start - 1]) || text[start - 1] in ".．,，/／+-＋－" ||
                text[start - 1] in 'a'..'z' || text[start - 1] in 'A'..'Z' || text[start - 1] == '_')) return null
        val negative = text[start] in "负負"
        var end = start + if (negative) 1 else 0
        val begin = end
        val ascii = end < limit && JapaneseNumbers.isDigit(text[end])
        while (end < limit && if (ascii) JapaneseNumbers.isDigit(text[end]) else text[end] in "零〇一二三四五六七八九十百千万萬亿億兆两兩") end++
        if (end == begin || end - begin > 32) return null
        val raw = text.substring(begin, end)
        var fraction = ""
        if (ascii && end < limit && text[end] in ".．" && end + 1 < limit && JapaneseNumbers.isDigit(text[end + 1])) {
            val from = ++end
            while (end < limit && JapaneseNumbers.isDigit(text[end])) end++
            if (end - from > 8) return null
            fraction = text.substring(from, end).map(::normalized).joinToString("")
        }
        val counter = Tables.counters.firstOrNull { end + it.length <= limit && text.startsWith(it, end) }.orEmpty()
        // A bare numeral is valid only in a selected Chinese locale. Never pronounce
        // a fragment of a Latin identifier or of unsupported grouped/date notation.
        if (counter.isEmpty() && (!ascii || end < limit &&
                (text[end] in 'a'..'z' || text[end] in 'A'..'Z' || text[end] == '_' ||
                    text[end] in ".．,，/／+-＋－" && end + 1 < limit && JapaneseNumbers.isDigit(text[end + 1])))) return null
        val digits = if (ascii) raw.map(::normalized).joinToString("") else {
            if (counter == "年" && raw.length >= 2 && raw.all { it in HAN_DIGITS || it == '〇' })
                raw.map { if (it == '〇') '0' else ('0'.code + HAN_DIGITS.indexOf(it)).toChar() }.joinToString("")
            else parseHan(raw)?.toString() ?: return null
        }
        if (digits.length > 16 || digits.toLongOrNull() == null) return null
        if (digits.length > 1 && digits.first() == '0' && counter != "年") return null
        return Match(digits, fraction, negative, end, end + counter.length, counter)
    }
    private fun parseHan(raw: String): Long? {
        var total = 0L; var section = 0L; var digit = 0L; var last = 10000L
        for (ch in raw) {
            val d = when (ch) { '〇' -> 0; '两', '兩' -> 2; else -> HAN_DIGITS.indexOf(ch) }
            if (d >= 0) { if (digit != 0L) return null; digit = d.toLong(); continue }
            val unit = when (ch) { '十' -> 10L; '百' -> 100L; '千' -> 1000L; '万', '萬' -> 10000L; '亿', '億' -> 100000000L; '兆' -> 1000000000000L; else -> return null }
            if (unit < 10000) { if (unit >= last) return null; section += (if (digit == 0L) 1 else digit) * unit; last = unit }
            else { val group = section + digit; if (group == 0L || group > 9999) return null; total += group * unit; section = 0; last = 10000 }
            digit = 0
        }
        return (total + section + digit).takeIf { it in 0..9999999999999999L }
    }
    private fun cardinal(n: Long, hk: Boolean): List<String> {
        val digits = if (hk) Tables.cantonese else Tables.mandarin
        if (n == 0L) return listOf(digits[0])
        val result = ArrayList<String>(); var rest = n; var gap = false
        for ((base, unit) in listOf(1000000000000L to if (hk) "siu6" else "zhao4", 100000000L to if (hk) "jik1" else "yi4", 10000L to if (hk) "maan6" else "wan4", 1L to "")) {
            val group = (rest / base).toInt(); rest %= base
            if (group == 0) { if (result.isNotEmpty()) gap = true; continue }
            if (result.isNotEmpty() && (gap || group < 1000)) result.add(digits[0])
            var pending = false
            for ((place, name) in listOf(1000 to if (hk) "cin1" else "qian1", 100 to if (hk) "baak3" else "bai3", 10 to if (hk) "sap6" else "shi2", 1 to "")) {
                val d = group / place % 10
                if (d == 0) { if (result.isNotEmpty()) pending = true; continue }
                if (pending && result.isNotEmpty() && result.last() != digits[0]) result.add(digits[0])
                if (!(d == 1 && place == 10 && result.isEmpty())) result.add(digits[d])
                if (name.isNotEmpty()) result.add(name)
                pending = false
            }
            if (unit.isNotEmpty()) result.add(unit)
            gap = false
        }
        return result
    }
    fun entries(text: String, start: Int, range: TextRange, lexicon: ReadingLexicon, workspace: ParseWorkspace): List<Pair<Int, Lexeme>> {
        val m = match(text, start, range.end) ?: return emptyList()
        val hk = lexicon.locale == ReadingLocale.CANTONESE_HK
        val number = m.digits.toLong()
        val asciiNumber = (start until m.digitsEnd).any { JapaneseNumbers.isDigit(text[it]) }
        val writtenYear = !asciiNumber && m.counter == "年" &&
            (start until m.digitsEnd).all { text[it] in "〇零一二三四五六七八九" }
        val year = m.counter == "年" && (asciiNumber && m.digits.length == 4 || writtenYear) && m.fraction.isEmpty() && !m.negative
        // Written Han numerals already carry their own readings (二 and 兩 are distinct).
        // Only digit-sequence years need generated Han handling, notably 〇.
        if (!asciiNumber && !writtenYear) return emptyList()
        val digits = if (hk) Tables.cantonese else Tables.mandarin
        val base = if (year) m.digits.map { digits[it - '0'] } else cardinal(number, hk)
        val prefix = (if (m.negative) listOf(if (hk) "fu6" else "fu4") else emptyList()) + base +
            if (m.fraction.isEmpty()) emptyList() else listOf(if (hk) "dim2" else "dian3") + m.fraction.map { digits[it - '0'] }
        val quantity = number == 2L && m.fraction.isEmpty() && m.counter.isNotEmpty() && m.counter !in listOf("年", "月", "日", "号", "號", "时", "時", "点", "點", "分", "秒")
        val duration = year && !writtenYear && text.substring(maxOf(range.start, start - 12), start).let { before ->
            listOf("等你", "等了", "等待", "持续", "持續", "长达", "長達", "历经", "歷經", "整整", "足足", "一共").any(before::endsWith)
        }
        val count = if (year && !writtenYear) cardinal(number, hk) else prefix
        val variants = when {
            quantity -> listOf((if (m.negative) prefix.dropLast(1) else emptyList()) + if (hk) "loeng5" else "liang3", prefix)
            year && !writtenYear -> if (duration) listOf(count, prefix) else listOf(prefix, count)
            else -> listOf(prefix)
        }
        var entries = if (m.counter.isEmpty()) listOf(Lexeme("", 0, 0, 0, false, emptyList())) else lexicon.lookup(m.counter, workspace)
        if (entries.isEmpty()) {
            entries = listOf(Lexeme("", 0, 0, 0, false, emptyList()))
            for (ch in m.counter) entries = entries.flatMap { prefixEntry -> lexicon.lookup(ch.toString(), workspace).map { unit ->
                unit.copy(reading = listOf(prefixEntry.reading, unit.reading).filter(String::isNotEmpty).joinToString(" "),
                    cost = prefixEntry.cost + unit.cost, aligned = false, sourceIds = (prefixEntry.sourceIds + unit.sourceIds).distinct().sorted())
            } }
        }
        // Counter meaning provides stronger evidence than an isolated character's cost.
        // In particular HK 人 jan2 and 名 meng2 are valid elsewhere, not these counters.
        val preferredReading = if (hk) when (m.counter) {
            "人" -> "jan4"; "名" -> "ming4"; "分" -> "fan1"; "只", "隻" -> "zek3"
            "年" -> "nin4"; "月" -> "jyut6"; "日" -> "jat6"; "个", "個" -> "go3"
            else -> null
        } else when (m.counter) {
            "年" -> "nian2"; "月" -> "yue4"; "日" -> "ri4"; "分" -> "fen1"
            "个", "個" -> "ge4"; "只", "隻" -> "zhi1"; "号", "號" -> "hao4"
            else -> null
        }
        val preferredCost = entries.minOfOrNull { it.cost }
        return entries.flatMap { entry -> variants.map { value ->
            val preferred = if (preferredReading != null && entries.any { it.reading == preferredReading }) entry.reading == preferredReading else entry.cost == preferredCost
            m.end to entry.copy(reading = (value + entry.reading).filter(String::isNotEmpty).joinToString(" "), cost = entry.cost - if (preferred) 12000 else 0,
                aligned = false, sourceIds = (entry.sourceIds + "accompanist-chinese-numbers").distinct().sorted(),
                selectionRules = listOf(if (duration) "zh-numeric-duration" else if (year) "zh-numeric-year" else if (m.counter.isEmpty()) "zh-numeric-cardinal" else "zh-numeric-counter"))
        } }
    }
}
