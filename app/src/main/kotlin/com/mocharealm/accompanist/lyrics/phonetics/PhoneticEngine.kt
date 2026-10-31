package com.mocharealm.accompanist.lyrics.phonetics

class PhoneticEngine(
    resolvers: List<LanguageResolver> = emptyList(),
    profiles: List<PhoneticProfile> = emptyList(),
) {
    private val profiles = profiles.toList() + listOf(CjkPhoneticProfile(resolvers))

    fun parse(text: String, options: ParseOptions = ParseOptions(), workspace: ParseWorkspace = ParseWorkspace()): ParseResult {
        val sortedHints = options.hints.sortedBy { it.range.start }
        sortedHints.forEachIndexed { index, hint ->
            require(hint.range.end <= text.length && hint.range.start < hint.range.end && text.isScalarBoundary(hint.range.start) && text.isScalarBoundary(hint.range.end))
            require(index == 0 || sortedHints[index - 1].range.end <= hint.range.start) { "Language hints must not overlap" }
        }
        // Adjacent timed fragments of the same language must retain their lexical context.
        val hints = ArrayList<LanguageHint>()
        for (hint in sortedHints) {
            val previous = hints.lastOrNull()
            if (previous?.languageTag == hint.languageTag && previous.range.end == hint.range.start)
                hints[hints.lastIndex] = previous.copy(range = TextRange(previous.range.start, hint.range.end))
            else hints.add(hint)
        }
        val normalized = options.copy(hints = hints)
        val overrides = options.overrides.sortedBy { it.range.start }
        overrides.forEachIndexed { index, value ->
            val range = value.range
            require(range.start < range.end && range.end <= text.length && text.isScalarBoundary(range.start) && text.isScalarBoundary(range.end))
            require(index == 0 || overrides[index - 1].range.end <= range.start) { "Reading overrides must not overlap" }
            val aligned = value.pronunciation.units.mapNotNull { it.sourceRange }
            require(aligned.isEmpty() || aligned.size == value.pronunciation.units.size) { "Override alignment must be complete or absent" }
            if (aligned.isNotEmpty()) {
                var cursor = range.start
                for (unit in aligned) {
                    require(unit.start == cursor && unit.end > cursor && unit.end <= range.end && text.isScalarBoundary(unit.end))
                    cursor = unit.end
                }
                require(cursor == range.end)
            }
        }
        check(!workspace.inUse) { "Use an independent workspace for each concurrent parse" }
        workspace.inUse = true
        try {
            val spans = ArrayList<PhoneticSpan>(); val ambiguities = ArrayList<ReadingAmbiguity>()
            val alternatives = ArrayList<LanguageAlternative>()
            var at = 0
            fun profileAt(index: Int) = profiles.firstOrNull { it.matches(text, index, normalized) }
            while (at < text.length) {
                val forced = overrides.firstOrNull { it.range.start == at }
                if (forced != null) {
                    val candidate = ReadingCandidate(forced.pronunciation, listOf(forced.sourceId), selectionRules = listOf("caller-override"))
                    spans.add(PhoneticSpan(forced.range, forced.pronunciation.locale, Resolution.RESOLVED, candidate, listOf(candidate), "Caller pronunciation"))
                    at = forced.range.end
                    continue
                }
                val hint = hints.firstOrNull { at >= it.range.start && at < it.range.end }
                val languageBoundary = hint?.range?.end ?: hints.firstOrNull { it.range.start > at }?.range?.start ?: text.length
                val boundary = minOf(languageBoundary, overrides.firstOrNull { it.range.start > at }?.range?.start ?: text.length)
                val profile = profileAt(at)
                var end = text.nextScalar(at)
                while (end < boundary && profileAt(end) === profile) end = text.nextScalar(end)
                val range = TextRange(at, end)
                if (profile == null) spans.add(PhoneticSpan(range, null, Resolution.PASSTHROUGH))
                else {
                    val result = profile.resolve(text, range, normalized, workspace)
                    var cursor = range.start
                    for (span in result.spans) {
                        require(span.range.start == cursor && span.range.end > cursor && span.range.end <= range.end && text.isScalarBoundary(span.range.end)) {
                            "Profile spans must cover their original run in order"
                        }
                        cursor = span.range.end
                    }
                    require(cursor == range.end) { "Profile spans must cover their original run" }
                    spans.addAll(result.spans); ambiguities.addAll(result.ambiguities); alternatives.addAll(result.languageAlternatives)
                }
                at = end
            }
            return ParseResult(text, spans.toList(), ambiguities.toList(), alternatives.toList())
        } finally { workspace.inUse = false }
    }
}
