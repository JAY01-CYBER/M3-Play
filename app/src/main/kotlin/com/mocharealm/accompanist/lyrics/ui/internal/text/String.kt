package com.mocharealm.accompanist.lyrics.ui.internal.text

internal fun Char.isCjk(): Boolean =
    when (code) {
        in 0x3400..0x4DBF,
        in 0x4E00..0x9FFF,
        in 0xF900..0xFAFF,
        in 0x20000..0x2FA1F -> true
        else -> false
    }

internal fun Char.isJapanese(): Boolean {
    return this.code in 0x3040..0x309F || this.code in 0x30A0..0x30FF || this.code in 0xFF66..0xFF9F
}

internal fun Char.isKorean(): Boolean {
    return this.code in 0xAC00..0xD7AF || this.code in 0x1100..0x11FF
}

internal fun Char.isArabic(): Boolean =
    code in 0x0600..0x06FF || code in 0x0750..0x077F ||
        code in 0x08A0..0x08FF || code in 0xFB50..0xFDFF ||
        code in 0xFE70..0xFEFF

internal fun Char.isDevanagari(): Boolean = code in 0x0900..0x097F

internal fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

internal fun String.containsJapanese(): Boolean = any { it.isJapanese() }

internal fun String.containsKorean(): Boolean = any { it.isKorean() }

internal fun String.isRtl(): Boolean {
    for (value in this) {
        when (Character.getDirectionality(value)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
        }
    }
    return false
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
