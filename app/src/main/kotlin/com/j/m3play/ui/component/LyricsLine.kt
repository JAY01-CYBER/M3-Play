/**
 * M3Play Project
 */
package com.j.m3play.ui.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.lyrics.LyricsEntry
import com.j.m3play.lyrics.WordTimestamp
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.ui.screens.settings.LyricsPosition
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LyricsLine(
    index: Int,
    item: LyricsEntry,
    isSynced: Boolean,
    isActiveLine: Boolean,
    bgVisible: Boolean,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    currentPositionState: Long,
    lyricsOffset: Long,
    playerConnection: PlayerConnection,
    lyricsTextSize: Float,
    lyricsLineSpacing: Float,
    expressiveAccent: Color,
    lyricsTextPosition: LyricsPosition,
    respectAgentPositioning: Boolean,
    isAutoScrollEnabled: Boolean,
    displayedCurrentLineIndex: Int,
    romanizeAsMain: Boolean,
    enabledLanguages: List<String>,
    romanizeLyrics: Boolean,
    onSizeChanged: (Int) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    val itemModifier = modifier
        .fillMaxWidth()
        .onSizeChanged { onSizeChanged(it.height) }
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
        .padding(
            start = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; LyricsPosition.CENTER -> 24.dp },
            end = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; LyricsPosition.CENTER -> 24.dp },
            top = if (item.isBackground) 0.dp else 12.dp,
            bottom = if (item.isBackground) 2.dp else 12.dp
        )

    val agentAlignment = when {
        respectAgentPositioning && item.agent == "v1" -> Alignment.Start
        respectAgentPositioning && item.agent == "v2" -> Alignment.End
        item.isBackground -> Alignment.CenterHorizontally
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    }
    
    val agentTextAlign = when {
        respectAgentPositioning && item.agent == "v1" -> TextAlign.Left
        respectAgentPositioning && item.agent == "v2" -> TextAlign.Right
        item.isBackground -> TextAlign.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }
    }

    Box(modifier = itemModifier, contentAlignment = when {
        respectAgentPositioning && item.agent == "v1" -> Alignment.CenterStart
        respectAgentPositioning && item.agent == "v2" -> Alignment.CenterEnd
        item.isBackground -> Alignment.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.CenterStart
            LyricsPosition.RIGHT -> Alignment.CenterEnd
            LyricsPosition.CENTER -> Alignment.Center
        }
    }) {
        @Composable
        fun LyricContent() {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = agentAlignment) {
                val inactiveAlpha = if (item.isBackground) 0.08f else 0.2f
                val activeAlpha = 1f
                val focusedAlpha = if (item.isBackground) 0.5f else 0.3f
                val targetAlpha = if (item.isBackground || isActiveLine) {
                    activeAlpha
                } else if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                    when (abs(index - displayedCurrentLineIndex)) {
                        0 -> focusedAlpha
                        1 -> 0.2f; 2 -> 0.2f; 3 -> 0.15f; 4 -> 0.1f; else -> 0.08f
                    }
                } else inactiveAlpha
                
                val animatedAlpha by animateFloatAsState(targetAlpha, tween(250), label = "lyricsLineAlpha")
                val lineColor = expressiveAccent.copy(alpha = if (item.isBackground) focusedAlpha else animatedAlpha)
                
                val romanizedTextState by item.romanizedTextFlow.collectAsStateWithLifecycle()
                val isRomanizedAvailable = romanizedTextState != null
                val mainTextRaw = if (romanizeAsMain && isRomanizedAvailable) romanizedTextState else item.text
                val subTextRaw = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedTextState
                val mainText = if (item.isBackground) mainTextRaw?.removePrefix("(")?.removeSuffix(")") else mainTextRaw
                val subText = if (item.isBackground) subTextRaw?.removePrefix("(")?.removeSuffix(")") else subTextRaw

                val lyricStyle = TextStyle(
                    fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal,
                    lineHeight = if (item.isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,
                    letterSpacing = (-0.5).sp,
                    textAlign = agentTextAlign,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
                )

                Text(text = mainText ?: "", style = lyricStyle.copy(color = if (isActiveLine) expressiveAccent else lineColor), modifier = Modifier.fillMaxWidth())
                
                if (romanizeLyrics && enabledLanguages.isNotEmpty()) {
                    subText?.let { Text(text = it, fontSize = 18.sp, color = expressiveAccent.copy(alpha = 0.6f), textAlign = agentTextAlign, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 2.dp)) }
                }
                
                val transText by item.translatedTextFlow.collectAsStateWithLifecycle()
                transText?.let { Text(text = it, fontSize = 16.sp, color = expressiveAccent.copy(alpha = 0.5f), textAlign = agentTextAlign, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 4.dp)) }
            }
        }

        if (item.isBackground) {
            AnimatedVisibility(visible = bgVisible, enter = fadeIn(tween(durationMillis = 250, delayMillis = 100)), exit = fadeOut(tween(250))) { LyricContent() }
        } else LyricContent()
    }
}
