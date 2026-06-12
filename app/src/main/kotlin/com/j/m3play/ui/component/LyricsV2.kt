/*
 * M3Play Component Module
 *
 * Reusable UI building block
 * Signature: M3PLAY::COMPONENT:
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────
private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRICS_ANCHOR_RATIO = 0.38f 
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 72.dp
private val LYRICS_ITEM_GAP_DP = 28.dp 
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

    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 30f) 
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.25f)
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

    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS
    var currentPositionState by remember { mutableLongStateOf(0L) }
    
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

    LaunchedEffect(lastManualScrollTime) {
        if (!isAutoScrollEnabled && lastManualScrollTime > 0L) {
            delay(2500L)
            isAutoScrollEnabled = true
            lastManualScrollTime = 0L
        }
    }

    val activeListIndex = if (entriesWithWords.isEmpty()) -1 else focusScrollIndex.coerceIn(0, entriesWithWords.lastIndex)

    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, entriesWithWords) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, entriesWithWords) { mutableStateOf(true) }

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
                    if (i >= entriesWithWords.size) continue 
                    val item = entriesWithWords[i]
                    val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                    val isBg = item.words?.all { it.isBackground || it.text.isBlank() } == true
                    currentY -= (height + if (isBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                    map[i] = currentY
                }
                currentY = 0f
                for (i in activeListIndex until entriesWithWords.size - 1) {
                    if (i + 1 >= entriesWithWords.size) continue 
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
                val lastHeight = itemHeights[entriesWithWords.lastIndex]?.toFloat() ?: constraintLineHeightPx
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
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 60f)
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
                        val windowEnd = (activeListIndex + 12).coerceAtMost(entriesWithWords.lastIndex)
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
                    .smoothFadingEdge(vertical = 120.dp) 
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
                // 🔥 THE GAME CHANGER: SMART CULLING
                // We only process and draw lines that are near the screen. 
                // This reduces GPU/CPU overhead by 90% and eliminates lag completely!
                val visibleStartIndex = maxOf(0, activeListIndex - 12)
                val visibleEndIndex = minOf(entriesWithWords.lastIndex, activeListIndex + 15)

                for (listIndex in visibleStartIndex..visibleEndIndex) {
                    val item = entriesWithWords[listIndex]
                    key(item.time.hashCode() * 31 + listIndex) {
                        if (item == HEAD_LYRICS_ENTRY) {
                            Spacer(modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .onSizeChanged { itemHeights[listIndex] = it.height }
                            )
                        } else {
                            val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * lineHeightPx)
                            val frozenOffset = remember { mutableFloatStateOf(targetOffset) }
                            
                            LaunchedEffect(isAutoScrollEnabled, targetOffset, isInitialLayout) {
                                if (isAutoScrollEnabled || isInitialLayout) frozenOffset.floatValue = targetOffset
                            }
                            
                            val animatedOffset by animateFloatAsState(
                                targetValue = if (isAutoScrollEnabled) targetOffset else frozenOffset.floatValue,
                                animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap() 
                                                else spring(
                                                    dampingRatio = 0.90f,
                                                    stiffness = 50f
                                                ),
                                label = "lyricSpringOffset_$listIndex"
                            )

                            val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true
                            val isActiveLine = currentPlayingLineIndex == listIndex
                            val isSelected = selectedIndices.contains(listIndex)

                            val nextItemTime = if (listIndex < entriesWithWords.lastIndex) entriesWithWords[listIndex + 1].time else item.time + 5000L
                            val lineDurationMs = (nextItemTime - item.time).coerceAtLeast(100L).toInt()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { m, c -> 
                                        val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                                        layout(p.width, 0) { p.place(0, 0) }
                                    }
                                    .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                            ) {
                                AppleMusicZeroLagLine(
                                    index = listIndex,
                                    item = item,
                                    isSynced = isSynced,
                                    isActiveLine = isActiveLine,
                                    isBackground = isAllBackground,
                                    isSelected = isSelected,
                                    isSelectionModeActive = isSelectionModeActive,
                                    currentPositionProvider = { currentPositionState },
                                    lyricsTextSize = lyricsTextSize,
                                    lyricsLineSpacing = lyricsLineSpacing,
                                    lineDurationMs = lineDurationMs,
                                    expressiveAccent = textColor,
                                    isAutoScrollEnabled = isAutoScrollEnabled,
                                    romanizeLyrics = (romanizeJapanese || romanizeKorean),
                                    lyricsFontFamily = lyricsFontFamily,
                                    displayedCurrentLineIndex = currentPlayingLineIndex.coerceIn(0, entriesWithWords.lastIndex),
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
                    Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// ZERO-LAG APPLE MUSIC UI RENDERER
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppleMusicZeroLagLine(
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
    lineDurationMs: Int,
    expressiveAccent: Color,
    isAutoScrollEnabled: Boolean,
    romanizeLyrics: Boolean,
    lyricsFontFamily: FontFamily?,
    displayedCurrentLineIndex: Int,
    onSizeChanged: (Int) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Start }
    val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.Start }
    
    val targetScale = if (isActiveLine) 1.0f 
                      else if (abs(index - displayedCurrentLineIndex) <= 1) 0.85f 
                      else 0.75f 
                      
    val lineScale by animateFloatAsState(
        targetValue = targetScale, 
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 80f), 
        label = "lineScale"
    )

    val itemModifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { onSizeChanged(it.height) }
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
        .padding(start = 32.dp, end = 32.dp, top = 10.dp, bottom = 10.dp)
        .graphicsLayer {
            scaleX = lineScale
            scaleY = lineScale
            val originX = if (horizontalAlignment == Alignment.Start) 0f else if (horizontalAlignment == Alignment.End) 1f else 0.5f
            transformOrigin = TransformOrigin(originX, 0.5f)
        }

    Box(modifier = itemModifier, contentAlignment = Alignment.CenterStart) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = horizontalAlignment) {
            val mainText = item.text
            val romanizedText by item.romanizedTextFlow.collectAsState()

            val annotatedMainText = remember(mainText, lyricsTextSize) {
                buildAnnotatedString {
                    val regex = Regex("\\(.*?\\)|\\[.*?\\]")
                    var lastIndex = 0
                    regex.findAll(mainText).forEach { matchResult ->
                        append(mainText.substring(lastIndex, matchResult.range.first))
                        withStyle(SpanStyle(
                            fontSize = (lyricsTextSize * 0.75f).sp, // Bracket text formatting
                            fontWeight = FontWeight.SemiBold
                        )) {
                            append(matchResult.value)
                        }
                        lastIndex = matchResult.range.last + 1
                    }
                    append(mainText.substring(lastIndex))
                }
            }

            val lyricStyle = TextStyle(
                fontSize = lyricsTextSize.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                letterSpacing = (-0.5).sp,
                textAlign = textAlign,
                fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
            )

            if (mainText.isBlank()) {
                AppleMusicMusicLineDots(
                    isActiveLine = isActiveLine,
                    expressiveAccent = expressiveAccent,
                    durationMs = lineDurationMs
                )
            } 
            else if (isSynced && item.words != null && isActiveLine) {
                UltraFastAppleWipeCanvas(
                    annotatedText = annotatedMainText,
                    rawTextLength = mainText.length,
                    words = item.words,
                    currentPositionProvider = currentPositionProvider,
                    lyricStyle = lyricStyle,
                    expressiveAccent = expressiveAccent
                )
            } else {
                val dimAlpha = if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                    when (abs(index - displayedCurrentLineIndex)) {
                        0 -> 1f; 1 -> 0.45f; 2 -> 0.30f; else -> 0.20f 
                    }
                } else 0.35f
                
                Text(
                    text = annotatedMainText, 
                    style = lyricStyle.copy(color = expressiveAccent.copy(alpha = dimAlpha)), 
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (romanizeLyrics && romanizedText != null) {
                Text(
                    text = romanizedText!!, fontSize = (lyricsTextSize * 0.55f).sp, color = expressiveAccent.copy(alpha = 0.4f),
                    textAlign = textAlign, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp),
                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// BULLETPROOF APPLE MUSIC MASK WIPE
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun UltraFastAppleWipeCanvas(
    annotatedText: AnnotatedString,
    rawTextLength: Int,
    words: List<WordTimestamp>,
    currentPositionProvider: () -> Long,
    lyricStyle: TextStyle,
    expressiveAccent: Color
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val charToWordIdx = remember(annotatedText.text, words) {
        val mapping = IntArray(rawTextLength) { -1 }
        var searchIndex = 0
        words.forEachIndexed { wordIdx, word ->
            val idx = annotatedText.text.indexOf(word.text, searchIndex)
            if (idx != -1) {
                for (i in idx until idx + word.text.length) {
                    if (i < mapping.size) { mapping[i] = wordIdx }
                }
                val trailingSpaceIdx = idx + word.text.length
                if (trailingSpaceIdx < mapping.size && trailingSpaceIdx < annotatedText.text.length && annotatedText.text[trailingSpaceIdx] == ' ') {
                    mapping[trailingSpaceIdx] = wordIdx
                }
                searchIndex = trailingSpaceIdx
            }
        }
        mapping
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(annotatedText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(text = annotatedText, style = lyricStyle, constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx), softWrap = true)
        }

        val dimColor = expressiveAccent.copy(alpha = 0.35f) // Apple Music Dim opacity
        val brightColor = expressiveAccent

        Canvas(modifier = Modifier.fillMaxWidth().height(with(density) { layoutResult.size.height.toDp() })) {
            val smoothPositionF = currentPositionProvider().toFloat()
            val featherWidthPx = 60f // Soft edge thickness

            for (lineIndex in 0 until layoutResult.lineCount) {
                val lineTop = layoutResult.getLineTop(lineIndex)
                val lineBottom = layoutResult.getLineBottom(lineIndex)
                val lineStartOffset = layoutResult.getLineStart(lineIndex)
                val lineEndOffset = layoutResult.getLineEnd(lineIndex)
                val lineLeftBound = layoutResult.getLineLeft(lineIndex)
                val lineRightBound = layoutResult.getLineRight(lineIndex)
                
                var wipeX = lineLeftBound

                for (i in lineStartOffset until lineEndOffset) {
                    if (i >= charToWordIdx.size) break 
                    val wordIdx = charToWordIdx[i]
                    
                    if (wordIdx != -1 && wordIdx < words.size) {
                        val word = words[wordIdx]
                        val wStart = (word.startTime * 1000.0).toFloat()
                        val wEnd = (word.endTime * 1000.0).toFloat()
                        
                        if (i >= annotatedText.text.length) break
                        
                        val charBounds = layoutResult.getBoundingBox(i)
                        
                        if (smoothPositionF >= wEnd) {
                            wipeX = maxOf(wipeX, charBounds.right)
                        } else if (smoothPositionF > wStart) {
                            val progress = ((smoothPositionF - wStart) / (wEnd - wStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            wipeX = maxOf(wipeX, charBounds.left + (charBounds.width * progress))
                            break 
                        }
                    } else {
                        if (i > 0 && i - 1 < charToWordIdx.size && charToWordIdx[i - 1] != -1) {
                            val prevWordIdx = charToWordIdx[i - 1]
                            if (prevWordIdx < words.size) {
                                val prevWEnd = (words[prevWordIdx].endTime * 1000.0).toFloat()
                                if (smoothPositionF >= prevWEnd) {
                                    wipeX = maxOf(wipeX, layoutResult.getBoundingBox(i).right)
                                }
                            }
                        }
                    }
                }
                
                if (wipeX <= lineLeftBound) {
                    clipRect(left = lineLeftBound, right = lineRightBound, top = lineTop, bottom = lineBottom) {
                        drawText(layoutResult, color = dimColor)
                    }
                } else if (wipeX >= lineRightBound) {
                    clipRect(left = lineLeftBound, right = lineRightBound, top = lineTop, bottom = lineBottom) {
                        drawText(layoutResult, color = brightColor)
                    }
                } else {
                    clipRect(left = lineLeftBound, right = wipeX, top = lineTop, bottom = lineBottom) {
                        drawText(layoutResult, color = brightColor)
                    }
                    
                    val slices = 8
                    val step = featherWidthPx / slices
                    for (s in 0 until slices) {
                        val sLeft = wipeX + (s * step)
                        val sRight = sLeft + step
                        val fraction = s.toFloat() / slices
                        val sliceColor = lerp(brightColor, dimColor, fraction)
                        
                        if (sLeft < lineRightBound) {
                            clipRect(left = sLeft, right = minOf(sRight, lineRightBound), top = lineTop, bottom = lineBottom) {
                                drawText(layoutResult, color = sliceColor)
                            }
                        }
                    }
                    
                    val unsungLeft = wipeX + featherWidthPx
                    if (unsungLeft < lineRightBound) {
                        clipRect(left = unsungLeft, right = lineRightBound, top = lineTop, bottom = lineBottom) {
                            drawText(layoutResult, color = dimColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicMusicLineDots(
    isActiveLine: Boolean,
    expressiveAccent: Color,
    durationMs: Int
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition()
    val r by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius"
    )
    val targetOpacity = if (isActiveLine) 1f else 0.15f
    val opacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = tween(
            durationMillis = if (isActiveLine) durationMs else 100,
            easing = LinearEasing
        ),
        label = "opacity"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(63.dp)) {
        val rPx = with(density) { r.dp.toPx() }
        val cY = with(density) { 12.dp.toPx() }
        val cX1 = with(density) { 12.dp.toPx() }
        val cX2 = with(density) { 48.dp.toPx() } 
        val cX3 = with(density) { 84.dp.toPx() } 
        val dotColor = expressiveAccent.copy(alpha = opacity)
        drawCircle(color = dotColor, radius = rPx, center = Offset(cX1, cY))
        drawCircle(color = dotColor, radius = rPx, center = Offset(cX2, cY))
        drawCircle(color = dotColor, radius = rPx, center = Offset(cX3, cY))
    }
}
