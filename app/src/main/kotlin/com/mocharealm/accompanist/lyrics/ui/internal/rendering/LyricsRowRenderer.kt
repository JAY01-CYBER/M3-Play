package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.drawText
import com.mocharealm.accompanist.lyrics.ui.internal.diagnostics.traceLyrics
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow
import com.mocharealm.accompanist.lyrics.ui.internal.effects.Bounce

internal fun DrawScope.drawPreparedRow(
    row: PreparedRow,
    time: Int,
    state: RowRenderState,
    color: Color,
    paints: RowPaints,
    debug: Boolean,
    layers: PreparedRowLayers? = null,
    glows: RowGlowLayers? = null,
    drawOriginal: Boolean = true,
    drawPhonetics: Boolean = true,
) {
    traceLyrics(if (drawOriginal) "Lyrics.drawRow" else "Lyrics.drawPhonetics") {
        val canvas = drawContext.canvas
        val inactive = row.animated && time < row.start
        val alpha = if (inactive) FocusedRowUnlitAlpha else 1f
        val drawColor = if (inactive) color.copy(alpha = color.alpha * alpha) else color
        val phoneticColor =
            if (inactive) paints.phoneticColor.copy(alpha = paints.phoneticColor.alpha * alpha)
            else paints.phoneticColor
        val masked = row.animated && !inactive && time < row.sweepEnd
        if (masked) {
            // Pronunciation has no glow or lift. Its separate reveal only needs a caption-sized
            // sweep layer, rather than another buffer covering the entire original text row.
            val bounds = if (drawOriginal) row.bounds else row.bounds.copy(
                top = row.top + row.height - row.phoneticHeight - 2f,
                bottom = row.top + row.height + 2f,
            )
            canvas.saveLayer(bounds, state.layerPaint)
        }
        try {
            if (drawOriginal) for (runIndex in row.runs.indices) {
                val run = row.runs[runIndex]
                for (groupIndex in run.groups.indices) {
                    val group = run.groups[groupIndex]
                    val cachedGroup = layers?.runs?.get(runIndex)?.get(groupIndex)
                    val staticText = group.staticText
                    if (
                        !debug &&
                            staticText != null &&
                            (!row.animated ||
                                time < group.sourceStart ||
                                time >= group.effectsEnd ||
                                (!group.awesome && group.commonSource))
                    ) {
                        val progress =
                            if (!row.animated) 1f
                            else
                                ((time.toDouble() - group.sourceStart) / 700f)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                        val lift =
                            if (time >= group.effectsEnd || !row.animated) 0f
                            else if (group.effects.lift) 4f * (1f - progress) * (1f - progress)
                            else 0f
                        translate(group.staticPosition.x, group.staticPosition.y + lift) {
                            if (cachedGroup?.combined != null)
                                with(cachedGroup.combined) { draw(alpha) }
                            else with(run.profile) { draw(staticText, drawColor, Shadow.None) }
                        }
                        continue
                    }
                    val groupShadowIndex =
                        if (group.effects.glow && group.effects.glowAsGroup) {
                            val progress =
                                if (!row.animated) 1f
                                else
                                    ((time.toDouble() - group.start) / group.animationDuration)
                                        .toFloat()
                                        .coerceIn(0f, 1f)
                            (Bounce.transform(progress).coerceIn(0f, 1f) * 64f).toInt()
                        } else 0
                    for (unitIndex in group.units.indices) {
                        val unit = group.units[unitIndex]
                        val progress =
                            if (!row.animated) 1f
                            else
                                ((time.toDouble() - unit.animationStart) / group.animationDuration)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                        val liftProgress =
                            if (!row.animated) 1f
                            else
                                ((time.toDouble() - unit.animation.timing.start) / 700f)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                        val lift =
                            if (group.effects.lift) 4f * (1f - liftProgress) * (1f - liftProgress)
                            else 0f
                        val scale =
                            if (group.effects.scale) 1f + group.swell.transform(progress) else 1f
                        val shadowIndex =
                            when {
                                !group.effects.glow -> 0
                                group.effects.glowAsGroup -> groupShadowIndex
                                else -> (Bounce.transform(progress).coerceIn(0f, 1f) * 64f).toInt()
                            }
                        withTransform({
                            scale(scale, scale, group.pivot)
                            translate(unit.position.x, unit.position.y + lift)
                        }) {
                            val cached = cachedGroup?.units?.get(unitIndex)
                            if (cached != null)
                                drawUnit(
                                    cached,
                                    if (shadowIndex > 0)
                                        glows?.glow(runIndex, groupIndex, unitIndex)
                                    else null,
                                    shadowIndex,
                                    alpha,
                                    paints,
                                )
                            else
                                with(run.profile) {
                                    draw(
                                        unit.text,
                                        drawColor,
                                        if (shadowIndex == 0) Shadow.None
                                        else paints.shadows[shadowIndex],
                                    )
                                }
                            if (debug)
                                drawRect(
                                    Color.Green,
                                    size = Size(unit.width, unit.text.height),
                                    style = paints.debugStroke,
                                )
                        }
                    }
                }
            }
            // Phonetics stay anchored below their source group. The row-level sweep mask below
            // still reveals them with playback progress, while text lift/scale effects do not.
            if (drawPhonetics) for (runIndex in row.runs.indices) {
                val run = row.runs[runIndex]
                for (groupIndex in run.groups.indices) {
                    val group = run.groups[groupIndex]
                    val cachedGroup = layers?.runs?.get(runIndex)?.get(groupIndex)
                    for (unitIndex in group.phoneticIndices) {
                        val unit = group.units[unitIndex]
                        val layout = unit.phonetic ?: continue
                        translate(
                            unit.position.x + unit.phoneticPosition.x,
                            unit.position.y + unit.phoneticPosition.y,
                        ) {
                            val cached = cachedGroup?.units?.get(unitIndex)?.phonetic
                            if (cached != null) with(cached) { draw(alpha) }
                            else drawText(layout, phoneticColor)
                        }
                    }
                }
            }
            if (masked) {
                val center = state.sweepCenter(time)
                // A canvas translation reuses the fixed-width shader on both Android and Skia.
                translate(left = center) {
                    drawRect(
                        state.mask,
                        Offset(row.bounds.left - center, row.bounds.top),
                        row.bounds.size,
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        } finally {
            if (masked) canvas.restore()
        }
    }
}
