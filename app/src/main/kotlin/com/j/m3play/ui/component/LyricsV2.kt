/*
 * M3Play Component Module
 * Signature: M3PLAY::COMPONENT
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
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
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

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

private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRIC_VISUAL_TUNING_OFFSET_MS = 150L
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

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
        if (parsed.isNotEmpty() && parsed.first().time >= 0) listOf(HEAD_LYRICS_ENTRY, providerEntry) + parsed else listOf(HEAD_LYRICS_ENTRY, providerEntry) + parsed
    }

    val entriesWithWords: List<LyricsEntry> = remember(lyricsEntries) {
        if (lyricsEntries.isEmpty()) emptyList() else {
            lyricsEntries.mapIndexed { index, entry ->
                if (entry.words != null || entry.time < 0 || entry.text.isBlank()) entry else {
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
                            } else currentWord.append(char)
                        }
                        if (currentWord.isNotEmpty()) chars.add(currentWord.toString())
                        val groupedTokens = mutableListOf<String>()
                        chars.forEachIndexed { _, c -> if (c.isBlank()) { if (groupedTokens.isNotEmpty()) groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c } else groupedTokens.add(c) }
                        groupedTokens
                    } else entry.text.split(Regex("\\s+"))
                    
                    if (tokens.isEmpty()) entry else {
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
    var currentLineIndex by remember { mutableIntStateOf(0) }

    
    LaunchedEffect(entriesWithWords, isSynced) {
        if (!isSynced || entriesWithWords.isEmpty()) return@LaunchedEffect
        var lastPlayerPos = player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()
        while (isActive) {
            delay(16L)
            val now = System.currentTimeMillis()
            val sliderPos = sliderPositionProvider()
            val rawPos = if (sliderPos != null) sliderPos else {
                val playerPos = player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                lastPlayerPos + (if (player.isPlaying) now - lastUpdateTime else 0)
            }
            currentPositionMs = (rawPos.coerceAtLeast(0L) + leadMs + LYRIC_VISUAL_TUNING_OFFSET_MS).coerceAtLeast(0L)
            currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionMs, 0L)
        }
    }

    val currentTimeProvider = remember { { currentPositionMs } }

    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var deferredCurrentLineIndex by remember { mutableIntStateOf(0) }
    var isManualScrolling by remember { mutableStateOf(false) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }

    LaunchedEffect(currentLineIndex, isAutoScrollEnabled) {
        if (isAutoScrollEnabled) {
            deferredCurrentLineIndex = currentLineIndex
        }
    }

    BackHandler(enabled = isSelectionModeActive) { isSelectionModeActive = false; selectedIndices.clear() }

    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BoxWithConstraints(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize().padding(bottom = 12.dp)) {
        if (lyrics == LYRICS_NOT_FOUND || entriesWithWords.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.6f)) }
            return@BoxWithConstraints
        }
        if (lyrics == null) {
            ShimmerHost { repeat(6) { TextPlaceholder() } }
            return@BoxWithConstraints
        }

        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * 0.35f
        val lineHeightPx = with(density) { 68.dp.toPx() }
        val gapPx = with(density) { 16.dp.toPx() }

        val positions = remember(itemHeights.toMap(), deferredCurrentLineIndex, entriesWithWords) {
            val map = mutableMapOf<Int, Float>()
            if (entriesWithWords.isEmpty()) return@remember map
            map[deferredCurrentLineIndex] = 0f
            var currentY = 0f
            for (i in deferredCurrentLineIndex - 1 downTo 0) {
                val h = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY -= (h + gapPx)
                map[i] = currentY
            }
            currentY = 0f
            for (i in deferredCurrentLineIndex until entriesWithWords.size - 1) {
                val h = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY += (h + gapPx)
                map[i + 1] = currentY
            }
            map
        }

        val minOffset = remember(itemHeights.toMap(), entriesWithWords, deferredCurrentLineIndex, anchorY) {
            if (entriesWithWords.isEmpty()) 0f else {
                val totalBelow = (deferredCurrentLineIndex until entriesWithWords.size - 1).sumOf { 
                    ((itemHeights[it]?.toFloat() ?: lineHeightPx) + gapPx).toDouble() 
                }.toFloat()
                val lastHeight = itemHeights[entriesWithWords.size - 1]?.toFloat() ?: lineHeightPx
                with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
            }
        }
        val maxOffset = remember(itemHeights.toMap(), entriesWithWords, deferredCurrentLineIndex, maxHeightPx, anchorY) {
            if (entriesWithWords.isEmpty()) 0f else {
                val totalAbove = (0 until deferredCurrentLineIndex).sumOf { 
                    ((itemHeights[it]?.toFloat() ?: lineHeightPx) + gapPx).toDouble() 
                }.toFloat()
                maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
            }
        }
        val scrollClampMin = minOf(minOffset, maxOffset)
        val scrollClampMax = maxOf(minOffset, maxOffset)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .smoothFadingEdge(vertical = 80.dp)
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            flingJob?.cancel()
                            velocityTracker.resetTracking()
                            isAutoScrollEnabled = false
                            isManualScrolling = true
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
            entriesWithWords.forEachIndexed { index, item ->
                if (abs(index - deferredCurrentLineIndex) > 15) return@forEachIndexed
                if (item == HEAD_LYRICS_ENTRY) return@forEachIndexed

                val distance = abs(index - deferredCurrentLineIndex)
                val rawTargetOffset = anchorY + (positions[index] ?: ((index - deferredCurrentLineIndex) * lineHeightPx))
                
                val animatedOffset by animateFloatAsState(
                    targetValue = rawTargetOffset,
                    animationSpec = tween(750, (distance * 20).coerceAtMost(200), FastOutSlowInEasing),
                    label = "offset_$index"
                )

                val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Start }
                val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.Start }
                val isActive = isSynced && index == currentLineIndex
                val isSelected = selectedIndices.contains(index)
                val isPast = isSynced && index < currentLineIndex
                
                
                val targetGlideOffsetY = when {
                    !isSynced -> 0.dp
                    isActive -> 0.dp
                    index > currentLineIndex -> 24.dp // Naye words neeche se aayenge
                    else -> (-12).dp // Purane words upar jayenge
                }

                val targetAlpha = when {
                    !isSynced -> 0.92f
                    isActive -> ACCORD_ACTIVE_ALPHA
                    !isAutoScrollEnabled -> 0.4f
                    else -> ACCORD_INACTIVE_ALPHA
                }
                val targetScale = if (isActive) ACCORD_ACTIVE_SCALE else ACCORD_INACTIVE_SCALE
                val targetBlur = if (!isSynced || isActive || (isSelectionModeActive && isSelected) || !isAutoScrollEnabled) 0f else (distance.toFloat() * ACCORD_BLUR_STEP).coerceAtMost(ACCORD_MAX_BLUR)

                val animatedLineScale by animateFloatAsState(targetValue = targetScale, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "S")
                val animatedLineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "A")
                val animatedBlur by animateFloatAsState(targetValue = targetBlur, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "B")
                val animatedGlideOffsetY by animateDpAsState(targetValue = targetGlideOffsetY, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "YGlide")
                
                val lineTransformOrigin = remember(item.agent) { when (item.agent?.lowercase()) { "v2" -> TransformOrigin(1f, 0.5f); "v1", null -> TransformOrigin(0f, 0.5f); else -> TransformOrigin(0.5f, 0.5f) } }
                val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true
                
                val baseLayoutDirection = LocalLayoutDirection.current
                val lineText = remember(item.text, item.words) { item.words?.joinToString("") { it.text }?.takeIf { it.isNotBlank() } ?: item.text }
                val lineIsRtl = remember(lineText) { isRtlText(lineText) }
                val lineLayoutDirection = remember(lineIsRtl, baseLayoutDirection) { if (lineIsRtl) LayoutDirection.Rtl else baseLayoutDirection }

                CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { m, c -> 
                                val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                                layout(p.width, 0) { p.place(0, 0) } 
                            }
                            .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { itemHeights[index] = it.height }
                                .background(color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                .padding(start = 32.dp, end = 32.dp, top = if (index <= 1) 0.dp else 14.dp, bottom = 14.dp)
                                .blur(radiusX = animatedBlur.dp, radiusY = animatedBlur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                .graphicsLayer { 
                                    scaleX = animatedLineScale
                                    scaleY = animatedLineScale
                                    alpha = animatedLineAlpha
                                    translationY = animatedGlideOffsetY.toPx() 
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
                                            flingJob?.cancel()
                                            deferredCurrentLineIndex = index
                                            isAutoScrollEnabled = true
                                            scope.launch {
                                                Animatable(userManualOffset).animateTo(0f, tween(400, easing = FastOutSlowInEasing)) { userManualOffset = value }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true; selectedIndices.add(index)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else if (!isSelected) showMaxSelectionToast = true
                                    }
                                ),
                            horizontalAlignment = horizontalAlignment,
                        ) {
                            val romanizedText = item.romanizedTextFlow.collectAsState().value
                            if (romanizedText != null) {
                                Text(text = romanizedText, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (lyricsTextSize * 0.55f).sp, lineHeight = (lyricsTextSize * 0.75f).sp, fontWeight = FontWeight.Normal, fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily), color = textColor.copy(alpha = if (isActive) 0.76f else 0.42f), textAlign = textAlign, modifier = Modifier.fillMaxWidth().padding(bottom = (lyricsTextSize * 0.18f).dp))
                            }

                            val currentFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                            if (item.words != null && isSynced) {
                                LyricsLineV2(
                                    words = item.words!!, isActive = isActive, isPast = isPast, currentTimeProvider = currentTimeProvider, textColor = textColor, baseFontSize = lyricsTextSize, isLineAllBackground = isAllBackground, textAlign = textAlign, lyricsFontFamily = lyricsFontFamily, isRtl = lineIsRtl, fontWeight = currentFontWeight
                                )
                            } else if (isSynced) {
                                LyricsLineLrcBounce(
                                    text = item.text, isActive = isActive, textColor = textColor, fontSize = lyricsTextSize, lineSpacing = lyricsLineSpacing, isAllBackground = isAllBackground, lyricsFontFamily = lyricsFontFamily, textAlign = textAlign, fontWeight = currentFontWeight
                                )
                            } else {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp, fontWeight = currentFontWeight, fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)),
                                    color = textColor, textAlign = textAlign, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    flingJob?.cancel()
                    deferredCurrentLineIndex = currentLineIndex
                    isAutoScrollEnabled = true
                    isManualScrolling = false
                    scope.launch {
                        Animatable(userManualOffset).animateTo(0f, tween(400, easing = FastOutSlowInEasing)) { userManualOffset = value }
                    }
                },
                shapes = ButtonDefaults.shapes()
            ) { Text(text = "Resume", style = MaterialTheme.typography.labelLarge) }
        }

        if (isSelectionModeActive) {
            mediaMetadata?.let { metadata ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape).clickable { isSelectionModeActive = false; selectedIndices.clear() },
                            contentAlignment = Alignment.Center
                        ) { Icon(painter = painterResource(id = R.drawable.close), contentDescription = stringResource(R.string.cancel), tint = Color.White, modifier = Modifier.size(20.dp)) }

                        Row(
                            modifier = Modifier
                                .background(color = if (selectedIndices.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
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
                                action = Intent.ACTION_SEND
                                type = "text/plain"
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
                            showColorPickerDialog = true
                            showShareDialog = false
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
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            paletteGlassStyle = LyricsGlassStyle.fromPalette(palette)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        val availableStyles = remember(paletteGlassStyle) {
            val base = LyricsGlassStyle.allPresets.toMutableList()
            paletteGlassStyle?.let { base.add(0, it) }
            base
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(text = stringResource(id = R.string.customize_colors), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(20.dp))) {
                        LyricsImageCard(lyricText = lyricsText, mediaMetadata = mediaMetadata ?: return@Box, glassStyle = selectedGlassStyle)
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
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val exportSize = 1080
                                    val image = ComposeToImage.createLyricsImage(context = context, coverArtUrl = coverUrl, songTitle = songTitle, artistName = artists, lyrics = lyricsText, width = exportSize, height = exportSize, glassStyle = selectedGlassStyle)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineV2(
    words: List<WordTimestamp>, isActive: Boolean, isPast: Boolean, currentTimeProvider: () -> Long, textColor: Color,
    baseFontSize: Float, isLineAllBackground: Boolean, textAlign: TextAlign, lyricsFontFamily: FontFamily?, isRtl: Boolean, fontWeight: FontWeight
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
                AnimatedWordV2(word = word, isLineActive = isActive, isLinePast = isPast, currentTimeProvider = currentTimeProvider, textColor = textColor, fontSize = if (isLineAllBackground) baseFontSize * 0.82f else baseFontSize, isBackground = isLineAllBackground, lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, fontWeight = fontWeight)
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
                AnimatedWordV2(word = word, isLineActive = isActive, isLinePast = isPast, currentTimeProvider = currentTimeProvider, textColor = textColor, fontSize = baseFontSize * 0.65f, isBackground = true, lyricsFontFamily = lyricsFontFamily, isRtl = isRtl, fontWeight = fontWeight)
            }
        }
    }
}


@Composable
private fun AnimatedWordV2(
    word: WordTimestamp, isLineActive: Boolean, isLinePast: Boolean, currentTimeProvider: () -> Long, textColor: Color, fontSize: Float, isBackground: Boolean, lyricsFontFamily: FontFamily?, isRtl: Boolean, fontWeight: FontWeight
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize
    val lyricStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = actualFontSize.sp, fontWeight = fontWeight, fontStyle = FontStyle.Normal, lineHeight = (actualFontSize * 1.35f).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None))
    
    val chars = word.text.toCharArray()
    val charDurationMs = wordDuration / chars.size.coerceAtLeast(1)

    // Using Row to render individual characters
    Row(
        modifier = Modifier.padding(horizontal = 4.dp), // Word spacing
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp) // Tight character spacing
    ) {
        chars.forEachIndexed { charIndex, char ->
            val charStartMs = wordStartMs + (charIndex * charDurationMs)
            val charEndMs = charStartMs + charDurationMs
            
            Box(
                modifier = Modifier.graphicsLayer { 
                    clip = false
                    val currentTime = currentTimeProvider()
                    val maxShiftY = -8f 
                    val attackDuration = 80L 
                    val decayDuration = 200L 
                    val totalImpulseTime = attackDuration + decayDuration
                    
                    var shiftY = 0f
                    var scalePop = 0f
                    if (isLineActive && currentTime >= charStartMs && currentTime < charStartMs + totalImpulseTime) {
                        val t = currentTime - charStartMs
                        if (t < attackDuration) {
                            val p = t.toFloat() / attackDuration.toFloat()
                            shiftY = maxShiftY * kotlin.math.sin(p * Math.PI / 2.0).toFloat()
                            scalePop = 0.05f * kotlin.math.sin(p * Math.PI / 2.0).toFloat() // 5% scale pop
                        } else {
                            val p = (t - attackDuration).toFloat() / decayDuration.toFloat()
                            shiftY = maxShiftY * kotlin.math.cos(p * Math.PI / 2.0).toFloat()
                            scalePop = 0.05f * kotlin.math.cos(p * Math.PI / 2.0).toFloat()
                        }
                    }
                    translationY = shiftY
                    scaleX = 1f + scalePop
                    scaleY = 1f + scalePop
                }
            ) {
                // Inactive Character Base
                Text(text = char.toString(), style = lyricStyle, color = textColor.copy(alpha = 0.35f))
                
                // Active Character Mask
                Text(
                    text = char.toString(), style = lyricStyle, color = textColor.copy(alpha = if (isBackground) 0.85f else 1f),
                    modifier = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                        val currentTime = currentTimeProvider()
                        val progress = when {
                            isLinePast || currentTime >= charEndMs -> 1f
                            currentTime <= charStartMs -> 0f
                            else -> ((currentTime - charStartMs).toFloat() / charDurationMs).coerceIn(0f, 1f)
                        }
                        
                        if (progress >= 1f) {
                            drawContent()
                        } else if (progress > 0f) {
                            drawContent()
                            val width = size.width * progress
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Black,
                                    1f to Color.Transparent,
                                    startX = (width - 10f).coerceAtLeast(0f),
                                    endX = width + 5f
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineLrcBounce(
    text: String, isActive: Boolean, textColor: Color, fontSize: Float, lineSpacing: Float, isAllBackground: Boolean, lyricsFontFamily: FontFamily?, textAlign: TextAlign, fontWeight: FontWeight
) {
    val words = remember(text) { text.split(Regex("(?<=\\s)")) }
    val effectiveFontSize = if (isAllBackground) fontSize * 0.82f else fontSize
    val fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal

    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }) {
        words.forEach { word ->
            Text(text = word, style = MaterialTheme.typography.headlineMedium.copy(fontSize = effectiveFontSize.sp, fontWeight = fontWeight, fontStyle = fontStyle, lineHeight = (effectiveFontSize * lineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)), color = textColor, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp))
        }
    }
}
