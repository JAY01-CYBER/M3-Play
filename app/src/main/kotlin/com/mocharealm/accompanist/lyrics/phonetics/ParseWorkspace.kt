package com.mocharealm.accompanist.lyrics.phonetics

/** One workspace per concurrent parse. Engine and read-only pack storage may be shared.
 * Cache capacity only changes performance, never candidate pruning or scoring.
 */
class ParseWorkspace(val cacheCapacity: Int = 256) {
    init { require(cacheCapacity >= 0) }
    private data class Key(val lexicon: ReadingLexicon, val surface: String)
    private val cache = LinkedHashMap<Key, List<Lexeme>>()
    internal val lattice = ArrayList<MutableList<PathNode>>()
    internal val starts = ArrayList<MutableList<PathNode>>()
    internal var inUse = false
    internal fun cached(lexicon: ReadingLexicon, surface: String): List<Lexeme>? = cache[Key(lexicon, surface)]
    internal fun cache(lexicon: ReadingLexicon, surface: String, value: List<Lexeme>) {
        if (cacheCapacity == 0) return
        val key = Key(lexicon, surface)
        if (key !in cache && cache.size >= cacheCapacity) cache.remove(cache.keys.first())
        cache[key] = value
    }
    internal fun prepare(length: Int) {
        while (lattice.size <= length) { lattice.add(ArrayList()); starts.add(ArrayList()) }
        clearPaths(length)
    }
    internal fun clearPaths(length: Int) {
        for (index in 0..length) { lattice[index].clear(); starts[index].clear() }
    }
    fun clearCache() { check(!inUse); cache.clear() }
    /** Release retained scratch capacity when leaving a large document. */
    fun trim() { check(!inUse); lattice.clear(); starts.clear(); cache.clear() }
}

internal class PathNode(val start: Int, val end: Int, val lexeme: Lexeme?, val fallback: String? = null) {
    var forward = Long.MAX_VALUE / 4
    var backward = Long.MAX_VALUE / 4
    var evidenceForward = Long.MAX_VALUE / 4
    var evidenceBackward = Long.MAX_VALUE / 4
    var previous: PathNode? = null
    var grammarPreference: Int = 0
    val left: Int get() = lexeme?.leftId ?: 0
    val right: Int get() = lexeme?.rightId ?: 0
    val cost: Int get() = lexeme?.cost ?: when (fallback) { "literal" -> 0; "kana" -> 10000; else -> 30000 }
}
