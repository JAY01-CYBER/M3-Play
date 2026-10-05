package com.mocharealm.accompanist.lyrics.ui.internal.rendering

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealAlpha
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealBlurIndex
import com.mocharealm.accompanist.lyrics.ui.internal.effects.revealScale
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedLine
import com.mocharealm.accompanist.lyrics.ui.preparation.PreparedRow
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileTextUnit
import kotlin.math.ceil

/** Fixed-origin raster tiles are prepared on the scene worker, before lazy items attach. */
internal class PreparedRowLayers(
    density: Density,
    layoutDirection: LayoutDirection,
    row: PreparedRow,
    color: Color,
    paints: RowPaints,
) {
    private val atlas = TextAtlas(density, layoutDirection)
    val runs =
        Array(row.runs.size) { runIndex ->
            val run = row.runs[runIndex]
            Array(run.groups.size) { groupIndex ->
                val group = run.groups[groupIndex]
                val combined =
                    group.staticText?.let { text -> atlas.text(run.profile, text, color) }
                GroupLayers(
                    combined,
                    Array(group.units.size) { unitIndex ->
                        val unit = group.units[unitIndex]
                        UnitLayers(
                            if (unit.text === group.staticText && combined != null) combined
                            else atlas.text(run.profile, unit.text, color),
                            group.effects.glow,
                            unit.phonetic?.let { layout ->
                                atlas.add(layout.size.width.toFloat(), layout.size.height, 2) {
                                    drawText(layout, paints.phoneticColor)
                                }
                            },
                        )
                    },
                )
            }
        }

    val hasGlow = runs.any { run -> run.any { it.hasGlow } }
    val hasPhonetics = runs.any { run -> run.any { it.hasPhonetics } }

    init {
        atlas.finish()
    }

    internal val pageCount
        get() = atlas.pageCount

    internal val pages: List<ImageBitmap>
        get() = atlas.pages
}

internal class GroupLayers(val combined: TextLayer?, val units: Array<UnitLayers>) {
    val hasGlow = units.any { it.glow }
    val hasPhonetics = units.any { it.phonetic != null }
}

internal class UnitLayers(val text: TextLayer, val glow: Boolean, val phonetic: TextLayer?)

/**
 * Only records references to already-rasterized pixels; never calls profiles or rasterizes text.
 */
internal class RowGlowLayers(scope: CacheDrawScope, raster: PreparedRowLayers) {
    val runs =
        if (!raster.hasGlow) null
        else
            Array(raster.runs.size) { r ->
                val run = raster.runs[r]
                if (run.none { it.hasGlow }) null
                else
                    Array(run.size) { g ->
                        val group = run[g]
                        if (!group.hasGlow) null
                        else
                            Array(group.units.size) { u ->
                                val unit = group.units[u]
                                if (!unit.glow) null
                                else
                                    scope.obtainGraphicsLayer().apply {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        record(scope, scope.layoutDirection, unit.text.dimensions) {
                                            translate(
                                                unit.text.padding.toFloat(),
                                                unit.text.padding.toFloat(),
                                            ) {
                                                with(unit.text) { draw() }
                                            }
                                        }
                                    }
                            }
                    }
            }
    val phonetics =
        if (!raster.hasPhonetics) null
        else
            Array(raster.runs.size) { r ->
                val run = raster.runs[r]
                if (run.none { it.hasPhonetics }) null
                else
                    Array(run.size) { g ->
                        val group = run[g]
                        if (!group.hasPhonetics) null
                        else
                            Array(group.units.size) { u ->
                                group.units[u].phonetic?.let { tile ->
                                    scope.obtainGraphicsLayer().apply {
                                        record(scope, scope.layoutDirection, tile.dimensions) {
                                            translate(
                                                tile.padding.toFloat(),
                                                tile.padding.toFloat(),
                                            ) {
                                                with(tile) { draw() }
                                            }
                                        }
                                    }
                                }
                            }
                    }
            }
}

internal fun DrawScope.drawUnit(
    unit: UnitLayers,
    glow: GraphicsLayer?,
    shadowIndex: Int,
    opacity: Float,
    paints: RowPaints,
) {
    if (shadowIndex > 0 && glow != null) {
        glow.renderEffect = paints.blurEffects[shadowIndex]
        glow.alpha = opacity * 0.4f * shadowIndex / 64f
        translate(-unit.text.padding.toFloat(), -unit.text.padding.toFloat()) { drawLayer(glow) }
    }
    with(unit.text) { draw(opacity) }
}

internal class TextLayer(val source: IntOffset, val dimensions: IntSize, val padding: Int) {
    lateinit var image: ImageBitmap
    private val destination = IntOffset(-padding, -padding)
    private val paint = Paint().apply { filterQuality = FilterQuality.Low }

    fun DrawScope.draw(alpha: Float = 1f) {
        paint.alpha = alpha
        drawContext.canvas.drawImageRect(image, source, dimensions, destination, dimensions, paint)
    }
}

/** Shelf packing is performed once during raster preparation, with bounded page dimensions. */
private class TextAtlas(
    private val density: Density,
    private val layoutDirection: LayoutDirection,
) {
    private class Entry(val tile: TextLayer, val padding: Int, val draw: DrawScope.() -> Unit)

    private val entries = mutableListOf<Entry>()
    private var x = 0
    private var y = 0
    private var shelfHeight = 0
    private var width = 0
    var pageCount = 0
        private set
    val pages = mutableListOf<ImageBitmap>()

    fun text(
        profile: LyricsProfile,
        text: ProfileTextUnit,
        color: Color,
        shadow: Shadow = Shadow.None,
    ): TextLayer =
        add(text.width, kotlin.math.ceil(text.height).toInt(), 32) {
            with(profile) { draw(text, color, shadow) }
        }

    fun add(textWidth: Float, height: Int, padding: Int, draw: DrawScope.() -> Unit): TextLayer {
        val size =
            IntSize(
                ceil(textWidth).toInt().coerceAtLeast(1) + 2 * padding,
                height.coerceAtLeast(1) + 2 * padding,
            )
        if (x > 0 && x + size.width > 2048) {
            y += shelfHeight
            x = 0
            shelfHeight = 0
        }
        if (y + size.height > 2048 && entries.isNotEmpty()) finish()
        val tile = TextLayer(IntOffset(x, y), size, padding)
        entries.add(Entry(tile, padding, draw))
        x += size.width
        width = maxOf(width, x)
        shelfHeight = maxOf(shelfHeight, size.height)
        return tile
    }

    fun finish() {
        if (entries.isEmpty()) return
        val image = ImageBitmap(width, y + shelfHeight)
        CanvasDrawScope().draw(
            density,
            layoutDirection,
            Canvas(image),
            Size(width.toFloat(), (y + shelfHeight).toFloat()),
        ) {
            for (entry in entries) {
                val tile = entry.tile
                tile.image = image
                clipRect(
                    tile.source.x.toFloat(),
                    tile.source.y.toFloat(),
                    (tile.source.x + tile.dimensions.width).toFloat(),
                    (tile.source.y + tile.dimensions.height).toFloat(),
                ) {
                    translate(
                        (tile.source.x + entry.padding).toFloat(),
                        (tile.source.y + entry.padding).toFloat(),
                        entry.draw,
                    )
                }
            }
        }
        // Upload near the viewport, not while rasterizing every line in the song.
        // Warming the entire song here evicts useful textures before they are drawn.
        pages.add(image)
        pageCount++
        entries.clear()
        x = 0
        y = 0
        shelfHeight = 0
        width = 0
    }
}

internal class PreparedLineRaster(
    val rows: List<PreparedRowLayers>,
)

/** Caller prepares off the UI thread and publishes the complete line only after all pages exist. */
internal fun prepareLineRaster(
    line: PreparedLine,
    color: Color,
    density: Density,
    direction: LayoutDirection,
): PreparedLineRaster {
    val paints = RowPaints(color)
    return PreparedLineRaster(
        line.rows.map { PreparedRowLayers(density, direction, it, color, paints) },
    )
}

internal fun DrawScope.drawPhonetic(
    tile: TextLayer?,
    layer: GraphicsLayer?,
    layout: androidx.compose.ui.text.TextLayoutResult,
    color: Color,
    alpha: Float,
    progress: Float,
    paints: RowPaints,
) {
    if (progress <= 0f) return
    if (layer != null && tile != null) {
        layer.scaleX = revealScale(progress)
        layer.scaleY = layer.scaleX
        layer.alpha = alpha * revealAlpha(progress)
        layer.renderEffect = paints.blurEffects[revealBlurIndex(progress)]
        translate(-tile.padding.toFloat(), -tile.padding.toFloat()) { drawLayer(layer) }
    } else {
        scale(
            revealScale(progress),
            pivot = Offset(layout.size.width / 2f, layout.size.height / 2f),
        ) {
            if (tile != null) with(tile) { draw(alpha * revealAlpha(progress)) }
            else drawText(layout, color.copy(alpha = color.alpha * revealAlpha(progress)))
        }
    }
}
