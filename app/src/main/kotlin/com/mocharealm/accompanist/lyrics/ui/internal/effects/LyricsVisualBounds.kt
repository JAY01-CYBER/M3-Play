package com.mocharealm.accompanist.lyrics.ui.internal.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

// Alignment lines propagate through Columns/Boxes with their placement offsets. They describe
// retained drawing space, independently of the shrinking height consumed by list layout.
internal val LyricsVisualTop = HorizontalAlignmentLine(::minOf)
internal val LyricsVisualBottom = HorizontalAlignmentLine(::maxOf)

internal fun Modifier.lyricsVisualLayer(
    observePaint: () -> Unit,
    block: GraphicsLayerScope.() -> Unit,
): Modifier =
    lyricsLayerPaintObserver(observePaint).layout { measurable, constraints ->
        val child = measurable.measure(constraints)
        layout(child.width, child.height) {
            // Querying descendants during measure can place them before their parent enters
            // Android's RectList. Retained drawing bounds only belong to the placement layer.
            val top =
                child[LyricsVisualTop].let { if (it == AlignmentLine.Unspecified) 0 else minOf(0, it) }
            val bottom =
                child[LyricsVisualBottom].let {
                    if (it == AlignmentLine.Unspecified) child.height else maxOf(child.height, it)
                }
            // Three times the reveal blur radius, outside the retained unscaled drawing bounds.
            val visualOutsets =
                LayerOutsets(
                    left = 24.dp,
                    top = (-top).toDp() + 24.dp,
                    right = 24.dp,
                    bottom = (bottom - child.height).toDp() + 24.dp,
                )
            child.placeWithLayer(0, 0) {
                block()
                clip = false
                outsets = visualOutsets
            }
        }
    }

// Skia records the paint for a layer with outsets in its parent's display list. Observing only
// the child layer's properties leaves that recorded alpha/filter unchanged. Observe those values
// in the parent draw scope too; text rasters and layout remain cached. Android's RenderNode
// applies these properties directly and needs no additional draw observation.
