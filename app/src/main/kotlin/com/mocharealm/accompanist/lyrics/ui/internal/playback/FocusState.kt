package com.mocharealm.accompanist.lyrics.ui.internal.playback

internal data class FocusState(
    val firstIndex: Int,
    val allIndices: List<Int>,
    val activeInterludeIndex: Int?,
    val activeIntro: Boolean,
)

