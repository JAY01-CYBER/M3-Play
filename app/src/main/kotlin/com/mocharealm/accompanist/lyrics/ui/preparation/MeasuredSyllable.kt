package com.mocharealm.accompanist.lyrics.ui.preparation

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileAnimationUnit

data class MeasuredSyllable(
    val source: KaraokeSyllable,
    val textStart: Int,
    val textEnd: Int,
    val animation: ProfileAnimationUnit,
)
