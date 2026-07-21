/*
 * M3Play Component Module 
 * Signature: M3PLAY::COMPONENT
 *
 * Credits: 
 * Advanced playback drift correction, syllable syncing logic, 
 * and Material 3 Bottom Sheet UI paradigms are adapted from 
 * ArchiveTune (© Rukamori - github.com/rukamori).
 * Respect and thanks to the original developer!
 * 
 * Performance Optimized: Uses Native LazyColumn & GPU graphicsLayer.
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.layout
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
import kotlin.math.roundToLong

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
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

// Playback Drift Correction Constants
private const val SMOOTH_PLAYBACK_MAX_FORWARD_DRIFT_MS = 80L
private const val SMOOTH_PLAYBACK_MAX_BACKWARD_DRIFT_MS = 180L
private const val SMOOTH_PLAYBACK_DRIFT_CORRECTION = 0.55f

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

@Composable
private fun KaraokeWord(
    text: String,
    startTime: Long,
    endTime: Long,
    currentTimeProvider: () -> Long,
    isRtl: Boolean,
    fontSize: TextUnit,
    textColor: Color,
    inactiveAlpha: Float,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    isBackground: Boolean = false,
    nudgeEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val duration = endTime - startTime
    val glowPadding = 10.dp

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val glowPaddingPx = glowPadding.roundToPx()
                val looseConstraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity, minHeight = 0, maxHeight = Constraints.Infinity)
                val placeable = measurable.measure(looseConstraints)
                layout((placeable.width - glowPaddingPx * 2).coerceAtLeast(0), (placeable.height - glowPaddingPx * 2).coerceAtLeast(0)) {
                    placeable.place(-glowPaddingPx, -glowPaddingPx)
                }
            }
            .graphicsLayer {
                clip = false
                val currentTime = currentTimeProvider()
                val maxShift = 5f
                val attackDuration = 120L
                val decayDuration = 250L
                val totalImpulseTime = attackDuration + decayDuration
                
                val shift = if (nudgeEnabled && currentTime >= startTime && currentTime < startTime + totalImpulseTime) {
                    val timeSinceStart = currentTime - startTime
                    if (timeSinceStart < attackDuration) {
                        androidx.compose.ui.util.lerp(0f, maxShift, timeSinceStart.toFloat() / attackDuration.toFloat())
                    } else {
                        androidx.compose.ui.util.lerp(maxShift, 0f, (timeSinceStart - attackDuration).toFloat() / decayDuration.toFloat())
                    }
                } else 0f
                
                translationX = if (isRtl) -shift else shift
            }
    ) {
        val effectiveFontSize = if (isBackground) fontSize * 0.7f else fontSize
        val effectiveAlpha = if (isBackground) 0.6f else 1f
        
        Text(text = text, fontSize = effectiveFontSize, color = textColor.copy(alpha = inactiveAlpha * effectiveAlpha), fontWeight = fontWeight, modifier = Modifier.padding(glowPadding))

        Text(
            text = text, fontSize = effectiveFontSize, color = textColor.copy(alpha = effectiveAlpha), fontWeight = fontWeight,
            modifier = Modifier.padding(glowPadding).drawWithContent {
                if (currentTimeProvider() >= endTime) drawContent()
            }
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                     compositingStrategy = CompositingStrategy.Offscreen
                     val currentTime = currentTimeProvider()
                     if (currentTime >= endTime) {
                         val fadeProgress = ((currentTime - endTime).toFloat() / 200f).coerceIn(0f, 1f)
                         alpha = 1f - fadeProgress
                     } else alpha = 1f
                }
                .drawWithContent {
                    val currentTime = currentTimeProvider()
                    val progress = if (duration > 0) ((currentTime - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f) else if (currentTime >= endTime) 1f else 0f
                    val isFading = currentTime >= endTime && currentTime < (endTime + 200L)
                    
                    if ((progress > 0f && progress < 1f) || isFading) {
                        drawContent()
                        val fadeWidth = 20f 
                        val paddingPx = glowPadding.toPx()
                        val textWidth = size.width - (paddingPx * 2)
                        val fillWidth = textWidth * progress
                        
                        val endFraction = (paddingPx + fillWidth + fadeWidth) / size.width
                        val solidFraction = (paddingPx + fillWidth) / size.width

                        val softFillBrush = if (!isRtl) {
                            Brush.horizontalGradient(0f to Color.Black, solidFraction.coerceAtLeast(0f) to Color.Black, endFraction.coerceAtMost(1f) to Color.Transparent)
                        } else {
                            val solidStartX = (paddingPx + (textWidth - fillWidth)).coerceIn(0f, size.width)
                            val fadeStartX = (solidStartX - fadeWidth).coerceIn(0f, size.width)
                            val fadeStartFraction = (fadeStartX / size.width).coerceIn(0f, 1f)
                            val solidStartFraction = (solidStartX / size.width).coerceIn(0f, 1f)
                            Brush.horizontalGradient(0f to Color.Transparent, fadeStartFraction to Color.Transparent, solidStartFraction to Color.Black, 1f to Color.Black)
                        }
                        drawRect(brush = softFillBrush, blendMode = BlendMode.DstIn)
                    }
                }
                .padding(glowPadding)
        ) {
             Text(text = text, fontSize = effectiveFontSize, color = textColor.copy(alpha = effectiveAlpha), fontWeight = fontWeight)
        }
    }
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
        var anchorPlayerPositionMs = player.currentPosition.coerceAtLeast(0L)
        var anchorFrameNanos = 0L

        while (isActive) {
            val sliderPos = sliderPositionProvider()
            val rawPos = (sliderPos ?: player.currentPosition).coerceAtLeast(0L)

            if (sliderPos != null || !player.isPlaying) {
                anchorPlayerPositionMs = rawPos
                anchorFrameNanos = 0L
                currentPositionMs = (rawPos + leadMs + LYRIC_VISUAL_TUNING_OFFSET_MS).coerceAtLeast(0L)
                if (sliderPos == null) {
                    delay(100L)
                } else {
                    withFrameNanos { }
                }
            } else {
                val frameNanos = withFrameNanos { it }
                if (anchorFrameNanos == 0L) {
                    anchorFrameNanos = frameNanos
                    anchorPlayerPositionMs = rawPos
                }

                val elapsedMs = ((frameNanos - anchorFrameNanos) / 1_000_000f) 
                val projectedPosition = anchorPlayerPositionMs + elapsedMs.roundToLong()
                val driftMs = rawPos - projectedPosition
                
                val nextPosition = when {
                    driftMs > SMOOTH_PLAYBACK_MAX_FORWARD_DRIFT_MS || driftMs < -SMOOTH_PLAYBACK_MAX_BACKWARD_DRIFT_MS -> {
                        anchorPlayerPositionMs = rawPos
                        anchorFrameNanos = frameNanos
                        rawPos
                    }
                    driftMs != 0L -> {
                        projectedPosition + (driftMs * SMOOTH_PLAYBACK_DRIFT_CORRECTION).roundToLong()
                    }
                    else -> {
                        projectedPosition
                    }
                }.coerceAtLeast(0L)
                
                currentPositionMs = (nextPosition + leadMs + LYRIC_VISUAL_TUNING_OFFSET_MS).coerceAtLeast(0L)
            }
            currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionMs, 0L)
        }
    }

    val currentTimeProvider = remember { { currentPositionMs } }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var isManualScrolling by remember { mutableStateOf(false) }

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
        val listState = rememberLazyListState()
        val anchorRatio = 0.35f 

        // Smooth Auto-Scrolling logic using native LazyListState
        LaunchedEffect(currentLineIndex, isAutoScrollEnabled) {
            if (isAutoScrollEnabled && entriesWithWords.isNotEmpty()) {
                val targetIndex = maxOf(0, currentLineIndex)
                val offsetPx = -(maxHeightPx * anchorRatio).roundToInt()
                listState.animateScrollToItem(index = targetIndex, scrollOffset = offsetPx)
            }
        }
        
        // Detect manual scrolling to disable auto-scroll
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                isAutoScrollEnabled = false
                isManualScrolling = true
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().smoothFadingEdge(vertical = 80.dp),
            contentPadding = PaddingValues(
                top = with(density) { (maxHeightPx * 0.15f).toDp() }, 
                bottom = with(density) { (maxHeightPx * 0.45f).toDp() }
            )
        ) {
            itemsIndexed(
                items = entriesWithWords,
                key = { index, item -> item.text.hashCode() + index }
            ) { index, item ->
                if (item == HEAD_LYRICS_ENTRY) return@itemsIndexed

                val distance = abs(index - currentLineIndex)
                val isActive = isSynced && index == currentLineIndex
                val isSelected = selectedIndices.contains(index)
                
                val targetAlpha = when {
                    !isSynced -> 0.92f
                    isActive -> ACCORD_ACTIVE_ALPHA
                    isManualScrolling -> 0.4f
                    else -> ACCORD_INACTIVE_ALPHA
                }
                val targetScale = if (isActive) ACCORD_ACTIVE_SCALE else ACCORD_INACTIVE_SCALE
                val targetBlur = if (!isSynced || isActive || (isSelectionModeActive && isSelected) || isManualScrolling) 0f else (distance.toFloat() * ACCORD_BLUR_STEP).coerceAtMost(ACCORD_MAX_BLUR)

                // GPU Hardware Accelerated Animations
                val animatedLineScale by animateFloatAsState(targetValue = targetScale, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "S")
                val animatedLineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "A")
                val animatedBlur by animateFloatAsState(targetValue = targetBlur, animationSpec = tween(500, easing = AccordDecelerateEasing), label = "B")
                
                val textAlign = when (item.agent?.lowercase()) { "v1" -> TextAlign.Start; "v2" -> TextAlign.End; else -> TextAlign.Start }
                val horizontalAlignment = when (item.agent?.lowercase()) { "v1" -> Alignment.Start; "v2" -> Alignment.End; else -> Alignment.Start }
                val lineTransformOrigin = remember(item.agent) { when (item.agent?.lowercase()) { "v2" -> TransformOrigin(1f, 0.5f); "v1", null -> TransformOrigin(0f, 0.5f); else -> TransformOrigin(0.5f, 0.5f) } }
                
                val mainWords = remember(item.words) { item.words?.filter { !it.isBackground } ?: emptyList() }
                val bgWords = remember(item.words) { item.words?.filter { it.isBackground } ?: emptyList() }
                val isAllBackground = mainWords.isEmpty() && bgWords.isNotEmpty()
                
                val baseLayoutDirection = LocalLayoutDirection.current
                val lineText = remember(item.text, item.words) { item.words?.joinToString("") { it.text }?.takeIf { it.isNotBlank() } ?: item.text }
                val lineIsRtl = remember(lineText) { isRtlText(lineText) }
                val lineLayoutDirection = remember(lineIsRtl, baseLayoutDirection) { if (lineIsRtl) LayoutDirection.Rtl else baseLayoutDirection }

                CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 32.dp, top = if (index <= 1) 0.dp else 14.dp, bottom = 14.dp)
                            .background(color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                            .graphicsLayer {
                                scaleX = animatedLineScale
                                scaleY = animatedLineScale
                                alpha = animatedLineAlpha
                                transformOrigin = lineTransformOrigin
                                
                                // Hardware Accelerated RenderEffect for Blur
                                if (animatedBlur > 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    val blurPx = animatedBlur * density.density
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
                                    ).asComposeRenderEffect()
                                }
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
                                        isAutoScrollEnabled = true
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
                            val arrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }

                            if (mainWords.isNotEmpty()) {
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                                    mainWords.forEach { word ->
                                        if (word.text == " " || word.text == "\n") Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = lyricsTextSize.sp), color = Color.Transparent)
                                        else KaraokeWord(text = word.text, startTime = (word.startTime * 1000).toLong(), endTime = (word.endTime * 1000).toLong(), currentTimeProvider = currentTimeProvider, isRtl = lineIsRtl, fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp, textColor = textColor, inactiveAlpha = if (isActive) 0.35f else 0.7f, fontWeight = currentFontWeight, isBackground = isAllBackground, nudgeEnabled = isActive)
                                    }
                                }
                            }

                            if (bgWords.isNotEmpty()) {
                                if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth().alpha(0.85f), horizontalArrangement = arrangement) {
                                    bgWords.forEach { word ->
                                        if (word.text == " " || word.text == "\n") Text(text = " ", style = MaterialTheme.typography.headlineMedium.copy(fontSize = (lyricsTextSize * 0.65f).sp), color = Color.Transparent)
                                        else KaraokeWord(text = word.text, startTime = (word.startTime * 1000).toLong(), endTime = (word.endTime * 1000).toLong(), currentTimeProvider = currentTimeProvider, isRtl = lineIsRtl, fontSize = (lyricsTextSize * 0.65f).sp, textColor = textColor, inactiveAlpha = if (isActive) 0.35f else 0.7f, fontWeight = currentFontWeight, isBackground = true, nudgeEnabled = isActive)
                                    }
                                }
                            }
                        } else if (isSynced) {
                            val words = item.text.split(Regex("(?<=\\s)"))
                            val effectiveFontSize = if (isAllBackground) lyricsTextSize * 0.82f else lyricsTextSize
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }) {
                                words.forEach { word -> Text(text = word, style = MaterialTheme.typography.headlineMedium.copy(fontSize = effectiveFontSize.sp, fontWeight = currentFontWeight, fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal, lineHeight = (effectiveFontSize * lyricsLineSpacing).sp, fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily, letterSpacing = (-0.5).sp, platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)), color = textColor, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) }
                            }
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

        AnimatedVisibility(
            visible = isManualScrolling && !isSelectionModeActive,
            enter = slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), initialOffsetY = { it * 2 }) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing), targetOffsetY = { it * 2 }) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp))
                    .clickable { isManualScrolling = false; isAutoScrollEnabled = true }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Text(text = stringResource(R.string.resume_autoscroll), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
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
                                .background(color = if (selectedIndices.isNotEmpty()) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    if (selectedIndices.isNotEmpty()) {
                                        shareDialogData = Triple("", metadata.title ?: "", metadata.artists.joinToString { it.name })
                                        showShareDialog = true
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.share), contentDescription = stringResource(R.string.share_selected), tint = Color.Black, modifier = Modifier.size(20.dp))
                            Text(text = "Share (${selectedIndices.size})", color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
        val (_, songTitle, artists) = shareDialogData!! 
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        // Simple plain text for normal intent sharing
        val plainLyricsText = remember(selectedIndices) {
            selectedIndices.sorted().mapNotNull { entriesWithWords.getOrNull(it)?.text }.joinToString("\n")
        }

        ModalBottomSheet(
            onDismissRequest = { showShareDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Card(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                            putExtra(Intent.EXTRA_TEXT, "\"$plainLyricsText\"\n\n$songTitle - $artists\n$songLink")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                        showShareDialog = false
                        isSelectionModeActive = false
                        selectedIndices.clear()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = stringResource(R.string.share_as_text), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }

                Card(
                    onClick = {
                        showColorPickerDialog = true
                        showShareDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = stringResource(R.string.share_as_image), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (_, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl

        var selectedAspectRatio by remember { mutableFloatStateOf(1f) } 
        var selectedTextAlign by remember { mutableStateOf(TextAlign.Center) }
        var customBlur by remember { mutableFloatStateOf(-1f) } 
        var customDarkness by remember { mutableFloatStateOf(-1f) } 
        var textScale by remember { mutableFloatStateOf(1f) }
        var fontStyle by remember { mutableIntStateOf(0) }
        var bgMode by remember { mutableIntStateOf(0) }
        var textGlow by remember { mutableStateOf(false) }
        var showWatermark by remember { mutableStateOf(true) }
        var showBarcode by remember { mutableStateOf(true) }
        var showTrackInfo by remember { mutableStateOf(true) } 
        var showRomanized by remember { mutableStateOf(false) }

        val displayLyricsText = remember(selectedIndices.toList(), showRomanized) {
            selectedIndices.sorted().mapNotNull { i ->
                val entry = entriesWithWords.getOrNull(i)
                val text = entry?.text ?: ""
                val rom = entry?.romanizedTextFlow?.value
                if (showRomanized && !rom.isNullOrBlank()) "$text\n$rom" else text
            }.joinToString("\n")
        }

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

        val colorPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showColorPickerDialog = false },
            sheetState = colorPickerSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            ) {
                Text(
                    text = "Design Options",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().height(380.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.fillMaxHeight().aspectRatio(selectedAspectRatio)) {
                        LyricsImageCard(
                            lyricText = displayLyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            glassStyle = selectedGlassStyle,
                            aspectRatio = selectedAspectRatio,
                            textAlign = selectedTextAlign,
                            customBlur = if (customBlur < 0f) null else customBlur.toInt(),
                            showWatermark = showWatermark,
                            showTrackInfo = showTrackInfo,
                            textScale = textScale,
                            customDarkness = if (customDarkness < 0f) null else customDarkness,
                            fontStyle = fontStyle,
                            bgMode = bgMode,
                            textGlow = textGlow,
                            showBarcode = showBarcode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Format & Style", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = selectedAspectRatio == 1f, onClick = { selectedAspectRatio = 1f }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("1:1") }
                        SegmentedButton(selected = selectedAspectRatio == 9f/16f, onClick = { selectedAspectRatio = 9f/16f }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("9:16") }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = fontStyle == 0, onClick = { fontStyle = 0 }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)) { Text("Mod") }
                        SegmentedButton(selected = fontStyle == 1, onClick = { fontStyle = 1 }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)) { Text("Serif") }
                        SegmentedButton(selected = fontStyle == 2, onClick = { fontStyle = 2 }, shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)) { Text("Mono") }
                        SegmentedButton(selected = fontStyle == 3, onClick = { fontStyle = 3 }, shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)) { Text("Hand") }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = bgMode == 0, onClick = { bgMode = 0 }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Glass Blur") }
                        SegmentedButton(selected = bgMode == 1, onClick = { bgMode = 1 }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Gradient") }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = selectedTextAlign == TextAlign.Start, onClick = { selectedTextAlign = TextAlign.Start }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)) { Text("Left") }
                        SegmentedButton(selected = selectedTextAlign == TextAlign.Center, onClick = { selectedTextAlign = TextAlign.Center }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)) { Text("Center") }
                        SegmentedButton(selected = selectedTextAlign == TextAlign.End, onClick = { selectedTextAlign = TextAlign.End }, shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)) { Text("Right") }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(text = "Adjustments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    Column {
                        Text(text = "Text Zoom", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = textScale, onValueChange = { textScale = it }, valueRange = 0.5f..1.5f)
                    }

                    Column {
                        Text(text = "Blur Intensity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = if (customBlur < 0f) selectedGlassStyle.cloudyRadius.toFloat() else customBlur, onValueChange = { customBlur = it }, valueRange = 0f..50f)
                    }

                    Column {
                        Text(text = "Darkness", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = if (customDarkness < 0f) selectedGlassStyle.backgroundDimAlpha else customDarkness, onValueChange = { customDarkness = it }, valueRange = 0f..1f)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(text = "Elements", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Show Romanization", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = showRomanized, onCheckedChange = { showRomanized = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Neon Text Glow", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = textGlow, onCheckedChange = { textGlow = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Show Song Info", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = showTrackInfo, onCheckedChange = { showTrackInfo = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Show Aesthetic Barcode", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = showBarcode, onCheckedChange = { showBarcode = it })
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(text = "Glass Theme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        availableStyles.forEach { style ->
                            val isSelected = selectedGlassStyle == style
                            Box(
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).then(if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))).clickable { 
                                    selectedGlassStyle = style; customBlur = -1f; customDarkness = -1f
                                }, contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(style.surfaceTint.copy(alpha = 0.6f), style.overlayColor.copy(alpha = 0.4f)))))
                                Box(modifier = Modifier.padding(6.dp).fillMaxSize().background(style.surfaceTint.copy(alpha = style.surfaceAlpha), RoundedCornerShape(10.dp)).border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(text = "Aa", color = style.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showColorPickerDialog = false
                        showProgressDialog = true
                        scope.launch {
                            try {
                                val exportWidth = 1080
                                val exportHeight = (exportWidth / selectedAspectRatio).toInt()
                                
                                val image = ComposeToImage.createLyricsImage(
                                    context = context, 
                                    coverArtUrl = coverUrl, 
                                    songTitle = songTitle, 
                                    artistName = artists, 
                                    lyrics = displayLyricsText, 
                                    width = exportWidth, 
                                    height = exportHeight, 
                                    glassStyle = selectedGlassStyle,
                                    aspectRatio = selectedAspectRatio, 
                                    textAlign = selectedTextAlign, 
                                    customBlur = if (customBlur < 0f) null else customBlur.toInt(), 
                                    showWatermark = showWatermark,
                                    showTrackInfo = showTrackInfo,
                                    textScale = textScale,
                                    customDarkness = if (customDarkness < 0f) null else customDarkness,
                                    fontStyle = fontStyle,
                                    bgMode = bgMode,
                                    textGlow = textGlow,
                                    showBarcode = showBarcode
                                )
                                val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Lyrics"))
                                isSelectionModeActive = false
                                selectedIndices.clear()
                            } catch (e: Exception) { Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() } finally { showProgressDialog = false }
                        }
                    },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(text = stringResource(id = R.string.share), fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            }
        }
    }
}
