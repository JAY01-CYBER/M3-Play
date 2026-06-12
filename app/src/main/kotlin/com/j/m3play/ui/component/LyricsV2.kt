/*
 * M3Play Component Module
 *
 * Reusable UI building block
 * Signature: M3PLAY::COMPONENT::V1::METROLIST_EDITION_FIXED_R8_SAFE
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.LyricsClickKey
import com.j.m3play.constants.LyricsTextSizeKey
import com.j.m3play.constants.LyricsLineSpacingKey
import com.j.m3play.constants.LyricsRomanizeJapaneseKey
import com.j.m3play.constants.LyricsRomanizeKoreanKey
import com.j.m3play.constants.PlayerBackgroundStyle
import com.j.m3play.constants.PlayerBackgroundStyleKey
import com.j.m3play.constants.UseSystemFontKey
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.lyrics.LyricsEntry
import com.j.m3play.lyrics.LyricsUtils.findCurrentLineIndex
import com.j.m3play.lyrics.LyricsUtils.isChinese
import com.j.m3play.lyrics.LyricsUtils.isJapanese
import com.j.m3play.lyrics.LyricsUtils.isKorean
import com.j.m3play.lyrics.LyricsUtils.isTtml
import com.j.m3play.lyrics.LyricsUtils.parseLyrics
import com.j.m3play.lyrics.LyricsUtils.parseTtml
import com.j.m3play.lyrics.LyricsUtils.romanizeJapanese
import com.j.m3play.lyrics.LyricsUtils.romanizeKorean
import com.j.m3play.lyrics.WordTimestamp
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.utils.smoothFadingEdge
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

// ──────────────────────────────────────────────────────────────────────
// Helper Extensions for Metrolist Canvas Rendering
// ──────────────────────────────────────────────────────────────────────

private data class HyphenGroupWord(
    val pos: Int,
    val size: Int,
    val isLast: Boolean,
    val groupStartMs: Long,
    val groupEndMs: Long
)

private fun String.containsRtl(): Boolean {
    for (c in this) {
        val directionality = Character.getDirectionality(c).toInt()
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt() ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt()
        ) {
            return true
        }
    }
    return false
}

private fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(substring(start, end))
        start = end
        end = it.next()
    }
    return result
}

// ──────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────
private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

// ──────────────────────────────────────────────────────────────────────
// Main Composable
// ──────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "UseOfNonSpaceChar")
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ── M3-Play Preferences ──
    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val lyricsFontFamily = remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onBackground else Color.White

    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val maxSelectionLimit = 5

    // ── M3-Play Data Parsing ──
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics
    val isSynced = remember(lyrics) { lyrics != null && (lyrics.startsWith("[") || isTtml(lyrics)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else {
            val parsed = when {
                isTtml(lyrics) -> parseTtml(lyrics)
                lyrics.startsWith("[") -> parseLyrics(lyrics)
                else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { _, line -> LyricsEntry(time = -1L, text = line.trim()) }
            }
            if (parsed.isNotEmpty() && parsed.first().time >= 0) listOf(HEAD_LYRICS_ENTRY) + parsed else parsed
        }
    }

    val entriesWithWords: List<LyricsEntry> = remember(lyricsEntries) {
        if (lyricsEntries.isEmpty()) {
            emptyList()
        } else {
            lyricsEntries.mapIndexed { index, entry ->
                if (entry.words != null || entry.time < 0 || entry.text.isBlank()) {
                    entry 
                } else {
                    val nextEntryTime = if (index < lyricsEntries.lastIndex) lyricsEntries[index + 1].time else entry.time + 5000L
                    val lineDurationMs = (nextEntryTime - entry.time).coerceAtLeast(500L)
                    val lineStartSec = entry.time / 1000.0
                    val isCjkText = isJapanese(entry.text) || isChinese(entry.text) || isKorean(entry.text)
                    val tokens = if (isCjkText) {
                        val chars = mutableListOf<String>()
                        var currentWord = StringBuilder()
                        entry.text.forEach { char ->
                            if (char.isWhitespace()) {
                                if (currentWord.isNotEmpty()) { chars.add(currentWord.toString()); currentWord.clear() }
                                chars.add(char.toString())
                            } else if (isJapanese(char.toString()) || isChinese(char.toString()) || isKorean(char.toString())) {
                                if (currentWord.isNotEmpty()) { chars.add(currentWord.toString()); currentWord.clear() }
                                chars.add(char.toString())
                            } else { currentWord.append(char) }
                        }
                        if (currentWord.isNotEmpty()) chars.add(currentWord.toString())
                        val groupedTokens = mutableListOf<String>()
                        chars.forEachIndexed { _, c ->
                            if (c.isBlank()) {
                                if (groupedTokens.isNotEmpty()) groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                            } else { groupedTokens.add(c) }
                        }
                        groupedTokens
                    } else {
                        entry.text.split(Regex("\\s+"))
                    }
                    
                    if (tokens.isEmpty()) {
                        entry
                    } else {
                        val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
                        val words = mutableListOf<WordTimestamp>()
                        var currentOffsetMs = 0.0

                        tokens.forEachIndexed { wordIdx, token ->
                            val weight = token.length.toDouble() / totalChars
                            val wordDurMs = lineDurationMs * weight
                            val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                            val wordEndSec = wordStartSec + (wordDurMs / 1000.0)
                            val wordText = if (wordIdx < tokens.lastIndex && !isCjkText) "$token " else token
                            words.add(WordTimestamp(text = wordText, startTime = wordStartSec, endTime = wordEndSec))
                            currentOffsetMs += wordDurMs
                        }
                        entry.copy(words = words)
                    }
                }
            }
        }
    }

    LaunchedEffect(entriesWithWords, romanizeJapanese, romanizeKorean) {
        if (romanizeJapanese || romanizeKorean) {
            entriesWithWords.forEach { entry ->
                if (entry.text.isNotBlank() && entry.romanizedTextFlow.value == null) {
                    scope.launch(Dispatchers.Default) {
                        val romanized = when {
                            romanizeJapanese && isJapanese(entry.text) -> romanizeJapanese(entry.text)
                            romanizeKorean && isKorean(entry.text) -> romanizeKorean(entry.text)
                            else -> null
                        }
                        if (romanized != null) entry.romanizedTextFlow.value = romanized
                    }
                }
            }
        }
    }

    // ── Metrolist Staggered Scroll Mechanics ──
    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS
    var currentPositionState by remember { mutableLongStateOf(0L) }
    
    // Split Highlight logic and Scroll logic entirely
    var currentPlayingLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var focusScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(entriesWithWords, isSynced) {
        if (isSynced && entriesWithWords.isNotEmpty()) {
            while (isActive) {
                withFrameMillis { frameTime ->
                    val sliderPos = sliderPositionProvider()
                    isSeeking = sliderPos != null
                    val pos = sliderPos ?: player.currentPosition
                    val visualTuningOffsetMs = 150L 
                    currentPositionState = pos + leadMs + visualTuningOffsetMs
                    
                    val currentLineIdx = findCurrentLineIndex(entriesWithWords, currentPositionState, 0L)
                    if (currentLineIdx != -1) {
                        if (currentLineIdx != currentPlayingLineIndex) {
                            currentPlayingLineIndex = currentLineIdx
                        }
                        if ((isAutoScrollEnabled || isSeeking) && currentLineIdx != focusScrollIndex) {
                            focusScrollIndex = currentLineIdx
                        }
                    }
                }
            }
        }
    }

    // Robust Auto-Scroll Resume Timeout Fix
    LaunchedEffect(lastManualScrollTime) {
        if (!isAutoScrollEnabled && lastManualScrollTime > 0L) {
            delay(2500L) // Wait 2.5 seconds after last manual interaction
            isAutoScrollEnabled = true
            lastManualScrollTime = 0L // Reset to prevent loop
        }
    }

    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, entriesWithWords) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, entriesWithWords) { mutableStateOf(true) }

    val activeListIndex = focusScrollIndex.coerceAtLeast(0)

    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * LYRICS_ANCHOR_RATIO
        val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }
        val constraintLineHeightPx = with(density) { 120.dp.toPx() }

        val positions = remember(itemHeights.toMap(), activeListIndex, entriesWithWords) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex != -1 && entriesWithWords.isNotEmpty()) {
                map[activeListIndex] = 0f
                var currentY = 0f
                for (i in activeListIndex - 1 downTo 0) {
                    val item = entriesWithWords[i]
                    val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                    val isBg = item.words?.all { it.isBackground || it.text.isBlank() } == true
                    currentY -= (height + if (isBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                    map[i] = currentY
                }
                currentY = 0f
                for (i in activeListIndex until entriesWithWords.size - 1) {
                    val nextItem = entriesWithWords[i + 1]
                    val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                    val isNextBg = nextItem.words?.all { it.isBackground || it.text.isBlank() } == true
                    currentY += (height + if (isNextBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                    map[i + 1] = currentY
                }
            }
            map
        }

        val scrollClampMin = remember(itemHeights.toMap(), entriesWithWords, activeListIndex, anchorY) {
            if (entriesWithWords.isEmpty() || activeListIndex == -1) {
                0f
            } else {
                val totalBelow = (activeListIndex until entriesWithWords.size - 1).sumOf { i ->
                    val height = itemHeights[i]?.toFloat() ?: constraintLineHeightPx
                    val isNextBg = entriesWithWords[i + 1].words?.all { it.isBackground || it.text.isBlank() } == true
                    (height + if (isNextBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                }.toFloat()
                val lastHeight = itemHeights[entriesWithWords.size - 1]?.toFloat() ?: constraintLineHeightPx
                with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
            }
        }

        val scrollClampMax = remember(itemHeights.toMap(), entriesWithWords, activeListIndex, maxHeightPx, anchorY) {
            if (entriesWithWords.isEmpty() || activeListIndex == -1) {
                0f
            } else {
                val totalAbove = (0 until activeListIndex).sumOf { i ->
                    val height = itemHeights[i]?.toFloat() ?: constraintLineHeightPx
                    val isBg = entriesWithWords[i].words?.all { it.isBackground || it.text.isBlank() } == true
                    (height + if (isBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                }.toFloat()
                maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
            }
        }

        val finalClampMin = minOf(scrollClampMin, scrollClampMax)
        val finalClampMax = maxOf(scrollClampMin, scrollClampMax)

        LaunchedEffect(isAutoScrollEnabled) {
            if (isAutoScrollEnabled && userManualOffset != 0f) {
                androidx.compose.animation.core.animate(
                    initialValue = userManualOffset,
                    targetValue = 0f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    userManualOffset = value
                }
            }
        }

        LaunchedEffect(lyrics, entriesWithWords.size) {
            if (entriesWithWords.isNotEmpty()) {
                isInitialLayout = true
                withTimeoutOrNull(500) {
                    snapshotFlow { 
                        val h = itemHeights.toMap()
                        val windowStart = (activeListIndex - 8).coerceAtLeast(0)
                        val windowEnd = (activeListIndex + 12).coerceAtMost(entriesWithWords.size - 1)
                        (windowStart..windowEnd).all { h.containsKey(it) } 
                    }.first { it }
                }
                isInitialLayout = false
            }
        }

        if (lyrics == LYRICS_NOT_FOUND || entriesWithWords.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.6f))
            }
        } else if (lyrics == null) {
            Column(modifier = Modifier.padding(top = 100.dp)) {
                 ShimmerHost { repeat(6) { Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) { TextPlaceholder() } } }
             }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .smoothFadingEdge(vertical = 80.dp)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (isInitialLayout) continue
                                flingJob?.cancel()
                                velocityTracker.resetTracking()
                                isAutoScrollEnabled = false
                                lastManualScrollTime = System.currentTimeMillis()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                verticalDrag(down.id) { change ->
                                    userManualOffset = (userManualOffset + change.positionChange().y).coerceIn(finalClampMin, finalClampMax)
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    lastManualScrollTime = System.currentTimeMillis()
                                    change.consume()
                                }
                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                        val clamped = value.coerceIn(finalClampMin, finalClampMax)
                                        userManualOffset = clamped
                                        if (value != clamped) cancelAnimation()
                                    }
                                }
                            }
                        }
                    }
            ) {
                entriesWithWords.forEachIndexed { listIndex, item ->
                    key(item.time.hashCode() * 31 + listIndex) {
                        if (item == HEAD_LYRICS_ENTRY) {
                            Spacer(modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .onSizeChanged { itemHeights[listIndex] = it.height }
                            )
                        } else {
                            val distance = abs(listIndex - activeListIndex)
                            val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * lineHeightPx)
                            val frozenOffset = remember { mutableFloatStateOf(targetOffset) }
                            
                            LaunchedEffect(isAutoScrollEnabled, targetOffset, isInitialLayout) {
                                if (isAutoScrollEnabled || isInitialLayout) frozenOffset.floatValue = targetOffset
                            }
                            
                            val animatedOffset by animateFloatAsState(
                                targetValue = if (isAutoScrollEnabled) targetOffset else frozenOffset.floatValue,
                                animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap() 
                                                else tween(750, (distance * LYRICS_STAGGER_DELAY_PER_DISTANCE).coerceAtMost(LYRICS_STAGGER_DELAY_MAX_MS), FastOutSlowInEasing),
                                label = "lyricStaggeredOffset_$listIndex"
                            )

                            val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true
                            val isActiveLine = currentPlayingLineIndex == listIndex
                            val isSelected = selectedIndices.contains(listIndex)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { m, c -> 
                                        val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                                        layout(p.width, 0) { p.place(0, 0) }
                                    }
                                    .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                            ) {
                                MetrolistCanvasLyricsLine(
                                    index = listIndex,
                                    item = item,
                                    isSynced = isSynced,
                                    isActiveLine = isActiveLine,
                                    isBackground = isAllBackground,
                                    isSelected = isSelected,
                                    isSelectionModeActive = isSelectionModeActive,
                                    currentPositionProvider = { currentPositionState }, // Pass lambda to prevent recomposition!
                                    lyricsTextSize = lyricsTextSize,
                                    lyricsLineSpacing = lyricsLineSpacing,
                                    expressiveAccent = textColor,
                                    isAutoScrollEnabled = isAutoScrollEnabled,
                                    displayedCurrentLineIndex = currentPlayingLineIndex,
                                    romanizeLyrics = (romanizeJapanese || romanizeKorean),
                                    lyricsFontFamily = lyricsFontFamily,
                                    onSizeChanged = { itemHeights[listIndex] = it },
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (isSelected) {
                                                selectedIndices.remove(listIndex)
                                                if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                            } else if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(listIndex) else showMaxSelectionToast = true
                                        } else if (lyricsClick && isSynced && item.time > 0) {
                                            player.seekTo(item.time)
                                            isAutoScrollEnabled = true
                                            lastManualScrollTime = 0L
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true; selectedIndices.add(listIndex)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) selectedIndices.add(listIndex)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!isAutoScrollEnabled && isSynced) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        isAutoScrollEnabled = true
                        lastManualScrollTime = 0L
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(text = "Resume", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Metrolist Canvas Drawing Logic for Lyrics
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MetrolistCanvasLyricsLine(
    index: Int,
    item: LyricsEntry,
    isSynced: Boolean,
    isActiveLine: Boolean,
    isBackground: Boolean,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    currentPositionProvider: () -> Long,
    lyricsTextSize: Float,
    lyricsLineSpacing: Float,
    expressiveAccent: Color,
    isAutoScrollEnabled: Boolean,
    displayedCurrentLineIndex: Int,
    romanizeLyrics: Boolean,
    lyricsFontFamily: FontFamily?,
    onSizeChanged: (Int) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Center }
    val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.CenterHorizontally }
    
    val itemModifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { onSizeChanged(it.height) }
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
        .padding(
            start = if (isBackground) 24.dp else 12.dp,
            end = 12.dp,
            top = if (isBackground) 0.dp else 12.dp,
            bottom = if (isBackground) 2.dp else 12.dp
        )

    Box(modifier = itemModifier, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = horizontalAlignment) {
            val inactiveAlpha = if (isBackground) 0.15f else 0.35f
            val activeAlpha = 1f
            val focusedAlpha = if (isBackground) 0.5f else 0.4f
            
            val targetAlpha = if (!isSynced || isBackground || isActiveLine) activeAlpha 
            else if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                when (abs(index - displayedCurrentLineIndex)) {
                    0 -> focusedAlpha; 1, 2 -> 0.25f; 3 -> 0.2f; else -> inactiveAlpha
                }
            } else inactiveAlpha
            
            val animatedAlpha by animateFloatAsState(targetAlpha, tween(250), label = "lyricsLineAlpha")
            val lineColor = expressiveAccent.copy(alpha = if (isBackground) focusedAlpha else animatedAlpha)
            
            val mainText = item.text
            val romanizedText by item.romanizedTextFlow.collectAsState()

            val lyricStyle = TextStyle(
                fontSize = if (isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                lineHeight = if (isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,
                letterSpacing = (-0.5).sp,
                textAlign = textAlign,
                fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
            )

            if (isSynced && item.words != null && (isActiveLine || abs(index - displayedCurrentLineIndex) <= 3) && mainText.isNotBlank()) {
                MetrolistWordLevelCanvas(
                    mainText = mainText,
                    words = item.words,
                    isActiveLine = isActiveLine,
                    currentPositionProvider = currentPositionProvider,
                    lyricStyle = lyricStyle,
                    lineColor = lineColor,
                    expressiveAccent = expressiveAccent,
                    isBackground = isBackground,
                    focusedAlpha = focusedAlpha,
                    alignment = textAlign
                )
            } else {
                Text(text = mainText, style = lyricStyle.copy(color = if (isActiveLine) expressiveAccent else lineColor), modifier = Modifier.fillMaxWidth())
            }
            
            if (romanizeLyrics && romanizedText != null) {
                Text(
                    text = romanizedText!!, fontSize = (lyricsTextSize * 0.55f).sp, color = expressiveAccent.copy(alpha = 0.6f),
                    textAlign = textAlign, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 2.dp),
                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily
                )
            }
        }
    }
}

@Composable
private fun MetrolistWordLevelCanvas(
    mainText: String,
    words: List<WordTimestamp>,
    isActiveLine: Boolean,
    currentPositionProvider: () -> Long,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    isBackground: Boolean,
    focusedAlpha: Float,
    alignment: TextAlign
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Performance fix: Create BlurMaskFilter ONCE, not on every frame
    val glowBlurRadius = with(density) { 12.dp.toPx() }
    val glowBlurMask = remember(glowBlurRadius) { BlurMaskFilter(glowBlurRadius, BlurMaskFilter.Blur.NORMAL) }
    
    val glowPaint = remember { 
        android.graphics.Paint().apply { 
            isAntiAlias = true 
            maskFilter = glowBlurMask
        } 
    }

    val (effectiveWords, effectiveToOriginalIdx) = remember(words, isBackground) {
        words.flatMapIndexed { originalIdx, word ->
            val hasTrailingSpace = word.text.endsWith(" ")
            val shouldSplit = word.text.contains('-') && word.text.length > 1 && (!hasTrailingSpace || words.size == 1)
            if (shouldSplit) {
                val segments = mutableListOf<String>()
                var start = 0
                for (i in 0 until word.text.length) {
                    if (word.text[i] == '-') { segments.add(word.text.substring(start, i + 1)); start = i + 1 }
                }
                if (start < word.text.length) segments.add(word.text.substring(start))

                if (segments.size > 1) {
                    val totalDuration = word.endTime - word.startTime
                    val segmentDuration = totalDuration / segments.size
                    segments.mapIndexed { index, segmentText ->
                        WordTimestamp(text = segmentText, startTime = word.startTime + index * segmentDuration, endTime = word.startTime + (index + 1) * segmentDuration) to originalIdx
                    }
                } else listOf(word to originalIdx)
            } else listOf(word to originalIdx)
        }.let { data -> data.map { it.first } to data.map { it.second } }
    }

    val graphemeClusters = remember(mainText) { mainText.toGraphemeClusters() }
    val clusterCount = graphemeClusters.size
    val clusterCharOffsets = remember(mainText) {
        IntArray(clusterCount).also { offsets ->
            var charOffset = 0
            graphemeClusters.forEachIndexed { i, cluster -> offsets[i] = charOffset; charOffset += cluster.length }
        }
    }

    val charToWordData = remember(mainText, effectiveWords, isBackground, graphemeClusters, clusterCharOffsets) {
        val wordIdxMap = IntArray(clusterCount) { -1 }
        val charInWordMap = IntArray(clusterCount)
        val wordLenMap = IntArray(clusterCount) { 1 }
        var currentPos = 0
        var clCursor = 0
        effectiveWords.forEachIndexed { wordIdx, word ->
            val rawWordText = word.text
            val indexInMain = mainText.indexOf(rawWordText, currentPos)
            if (indexInMain != -1) {
                val wordEndInMain = indexInMain + rawWordText.length
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < indexInMain) clCursor++
                val wordClusterIndices = mutableListOf<Int>()
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < wordEndInMain) {
                    wordClusterIndices.add(clCursor); clCursor++
                }
                val wordClusterLen = wordClusterIndices.size
                wordClusterIndices.forEachIndexed { posInWord, clIdx ->
                    wordIdxMap[clIdx] = wordIdx; charInWordMap[clIdx] = posInWord; wordLenMap[clIdx] = wordClusterLen
                }
                if (clCursor < clusterCount && clusterCharOffsets[clCursor] == wordEndInMain && wordEndInMain < mainText.length && mainText[wordEndInMain] == ' ') {
                    val spaceClIdx = clCursor
                    wordIdxMap[spaceClIdx] = wordIdx; charInWordMap[spaceClIdx] = wordClusterLen; wordLenMap[spaceClIdx] = wordClusterLen + 1
                    clCursor++
                }
                currentPos = wordEndInMain
            }
        }
        Triple(wordIdxMap, charInWordMap, wordLenMap)
    }

    val hyphenGroupData = remember(effectiveWords) {
        val map = mutableMapOf<Int, HyphenGroupWord>()
        var currentGroup = mutableListOf<Int>()
        effectiveWords.forEachIndexed { wordIdx, word ->
            currentGroup.add(wordIdx)
            if (!word.text.endsWith("-")) {
                if (currentGroup.size > 1) {
                    val groupSize = currentGroup.size
                    // Fixed: Double multiplication safely cast to Long
                    val groupStartMs = (effectiveWords[currentGroup.first()].startTime * 1000.0).toLong()
                    val groupEndMs = (word.endTime * 1000.0).toLong()
                    currentGroup.forEachIndexed { pos, idx ->
                        map[idx] = HyphenGroupWord(pos, groupSize, pos == groupSize - 1, groupStartMs, groupEndMs)
                    }
                }
                currentGroup = mutableListOf()
            }
        }
        map
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(mainText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(text = mainText, style = lyricStyle, constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx), softWrap = true)
        }
        val letterLayouts = remember(mainText, lyricStyle) { graphemeClusters.map { cluster -> textMeasurer.measure(cluster, lyricStyle) } }
        val isRtlText = remember(mainText) { mainText.containsRtl() }
        
        // Performance Fix: Cache char bounds to prevent recreating Rects inside 120fps draw loop
        val charBoundsArray = remember(layoutResult, clusterCharOffsets) {
            Array(clusterCount) { layoutResult.getBoundingBox(clusterCharOffsets[it]) }
        }
        val charLinesArray = remember(layoutResult, clusterCharOffsets) {
            IntArray(clusterCount) { layoutResult.getLineForOffset(clusterCharOffsets[it]) }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(with(density) { layoutResult.size.height.toDp() }).graphicsLayer(clip = false, compositingStrategy = CompositingStrategy.Offscreen)) {
            if (mainText.isNotEmpty()) {
                if (!isActiveLine) {
                    drawText(layoutResult, color = lineColor)
                } else {
                    if (isRtlText) {
                        drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))
                    } else {
                        // Cast to Float ONCE, completely isolating 'Double' Math issues!
                        val smoothPosition = currentPositionProvider()
                        val smoothPositionF = smoothPosition.toFloat()
                        val piF = PI.toFloat()
                        
                        val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData
                        
                        val wordWobbles = FloatArray(words.size)
                        val sungFactors = FloatArray(effectiveWords.size)
                        val isWordSungs = BooleanArray(effectiveWords.size)

                        // 1. Single efficient loop for word metrics
                        for (w in effectiveWords.indices) {
                            val word = effectiveWords[w]
                            val startMs = word.startTime.toFloat() * 1000f
                            val endMs = word.endTime.toFloat() * 1000f
                            val timeSinceStart = smoothPositionF - startMs
                            
                            if (smoothPositionF > endMs) {
                                isWordSungs[w] = true
                                sungFactors[w] = 1f
                            } else if (smoothPositionF >= startMs) {
                                sungFactors[w] = (timeSinceStart / (endMs - startMs).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            }

                            val originalWordIdx = effectiveToOriginalIdx[w]
                            if (originalWordIdx != -1) {
                                val oStartMs = words[originalWordIdx].startTime.toFloat() * 1000f
                                val oTimeSince = smoothPositionF - oStartMs
                                if (oTimeSince in 0f..750f) {
                                    wordWobbles[originalWordIdx] = if (oTimeSince < 125f) oTimeSince / 125f else (1f - (oTimeSince - 125f) / 625f).coerceAtLeast(0f)
                                }
                            }
                        }

                        val lineCurrentPushes = FloatArray(layoutResult.lineCount)
                        val lineTotalPushes = FloatArray(layoutResult.lineCount)
                        
                        // 2. Pre-calculate layout pushes without allocating objects
                        for (i in 0 until clusterCount) {
                            val lineIdx = charLinesArray[i]
                            val wordIdx = wordIdxMap[i]
                            var charScaleX = 1f
                            
                            if (wordIdx != -1) {
                                val originalWordIdx = effectiveToOriginalIdx[wordIdx]
                                val sungFactor = sungFactors[wordIdx]
                                val isWordSung = isWordSungs[wordIdx]
                                val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                                
                                var crescendoDeltaX = 0f
                                val groupWord = hyphenGroupData[wordIdx]
                                if (groupWord != null) {
                                    val pOut = ((smoothPosition - groupWord.groupEndMs).toFloat() / 600f).coerceIn(0f, 1f)
                                    if (pOut > 0f) crescendoDeltaX = (groupWord.pos * 0.012f + 0.06f) * exp(-2.5f * pOut) * cos(10.0f * pOut * piF) * (1f - pOut)
                                    else if (groupWord.isLast) crescendoDeltaX = groupWord.pos * 0.012f + 0.06f * (1f - exp(-2.5f * sungFactor) * cos(10.0f * sungFactor * piF) * (1f - sungFactor))
                                    else crescendoDeltaX = (groupWord.pos * 0.012f) + if (sungFactor > 0f) 0.02f * (1f - sungFactor) else 0f
                                }

                                val wordItem = effectiveWords[wordIdx]
                                val wStart = wordItem.startTime.toFloat() * 1000f
                                val wEnd = wordItem.endTime.toFloat() * 1000f
                                val charLp = if (wordLenMap[i] > 0) {
                                    val dur = (wEnd - wStart).coerceAtLeast(100f)
                                    (((smoothPositionF - wStart) / dur - charInWordMap[i].toFloat() / wordLenMap[i].toFloat()) * wordLenMap[i].toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                val nudgeScale = if (!isWordSung && sungFactor > 0f) 0.038f * sin(charLp * piF) * exp(-3f * charLp) else 0f
                                charScaleX += (wobble * 0.025f) + crescendoDeltaX + (nudgeScale * 0.3f)
                            }
                            lineTotalPushes[lineIdx] += charBoundsArray[i].width * (charScaleX - 1f)
                        }

                        // 3. Draw characters highly optimized
                        for (i in 0 until clusterCount) {
                            val lineIdx = charLinesArray[i]
                            val charBounds = charBoundsArray[i]
                            val wordIdx = wordIdxMap[i]
                            val alignShift = when(alignment) { TextAlign.Center -> -lineTotalPushes[lineIdx] / 2f; TextAlign.Right -> -lineTotalPushes[lineIdx]; else -> 0f }
                            
                            var charScaleX = 1f
                            var charScaleY = 1f
                            var sungFactor = 0f
                            var isWordSung = false
                            var charLp = 0f
                            
                            if (wordIdx != -1) {
                                val originalWordIdx = effectiveToOriginalIdx[wordIdx]
                                sungFactor = sungFactors[wordIdx]
                                isWordSung = isWordSungs[wordIdx]
                                val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                                
                                val wordItem = effectiveWords[wordIdx]
                                val wStart = wordItem.startTime.toFloat() * 1000f
                                val wEnd = wordItem.endTime.toFloat() * 1000f
                                val dur = (wEnd - wStart).coerceAtLeast(100f)
                                
                                charLp = (((smoothPositionF - wStart) / dur - charInWordMap[i].toFloat() / wordLenMap[i].toFloat()) * wordLenMap[i].toFloat()).coerceIn(0f, 1f)

                                var crescendoDeltaX = 0f
                                var crescendoDeltaY = 0f
                                val groupWord = hyphenGroupData[wordIdx]
                                if (groupWord != null) {
                                    val pOut = ((smoothPosition - groupWord.groupEndMs).toFloat() / 600f).coerceIn(0f, 1f)
                                    if (pOut > 0f) { val spr = (groupWord.pos * 0.012f + 0.06f) * exp(-3.5f * pOut) * cos(5.0f * pOut * piF) * (1f - pOut); crescendoDeltaX = spr; crescendoDeltaY = spr }
                                    else if (groupWord.isLast) { val bas = groupWord.pos * 0.012f + 0.06f * (1f - exp(-3.5f * sungFactor) * cos(5.0f * sungFactor * piF) * (1f - sungFactor)); crescendoDeltaX = bas; crescendoDeltaY = bas }
                                    else { val bas = (groupWord.pos * 0.012f) + if (sungFactor > 0f) 0.02f * (1f - sungFactor) else 0f; crescendoDeltaX = bas; crescendoDeltaY = bas }
                                }

                                val nudgeScale = if (!isWordSung && sungFactor > 0f) 0.038f * sin(charLp * piF) * exp(-3f * charLp) else 0f
                                charScaleX += (wobble * 0.025f) + crescendoDeltaX + nudgeScale * 0.3f
                                charScaleY += (wobble * 0.015f) + crescendoDeltaY + nudgeScale
                            }

                            withTransform({
                                translate(left = alignShift + lineCurrentPushes[lineIdx] + charBounds.left, top = charBounds.top)
                                if (wordIdx != -1) scale(charScaleX, charScaleY, pivot = Offset(charBounds.width / 2f, charBounds.height))
                            }) {
                                if (wordIdx != -1 && !isWordSung && sungFactor > 0.001f) {
                                    val wordItem = effectiveWords[wordIdx]
                                    val wStart = wordItem.startTime.toFloat() * 1000f
                                    val wEnd = wordItem.endTime.toFloat() * 1000f
                                    val wordDur = wEnd - wStart
                                    
                                    val impactFactor = ((((wordDur / wordItem.text.length.coerceAtLeast(1)) - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((wordDur - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                                    
                                    if (impactFactor > 0.01f) {
                                        drawIntoCanvas { canvas ->
                                            glowPaint.color = expressiveAccent.copy(alpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)).toArgb()
                                            glowPaint.textSize = lyricStyle.fontSize.toPx()
                                            glowPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                            canvas.nativeCanvas.drawText(letterLayouts[i].layoutInput.text.text, 0f, letterLayouts[i].firstBaseline, glowPaint)
                                        }
                                    }
                                }
                                
                                val baseAlpha = if (isWordSung || charLp > 0.99f) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                                drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = if (wordIdx == -1) focusedAlpha else baseAlpha))
                                
                                if (wordIdx != -1 && !isWordSung && charLp > 0f && charLp < 1f) {
                                    val fXL = charBounds.width * charLp
                                    val eW = (charBounds.width * 0.45f).coerceAtLeast(1f)
                                    val sWL = (fXL - eW).coerceAtLeast(0f)
                                    
                                    if (sWL > 0f) clipRect(left = 0f, top = 0f, right = sWL, bottom = charBounds.height) { drawText(letterLayouts[i], color = expressiveAccent) }
                                    
                                    // Performance Fix: Reduced 12 slice loops to 4 for same soft edge but 3x faster rendering
                                    val steps = 4
                                    for (j in 0 until steps) {
                                        val start = sWL + (j * eW / steps)
                                        val end = (sWL + ((j + 1) * eW / steps) + 0.5f).coerceAtMost(fXL)
                                        if (end > start) clipRect(left = start, top = 0f, right = end, bottom = charBounds.height) { 
                                            drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = 1f - (j + 0.5f) / steps)) 
                                        }
                                    }
                                }
                            }
                            lineCurrentPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
                        }
                    }
                }
            }
        }
    }
}
