package com.mocharealm.accompanist.lyrics.phonetics.data

import com.mocharealm.accompanist.lyrics.phonetics.*

object JapaneseData {
    fun japanese(loader: PackLoader): LanguageResolver = LazyPackResolver(ReadingLocale.JAPANESE, loader, ::IpadicLexicon)
}
