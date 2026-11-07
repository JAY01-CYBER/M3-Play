package com.mocharealm.accompanist.lyrics.ui.preparation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile
import com.mocharealm.accompanist.lyrics.ui.internal.effects.Swell

class PreparedGroup
internal constructor(
    val units: List<PreparedTextUnit>,
    accompaniment: Boolean,
    profile: LyricsProfile,
) {
    val sharedLayout =
        units.isNotEmpty() && units.all { it.text.layout === units.first().text.layout }
    val shapingLeft = units.minOfOrNull { it.text.left } ?: 0f
    val textWidth =
        if (sharedLayout) (units.maxOfOrNull { it.text.right } ?: 0f) - shapingLeft
        else units.sumOf { it.width.toDouble() }.toFloat()
    internal val phoneticWidth =
        units.maxOfOrNull { it.phonetic?.size?.width?.toFloat() ?: 0f } ?: 0f
    val width = maxOf(textWidth, phoneticWidth)
    private val textUnits = units.map { it.text }
    val staticText = profile.combine(textUnits)
    internal val phoneticIndices = units.indices.filter { units[it].phonetic != null }.toIntArray()
    val commonSource =
        units.isNotEmpty() && units.all { it.text.animation == units.first().text.animation }
    val effects = profile.effects(textUnits, accompaniment)
    val animationUnits =
        units
            .groupBy { it.text.animation }
            .map { (timing, drawables) ->
                PreparedAnimationUnit(timing, drawables).also { animation ->
                    drawables.forEach { it.animation = animation }
                }
            }
    val sourceStart = units.minOfOrNull { it.text.sourceStart } ?: 0
    var staticPosition = Offset.Zero
        internal set

    var effectsEnd = 0f
        internal set

    val start = units.minOfOrNull { it.text.start } ?: 0
    val end = units.maxOfOrNull { it.text.end } ?: start
    val duration = (end.toLong() - start).toFloat()
    val awesome = effects.scale || effects.glow
    val animationDuration = if (awesome) (duration * 0.8f).coerceAtLeast(1f) else 700f
    val intensity = ((duration - 200f * units.size) / 1000f).coerceIn(0f, 1f)
    internal val swell = Swell(0.1 * intensity)
    var pivot = Offset.Zero
        internal set
}
