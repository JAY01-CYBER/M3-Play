package com.mocharealm.accompanist.lyrics.ui.internal.text

internal expect fun Char.isCjk(): Boolean

internal fun Char.isJapanese(): Boolean {
    return this.code in 0x3040..0x309F || this.code in 0x30A0..0x30FF || this.code in 0xFF66..0xFF9F
}

internal fun Char.isKorean(): Boolean {
    return this.code in 0xAC00..0xD7AF || this.code in 0x1100..0x11FF
}

internal expect fun Char.isArabic(): Boolean

internal expect fun Char.isDevanagari(): Boolean

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
