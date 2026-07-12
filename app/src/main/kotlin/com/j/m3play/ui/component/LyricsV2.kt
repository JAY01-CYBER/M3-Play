/*
 * M3Play Component Module - Premium Flow Edition (ArchiveTune Clone)
 * Signature: M3PLAY::COMPONENT::PREMIUM::FLOW
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.LyricsClickKey
import com.j.m3play.constants.LyricsLineSpacingKey
import com.j.m3play.constants.LyricsRomanizeJapaneseKey
import com.j.m3play.constants.LyricsRomanizeKoreanKey
import com.j.m3play.constants.LyricsTextSizeKey
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

private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRIC_VISUAL_TUNING_OFFSET_MS = 150L
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

// Hardcoded ArchiveTune premium visuals configurations
private const val BOUNCE_FACTOR = 1f
private const val GLOW_FACTOR = 0f 
private const val FILL_TRANSITION_WIDTH = 8f

private fun isRtlText(text: String): Boolean {
    for (ch in text) {
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true
            Character.DIRECTIONALITY_LEFT_TO_RIGHT,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return false
        }
    }
    return false
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 28f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)

    val lyricsFontFamily = remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White
    }

    val inactiveAlpha = 0.35f
    
    // Selection state & variables added back
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val maxSelectionLimit = 5
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
            showMaxSelectionToast = false
        }
    }

    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics

    val isSynced = remember(lyrics) { lyrics != null && (lyrics.startsWith("[") || isTtml(lyrics)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) return@remember emptyList()
        val parsed = when {
            isTtml(lyrics) -> parseTtml(lyrics)
            lyrics.startsWith("[") -> parseLyrics(lyrics)
            else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { _, line -> LyricsEntry(time = -1L, text = line.trim()) }
        }
        if (parsed.isNotEmpty() && parsed.first().time >= 0) listOf(HEAD_LYRICS_ENTRY) + parsed else parsed
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

    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(entriesWithWords, isSynced) {
        if (!isSynced || entriesWithWords.isEmpty()) return@LaunchedEffect
        val pollIntervalMs = if (isTtmlFormat) 16L else 50L
        while (isActive) {
            val sliderPos = sliderPositionProvider()
            val pos = sliderPos ?: player.currentPosition

            playbackPositionMs = pos.coerceAtLeast(0L)
            currentPositionMs = (playbackPositionMs + leadMs + LYRIC_VISUAL_TUNING_OFFSET_MS).coerceAtLeast(0L)

            currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionMs, 0L)
            delay(pollIntervalMs)
        }
    }

    val listState = rememberLazyListState()
    var isManualScrolling by remember { mutableStateOf(false) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isSelectionModeActive && source == NestedScrollSource.UserInput) {
                    isManualScrolling = true
                    lastManualScrollTime = System.currentTimeMillis()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(isManualScrolling, lastManualScrollTime) {
        if (isManualScrolling) {
            delay(MANUAL_SCROLL_TIMEOUT_MS)
            isManualScrolling = false
        }
    }

    LaunchedEffect(currentLineIndex, isManualScrolling) {
        if (isManualScrolling || !isSynced) return@LaunchedEffect
        if (currentLineIndex < 0 || currentLineIndex >= entriesWithWords.size) return@LaunchedEffect

        val viewportHeight = listState.layoutInfo.viewportSize.height
        val targetOffset = (viewportHeight * 0.35f).toInt()

        val distance = abs(currentLineIndex - listState.firstVisibleItemIndex)
        if (distance > 15) {
            listState.scrollToItem((currentLineIndex - 2).coerceAtLeast(0), 0)
        }
        listState.animateScrollToItem(index = currentLineIndex, scrollOffset = -targetOffset)
    }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BoxWithConstraints(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize().padding(bottom = 12.dp)) {
        if (lyrics == LYRICS_NOT_FOUND || entriesWithWords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.6f))
            }
            return@BoxWithConstraints
        }

        if (lyrics == null) {
            ShimmerHost { repeat(6) { TextPlaceholder() } }
            return@BoxWithConstraints
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .smoothFadingEdge(vertical = 80.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(
                items = entriesWithWords,
                key = { index, entry -> "${index}_${entry.time}" }
            ) { index, item ->
                if (item == HEAD_LYRICS_ENTRY) {
                    Spacer(modifier = Modifier.height(120.dp))
                    return@itemsIndexed
                }

                val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Start }
                val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.Start }

                val isActive = isSynced && index == currentLineIndex
                val isPast = isSynced && index < currentLineIndex
                val isSelected = selectedIndices.contains(index)
                val distanceFromActive = if (isSynced) abs(index - currentLineIndex) else 0
                
                val lineAlpha = when {
                    !isSynced -> 0.92f
                    isActive -> 1f
                    isManualScrolling -> when {
                        distanceFromActive == 1 -> 0.72f
                        distanceFromActive == 2 -> 0.56f
                        distanceFromActive == 3 -> 0.40f
                        else -> 0.28f
                    }
                    distanceFromActive == 1 -> 0.52f
                    distanceFromActive == 2 -> 0.30f
                    distanceFromActive == 3 -> 0.18f
                    else -> 0.10f
                }
                
                val animatedLineScale by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.85f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "v2LineScale"
                )
                
                val animatedLineAlpha by animateFloatAsState(
                    targetValue = lineAlpha,
                    animationSpec = tween(durationMillis = if (isActive) 330 else 500, easing = FastOutSlowInEasing),
                    label = "v2LineAlpha"
                )
                
                val lineTransformOrigin = remember(item.agent) {
                    when (item.agent?.lowercase()) {
                        "v2" -> androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                        "v1", null -> androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        else -> androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    }
                }

                val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true
                val baseLayoutDirection = LocalLayoutDirection.current
                val lineText = remember(item.text, item.words) { item.words?.joinToString("") { it.text }?.takeIf { it.isNotBlank() } ?: item.text }
                val lineIsRtl = remember(lineText) { isRtlText(lineText) }
                val lineLayoutDirection = remember(lineIsRtl, baseLayoutDirection) { if (lineIsRtl) LayoutDirection.Rtl else baseLayoutDirection }

                CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                            .padding(
                                start = if (isAllBackground) 24.dp else 24.dp,
                                end = 24.dp,
                                top = if (index == 0 || (index == 1 && entriesWithWords[0] == HEAD_LYRICS_ENTRY)) 0.dp else (lyricsLineSpacing * 8).dp,
                                bottom = (lyricsLineSpacing * 8).dp
                            )
                            .graphicsLayer {
                                scaleX = animatedLineScale
                                scaleY = animatedLineScale
                                alpha = animatedLineAlpha
                                transformOrigin = lineTransformOrigin
                            }
                            .combinedClickable(
                                enabled = true,
                                onClick = {
                                    if (isSelectionModeActive) {
                                        if (isSelected) {
                                            selectedIndices.remove(index)
                                            if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                        } else {
                                            if (selectedIndices.size < maxSelectionLimit) {
                                                selectedIndices.add(index)
                                            } else {
                                                showMaxSelectionToast = true
                                            }
                                        }
                                    } else if (lyricsClick && isSynced && item.time > 0) {
                                        player.seekTo(item.time)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionModeActive) {
                                        isSelectionModeActive = true
                                        selectedIndices.add(index)
                                    } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                        selectedIndices.add(index)
                                    } else if (!isSelected) {
                                        showMaxSelectionToast = true
                                    }
                                }
                            ),
                        horizontalAlignment = horizontalAlignment,
                    ) {
                        val romanizedText = item.romanizedTextFlow.collectAsState().value
                        val supplementaryTextStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (lyricsTextSize * 0.55f).sp,
                            lineHeight = (lyricsTextSize * 0.75f).sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                            fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily
                        )

                        if (romanizedText != null) {
                            Text(text = romanizedText, style = supplementaryTextStyle, color = textColor.copy(alpha = if (isActive) 0.76f else 0.42f), textAlign = textAlign, modifier = Modifier.fillMaxWidth().padding(bottom = (lyricsTextSize * 0.18f).dp))
                        }

                        if (item.words != null && isSynced) {
                            LyricsLineV2(
                                words = item.words!!, isActive = isActive, isPast = isPast, currentPositionMs = currentPositionMs,
                                textColor = textColor, inactiveAlpha = inactiveAlpha, baseFontSize = lyricsTextSize,
                                isLineAllBackground = isAllBackground, textAlign = textAlign, lyricsFontFamily = lyricsFontFamily,
                                isRtl = lineIsRtl, bounceFactor = BOUNCE_FACTOR, glowFactor = GLOW_FACTOR, fillTransitionWidth = FILL_TRANSITION_WIDTH
                            )
                        } else if (isSynced) {
                            LyricsLineLrcBounce(
                                text = item.text, isActive = isActive, textColor = textColor.copy(alpha = if (isActive) 1f else 0.52f),
                                fontSize = lyricsTextSize, lineSpacing = lyricsLineSpacing, isAllBackground = isAllBackground,
                                lyricsFontFamily = lyricsFontFamily, textAlign = textAlign, bounceFactor = BOUNCE_FACTOR
                            )
                        } else {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                                    letterSpacing = (-1.0).sp 
                                ),
                                color = textColor.copy(alpha = if (isActive) 1f else 0.52f),
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(300.dp)) }
        }

        if (isManualScrolling && isSynced) {
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    isManualScrolling = false
                    scope.launch { listState.animateScrollToItem(index = currentLineIndex, scrollOffset = -(listState.layoutInfo.viewportSize.height * 0.35f).toInt()) }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                shapes = ButtonDefaults.shapes()
            ) { Text(text = "Resume", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineV2(
    words: List<WordTimestamp>, isActive: Boolean, isPast: Boolean, currentPositionMs: Long, textColor: Color,
    inactiveAlpha: Float, baseFontSize: Float, isLineAllBackground: Boolean, textAlign: TextAlign,
    lyricsFontFamily: FontFamily?, isRtl: Boolean, bounceFactor: Float, glowFactor: Float, fillTransitionWidth: Float
) {
    val arrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }
    val mainWords = words.filter { !it.isBackground }
    val bgWords = words.filter { it.isBackground }

    if (mainWords.isNotEmpty()) {
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
            mainWords.forEachIndexed { wordIndex, word ->
                if (word.text == " " || word.text == "\n") {
                    Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = baseFontSize.sp), color = Color.Transparent)
                    return@forEachIndexed
                }
                AnimatedWordV2(
                    word = word, wordIndex = wordIndex, isLineActive = isActive, isLinePast = isPast, currentPositionMs = currentPositionMs,
                    textColor = textColor, inactiveAlpha = inactiveAlpha, fontSize = if (isLineAllBackground) baseFontSize * 0.82f else baseFontSize,
                    isBackground = isLineAllBackground, lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, bounceFactor = bounceFactor, glowFactor = glowFactor, fillTransitionWidth = fillTransitionWidth
                )
            }
        }
    }

    if (bgWords.isNotEmpty()) {
        if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
        FlowRow(modifier = Modifier.fillMaxWidth().alpha(0.85f), horizontalArrangement = arrangement) {
            bgWords.forEachIndexed { wordIndex, word ->
                if (word.text == " " || word.text == "\n") {
                    Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = (baseFontSize * 0.65f).sp), color = Color.Transparent)
                    return@forEachIndexed
                }
                AnimatedWordV2(
                    word = word, wordIndex = wordIndex + mainWords.size, isLineActive = isActive, isLinePast = isPast, currentPositionMs = currentPositionMs,
                    textColor = textColor, inactiveAlpha = inactiveAlpha, fontSize = baseFontSize * 0.65f, isBackground = true,
                    lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, bounceFactor = bounceFactor, glowFactor = glowFactor, fillTransitionWidth = fillTransitionWidth
                )
            }
        }
    }
}

@Composable
private fun AnimatedWordV2(
    word: WordTimestamp, wordIndex: Int, isLineActive: Boolean, isLinePast: Boolean, currentPositionMs: Long,
    textColor: Color, inactiveAlpha: Float, fontSize: Float, isBackground: Boolean, lyricsFontFamily: FontFamily?,
    isRtl: Boolean, bounceFactor: Float, glowFactor: Float, fillTransitionWidth: Float
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)
    val isWordComplete = currentPositionMs >= wordEndMs
    val isWordActive = currentPositionMs in wordStartMs until wordEndMs

    val progress = when {
        isWordComplete -> 1f
        currentPositionMs <= wordStartMs -> 0f
        else -> ((currentPositionMs - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    val sinProgress = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
    val wordScale = 1f + (0.015f * bounceFactor * sinProgress)
    val targetFloat = if (isWordActive) -4f * bounceFactor * sinProgress else 0f
    
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(durationMillis = if (isWordActive) 50 else 350, easing = FastOutSlowInEasing),
        label = "v2FloatOffset"
    )

    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize
    val fontWeight = if (isLineActive || isLinePast) FontWeight.Black else FontWeight.ExtraBold

    Box(modifier = Modifier.graphicsLayer { clip = false; translationY = floatOffset * density; scaleX = wordScale; scaleY = wordScale }) {
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = actualFontSize.sp, fontWeight = fontWeight, fontStyle = FontStyle.Normal,
                lineHeight = (actualFontSize * 1.35f).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                letterSpacing = (-1.0).sp
            ),
            color = textColor.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha)
        )

        if (isWordComplete || isWordActive || isLinePast) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = actualFontSize.sp, fontWeight = fontWeight, fontStyle = FontStyle.Normal,
                    lineHeight = (actualFontSize * 1.35f).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                    letterSpacing = (-1.0).sp
                ),
                color = textColor.copy(alpha = if (isBackground) 0.75f else 1f),
                modifier = if (isWordActive && !isWordComplete) {
                    Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                        drawContent()
                        val edgeWidth = fillTransitionWidth.dp.toPx()
                        val center = if (isRtl) size.width - ((size.width + edgeWidth * 2) * progress - edgeWidth) else (size.width + edgeWidth * 2) * progress - edgeWidth
                        drawRect(brush = Brush.horizontalGradient(colors = if (isRtl) listOf(Color.Transparent, Color.Black) else listOf(Color.Black, Color.Transparent), startX = center - edgeWidth, endX = center + edgeWidth), blendMode = BlendMode.DstIn)
                    }
                } else Modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineLrcBounce(
    text: String, isActive: Boolean, textColor: Color, fontSize: Float, lineSpacing: Float,
    isAllBackground: Boolean, lyricsFontFamily: FontFamily?, textAlign: TextAlign, bounceFactor: Float
) {
    val words = remember(text) { text.split(Regex("(?<=\\s)")) }
    val effectiveFontSize = if (isAllBackground) fontSize * 0.82f else fontSize
    val fontWeight = if (isActive) FontWeight.Black else FontWeight.ExtraBold
    val fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal
    val scaleAnimatables = remember(words.size) { List(words.size) { Animatable(1f) } }
    val floatAnimatables = remember(words.size) { List(words.size) { Animatable(0f) } }

    LaunchedEffect(isActive) {
        if (!isActive || bounceFactor == 0f) return@LaunchedEffect
        words.indices.forEach { i ->
            launch {
                delay(i * 40L)
                try {
                    scaleAnimatables[i].animateTo(targetValue = 1f + 0.045f * bounceFactor, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    scaleAnimatables[i].animateTo(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                } finally { withContext(NonCancellable) { scaleAnimatables[i].snapTo(1f) } }
            }
            launch {
                delay(i * 40L)
                try {
                    floatAnimatables[i].animateTo(targetValue = -5f * bounceFactor, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    floatAnimatables[i].animateTo(targetValue = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                } finally { withContext(NonCancellable) { floatAnimatables[i].snapTo(0f) } }
            }
        }
    }

    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }) {
        words.forEachIndexed { i, word ->
            Text(
                text = word,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = effectiveFontSize.sp, fontWeight = fontWeight, fontStyle = fontStyle, lineHeight = (effectiveFontSize * lineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-1.0).sp),
                color = textColor,
                modifier = Modifier.graphicsLayer { scaleX = scaleAnimatables[i].value; scaleY = scaleAnimatables[i].value; translationY = floatAnimatables[i].value }
            )
        }
    }
}
