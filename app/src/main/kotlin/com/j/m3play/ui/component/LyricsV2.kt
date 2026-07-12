/*
 * M3Play Component Module 
 * Signature: M3PLAY::COMPONENT:
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.j.m3play.ui.component.share.LyricsShareImageDialog
import com.j.m3play.ui.component.share.LyricsSharePayload
import com.j.m3play.ui.component.share.shareLyricsAsText

// Accord Core Constants
private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRIC_VISUAL_TUNING_OFFSET_MS = 150L
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

// Accord Visual Constants
private const val ACCORD_ACTIVE_ALPHA = 1f
private const val ACCORD_INACTIVE_ALPHA = 0.2f
private const val ACCORD_ACTIVE_SCALE = 1f
private const val ACCORD_INACTIVE_SCALE = 0.96f
private const val ACCORD_MAX_BLUR = 8f
private const val ACCORD_BLUR_STEP = 2f
private val AccordDecelerateEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

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
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 34f)
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
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val maxSelectionLimit = 5
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showShareImageDialog by remember { mutableStateOf(false) }

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

    val lyricsEntries: List<LyricsEntry> = remember(lyrics, currentLyrics?.provider) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) return@remember emptyList()
        val parsed = when {
            isTtml(lyrics) -> parseTtml(lyrics)
            lyrics.startsWith("[") -> parseLyrics(lyrics)
            else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { _, line -> LyricsEntry(time = -1L, text = line.trim()) }
        }
        
        val providerName = currentLyrics?.provider?.uppercase() ?: "UNKNOWN"
        val providerEntry = LyricsEntry(time = 0L, text = "✨ Provided by $providerName")

        if (parsed.isNotEmpty() && parsed.first().time >= 0) {
            listOf(HEAD_LYRICS_ENTRY, providerEntry) + parsed
        } else {
            listOf(HEAD_LYRICS_ENTRY, providerEntry) + parsed
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
                key = { index, entry -> "${index}_${entry.time}_${entry.text.hashCode()}" }
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
                
                val targetAlpha = when {
                    !isSynced -> 0.92f
                    isActive -> ACCORD_ACTIVE_ALPHA
                    isManualScrolling -> 0.4f
                    else -> ACCORD_INACTIVE_ALPHA
                }
                
                val targetScale = if (isActive) ACCORD_ACTIVE_SCALE else ACCORD_INACTIVE_SCALE
                
                val targetBlur = when {
                    !isSynced || isActive || (isSelectionModeActive && isSelected) || isManualScrolling -> 0f
                    else -> (distanceFromActive * ACCORD_BLUR_STEP).coerceAtMost(ACCORD_MAX_BLUR)
                }

                val targetOffsetY = when {
                    !isSynced -> 0.dp
                    isActive -> 0.dp
                    index > currentLineIndex -> 16.dp 
                    else -> (-8).dp 
                }

                val animatedLineScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(durationMillis = 500, easing = AccordDecelerateEasing),
                    label = "accordLineScale"
                )
                
                val animatedLineAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 500, easing = AccordDecelerateEasing),
                    label = "accordLineAlpha"
                )

                val animatedBlur by animateFloatAsState(
                    targetValue = targetBlur,
                    animationSpec = tween(durationMillis = 500, easing = AccordDecelerateEasing),
                    label = "accordLineBlur"
                )

                val animatedLineOffsetY by animateDpAsState(
                    targetValue = targetOffsetY,
                    animationSpec = tween(durationMillis = 500, easing = AccordDecelerateEasing),
                    label = "appleLineGlide"
                )
                
                val lineTransformOrigin = remember(item.agent) {
                    when (item.agent?.lowercase()) {
                        "v2" -> TransformOrigin(1f, 0.5f)
                        "v1", null -> TransformOrigin(0f, 0.5f)
                        else -> TransformOrigin(0.5f, 0.5f)
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
                                start = 32.dp,
                                end = 32.dp,
                                top = if (index <= 1) 0.dp else 14.dp,
                                bottom = 14.dp
                            )
                            .blur(
                                radiusX = animatedBlur.dp,
                                radiusY = animatedBlur.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                            .graphicsLayer {
                                scaleX = animatedLineScale
                                scaleY = animatedLineScale
                                alpha = animatedLineAlpha
                                translationY = animatedLineOffsetY.toPx() 
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
                                            if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index) else showMaxSelectionToast = true
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

                        val currentFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                        if (item.words != null && isSynced) {
                            LyricsLineV2(
                                words = item.words!!, isActive = isActive, isPast = isPast, currentPositionMs = currentPositionMs,
                                textColor = textColor, inactiveAlpha = 1f, baseFontSize = lyricsTextSize,
                                isLineAllBackground = isAllBackground, textAlign = textAlign, lyricsFontFamily = lyricsFontFamily,
                                isRtl = lineIsRtl, fontWeight = currentFontWeight
                            )
                        } else if (isSynced) {
                            LyricsLineLrcBounce(
                                text = item.text, isActive = isActive, textColor = textColor,
                                fontSize = lyricsTextSize, lineSpacing = lyricsLineSpacing, isAllBackground = isAllBackground,
                                lyricsFontFamily = lyricsFontFamily, textAlign = textAlign, fontWeight = currentFontWeight
                            )
                        } else {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp,
                                    fontWeight = currentFontWeight,
                                    fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                                    letterSpacing = (-0.5).sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
                                ),
                                color = textColor,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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

        if (isSelectionModeActive) {
            mediaMetadata?.let { metadata ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                                .clickable {
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.close), contentDescription = stringResource(R.string.cancel), tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (selectedIndices.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    if (selectedIndices.isNotEmpty()) {
                                        val sortedIndices = selectedIndices.sorted()
                                        val selectedLyricsText = sortedIndices.mapNotNull { entriesWithWords.getOrNull(it)?.text }.joinToString("\n")

                                        if (selectedLyricsText.isNotBlank()) {
                                            shareDialogData = Triple(selectedLyricsText, metadata.title ?: "", metadata.artists.joinToString { it.name })
                                            showShareDialog = true
                                        }
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.share), contentDescription = stringResource(R.string.share_selected), tint = Color.Black, modifier = Modifier.size(20.dp))
                            Text(text = stringResource(R.string.share), color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp).fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = stringResource(R.string.share_lyrics), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                shareLyricsAsText(context = context, payload = LyricsSharePayload(lyricsText, songTitle, artists), songId = mediaMetadata?.id)
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.share_as_text), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showShareImageDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.share_as_image), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showShareImageDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        LyricsShareImageDialog(
            mediaMetadata = mediaMetadata,
            payload = LyricsSharePayload(lyricsText, songTitle, artists),
            onDismissRequest = { showShareImageDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineV2(
    words: List<WordTimestamp>, isActive: Boolean, isPast: Boolean, currentPositionMs: Long, textColor: Color,
    inactiveAlpha: Float, baseFontSize: Float, isLineAllBackground: Boolean, textAlign: TextAlign,
    lyricsFontFamily: FontFamily?, isRtl: Boolean, fontWeight: FontWeight
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
                    isBackground = isLineAllBackground, lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, fontWeight = fontWeight
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
                    lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, fontWeight = fontWeight
                )
            }
        }
    }
}

@Composable
private fun AnimatedWordV2(
    word: WordTimestamp, wordIndex: Int, isLineActive: Boolean, isLinePast: Boolean, currentPositionMs: Long,
    textColor: Color, inactiveAlpha: Float, fontSize: Float, isBackground: Boolean, lyricsFontFamily: FontFamily?,
    isRtl: Boolean, fontWeight: FontWeight
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

    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize

    val lyricStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = actualFontSize.sp, fontWeight = fontWeight, fontStyle = FontStyle.Normal,
        lineHeight = (actualFontSize * 1.35f).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
        letterSpacing = (-0.5).sp, 
        platformStyle = PlatformTextStyle(includeFontPadding = false), 
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
    )

    val safePadding = 8.dp
    
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val paddingPx = safePadding.roundToPx()
                val looseConstraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = if (constraints.maxWidth == Constraints.Infinity) Constraints.Infinity else constraints.maxWidth + paddingPx * 2,
                    minHeight = 0,
                    maxHeight = if (constraints.maxHeight == Constraints.Infinity) Constraints.Infinity else constraints.maxHeight + paddingPx * 2
                )
                val placeable = measurable.measure(looseConstraints)
                val width = (placeable.width - paddingPx * 2).coerceAtLeast(0)
                val height = (placeable.height - paddingPx * 2).coerceAtLeast(0)
                layout(width, height) { placeable.place(-paddingPx, -paddingPx) }
            }
            .graphicsLayer { clip = false }
    ) {
        Text(
            text = word.text,
            style = lyricStyle,
            color = textColor.copy(alpha = if (isBackground) 0.5f else 0.5f), 
            modifier = Modifier.padding(safePadding)
        )

        if (isWordComplete || isWordActive || isLinePast) {
            Text(
                text = word.text,
                style = lyricStyle,
                color = textColor.copy(alpha = if (isBackground) 0.85f else 1f),
                modifier = if (isWordActive && !isWordComplete) {
                    Modifier
                        .padding(safePadding)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 8.dp.toPx()
                            val center = if (isRtl) size.width - ((size.width + edgeWidth * 2) * progress - edgeWidth) else (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(brush = Brush.horizontalGradient(colors = if (isRtl) listOf(Color.Transparent, Color.Black) else listOf(Color.Black, Color.Transparent), startX = center - edgeWidth, endX = center + edgeWidth), blendMode = BlendMode.DstIn)
                        }
                } else Modifier.padding(safePadding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineLrcBounce(
    text: String, isActive: Boolean, textColor: Color, fontSize: Float, lineSpacing: Float,
    isAllBackground: Boolean, lyricsFontFamily: FontFamily?, textAlign: TextAlign, fontWeight: FontWeight
) {
    val words = remember(text) { text.split(Regex("(?<=\\s)")) }
    val effectiveFontSize = if (isAllBackground) fontSize * 0.82f else fontSize
    val fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal

    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }) {
        words.forEach { word ->
            Text(
                text = word,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = effectiveFontSize.sp, fontWeight = fontWeight, fontStyle = fontStyle, lineHeight = (effectiveFontSize * lineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false), 
                    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
                ),
                color = textColor,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )
        }
    }
}
p
