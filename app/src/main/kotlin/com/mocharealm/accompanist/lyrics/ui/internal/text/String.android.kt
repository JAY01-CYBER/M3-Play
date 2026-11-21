package com.mocharealm.accompanist.lyrics.ui.internal.text

import android.os.Build

internal fun platformCodePointDirectionality(codePoint: Int): Int =
    when (Character.getDirectionality(codePoint)) {
        Character.DIRECTIONALITY_LEFT_TO_RIGHT -> 0
        Character.DIRECTIONALITY_RIGHT_TO_LEFT -> 1
        Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> 13
        else -> -1
    }

private val cjkBlocks: Set<Character.UnicodeBlock> by lazy {
    mutableSetOf(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.HANGUL_SYLLABLES,
            Character.UnicodeBlock.HANGUL_JAMO,
            Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        )
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E)
                add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F)
                add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H)
            }
        }
}

private val arabicBlocks: Set<Character.UnicodeBlock> by lazy {
    mutableSetOf(
            Character.UnicodeBlock.ARABIC,
            Character.UnicodeBlock.ARABIC_SUPPLEMENT,
            Character.UnicodeBlock.ARABIC_EXTENDED_A,
            Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
            Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B,
        )
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                add(Character.UnicodeBlock.ARABIC_EXTENDED_B)
            }
        }
}

private val devanagariBlocks: Set<Character.UnicodeBlock> by lazy {
    setOf(Character.UnicodeBlock.DEVANAGARI, Character.UnicodeBlock.DEVANAGARI_EXTENDED)
}

internal fun Char.isCjk(): Boolean {
    return try {
        Character.UnicodeBlock.of(this) in cjkBlocks
    } catch (e: Exception) {
        false
    }
}

internal fun Char.isArabic(): Boolean {
    return try {
        Character.UnicodeBlock.of(this) in arabicBlocks
    } catch (e: Exception) {
        false
    }
}

internal fun Char.isDevanagari(): Boolean {
    return try {
        Character.UnicodeBlock.of(this) in devanagariBlocks
    } catch (e: Exception) {
        false
    }
}
