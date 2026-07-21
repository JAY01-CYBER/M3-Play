/*
 * M3Play Component Module 
 * Signature: M3PLAY::COMPONENT
 * Native Jetpack Compose 
 * Optimized for GPU rendering, dynamic blur, and spring animations.
 */

package com.j.m3play.ui.component

import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
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

// Playback Drift Correction
private const val SMOOTH_PLAYBACK_MAX_FORWARD_DRIFT_MS = 80L
private const val SMOOTH_PLAYBACK_MAX_BACKWARD_DRIFT_MS = 180L
private const val SMOOTH_PLAYBACK_DRIFT_CORRECTION = 0.55f

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
private fun AppleMusicKaraokeWord(
    text: String, startTime: Long, endTime: Long, currentTimeProvider: () -> Long,
    isRtl: Boolean, fontSize: TextUnit, textColor: Color, inactiveAlpha: Float,
    fontWeight: FontWeight = FontWeight.ExtraBold, isBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val duration = endTime - startTime
    val glowPadding = 12.dp

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
    ) {
        val effectiveFontSize = if (isBackground) fontSize * 0.75f else fontSize
        
        // Inactive/Unsung Text
        Text(
            text = text, 
            fontSize = effectiveFontSize, 
            color = textColor.copy(alpha = inactiveAlpha), 
            fontWeight = fontWeight, 
            modifier = Modifier.padding(glowPadding)
        )
        
        // Active/Sung Text (Drawn only when fully passed for optimization)
        Text(
            text = text, 
            fontSize = effectiveFontSize, 
            color = textColor, 
            fontWeight = fontWeight,
            modifier = Modifier
                .padding(glowPadding)
                .drawWithContent { if (currentTimeProvider() >= endTime) drawContent() }
        )

        // Apple Style Sweep Animation (Draws progressively)
        Box(
            modifier = Modifier
                .graphicsLayer {
                     compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    val currentTime = currentTimeProvider()
                    val progress = if (duration > 0) ((currentTime - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f) else if (currentTime >= endTime) 1f else 0f
                    
                    if (progress > 0f && progress < 1f) {
                        drawContent()
                        val fadeWidth = 30f 
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
             Text(text = text, fontSize = effectiveFontSize, color = textColor, fontWeight = fontWeight)
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
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 36f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.25f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)

    // Using SF Pro Display (Apple Font) if available and useSystemFont is false
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
                if (sliderPos == null) delay(100L) else withFrameNanos { }
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
                        anchorPlayerPositionMs = rawPos; anchorFrameNanos = frameNanos; rawPos
                    }
                    driftMs != 0L -> projectedPosition + (driftMs * SMOOTH_PLAYBACK_DRIFT_CORRECTION).roundToLong()
                    else -> projectedPosition
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

        LaunchedEffect(currentLineIndex, isAutoScrollEnabled) {
            if (isAutoScrollEnabled && entriesWithWords.isNotEmpty()) {
                val targetIndex = maxOf(0, currentLineIndex)
                val offsetPx = -(maxHeightPx * anchorRatio).roundToInt()
                // Apple Music uses a slightly bouncy spring for scrolling
                listState.animateScrollToItem(index = targetIndex, scrollOffset = offsetPx)
            }
        }
        
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                isAutoScrollEnabled = false
                isManualScrolling = true
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().smoothFadingEdge(vertical = 100.dp),
            contentPadding = PaddingValues(top = (maxHeightPx * 0.15f).dp, bottom = (maxHeightPx * 0.45f).dp)
        ) {
            itemsIndexed(
                items = entriesWithWords,
                key = { index, item -> item.text.hashCode() + index }
            ) { index, item ->
                if (item == HEAD_LYRICS_ENTRY) return@itemsIndexed

                val distance = abs(index - currentLineIndex)
                val isActive = isSynced && index == currentLineIndex
                val isSelected = selectedIndices.contains(index)
                
                // Apple Music Scale, Alpha, and Blur logic
                val targetAlpha = when {
                    !isSynced -> 0.8f
                    isActive -> 1f
                    isManualScrolling -> 0.5f
                    distance == 1 -> 0.4f
                    distance == 2 -> 0.15f
                    else -> 0.05f 
                }
                
                val targetScale = if (isActive) 1.05f else 0.92f
                
                // Exponential blur for distant lines
                val targetBlur = if (!isSynced || isActive || isManualScrolling) 0f else (distance * 3f).coerceAtMost(12f)
                
                // Optimized Animations
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                    label = "Scale"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
                    label = "Alpha"
                )
                val animatedBlur by animateFloatAsState(
                    targetValue = targetBlur, 
                    animationSpec = tween(400, easing = FastOutSlowInEasing), 
                    label = "Blur"
                )

                // Default alignment to Start for Apple Music look, but fallback if agent specifies
                val textAlign = when (item.agent?.lowercase()) { "v2" -> TextAlign.End; else -> TextAlign.Start }
                val horizontalAlignment = when (item.agent?.lowercase()) { "v2" -> Alignment.End; else -> Alignment.Start }
                val lineTransformOrigin = remember(item.agent) { when (item.agent?.lowercase()) { "v2" -> TransformOrigin(1f, 0.5f); else -> TransformOrigin(0f, 0.5f) } }

                val mainWords = remember(item.words) { item.words?.filter { !it.isBackground } ?: emptyList() }
                val bgWords = remember(item.words) { item.words?.filter { it.isBackground } ?: emptyList() }
                val isAllBackground = mainWords.isEmpty() && bgWords.isNotEmpty()
                val lineText = remember(item.text, item.words) { item.words?.joinToString("") { it.text }?.takeIf { it.isNotBlank() } ?: item.text }
                val lineIsRtl = remember(lineText) { isRtlText(lineText) }
                val lineLayoutDirection = remember(lineIsRtl) { if (lineIsRtl) LayoutDirection.Rtl else LocalLayoutDirection.current }

                CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 28.dp, top = if (index <= 1) 0.dp else 12.dp, bottom = 12.dp)
                            .background(color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .graphicsLayer {
                                // Optimized hardware rendering for Scale and Alpha
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                                transformOrigin = lineTransformOrigin
                                
                                // Hardware accelerated blur effect (Android 12+)
                                if (animatedBlur > 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        animatedBlur * density, animatedBlur * density, android.graphics.Shader.TileMode.DECAL
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
                                    }
                                }
                            ),
                        horizontalAlignment = horizontalAlignment,
                    ) {
                        val currentFontWeight = if (isActive) FontWeight.Black else FontWeight.Bold

                        if (item.words != null && isSynced) {
                            val arrangement = when (textAlign) { TextAlign.Center -> Arrangement.Center; TextAlign.End -> Arrangement.End; else -> Arrangement.Start }

                            if (mainWords.isNotEmpty()) {
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                                    mainWords.forEach { word ->
                                        if (word.text == " " || word.text == "\n") Text(text = " ", style = MaterialTheme.typography.headlineLarge.copy(fontSize = lyricsTextSize.sp), color = Color.Transparent)
                                        else AppleMusicKaraokeWord(text = word.text, startTime = (word.startTime * 1000).toLong(), endTime = (word.endTime * 1000).toLong(), currentTimeProvider = currentTimeProvider, isRtl = lineIsRtl, fontSize = if (isAllBackground) (lyricsTextSize * 0.85f).sp else lyricsTextSize.sp, textColor = textColor, inactiveAlpha = 0.35f, fontWeight = currentFontWeight, isBackground = isAllBackground)
                                    }
                                }
                            }

                            if (bgWords.isNotEmpty()) {
                                if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth().alpha(0.85f), horizontalArrangement = arrangement) {
                                    bgWords.forEach { word ->
                                        if (word.text == " " || word.text == "\n") Text(text = " ", style = MaterialTheme.typography.headlineLarge.copy(fontSize = (lyricsTextSize * 0.7f).sp), color = Color.Transparent)
                                        else AppleMusicKaraokeWord(text = word.text, startTime = (word.startTime * 1000).toLong(), endTime = (word.endTime * 1000).toLong(), currentTimeProvider = currentTimeProvider, isRtl = lineIsRtl, fontSize = (lyricsTextSize * 0.7f).sp, textColor = textColor, inactiveAlpha = 0.35f, fontWeight = currentFontWeight, isBackground = true)
                                    }
                                }
                            }
                        } else {
                            // Plain Text Rendering with Apple typography spacing
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = lyricsTextSize.sp, 
                                    fontWeight = currentFontWeight, 
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp, 
                                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineLarge.fontFamily,
                                    letterSpacing = (-0.5).sp // Apple Music tight tracking
                                ),
                                color = textColor, 
                                textAlign = textAlign, 
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Resume Autoscroll Floating Button
        AnimatedVisibility(
            visible = isManualScrolling && !isSelectionModeActive,
            enter = slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), initialOffsetY = { it * 2 }) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing), targetOffsetY = { it * 2 }) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), shape = RoundedCornerShape(24.dp))
                    .clickable { isManualScrolling = false; isAutoScrollEnabled = true }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Text(text = stringResource(R.string.resume_autoscroll), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        // Selection / Share Mode Toolbar
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
                            Text(text = "Share (${selectedIndices.size})", color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (_, songTitle, artists) = shareDialogData!! 
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val plainLyricsText = remember(selectedIndices) {
            selectedIndices.sorted().mapNotNull { entriesWithWords.getOrNull(it)?.text }.joinToString("\n")
        }

        ModalBottomSheet(
            onDismissRequest = { showShareDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)) {
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
            }
        }
    }
}
