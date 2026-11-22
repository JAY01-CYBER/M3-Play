package com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine


fun SyncedLine.toKaraokeLine(): KaraokeLine {
    return KaraokeLine.MainKaraokeLine(
        syllables = listOf(
            KaraokeSyllable(
                this.content,
                this.start,
                this.end,
                languageTag = this.languageTag,
            )
        ),
        translation = this.translation,
        alignment = KaraokeAlignment.Unspecified,
        start = this.start,
        end = this.end,
        phonetic = this.phonetic,
        languageTag = this.languageTag,
    )
}
