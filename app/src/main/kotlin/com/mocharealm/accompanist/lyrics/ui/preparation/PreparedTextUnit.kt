package com.mocharealm.accompanist.lyrics.ui.preparation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextLayoutResult
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileTextUnit

class PreparedTextUnit
internal constructor(val text: ProfileTextUnit, val phonetic: TextLayoutResult?) {
    internal lateinit var animation: PreparedAnimationUnit
    val width = text.width
    var phoneticPosition = Offset.Zero
        internal set

    var position = Offset.Zero
        internal set

    var animationStart = 0f
        internal set
}
