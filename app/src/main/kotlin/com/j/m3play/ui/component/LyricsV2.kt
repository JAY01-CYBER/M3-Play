/*
 * M3Play Component Module 
 * Signature: M3PLAY::COMPONENT
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.lyrics.*
import com.j.m3play.lyrics.LyricsUtils.findCurrentLineIndex
import com.j.m3play.lyrics.LyricsUtils.isChinese
import com.j.m3play.lyrics.LyricsUtils.isJapanese
import com.j.m3play.lyrics.LyricsUtils.isKorean
import com.j.m3play.lyrics.LyricsUtils.isTtml
import com.j.m3play.lyrics.LyricsUtils.parseLyrics
import com.j.m3play.lyrics.LyricsUtils.parseTtml
import com.j.m3play.lyrics.LyricsUtils.romanizeJapanese
import com.j.m3play.lyrics.LyricsUtils.romanizeKorean
import com.j.m3play.ui.component.shimmer.*
import com.j.m3play.ui.utils.smoothFadingEdge
import com.j.m3play.utils.*

private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L

private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")
private val AppleEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

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

// ── Wipe Fill Animation ──
@Composable
private fun AppleMusicWordWipe(
    word: WordTimestamp, currentTimeProvider: () -> Long, textColor: Color, fontSize: Float,
    isBackground: Boolean, fontWeight: FontWeight, lyricsFontFamily: FontFamily?, isRtl: Boolean
) {
    val startTime = (word.startTime * 1000).toLong()
    val endTime = (word.endTime * 1000).toLong()
    val duration = (endTime - startTime).coerceAtLeast(1L)
    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize
    val currentTime = currentTimeProvider()
    
    val progress = when {
        currentTime >= endTime -> 1f
        currentTime <= startTime -> 0f
        else -> ((currentTime - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val baseStyle = MaterialTheme.typography.headlineMedium
    val fallbackFontFamily = baseStyle.fontFamily
    val lyricStyle = remember(actualFontSize, fontWeight, lyricsFontFamily, baseStyle, fallbackFontFamily) {
        baseStyle.copy(
            fontSize = actualFontSize.sp, fontWeight = fontWeight, fontStyle = FontStyle.Normal,
            lineHeight = (actualFontSize * 1.35f).sp, fontFamily = lyricsFontFamily ?: fallbackFontFamily,
            letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
        )
    }

    val activeColor = textColor.copy(alpha = if (isBackground) 0.75f else 1f)
    val inactiveColor = textColor.copy(alpha = 0.25f)

    val textBrush = remember(progress, isRtl, activeColor, inactiveColor) {
        if (progress >= 1f) SolidColor(activeColor)
        else if (progress <= 0f) SolidColor(inactiveColor)
        else {
            val softEdge = 0.15f
            if (!isRtl) {
                Brush.horizontalGradient(
                    0f to activeColor, (progress - softEdge).coerceAtLeast(0f) to activeColor,
                    (progress + softEdge).coerceAtMost(1f) to inactiveColor, 1f to inactiveColor
                )
            } else {
                Brush.horizontalGradient(
                    0f to inactiveColor, (1f - progress - softEdge).coerceAtLeast(0f) to inactiveColor,
                    (1f - progress + softEdge).coerceAtMost(1f) to activeColor, 1f to activeColor
                )
            }
        }
    }

    val opticalScale = if (progress in 0.01f..0.99f) 1.015f else 1f

    Text(
        text = word.text,
        style = lyricStyle.copy(brush = textBrush),
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp).scale(opticalScale)
    )
}

// ── Music Dots Indicator ──
@Composable
private fun MusicLineDots(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Dots")
    val radius by infiniteTransition.animateFloat(
        initialValue = if (isActive) 8f else 4f, targetValue = if (isActive) 12f else 4f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "Radius"
    )
    val alpha by animateFloatAsState(targetValue = if (isActive) 0.8f else 0.2f, animationSpec = tween(300), label = "Alpha")
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp).alpha(alpha)) {
        repeat(3) { Box(modifier = Modifier.size((radius * 2).dp).background(Color.White, CircleShape)) }
    }
}

// ── Main UI ──
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    // ── M3-Play Native Preferences ──
    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 34f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)

    val lyricsFontFamily = remember(useSystemFont) { if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold)) }
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onBackground else Color.White

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val maxSelectionLimit = 5
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var selectedGlassStyle by remember { mutableStateOf(LyricsGlassStyle.FrostedDark) }
    var paletteGlassStyle by remember { mutableStateOf<LyricsGlassStyle?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
            showMaxSelectionToast = false
        }
    }

    // ── Parse Data ──
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics
    val isSynced = remember(lyrics) { lyrics != null && (lyrics.startsWith("[") || isTtml(lyrics)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics) }

    val entriesWithWords: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else {
            val parsed = when {
                isTtml(lyrics) -> parseTtml(lyrics)
                lyrics.startsWith("[") -> parseLyrics(lyrics)
                else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { _, line -> LyricsEntry(time = -1L, text = line.trim()) }
            }
            val cleanParsed = if (parsed.isNotEmpty() && parsed.first().time >= 0) listOf(HEAD_LYRICS_ENTRY) + parsed else parsed

            cleanParsed.mapIndexed { index, entry ->
                if (entry.words != null || entry.time < 0 || entry.text.isBlank()) {
                    entry
                } else {
                    val nextEntryTime = if (index < cleanParsed.lastIndex) cleanParsed[index + 1].time else entry.time + 5000L
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
                            } else currentWord.append(char)
                        }
                        if (currentWord.isNotEmpty()) chars.add(currentWord.toString())
                        val groupedTokens = mutableListOf<String>()
                        chars.forEachIndexed { _, c -> if (c.isBlank()) { if (groupedTokens.isNotEmpty()) groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c } else groupedTokens.add(c) }
                        groupedTokens
                    } else {
                        entry.text.split(Regex("(?<=\\s)|(?=\\s)"))
                    }
                    
                    val cleanTokens = tokens.filter { it.isNotEmpty() }
                    if (cleanTokens.isEmpty()) {
                        entry
                    } else {
                        val tokenWeights = cleanTokens.map { it.length.toDouble() + (if (it.matches(Regex(".*[,.?!;].*"))) 3.0 else 0.0) }
                        val totalWeight = tokenWeights.sum().coerceAtLeast(1.0)
                        val words = mutableListOf<WordTimestamp>()
                        var currentOffsetMs = 0.0
                        cleanTokens.forEachIndexed { wordIdx, token ->
                            val weight = tokenWeights[wordIdx] / totalWeight
                            val wordDurMs = lineDurationMs * weight
                            val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                            val wordEndSec = wordStartSec + (wordDurMs / 1000.0)
                            words.add(WordTimestamp(text = token, startTime = wordStartSec, endTime = wordEndSec))
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

    val leadMs = if (isTtmlFormat) 0L else 300L
    var currentPositionState by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(0) }

    // ── VSync Interpolation ──
    LaunchedEffect(entriesWithWords, isSynced) {
        if (!isSynced || entriesWithWords.isEmpty()) return@LaunchedEffect
        var lastPlayerPos = player.currentPosition
        var baseFrameTime = 0L
        while (isActive) {
            withFrameMillis { frameTime ->
                val playerPos = player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    baseFrameTime = frameTime
                }
                val interpolatedPos = if (player.isPlaying) lastPlayerPos + (frameTime - baseFrameTime) else lastPlayerPos
                currentPositionState = (interpolatedPos.coerceAtLeast(0L) + leadMs + 150L).coerceAtLeast(0L)
                currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionState, 0L)
            }
        }
    }

    val currentTimeProvider = remember { { currentPositionState } }

    // ── STAGGERED SCROLL PHYSICS LOGIC ──
    val listState = rememberLazyListState()
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, entriesWithWords) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, entriesWithWords) { mutableStateOf(true) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isAutoScrollEnabled, lastManualScrollTime) {
        if (!isAutoScrollEnabled) {
            delay(MANUAL_SCROLL_TIMEOUT_MS)
            isAutoScrollEnabled = true
        }
    }

    val activeListIndex = currentLineIndex

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        // Safe early conditions replaced using if-else for R8 Minify Safety
        if (lyrics == LYRICS_NOT_FOUND || entriesWithWords.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.6f)) 
            }
        } else if (lyrics == null) {
            ShimmerHost { repeat(6) { TextPlaceholder() } }
        } else {
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
                        val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                        currentY -= (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                        map[i] = currentY
                    }
                    currentY = 0f
                    for (i in activeListIndex until entriesWithWords.size - 1) {
                        val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                        currentY += (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                        map[i + 1] = currentY
                    }
                }
                map
            }

            val minOffset = remember(itemHeights.toMap(), entriesWithWords, activeListIndex, anchorY) {
                if (entriesWithWords.isEmpty() || activeListIndex == -1) {
                    0f
                } else {
                    val totalBelow = (activeListIndex until entriesWithWords.size - 1).sumOf { i ->
                        val height = itemHeights[i]?.toFloat() ?: constraintLineHeightPx
                        (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                    }.toFloat()
                    val lastHeight = itemHeights[entriesWithWords.size - 1]?.toFloat() ?: constraintLineHeightPx
                    with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
                }
            }

            val maxOffset = remember(itemHeights.toMap(), entriesWithWords, activeListIndex, maxHeightPx, anchorY) {
                if (entriesWithWords.isEmpty() || activeListIndex == -1) {
                    0f
                } else {
                    val totalAbove = (0 until activeListIndex).sumOf { i ->
                        val height = itemHeights[i]?.toFloat() ?: constraintLineHeightPx
                        (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                    }.toFloat()
                    maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
                }
            }

            val scrollClampMin = minOf(minOffset, maxOffset)
            val scrollClampMax = maxOf(minOffset, maxOffset)

            LaunchedEffect(scrollClampMin, scrollClampMax) {
                if (userManualOffset < scrollClampMin || userManualOffset > scrollClampMax) {
                    userManualOffset = userManualOffset.coerceIn(scrollClampMin, scrollClampMax)
                }
            }

            LaunchedEffect(isAutoScrollEnabled, entriesWithWords) {
                if (isAutoScrollEnabled) {
                    val start = userManualOffset
                    if (abs(start) < 1f) {
                        userManualOffset = 0f
                    } else {
                        val anim = Animatable(start)
                        var lastValue = start
                        anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                            userManualOffset += (value - lastValue)
                            lastValue = value
                        }
                        userManualOffset = 0f
                    }
                }
            }

            LaunchedEffect(lyrics, entriesWithWords.size) {
                if (entriesWithWords.isNotEmpty()) {
                    isInitialLayout = true
                    snapshotFlow { 
                        val h = itemHeights.toMap()
                        val windowStart = (activeListIndex - 8).coerceAtLeast(0)
                        val windowEnd = (activeListIndex + 12).coerceAtMost(entriesWithWords.size - 1)
                        (windowStart..windowEnd).all { h.containsKey(it) } 
                    }.first { it }
                    isInitialLayout = false
                }
            }

            // ── Drag physics handler ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .smoothFadingEdge(vertical = 120.dp)
                    .clipToBounds()
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source == NestedScrollSource.UserInput) isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastManualScrollTime = System.currentTimeMillis()
                                return super.onPostScroll(consumed, available, source)
                            }
                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastManualScrollTime = System.currentTimeMillis()
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
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
                                    userManualOffset = (userManualOffset + change.positionChange().y).coerceIn(scrollClampMin, scrollClampMax)
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }
                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                        val clamped = value.coerceIn(scrollClampMin, scrollClampMax)
                                        userManualOffset = clamped
                                        if (value != clamped) cancelAnimation()
                                    }
                                }
                            }
                        }
                    }
            ) {
                
                // ── Render Items using exact offset staggered math ──
                entriesWithWords.forEachIndexed { listIndex, item ->
                    if (item != HEAD_LYRICS_ENTRY) {
                        key(item.time, item.text.hashCode()) {
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { m, c -> 
                                        val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                                        layout(p.width, p.height) { p.place(0, 0) }
                                    }
                                    .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                                    .onSizeChanged { itemHeights[listIndex] = it.height }
                            ) {
                                
                                val isActive = isSynced && listIndex == currentLineIndex
                                val isSelected = selectedIndices.contains(listIndex)
                                
                                val targetAlpha = when {
                                    !isSynced -> 0.92f
                                    isActive -> 1f
                                    !isAutoScrollEnabled -> 0.55f
                                    distance == 1 -> 0.48f
                                    distance == 2 -> 0.32f
                                    else -> 0.20f
                                }
                                
                                val targetScale = when {
                                    !isSynced -> 1f
                                    isActive -> 1f
                                    distance == 1 -> 0.975f
                                    else -> 0.95f
                                }

                                val targetBlur = when {
                                    !isSynced || isActive || !isAutoScrollEnabled -> 0f
                                    distance <= 2 -> 0f
                                    else -> ((distance - 2) * 1.5f).coerceAtMost(6f)
                                }

                                val animatedLineScale by animateFloatAsState(targetValue = targetScale, animationSpec = tween(500, easing = AppleEase), label = "S")
                                val animatedLineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "A")
                                val animatedBlur by animateFloatAsState(targetValue = targetBlur, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "B")

                                val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Start }
                                val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.Start }
                                val lineTransformOrigin = remember(item.agent) { when (item.agent?.lowercase()) { "v2" -> TransformOrigin(1f, 0.5f); "v1", null -> TransformOrigin(0f, 0.5f); else -> TransformOrigin(0.5f, 0.5f) } }
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
                                            .padding(start = 32.dp, end = 32.dp, top = if (listIndex <= 1) 0.dp else 16.dp, bottom = 16.dp)
                                            .blur(radiusX = animatedBlur.dp, radiusY = animatedBlur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
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
                                                            selectedIndices.remove(listIndex)
                                                            if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                                        } else {
                                                            if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(listIndex) else showMaxSelectionToast = true
                                                        }
                                                    } else if (lyricsClick && isSynced && item.time > 0) {
                                                        player.seekTo(item.time)
                                                        isAutoScrollEnabled = true
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isSelectionModeActive) {
                                                        isSelectionModeActive = true; selectedIndices.add(listIndex)
                                                    } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                                        selectedIndices.add(listIndex)
                                                    } else if (!isSelected) showMaxSelectionToast = true
                                                }
                                            ),
                                        horizontalAlignment = horizontalAlignment,
                                    ) {
                                        val romanizedText = item.romanizedTextFlow.collectAsState().value
                                        if (romanizedText != null) {
                                            Text(text = romanizedText, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (lyricsTextSize * 0.55f).sp, lineHeight = (lyricsTextSize * 0.75f).sp, fontWeight = FontWeight.Normal, fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily), color = textColor.copy(alpha = if (isActive) 0.76f else 0.42f), textAlign = textAlign, modifier = Modifier.fillMaxWidth().padding(bottom = (lyricsTextSize * 0.18f).dp))
                                        }

                                        val currentFontWeight = FontWeight.Bold

                                        if (item.text.isBlank() || item.text == " ") {
                                            MusicLineDots(isActive = isActive)
                                        } 
                                        else if (item.words != null && isSynced) {
                                            val arrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }
                                            val mainWords = item.words.filter { !it.isBackground }
                                            val bgWords = item.words.filter { it.isBackground }

                                            if (mainWords.isNotEmpty()) {
                                                @OptIn(ExperimentalLayoutApi::class)
                                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                                                    mainWords.forEach { word ->
                                                        if (word.text.isBlank()) {
                                                            Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = lyricsTextSize.sp), color = Color.Transparent)
                                                        } else {
                                                            AppleMusicWordWipe(word = word, currentTimeProvider = currentTimeProvider, textColor = textColor, fontSize = if (isAllBackground) lyricsTextSize * 0.82f else lyricsTextSize, isBackground = isAllBackground, fontWeight = currentFontWeight, lyricsFontFamily = lyricsFontFamily, isRtl = lineIsRtl)
                                                        }
                                                    }
                                                }
                                            }

                                            if (bgWords.isNotEmpty()) {
                                                if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                                                @OptIn(ExperimentalLayoutApi::class)
                                                FlowRow(modifier = Modifier.fillMaxWidth().alpha(0.85f), horizontalArrangement = arrangement) {
                                                    bgWords.forEach { word ->
                                                        if (word.text.isBlank()) {
                                                            Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = (lyricsTextSize * 0.65f).sp), color = Color.Transparent)
                                                        } else {
                                                            AppleMusicWordWipe(word = word, currentTimeProvider = currentTimeProvider, textColor = textColor, fontSize = lyricsTextSize * 0.65f, isBackground = true, fontWeight = currentFontWeight, lyricsFontFamily = lyricsFontFamily, isRtl = lineIsRtl)
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            val words = item.text.split(Regex("(?<=\\s)"))
                                            val effectiveFontSize = if (isAllBackground) lyricsTextSize * 0.82f else lyricsTextSize
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }) {
                                                words.forEach { word -> 
                                                    Text(text = word, style = MaterialTheme.typography.headlineMedium.copy(fontSize = effectiveFontSize.sp, fontWeight = currentFontWeight, fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal, lineHeight = (effectiveFontSize * lyricsLineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)), color = textColor, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) 
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Resume Button & Share Menus ──
            AnimatedVisibility(
                visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                FilledTonalButton(onClick = { isAutoScrollEnabled = true }, shapes = ButtonDefaults.shapes()) { 
                    Icon(painter = painterResource(id = R.drawable.play), contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                    Text(text = "Resume", style = MaterialTheme.typography.labelLarge) 
                }
            }

            if (isSelectionModeActive) {
                if (mediaMetadata != null) {
                    val metadata = mediaMetadata!!
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape).clickable { isSelectionModeActive = false; selectedIndices.clear() }, contentAlignment = Alignment.Center) { 
                                Icon(painter = painterResource(id = R.drawable.close), contentDescription = stringResource(R.string.cancel), tint = Color.White, modifier = Modifier.size(20.dp)) 
                            }

                            Row(
                                modifier = Modifier
                                    .background(color = if (selectedIndices.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
                                    .clickable(enabled = selectedIndices.isNotEmpty()) {
                                        if (selectedIndices.isNotEmpty()) {
                                            val selectedLyricsText = selectedIndices.sorted().mapNotNull { entriesWithWords.getOrNull(it)?.text }.joinToString("\n")
                                            if (selectedLyricsText.isNotBlank()) {
                                                shareDialogData = Triple(selectedLyricsText, metadata.title ?: "", metadata.artists.joinToString { it.name })
                                                showShareDialog = true
                                            }
                                            isSelectionModeActive = false; selectedIndices.clear()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(painter = painterResource(id = R.drawable.share), contentDescription = stringResource(R.string.share_selected), tint = Color.Black, modifier = Modifier.size(20.dp))
                                Text(text = stringResource(R.string.share), color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {  }) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                Box(modifier = Modifier.padding(32.dp)) { Text(text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait), color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! 
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.padding(16.dp).fillMaxWidth(0.85f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = stringResource(R.string.share_lyrics), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND; type = "text/plain"
                                val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                putExtra(Intent.EXTRA_TEXT, "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                            showShareDialog = false
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.share_as_text), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            shareDialogData = Triple(lyricsText, songTitle, artists)
                            showColorPickerDialog = true; showShareDialog = false
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.share_as_image), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
                        Text(text = stringResource(R.string.cancel), fontSize = 16.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { showShareDialog = false }.padding(vertical = 8.dp, horizontal = 12.dp))
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val bmp = loader.execute(req).image?.toBitmap()
                        if (bmp != null) paletteGlassStyle = LyricsGlassStyle.fromPalette(Palette.from(bmp).generate())
                    } catch (_: Exception) {}
                }
            }
        }

        val availableStyles = remember(paletteGlassStyle) { val base = LyricsGlassStyle.allPresets.toMutableList(); paletteGlassStyle?.let { base.add(0, it) }; base }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(text = stringResource(id = R.string.customize_colors), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    if (mediaMetadata != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(20.dp))) {
                            LyricsImageCard(lyricText = lyricsText, mediaMetadata = mediaMetadata!!, glassStyle = selectedGlassStyle)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = stringResource(id = R.string.customize_colors), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        availableStyles.forEach { style ->
                            val isSelected = selectedGlassStyle == style
                            Box(
                                modifier = Modifier.size(width = 72.dp, height = 72.dp).clip(RoundedCornerShape(16.dp)).then(if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))).clickable { selectedGlassStyle = style }, contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(style.surfaceTint.copy(alpha = 0.6f), style.overlayColor.copy(alpha = 0.4f))), shape = RoundedCornerShape(16.dp)))
                                Box(modifier = Modifier.padding(6.dp).fillMaxSize().background(style.surfaceTint.copy(alpha = style.surfaceAlpha), RoundedCornerShape(10.dp)).border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(text = "Aa", color = style.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false; showProgressDialog = true
                            scope.launch {
                                try {
                                    val exportSize = 1080
                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = exportSize,
                                        height = exportSize,
                                        glassStyle = selectedGlassStyle
                                    )
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Lyrics"))
                                } catch (e: Exception) { Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() } finally { showProgressDialog = false }
                            }
                        },
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text(text = stringResource(id = R.string.share), fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                }
            }
        }
    }
}
