package com.mocharealm.accompanist.lyrics.ui.internal.text

internal fun Char.isJapanese(): Boolean {
    return this.code in 0x3040..0x309F || this.code in 0x30A0..0x30FF || this.code in 0xFF66..0xFF9F
}

internal fun Char.isKorean(): Boolean {
    return this.code in 0xAC00..0xD7AF || this.code in 0x1100..0x11FF
}

/** Unicode bidi class: 0 = L, 1 = R, 13 = AL; other values do not establish direction. */

internal fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

internal fun String.containsJapanese(): Boolean = any { it.isJapanese() }

internal fun String.containsKorean(): Boolean = any { it.isKorean() }

internal fun String.isRtl(fallback: Boolean = false): Boolean {
    var index = 0
    while (index < length) {
        val value = this[index++]
        val codePoint =
            if (value.isHighSurrogate() && index < length && this[index].isLowSurrogate())
                0x10000 + ((value.code - 0xD800) shl 10) + this[index++].code - 0xDC00
            else value.code
        when (platformCodePointDirectionality(codePoint)) {
            0 -> return false
            1, 13 -> return true
        }
    }
    return fallback
}

internal fun Char.isProfilePunctuation(): Boolean =
    when (category) {
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION -> true
        else -> this == '～'
    }

internal fun String.isPunctuation(): Boolean =
    isNotEmpty() && all { it.isWhitespace() || it.isProfilePunctuation() }
