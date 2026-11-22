package com.mocharealm.accompanist.lyrics.phonetics.data

import com.mocharealm.accompanist.lyrics.phonetics.*

object MandarinData {
    fun mainland(loader: PackLoader): LanguageResolver = LazyPackResolver(ReadingLocale.MANDARIN_CN, loader)
    fun taiwan(loader: PackLoader): LanguageResolver = LazyPackResolver(ReadingLocale.MANDARIN_TW, loader)
}
