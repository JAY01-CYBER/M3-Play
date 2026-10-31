package com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable

fun Collection<KaraokeSyllable>.contentToString(): String = this.joinToString(separator = "") {
    it.content
}

fun Collection<KaraokeSyllable>.phoneticToString(): String = this.joinToString(separator = " ") {
    it.phonetic ?: ""
}

private fun Char.isOpenParen() = this == '(' || this == '（'
private fun Char.isCloseParen() = this == ')' || this == '）'

/**
 * True iff [text] begins with a parenthesis whose *matching* close is exactly its
 * last character — i.e. one balanced pair wraps the whole string. Handles nested
 * pairs (`(a (b) c)`) and rejects strings where the first pair closes early
 * (`(a)(b)`, `(a) b`), so inline parentheses used inside the lyric are preserved.
 * Half-width `()` and full-width `（）` are both recognised.
 */
private fun isWrappedInMatchedParens(text: String): Boolean {
    if (text.length < 2 || !text.first().isOpenParen() || !text.last().isCloseParen()) return false
    var depth = 0
    for (i in text.indices) {
        val c = text[i]
        when {
            c.isOpenParen() -> depth++
            c.isCloseParen() -> {
                depth--
                if (depth == 0) return i == text.lastIndex
                if (depth < 0) return false
            }
        }
    }
    return false
}

/** Rebuild the syllables with the characters at the given global (line-wide) offsets removed. */
private fun List<KaraokeSyllable>.removeCharsAtGlobalOffsets(offsets: Set<Int>): List<KaraokeSyllable> {
    var base = 0
    return map { syllable ->
        val content = syllable.content
        val sb = StringBuilder(content.length)
        for (i in content.indices) {
            if (base + i !in offsets) sb.append(content[i])
        }
        base += content.length
        syllable.copy(content = sb.toString())
    }
}

/**
 * Some sources mark harmony / background vocals by wrapping the whole line in
 * parentheses, e.g. Lyricify `[7](Yeah)` or Apple TTML `x-bg` `(And I want you,
 * baby)`. Those parentheses are a *marker*, not lyric content, so strip them.
 *
 * The pair is detected properly (see [isWrappedInMatchedParens]) rather than by
 * blindly dropping the first/last char: a line like `(ooh) yeah (ooh)` is *not*
 * wrapped by one outer pair, so it is left untouched. Only the single outermost
 * pair enclosing the whole (trimmed) line is removed; nested wrappers (`((x))`)
 * are peeled one level at a time. Syllable timings are preserved — only the
 * `content` strings change — so callers need not recompute start/end.
 */
fun List<KaraokeSyllable>.stripEnclosingParentheses(): List<KaraokeSyllable> {
    var current = this
    while (current.isNotEmpty()) {
        val joined = current.joinToString(separator = "") { it.content }
        val open = joined.indexOfFirst { !it.isWhitespace() }
        val close = joined.indexOfLast { !it.isWhitespace() }
        if (open < 0 || close <= open) break
        if (!isWrappedInMatchedParens(joined.substring(open, close + 1))) break
        current = current.removeCharsAtGlobalOffsets(setOf(open, close))
    }
    return current
}

fun String.stripEnclosingParentheses(): String {
    var current = this
    while (current.isNotEmpty()) {
        val open = current.indexOfFirst { !it.isWhitespace() }
        val close = current.indexOfLast { !it.isWhitespace() }
        if (open !in 0..<close) break
        if (!isWrappedInMatchedParens(current.substring(open, close + 1))) break
        current = StringBuilder(current).apply {
            deleteAt(close)
            deleteAt(open)
        }.toString()
    }
    return current
}