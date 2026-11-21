package com.mocharealm.accompanist.lyrics.phonetics

/** Original integer/counter morphology. No corpus or external word list is imported.
 * Conservative domain: 0..9999; signs, decimals, separators and identifiers are not guessed.
 */
internal object JapaneseNumbers {
    data class Match(val number: Int, val digitsEnd: Int, val end: Int, val counter: String)
    fun isDigit(ch: Char): Boolean = ch in '0'..'9' || ch in '０'..'９'
    private fun digit(ch: Char) = if (ch in '0'..'9') ch - '0' else ch - '０'
    fun match(text: String, start: Int, limit: Int): Match? {
        if (start >= limit || !isDigit(text[start])) return null
        if (start > 0 && (isDigit(text[start - 1]) || text[start - 1] in ".．,，/／+-＋－" ||
                text[start - 1] in 'a'..'z' || text[start - 1] in 'A'..'Z' || text[start - 1] == '_')) return null
        var end = start; var number = 0
        while (end < limit && isDigit(text[end])) {
            if (end - start >= 4) return null
            number = number * 10 + digit(text[end++])
        }
        if (end - start > 1 && digit(text[start]) == 0 || end == limit) return null
        val counter = when {
            text.startsWith("時間", end) && end + 2 <= limit -> "時間"
            text.startsWith("日間", end) && end + 2 <= limit -> "日間"
            text.startsWith("年間", end) && end + 2 <= limit -> "年間"
            text[end] in "年月日人分時秒回名歳才本枚個円" -> text[end].toString()
            else -> return null
        }
        if (counter == "月" && number !in 1..12) return null
        return Match(number, end, end + counter.length, counter)
    }
    private object Tables {
        val digits = arrayOf("れい", "いち", "に", "さん", "よん", "ご", "ろく", "なな", "はち", "きゅう")
    }
    fun cardinal(n: Int): String = buildString {
        val digits = Tables.digits
        require(n in 0..9999)
        if (n == 0) { append(digits[0]); return@buildString }
        val thousands = n / 1000
        if (thousands > 0) append(when (thousands) { 1 -> "せん"; 3 -> "さんぜん"; 8 -> "はっせん"; else -> digits[thousands] + "せん" })
        val hundreds = n / 100 % 10
        if (hundreds > 0) append(when (hundreds) { 1 -> "ひゃく"; 3 -> "さんびゃく"; 6 -> "ろっぴゃく"; 8 -> "はっぴゃく"; else -> digits[hundreds] + "ひゃく" })
        val tens = n / 10 % 10
        if (tens > 0) append((if (tens == 1) "" else digits[tens]) + "じゅう")
        if (n % 10 > 0) append(digits[n % 10])
    }
    private fun clipped(value: String): String = when {
        value.endsWith("いち") -> value.dropLast(2) + "いっ"
        value.endsWith("ろく") -> value.dropLast(2) + "ろっ"
        value.endsWith("はち") -> value.dropLast(2) + "はっ"
        value.endsWith("じゅう") -> value.dropLast(3) + "じゅっ"
        value.endsWith("ひゃく") -> value.dropLast(3) + "ひゃっ"
        value.endsWith("びゃく") -> value.dropLast(3) + "びゃっ"
        value.endsWith("ぴゃく") -> value.dropLast(3) + "ぴゃっ"
        else -> value
    }
    fun readings(match: Match, text: String, range: TextRange, start: Int): List<String> {
        val n = match.number; val ordinary = cardinal(n); val last = n % 10
        val reading = when (match.counter) {
            "人" -> when (n) { 1 -> "ひとり"; 2 -> "ふたり"; else -> (when (last) { 4 -> ordinary.dropLast(1); 7 -> ordinary.dropLast(2) + "しち"; else -> ordinary }) + "にん" }
            "月" -> when (n) { 4 -> "しがつ"; 7 -> "しちがつ"; 9 -> "くがつ"; else -> ordinary + "がつ" }
            "日", "日間" -> {
                val day = when (n) {
                    1 -> "いちにち"; 2 -> "ふつか"; 3 -> "みっか"; 4 -> "よっか"; 5 -> "いつか"
                    6 -> "むいか"; 7 -> "なのか"; 8 -> "ようか"; 9 -> "ここのか"; 10 -> "とおか"
                    14 -> "じゅうよっか"; 20 -> "はつか"; 24 -> "にじゅうよっか"
                    else -> (when (last) { 7 -> ordinary.dropLast(2) + "しち"; 9 -> ordinary.dropLast(3) + "く"; else -> ordinary }) + "にち"
                }
                if (match.counter == "日間") day + "かん" else day
            }
            "時", "時間" -> {
                val hour = when (last) { 4 -> ordinary.dropLast(2) + "よ"; 7 -> ordinary.dropLast(2) + "しち"; 9 -> ordinary.dropLast(3) + "く"; else -> ordinary }
                hour + if (match.counter == "時間") "じかん" else "じ"
            }
            "分" -> clipped(ordinary) + if (n > 0 && last in listOf(0, 1, 3, 4, 6, 8)) "ぷん" else "ふん"
            "本" -> clipped(ordinary) + when { n == 0 -> "ほん"; n % 1000 == 0 || last == 3 -> "ぼん"; last in listOf(0, 1, 6, 8) -> "ぽん"; else -> "ほん" }
            "個" -> clipped(ordinary) + "こ"
            "回" -> clipped(ordinary) + "かい"
            "歳", "才" -> if (n == 20) "はたち" else (if (last == 1 || last == 8 || n % 10 == 0 && n % 100 != 0) clipped(ordinary) else ordinary) + "さい"
            "年", "年間" -> (when (last) { 4 -> ordinary.dropLast(2) + "よ"; 7 -> ordinary.dropLast(2) + "しち"; else -> ordinary }) +
                if (match.counter == "年") "ねん" else "ねんかん"
            "秒" -> ordinary + "びょう"
            "名" -> ordinary + "めい"
            "枚" -> ordinary + "まい"
            else -> ordinary + "えん"
        }
        if (n == 1 && match.counter == "日") {
            val before = text.substring(range.start, start)
            if (before.endsWith("月")) return listOf("ついたち")
            return listOf(reading, "ついたち")
        }
        val alternatives = ArrayList<String>()
        alternatives.add(reading)
        if (last == 7 && match.counter in listOf("人", "年", "年間")) alternatives.add(ordinary +
            when (match.counter) { "人" -> "にん"; "年" -> "ねん"; else -> "ねんかん" })
        if (last == 9 && match.counter in listOf("年", "年間")) alternatives.add(ordinary.dropLast(3) + "く" +
            if (match.counter == "年") "ねん" else "ねんかん")
        if (n == 20 && match.counter in listOf("歳", "才")) alternatives.addAll(listOf("にじゅっさい", "にじっさい"))
        if (n == 0) alternatives.add("ぜろ" + reading.removePrefix("れい"))
        if (n > 0 && n % 10 == 0 && n % 100 != 0 && "じゅっ" in reading)
            alternatives.add(reading.replace("じゅっ", "じっ"))
        if (last == 8 && match.counter == "個") alternatives.add(ordinary + "こ")
        return alternatives.distinct()
    }
    /** Dictionary compounds such as 一人前 take priority over a generated prefix. */
    fun kanji(n: Int): String = buildString {
        val chars = "零一二三四五六七八九"
        if (n == 0) append(chars[0])
        for ((value, unit) in listOf(1000 to "千", 100 to "百", 10 to "十", 1 to "")) {
            val d = n / value % 10
            if (d > 0) { if (d != 1 || value == 1) append(chars[d]); append(unit) }
        }
    }
    fun entries(text: String, start: Int, range: TextRange, lexicon: ReadingLexicon, workspace: ParseWorkspace): List<Pair<Int, Lexeme>> {
        val match = match(text, start, range.end) ?: return emptyList()
        val readings = readings(match, text, range, start)
        val source = "accompanist-japanese-numbers"
        val result = ArrayList<Pair<Int, Lexeme>>()
        val numericLeft = lexicon.lookup("一", workspace).firstOrNull { lexicon.lexicalRole(it) == LexicalRole.NUMERAL }?.leftId ?: 0
        val counterEntries = lexicon.lookup(match.counter, workspace)
        val counterRight = counterEntries.firstOrNull { lexicon.lexicalRole(it) == LexicalRole.COUNTER }?.rightId
            ?: counterEntries.firstOrNull()?.rightId ?: 0
        val canonical = kanji(match.number)
        var end = match.end
        var compound = false
        while (end <= range.end && end - match.digitsEnd + canonical.length <= lexicon.maxKeyLength) {
            val suffix = text.substring(match.digitsEnd, end)
            for (entry in lexicon.lookup(canonical + suffix, workspace)) {
                if (lexicon.lexicalRole(entry) == LexicalRole.PROPER_NOUN || end == match.end && entry.reading !in readings) continue
                result.add(end to entry.copy(cost = entry.cost - 12000, sourceIds = (entry.sourceIds + source).distinct().sorted()))
                if (end > match.end) compound = true
            }
            if (end > match.end) for (entry in lexicon.lookup(suffix, workspace)) {
                if (lexicon.lexicalRole(entry) == LexicalRole.PROPER_NOUN) continue
                val stem = when (match.counter) { "年" -> "ねん"; "歳", "才" -> "さい"; "人" -> "にん"; else -> null }
                if (stem != null && entry.reading.startsWith(stem) && !(match.counter == "人" && match.number in 1..2)) {
                    val prefixes = readings.filter { it.endsWith(stem) }.map { it.removeSuffix(stem) }.distinct()
                    for (prefix in prefixes) result.add(end to entry.copy(reading = prefix + entry.reading, leftId = numericLeft,
                        cost = entry.cost - 12000, sourceIds = (entry.sourceIds + source).distinct().sorted()))
                    compound = true
                }
            }
            if (end == range.end) break
            end = text.nextScalar(end)
        }
        if (!compound) for (reading in readings) result.add(match.end to
            Lexeme(reading, -11000, numericLeft, counterRight, false, listOf(source)))
        return result
    }
}
