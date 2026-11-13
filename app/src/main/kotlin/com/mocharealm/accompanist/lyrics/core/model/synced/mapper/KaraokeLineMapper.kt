package com.mocharealm.accompanist.lyrics.core.model.synced.mapper

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.contentToString
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

fun KaraokeLine.toSyncedLine(): SyncedLine {
    return SyncedLine(
        content = this.syllables.contentToString().trim(),
        translation = this.translation,
        start = this.start,
        end = this.end,
        phonetic = this.phonetic,
        languageTag = this.languageTag,
    )
}
