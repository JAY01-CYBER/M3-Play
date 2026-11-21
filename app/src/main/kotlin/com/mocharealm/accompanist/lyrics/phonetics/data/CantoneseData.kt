package com.mocharealm.accompanist.lyrics.phonetics.data

import com.mocharealm.accompanist.lyrics.phonetics.*

object CantoneseData {
    fun hongKong(loader: PackLoader): LanguageResolver = LazyPackResolver(ReadingLocale.CANTONESE_HK, loader)
}
