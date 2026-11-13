package com.mocharealm.accompanist.lyrics.phonetics

/** Exact sentence lattice with dictionary costs. Japanese additionally uses POS connection costs.
 * No beam pruning: cache size, storage backend and workspace reuse cannot alter the result.
 */
class LexiconResolver(private val lexicon: ReadingLexicon) : LanguageResolver {
    override val locale: ReadingLocale get() = lexicon.locale

    override fun resolve(text: String, range: TextRange, options: ParseOptions, workspace: ParseWorkspace): LanguageResult {
        if (locale == ReadingLocale.JAPANESE && range.textIn(text).none { isHan(it.code) || it == '々' }) {
            val kana = normalizeKana(range.textIn(text))
            if (kana != null) {
                val reading = ReadingCandidate(Pronunciation(locale, listOf(ReadingUnit(kana, sourceRange = range))), listOf("accompanist-kana-rules"))
                return LanguageResult(listOf(PhoneticSpan(range, locale, Resolution.RESOLVED, reading, listOf(reading))))
            }
        }
        val length = range.end - range.start
        workspace.prepare(length)
        val ends = workspace.lattice
        val starts = workspace.starts
        fun grammarCost(previous: PathNode, next: PathNode): Int = if (locale == ReadingLocale.JAPANESE)
            JapaneseContext.connectionPreference(text, range, previous, next, lexicon) else 0
        val bos = PathNode(0, 0, null).also { it.forward = 0; it.evidenceForward = 0 }
        var hasWeakPreferences = false
        ends[0].add(bos)
        try {
            var at = 0
            while (at < length) {
                if (ends[at].isEmpty()) { at++; continue }
                val original = range.start + at
                if (locale != ReadingLocale.JAPANESE && ChineseNumbers.isStart(text[original])) {
                    ChineseNumbers.entries(text, original, range, lexicon, workspace).forEach { (end, entry) ->
                        starts[at].add(PathNode(at, end - range.start, entry))
                    }
                }
                if (locale == ReadingLocale.JAPANESE && JapaneseNumbers.isDigit(text[original])) {
                    JapaneseNumbers.entries(text, original, range, lexicon, workspace).forEach { (end, entry) ->
                        starts[at].add(PathNode(at, end - range.start, entry))
                    }
                }
                if (JapaneseNumbers.isDigit(text[original]) || text[original] in ".．") {
                    var literalEnd = text.nextScalar(original)
                    while (literalEnd < range.end && (JapaneseNumbers.isDigit(text[literalEnd]) || text[literalEnd] in ".．")) literalEnd++
                    starts[at].add(PathNode(at, literalEnd - range.start, null, "literal"))
                }
                var end = text.nextScalar(original)
                while (end <= range.end && end - original <= lexicon.maxKeyLength) {
                    val surface = text.substring(original, end)
                    val entries = lexicon.lookup(surface, workspace)
                    val contextual = if (locale == ReadingLocale.JAPANESE)
                        JapaneseContext.apply(text, range, original, surface, entries, options) else entries
                    contextual.forEach { entry -> starts[at].add(PathNode(at, end - range.start, entry)) }
                    if (end == range.end) break
                    end = text.nextScalar(end)
                }
                val next = text.nextScalar(original)
                // Kana fallback consumes a complete kana run so sokuon/digraph/long vowel formatting
                // is independent of dictionary coverage; it is always more expensive than known words.
                if (locale == ReadingLocale.JAPANESE && (isKana(text.scalarAt(original)) || text[original] == 'ー')) {
                    var kanaEnd = next
                    while (kanaEnd < range.end && (isKana(text.scalarAt(kanaEnd)) || text[kanaEnd] == 'ー')) kanaEnd = text.nextScalar(kanaEnd)
                    val normalized = normalizeKana(text.substring(original, kanaEnd))
                    if (normalized != null) starts[at].add(PathNode(at, kanaEnd - range.start,
                        Lexeme(normalized, 10000, 0, 0, false, listOf("accompanist-kana-rules")), "kana"))
                }
                // Unknown edges exist even beside known entries, so a longer contextual word may win.
                starts[at].add(PathNode(at, next - range.start, null, "unknown"))
                for (node in starts[at]) {
                    if ((node.lexeme?.weakPreference ?: 0) != 0) hasWeakPreferences = true
                    for (previous in ends[at]) {
                        val grammar = grammarCost(previous, node)
                        if (grammar == -1800) hasWeakPreferences = true
                        val connection = lexicon.connectionCost(previous.right, node.left)
                        val edge = node.cost.toLong() + connection + grammar
                        val cost = previous.forward + edge
                        if (hasWeakPreferences) {
                            val evidenceEdge = edge - (node.lexeme?.weakPreference ?: 0) - if (grammar == -1800) grammar else 0
                            node.evidenceForward = minOf(node.evidenceForward, previous.evidenceForward + evidenceEdge)
                        }
                        if (cost < node.forward) { node.forward = cost; node.previous = previous; node.grammarPreference = grammar }
                        if (!hasWeakPreferences) node.evidenceForward = node.forward
                    }
                    ends[node.end].add(node)
                }
                at = next - range.start
            }
            val last = ends[length].minBy { it.forward + lexicon.connectionCost(it.right, 0) }
            val best = last.forward + lexicon.connectionCost(last.right, 0)
            val evidenceBest = if (hasWeakPreferences) ends[length].minOf { it.evidenceForward + lexicon.connectionCost(it.right, 0) } else best
            ends[length].forEach { it.backward = lexicon.connectionCost(it.right, 0).toLong(); it.evidenceBackward = it.backward }
            for (position in length - 1 downTo 0) {
                for (previous in ends[position]) {
                    for (next in starts[position]) {
                        val grammar = grammarCost(previous, next)
                        val edge = next.cost.toLong() + lexicon.connectionCost(previous.right, next.left) + grammar
                        previous.backward = minOf(previous.backward, next.backward + edge)
                        if (hasWeakPreferences) {
                            val evidenceEdge = edge - (next.lexeme?.weakPreference ?: 0) - if (grammar == -1800) grammar else 0
                            previous.evidenceBackward = minOf(previous.evidenceBackward, next.evidenceBackward + evidenceEdge)
                        }
                    }
                }
            }
            val path = ArrayList<PathNode>()
            var node: PathNode? = last
            while (node != null && node !== bos) { path.add(node); node = node.previous }
            path.reverse()
            fun evidenceDelta(node: PathNode): Long = if (hasWeakPreferences)
                node.evidenceForward + node.evidenceBackward - evidenceBest else node.forward + node.backward - best
            fun candidate(node: PathNode): ReadingCandidate? {
                val entry = node.lexeme ?: return null
                val start = range.start + node.start; val end = range.start + node.end
                val units = if (locale == ReadingLocale.JAPANESE) listOf(ReadingUnit(entry.reading, sourceRange = TextRange(start, end))) else {
                    var source = start
                    entry.reading.split(' ').map { syllable ->
                        val unitRange = if (entry.aligned) TextRange(source, text.nextScalar(source)).also { source = it.end } else null
                        ReadingUnit(syllable.dropLast(1), syllable.last().digitToInt(), unitRange)
                    }
                }
                val delta = node.forward + node.backward - best
                val grammarRule = when (node.grammarPreference) { -1800 -> listOf("ja-attributive-person"); -5000 -> listOf("ja-attributive-interval"); else -> emptyList() }
                return ReadingCandidate(Pronunciation(locale, units), entry.sourceIds, delta,
                    evidenceDelta(node).coerceAtLeast(0), entry.selectionRules + grammarRule)
            }
            fun span(node: PathNode): PhoneticSpan {
                val selected = candidate(node)
                if (selected == null) return PhoneticSpan(TextRange(range.start + node.start, range.start + node.end), locale,
                    if (node.fallback == "literal") Resolution.PASSTHROUGH else Resolution.UNKNOWN, reason = if (node.fallback == "literal") null else "No supported reading")
                val candidates = starts[node.start].filter { it.end == node.end && it.lexeme != null }
                    .mapNotNull(::candidate).groupBy { it.pronunciation }.map { (_, duplicates) ->
                        duplicates.minBy { it.costDelta }.copy(sourceIds = duplicates.flatMap { it.sourceIds }.distinct().sorted(),
                            uncertaintyCostDelta = duplicates.minOf { it.uncertaintyCostDelta })
                    }.sortedWith(compareBy({ it.costDelta }, { it.pronunciation.units.joinToString { u -> u.value + u.tone } }))
                val unresolved = candidates.count { it.uncertaintyCostDelta <= options.ambiguityCostWindow || it.pronunciation == selected.pronunciation }
                return PhoneticSpan(TextRange(range.start + node.start, range.start + node.end), locale,
                    if (unresolved > 1) Resolution.AMBIGUOUS else Resolution.RESOLVED, selected, candidates)
            }
            val spans = path.map(::span)
            val chosen = if (hasWeakPreferences) path.toSet() else emptySet()
            val ambiguities = starts.flatMap { list -> list.filter { it.lexeme != null &&
                (evidenceDelta(it) <= options.ambiguityCostWindow || hasWeakPreferences && it in chosen) } }
                .groupBy { it.start }.mapNotNull { (start, nodes) ->
                    val distinct = nodes.map(::span).distinctBy { it.range to it.selected?.pronunciation }
                    if (distinct.size > 1) ReadingAmbiguity(TextRange(range.start + start, range.start + nodes.maxOf { it.end }), distinct) else null
                }
            val finalSpans = spans.map { selected ->
                if (selected.resolution == Resolution.RESOLVED && ambiguities.any { ambiguity ->
                        ambiguity.range.start < selected.range.end && ambiguity.range.end > selected.range.start
                    }) selected.copy(resolution = Resolution.AMBIGUOUS) else selected
            }
            return LanguageResult(finalSpans, ambiguities)
        } finally {
            // Do not retain sentence text or candidate paths across calls.
            workspace.clearPaths(length)
        }
    }
}
