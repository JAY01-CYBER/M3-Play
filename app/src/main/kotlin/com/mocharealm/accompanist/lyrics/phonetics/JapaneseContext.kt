package com.mocharealm.accompanist.lyrics.phonetics

/** Original, bounded grammatical preferences over existing dictionary readings.
 * This never adds a reading, removes a candidate, or mutates a cached lexeme.
 */
internal object JapaneseContext {
    private object Rules {
        val people = listOf("学生", "生徒", "先生", "医師", "患者", "お客様", "利用者", "参加者",
            "担当者", "初心者", "経験者", "希望者", "応募者", "会員", "受験生", "保護者", "業者", "高齢者", "お年寄り")
        val honorifics = listOf("される", "された", "いただける", "お越しになる", "ご存じの", "ご存知の", "お困りの", "お悩みの")
        val comparisons = listOf("がいい", "がよい", "が良", "が悪", "が早", "が安", "が高", "が楽", "が得",
            "が便利", "が安全", "が好き", "が多", "が少", "が長", "が短", "が重", "が軽", "が大", "が小",
            "が強", "が弱", "が速", "が遅", "が難", "が優", "を選", "にする", "でいい")
        val intervalPrefixes = listOf("の", "長い", "いる", "いた", "する", "した")
        val intervalDemonstratives = listOf("この", "その", "あの")
        val rooms = listOf("床の", "茶の", "束の", "つかの")
        val sayForms = setOf("云う", "云い", "云っ", "云え", "云わ", "云お")
    }

    private fun digit(char: Char): Int? = when (char) {
        in '0'..'9' -> char - '0'
        in '０'..'９' -> char - '０'
        else -> null
    }

    private fun precedingNumber(text: String, start: Int, options: ParseOptions): Long? {
        if (start == 0 || digit(text[start - 1]) == null) return null
        var from = start - 1
        while (from > 0 && digit(text[from - 1]) != null) from--
        // Never borrow a numeral from an explicitly different language, or from
        // outside a Japanese hint whose start is the counter itself.
        if (options.hints.any { it.range.start == start ||
                it.range.start < start && it.range.end > from && ReadingLocale.fromLanguageTag(it.languageTag) != ReadingLocale.JAPANESE }) return null
        var value = 0L
        for (at in from until start) {
            if (value > (Long.MAX_VALUE - 9) / 10) return null
            value = value * 10 + digit(text[at])!!
        }
        return value
    }

    fun apply(text: String, range: TextRange, start: Int, surface: String, entries: List<Lexeme>, options: ParseOptions): List<Lexeme> {
        if (entries.isEmpty()) return entries
        val grammatical = surface == "方" || surface == "間" || surface == "何" || surface == "何と" || surface.length == 2 && surface[0] == '云'
        val numeric = start == range.start && start > 0 && digit(text[start - 1]) != null && surface.first() in "年月歳才時日人名回秒"
        val insideNumeral = start > range.start && text[start - 1] in "一二三四五六七八九十百千万億兆〇零" &&
            (surface == "一人" || surface == "二人" || entries.any { "accompanist-common-polyphones" in it.sourceIds })
        if (!grammatical && !numeric && !insideNumeral) return entries
        val before = if (grammatical) text.substring(maxOf(range.start, start - 24), start) else ""
        val end = start + surface.length
        val after = if (grammatical) text.substring(end, minOf(range.end, end + 24)) else ""
        val number = if (numeric) precedingNumber(text, start, options) else null
        val counter = if (number == null) null else when (surface.first()) {
            '年' -> "ねん"; '月' -> "がつ"; '歳', '才' -> "さい"; '時' -> "じ"
            '日' -> if (number in 11L..31L && number != 14L && number != 20L && number != 24L) "にち" else null
            '人' -> if (number >= 3) "にん" else null
            '名' -> if (number > 0) "めい" else null
            '回' -> if (number > 0) "かい" else null
            '秒' -> if (number > 0) "びょう" else null
            else -> null
        }
        val person = surface == "方" && Rules.comparisons.none(after::startsWith) &&
            (Rules.people.any { before.endsWith(it + "の") } || Rules.honorifics.any(before::endsWith))
        val interval = surface == "間" && Rules.intervalPrefixes.any(before::endsWith) &&
            Rules.rooms.none(before::endsWith) && Rules.intervalDemonstratives.none(before::endsWith) &&
            !after.startsWith("に合") && !after.startsWith("にあ")
        // Do not generalize to 何か/何を/何も; both readings exist before と.
        val question = surface == "何" && (after.startsWith("の") || after.startsWith("だ") || after.startsWith("です"))
        val coordinatedQuestion = surface == "何" && after.startsWith("と何")
        return entries.map { entry ->
            var preference = 0
            var weak = 0
            val rules = ArrayList<String>()
            if (counter != null && entry.reading.startsWith(counter)) preference -= 12000
            if (insideNumeral && ((surface == "一人" && entry.reading == "ひとり") ||
                    (surface == "二人" && entry.reading == "ふたり") || "accompanist-common-polyphones" in entry.sourceIds)) preference += 12000
            if (person && entry.reading == "かた") { preference -= 1800; weak -= 1800; rules.add("ja-human-title") }
            if (interval && entry.reading == "あいだ") { preference -= 5000; rules.add("ja-interval") }
            if (question && entry.reading == "なん") { preference -= 1500; rules.add("ja-question-copula") }
            if (coordinatedQuestion && entry.reading == "なに") { preference -= 1500; weak -= 1500; rules.add("ja-coordinated-question") }
            // The interjection 何と must not swallow the conjunction in 何と何.
            if (surface == "何と" && after.startsWith("何")) { preference += 5000; weak += 5000; rules.add("ja-coordinated-question") }
            if (surface.length == 2 && surface[0] == '云' && surface in Rules.sayForms && entry.reading.startsWith("い")) { preference -= 350; weak -= 350; rules.add("ja-say-form") }
            if (preference == 0) entry else entry.copy(cost = (entry.cost.toLong() + preference)
                .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
                weakPreference = entry.weakPreference + weak, selectionRules = entry.selectionRules + rules)
        }
    }
    /** First-order grammatical edge scoring: forward and backward passes use the same cost.
     * No chosen-history lookback or beam pruning is used.
     */
    fun connectionPreference(text: String, range: TextRange, previous: PathNode, next: PathNode, lexicon: ReadingLexicon): Int {
        val entry = next.lexeme ?: return 0
        if (next.end - next.start != 1 || entry.selectionRules.isNotEmpty()) return 0
        val original = range.start + next.start
        val ch = text[original]
        if (ch != '方' && ch != '間') return 0
        val preceding = previous.lexeme ?: return 0
        if (!lexicon.isAttributive(preceding)) return 0
        val after = text.substring(range.start + next.end, minOf(range.end, range.start + next.end + 24))
        return when {
            ch == '方' && entry.reading == "かた" && lexicon.lexicalRole(preceding) == LexicalRole.VERB &&
                listOf("はこちらへ", "がお越し", "にお越し", "がいらっしゃ", "をお招き", "にお渡し").any(after::startsWith) -> -1800
            ch == '間' && entry.reading == "あいだ" && !after.startsWith("に合") && !after.startsWith("にあ") -> -5000
            else -> 0
        }
    }

}
