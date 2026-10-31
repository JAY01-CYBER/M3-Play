package com.mocharealm.accompanist.lyrics.phonetics.data

import com.mocharealm.accompanist.lyrics.phonetics.*

/** Compact categories derived from the pinned IPADIC CSV context IDs, under NAIST/ICOT.
 * No MeCab parser code or new language-data source is included.
 */
internal class IpadicLexicon(private val delegate: ReadingLexicon) : ReadingLexicon by delegate {
    override fun lexicalRole(entry: Lexeme): LexicalRole {
        if ("ipadic" !in entry.sourceIds && "accompanist-common-polyphones" !in entry.sourceIds) return LexicalRole.OTHER
        return when (entry.leftId) {
            in 11..146 -> LexicalRole.ADJECTIVE
            in 147..368 -> LexicalRole.PARTICLE
            in 369..554 -> LexicalRole.AUXILIARY
            in 561..1280 -> LexicalRole.VERB
            in 1288..1294 -> LexicalRole.PROPER_NOUN
            1295 -> LexicalRole.NUMERAL
            1300 -> LexicalRole.COUNTER
            in 1283..1314 -> LexicalRole.NOUN
            else -> LexicalRole.OTHER
        }
    }
    override fun isAttributive(entry: Lexeme): Boolean = "ipadic" in entry.sourceIds &&
        entry.leftId in 0 until ATTRIBUTIVE.length * 4 &&
        (ATTRIBUTIVE[entry.leftId / 4].digitToInt(16) and (1 shl (entry.leftId % 4))) != 0
    private companion object {
        // Bit per left context ID: 動詞/形容詞/助動詞 with 基本形 or 連体形.
        const val ATTRIBUTIVE = "0000810002800100e00000001000600080204000000000000000000000000000000000000000000000000000000040081000800c0004810102830020018108cff960604842008002080018000180010a0280280208100810081080804040404003000010c000e000012480180408040020800810020400e30000000870000081000e1000800000008fff0000000000000000000000000000f00000000f000002"
    }
}
