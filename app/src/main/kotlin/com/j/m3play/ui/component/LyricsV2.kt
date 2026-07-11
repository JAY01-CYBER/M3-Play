/*
 * M3Play Component Module - Premium Edition
 * Signature: M3PLAY::COMPONENT::PREMIUM
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.roundToInt

private const val LRC_LEAD_MS = 300L
private const val TTML_AUDIO_LATENCY_OFFSET_MS = 280L 
private const val LYRICS_ANCHOR_RATIO = 0.40f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 72.dp
private val LYRICS_ITEM_GAP_DP = 32.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 15
private const val LYRICS_STAGGER_DELAY_MAX_MS = 150
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

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
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 34f)
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

    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics
    val isSynced = remember(lyrics) { lyrics != null && (lyrics.startsWith("[") || isTtml(lyrics)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics, currentLyrics?.provider) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else {
            val parsed = when {
                isTtml(lyrics) -> parseTtml(lyrics)
                lyrics.startsWith("[") -> parseLyrics(lyrics)
                else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { _, line -> LyricsEntry(time = -1L, text = line.trim()) }
            }
            val providerEntry = LyricsEntry(time = 0L, text = "✨") // Minimalist aesthetic
            if (parsed.isNotEmpty() && parsed.first().time >= 0) listOf(HEAD_LYRICS_ENTRY, providerEntry) + parsed else listOf(providerEntry) + parsed
        }
    }

    val entriesWithWords: List<LyricsEntry> = remember(lyricsEntries) {
        if (lyricsEntries.isEmpty()) { emptyList() } else {
            lyricsEntries.mapIndexed { index, entry ->
                if (entry.words != null || entry.time < 0 || entry.text.isBlank()) { entry } else {
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
                            if (c.isBlank()) { if (groupedTokens.isNotEmpty()) groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c } else { groupedTokens.add(c) }
                        }
                        groupedTokens
                    } else { entry.text.split(Regex("\\s+")) }
                    
                    if (tokens.isEmpty()) { entry } else {
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

    val hasWordTimings = remember(entriesWithWords) { entriesWithWords.any { it.words != null } }
    val leadMs = if (isTtmlFormat || hasWordTimings) TTML_AUDIO_LATENCY_OFFSET_MS else LRC_LEAD_MS
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
                withFrameMillis { _ ->
                    val sliderPos = sliderPositionProvider()
                    isSeeking = sliderPos != null
                    val pos = sliderPos ?: player.currentPosition
                    val visualTuningOffsetMs = if (isTtmlFormat || hasWordTimings) 30L else 150L 
                    currentPositionState = pos + leadMs + visualTuningOffsetMs
                    val currentLineIdx = findCurrentLineIndex(entriesWithWords, currentPositionState, 0L)
                    if (currentLineIdx != -1) {
                        if (currentLineIdx != currentPlayingLineIndex) currentPlayingLineIndex = currentLineIdx
                        if ((isAutoScrollEnabled || isSeeking) && currentLineIdx != focusScrollIndex) focusScrollIndex = currentLineIdx
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

    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, entriesWithWords) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, entriesWithWords) { mutableStateOf(true) }
    val activeListIndex = focusScrollIndex.coerceAtLeast(0)

    BoxWithConstraints(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize().padding(bottom = 12.dp)) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * LYRICS_ANCHOR_RATIO
        val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }
        
        val positions = remember(itemHeights.toMap(), activeListIndex, entriesWithWords) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex != -1 && entriesWithWords.isNotEmpty()) {
                map[activeListIndex] = 0f
                var currentY = 0f
                for (i in activeListIndex - 1 downTo 0) {
                    val item = entriesWithWords[i]; val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                    val isBg = item.words?.all { it.isBackground || it.text.isBlank() } == true
                    currentY -= (height + if (isBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }); map[i] = currentY
                }
                currentY = 0f
                for (i in activeListIndex until entriesWithWords.size - 1) {
                    val nextItem = entriesWithWords[i + 1]; val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                    val isNextBg = nextItem.words?.all { it.isBackground || it.text.isBlank() } == true
                    currentY += (height + if (isNextBg) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }); map[i + 1] = currentY
                }
            }
            map
        }

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
                            flingJob?.cancel(); velocityTracker.resetTracking()
                            isAutoScrollEnabled = false; lastManualScrollTime = System.currentTimeMillis()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            verticalDrag(down.id) { change ->
                                userManualOffset = (userManualOffset + change.positionChange().y).coerceIn(-5000f, 5000f) // Simplified clamp
                                velocityTracker.addPosition(change.uptimeMillis, change.position); lastManualScrollTime = System.currentTimeMillis(); change.consume()
                            }
                            val velocity = velocityTracker.calculateVelocity().y
                            flingJob = scope.launch {
                                AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) { userManualOffset = value }
                            }
                        }
                    }
                }
        ) {
            entriesWithWords.forEachIndexed { listIndex, item ->
                key(item.time.hashCode() * 31 + listIndex) {
                    if (item == HEAD_LYRICS_ENTRY) {
                        Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).onSizeChanged { itemHeights[listIndex] = it.height })
                    } else {
                        val distance = abs(listIndex - activeListIndex)
                        val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * lineHeightPx)
                        
                        //  SPRING PHYSICS SCROLLING (Premium Apple Feel)
                        val animatedOffset by animateFloatAsState(
                            targetValue = if (isAutoScrollEnabled) targetOffset else (targetOffset + userManualOffset),
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
                            label = "lyricSpringOffset_$listIndex"
                        )

                        val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true
                        val isActiveLine = currentPlayingLineIndex == listIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .layout { m, c -> val p = m.measure(c.copy(maxHeight = Constraints.Infinity)); layout(p.width, 0) { p.place(0, 0) } }
                                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                        ) {
                            AppleMusicLyricsLine(
                                index = listIndex, item = item, isSynced = isSynced, isActiveLine = isActiveLine,
                                isBackground = isAllBackground, isSelected = selectedIndices.contains(listIndex),
                                isSelectionModeActive = isSelectionModeActive, currentPositionProvider = { currentPositionState },
                                lyricsTextSize = lyricsTextSize, lyricsLineSpacing = lyricsLineSpacing,
                                expressiveAccent = textColor, isAutoScrollEnabled = isAutoScrollEnabled,
                                displayedCurrentLineIndex = currentPlayingLineIndex,
                                romanizeLyrics = (romanizeJapanese || romanizeKorean),
                                lyricsFontFamily = lyricsFontFamily, onSizeChanged = { itemHeights[listIndex] = it },
                                onClick = { /* logic */ }, onLongClick = { /* logic */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Apple Music Exact Engine (NATIVE PATH CLIPPING)
// ──────────────────────────────────────────────────────────────────────

@Composable
internal fun AppleMusicLyricsLine(
    index: Int, item: LyricsEntry, isSynced: Boolean, isActiveLine: Boolean, isBackground: Boolean,
    isSelected: Boolean, isSelectionModeActive: Boolean, currentPositionProvider: () -> Long,
    lyricsTextSize: Float, lyricsLineSpacing: Float, expressiveAccent: Color, isAutoScrollEnabled: Boolean,
    displayedCurrentLineIndex: Int, romanizeLyrics: Boolean, lyricsFontFamily: FontFamily?,
    onSizeChanged: (Int) -> Unit, onClick: () -> Unit, onLongClick: () -> Unit,
) {
    val targetScale = if (isActiveLine) 1.0f else 0.85f
    val lineScale by animateFloatAsState(targetScale, spring(dampingRatio = 0.9f, stiffness = 200f), label = "lineScale")
    
    val itemModifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { onSizeChanged(it.height) }
        .padding(horizontal = 32.dp, vertical = 12.dp)
        .graphicsLayer {
            scaleX = lineScale; scaleY = lineScale
            alpha = if (isActiveLine) 1f else 0.4f
        }

    Box(modifier = itemModifier, contentAlignment = Alignment.CenterStart) {
        val lyricStyle = TextStyle(
            fontSize = lyricsTextSize.sp, fontWeight = FontWeight.Black, // Apple uses heavy weights
            lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
            letterSpacing = (-1.0).sp, //  TIGHT KERNING (The Premium Feel)
            textAlign = TextAlign.Start, fontFamily = lyricsFontFamily,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )

        if (isSynced && item.words != null && isActiveLine && item.text.isNotBlank()) {
            AppleMusicNativeClipCanvas(item.text, item.words, currentPositionProvider, lyricStyle, expressiveAccent)
        } else {
            Text(text = item.text, style = lyricStyle.copy(color = if (isActiveLine) expressiveAccent else expressiveAccent.copy(alpha = 0.4f)))
        }
    }
}

@Composable
private fun AppleMusicNativeClipCanvas(
    mainText: String, words: List<WordTimestamp>, currentPositionProvider: () -> Long,
    lyricStyle: TextStyle, expressiveAccent: Color
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val charToWordIdx = remember(mainText, words) {
        val mapping = IntArray(mainText.length) { -1 }
        var searchIndex = 0
        words.forEachIndexed { wordIdx, word ->
            val idx = mainText.indexOf(word.text, searchIndex)
            if (idx != -1) {
                for (i in idx until idx + word.text.length) mapping[i] = wordIdx
                searchIndex = idx + word.text.length
            }
        }
        mapping
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layoutResult = remember(mainText, constraints.maxWidth, lyricStyle) {
            textMeasurer.measure(text = mainText, style = lyricStyle, constraints = constraints)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(with(density) { layoutResult.size.height.toDp() })) {
            drawText(layoutResult, color = expressiveAccent.copy(alpha = 0.3f))
            val smoothPositionF = currentPositionProvider().toFloat()
            val highlightPath = Path()

            for (lineIndex in 0 until layoutResult.lineCount) {
                val lineTop = layoutResult.getLineTop(lineIndex)
                val lineBottom = layoutResult.getLineBottom(lineIndex)
                val lineStart = layoutResult.getLineStart(lineIndex)
                val lineEnd = layoutResult.getLineEnd(lineIndex)
                var lineWipeX = layoutResult.getLineLeft(lineIndex)

                for (i in lineStart until lineEnd) {
                    if (i >= charToWordIdx.size) break
                    val wordIdx = charToWordIdx[i]
                    if (wordIdx != -1) {
                        val word = words[wordIdx]
                        val wStart = (word.startTime * 1000.0).toFloat()
                        val wEnd = (word.endTime * 1000.0).toFloat()
                        if (smoothPositionF >= wEnd) {
                            lineWipeX = maxOf(lineWipeX, layoutResult.getBoundingBox(i).right)
                        } else if (smoothPositionF > wStart) {
                            val rawProgress = ((smoothPositionF - wStart) / (wEnd - wStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val progress = rawProgress * rawProgress * (3f - 2f * rawProgress)
                            val b = layoutResult.getBoundingBox(i)
                            lineWipeX = maxOf(lineWipeX, b.left + (b.right - b.left) * progress)
                            break
                        }
                    }
                }
                if (lineWipeX > layoutResult.getLineLeft(lineIndex)) {
                    highlightPath.addRect(Rect(layoutResult.getLineLeft(lineIndex), lineTop, lineWipeX, lineBottom))
                }
            }
            clipPath(highlightPath) { drawText(layoutResult, color = expressiveAccent) }
        }
    }
}
