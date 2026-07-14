package com.j.m3play.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j.m3play.LocalDatabase
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.j.m3play.lyrics.LyricsEntry
import com.j.m3play.lyrics.LyricsTranslationHelper
import com.j.m3play.lyrics.LyricsUtils
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.ui.utils.fadingEdge
import com.j.m3play.utils.ComposeToImage
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(val afterLineIndex: Int, val gapMs: Long, val gapStartMs: Long, val gapEndMs: Long, val nextAgent: String?) : LyricsListItem()
}

@Composable
internal fun IntervalIndicator(gapStartMs: Long, gapEndMs: Long, currentPositionMs: Long, visible: Boolean, color: Color, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) { rowHeightPx.animateTo(1f, tween(200)); alpha.animateTo(1f, tween(200)) } 
        else { alpha.animateTo(0f, tween(200)); rowHeightPx.animateTo(0f, tween(200)) }
    }
    val progress = if (gapEndMs > gapStartMs) ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f) else 0f
    Box(modifier = modifier.height(72.dp * rowHeightPx.value).padding(top = 16.dp * rowHeightPx.value).graphicsLayer { this.alpha = alpha.value; this.clip = true }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(36.dp).alpha(alpha.value), color = color, trackColor = color.copy(alpha = 0.2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    val showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)
    
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, OpenRouterDefaultBaseUrl)
    val openRouterModel by rememberPreference(OpenRouterModelKey, OpenRouterDefaultModel)
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    val aiSystemPrompt by rememberPreference(AiSystemPromptKey, "")
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val translationStatus by LyricsTranslationHelper.status.collectAsStateWithLifecycle()
    val currentLyricsEntity by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    
    val lyrics = remember(currentLyricsEntity) { currentLyricsEntity?.lyrics?.trim() }
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    // --- 🔥 ROBUST LOCAL PARSING LOGIC (BYPASSES BUGGY VIEWMODEL) 🔥 ---
    val lines = remember(lyrics) {
        when {
            lyrics.isNullOrBlank() || lyrics == LYRICS_NOT_FOUND -> emptyList()
            lyrics.contains("<tt") || lyrics.contains("xmlns=\"http://www.w3.org/ns/ttml\"") -> {
                try {
                    // Try using M3-Play's native parser first
                    LyricsUtils.parseTtml(lyrics)
                } catch (t: Throwable) {
                    // FALLBACK: If native parser fails, manually strip XML and extract time
                    val entries = mutableListOf<LyricsEntry>()
                    val pTagRegex = Regex("<p\\s+begin=\"([^\"]+)\"[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
                    val matches = pTagRegex.findAll(lyrics).toList()
                    
                    if (matches.isNotEmpty()) {
                        matches.forEach { matchResult ->
                            val timeStr = matchResult.groupValues[1]
                            val textWithTags = matchResult.groupValues[2]
                            var timeMs = 0L
                            try {
                                val parts = timeStr.split(":")
                                if (parts.size >= 2) {
                                    val m = parts[parts.size - 2].toLong()
                                    val sParts = parts[parts.size - 1].split(".")
                                    val s = sParts[0].toLong()
                                    val ms = if (sParts.size > 1) sParts[1].padEnd(3, '0').substring(0, 3).toLong() else 0L
                                    timeMs = m * 60000 + s * 1000 + ms
                                }
                            } catch (e: Exception) {}
                            
                            // Remove inner XML tags (like <span>)
                            val text = textWithTags.replace(Regex("<[^>]*>"), "").trim()
                            if (text.isNotBlank()) {
                                entries.add(LyricsEntry(timeMs, text))
                            }
                        }
                        entries
                    } else {
                        // Ultimate Fallback: Just strip all XML tags and show as plain text
                        val cleanText = lyrics.replace(Regex("<[^>]*>"), "").replace(Regex("(?m)^[ \t]*\r?\n"), "").trim()
                        cleanText.lines().filter { it.isNotBlank() }.mapIndexed { i, l -> LyricsEntry(i * 1000L, l.trim()) }
                    }
                }
            }
            lyrics.trim().startsWith("[") -> LyricsUtils.parseLyrics(lyrics)
            else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { i, l -> LyricsEntry(i * 1000L, l.trim()) }
        }
    }

    val mergedLyricsList = remember(lines, showIntervalIndicator) {
        val list = mutableListOf<LyricsListItem>()
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) list.add(LyricsListItem.Line(i, entry))
            val isSynced = lines.any { it.time > 0 }
            if (showIntervalIndicator && isSynced && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) (entry.words.last().endTime * 1000).toLong() else entry.time
                if (currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) list.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                }
            }
        }
        list
    }
    // ---------------------------------------------------------------------

    val isSynced = remember(lines) { lines.any { it.time > 0L } }

    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose { LyricsTranslationHelper.setCompositionActive(false); LyricsTranslationHelper.cancelTranslation() }
    }
    
    LaunchedEffect(showLyrics, lines) {
        LyricsTranslationHelper.manualTrigger.collectLatest {
            val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
            if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                LyricsTranslationHelper.translateLyrics(lines, translateLanguage, openRouterApiKey, openRouterBaseUrl, openRouterModel, translateMode, scope, context, aiProvider, deeplApiKey, deeplFormality, true, mediaMetadata?.id ?: "", database, aiSystemPrompt)
            } else if (effectiveApiKey.isBlank()) {
                Toast.makeText(context, "API Key required for AI Translation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        else -> Color.White
    }

    var currentPositionState by remember { mutableLongStateOf(0L) }
    var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    var showShareDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    BackHandler(enabled = isSelectionModeActive) { isSelectionModeActive = false; selectedIndices.clear() }

    val activeLineIndices = remember(lines, currentPositionState) {
        val index = lines.indexOfLast { it.time <= currentPositionState }
        if (index != -1) setOf(index) else emptySet()
    }

    LaunchedEffect(lyrics, lines) {
        if (lyrics.isNullOrEmpty() || lines.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(16)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPositionState = position
            
            if (isAutoScrollEnabled) {
                deferredCurrentLineIndex = activeLineIndices.maxOrNull() ?: -1
            }
        }
    }

    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, mergedLyricsList) { mutableStateMapOf<Int, Int>() }

    val activeListIndex by remember(mergedLyricsList, deferredCurrentLineIndex) {
        derivedStateOf {
            mergedLyricsList.indexOfFirst { 
                (it is LyricsListItem.Line && it.index == deferredCurrentLineIndex) ||
                (it is LyricsListItem.Indicator && it.afterLineIndex == deferredCurrentLineIndex)
            }.coerceAtLeast(0)
        }
    }

    BoxWithConstraints(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize().padding(bottom = 12.dp)) {
        val anchorY = constraints.maxHeight.toFloat() * 0.35f
        val lineHeightPx = with(density) { 68.dp.toPx() }

        val positions = remember(itemHeights.toMap(), activeListIndex, mergedLyricsList) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex == -1 || mergedLyricsList.isEmpty()) return@remember map
            map[activeListIndex] = 0f
            var currentY = 0f
            for (i in activeListIndex - 1 downTo 0) {
                val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY -= (height + with(density) { 16.dp.toPx() })
                map[i] = currentY
            }
            currentY = 0f
            for (i in activeListIndex until mergedLyricsList.size - 1) {
                val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY += (height + with(density) { 16.dp.toPx() })
                map[i + 1] = currentY
            }
            map
        }

        LaunchedEffect(isAutoScrollEnabled, lines) {
            if (isAutoScrollEnabled && abs(userManualOffset) > 1f) {
                Animatable(userManualOffset).animateTo(0f, tween(400, easing = FastOutSlowInEasing)) { userManualOffset = value }
            }
        }

        LyricsTranslationHeader(status = translationStatus, modifier = Modifier.zIndex(1f).padding(top = 56.dp))

        if (lyrics == LYRICS_NOT_FOUND) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "Lyrics not found", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.alpha(0.5f)) }
        } else if (lyrics == null) {
             Column(modifier = Modifier.padding(top = 100.dp)) { ShimmerHost { repeat(10) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) { TextPlaceholder() } } } }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().fadingEdge(top = 130.dp, bottom = 160.dp).clipToBounds()
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source == NestedScrollSource.UserInput) isAutoScrollEnabled = false
                                return super.onPostScroll(consumed, available, source)
                            }
                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                isAutoScrollEnabled = false
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                flingJob?.cancel()
                                velocityTracker.resetTracking()
                                isAutoScrollEnabled = false
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                verticalDrag(down.id) { change ->
                                    userManualOffset += change.positionChange().y
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }
                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                        userManualOffset = value
                                    }
                                }
                            }
                        }
                    }
            ) {
                mergedLyricsList.forEachIndexed { listIndex, listItem ->
                    val distance = abs(listIndex - activeListIndex)
                    val targetOffset = anchorY + (positions[listIndex] ?: ((listIndex - activeListIndex) * lineHeightPx))
                    val animatedOffset by animateFloatAsState(targetValue = targetOffset, animationSpec = tween(750, (distance * 20).coerceAtMost(200), FastOutSlowInEasing), label = "offset_$listIndex")

                    Box(modifier = Modifier.fillMaxWidth().layout { m, c -> val p = m.measure(c.copy(maxHeight = Constraints.Infinity)); layout(p.width, 0) { p.place(0, 0) } }.offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }) {
                        when (listItem) {
                            is LyricsListItem.Indicator -> {
                                val visible = isAutoScrollEnabled && currentPositionState >= listItem.gapStartMs && currentPositionState <= listItem.gapEndMs - 650L
                                IntervalIndicator(listItem.gapStartMs, listItem.gapEndMs - 650L, currentPositionState, visible, expressiveAccent, Modifier.fillMaxWidth().onSizeChanged { itemHeights[listIndex] = it.height }.padding(horizontal = 24.dp).wrapContentWidth(Alignment.CenterHorizontally))
                            }
                            is LyricsListItem.Line -> {
                                LyricsLine(
                                    index = listItem.index, item = listItem.entry, isSynced = isSynced, isActiveLine = activeLineIndices.contains(listItem.index),
                                    bgVisible = false, isSelected = selectedIndices.contains(listItem.index),
                                    isSelectionModeActive = isSelectionModeActive, currentPositionState = currentPositionState,
                                    lyricsOffset = 0L, playerConnection = playerConnection,
                                    lyricsTextSize = 34f, lyricsLineSpacing = 1.3f, expressiveAccent = expressiveAccent,
                                    lyricsTextPosition = lyricsTextPosition, respectAgentPositioning = respectAgentPositioning,
                                    isAutoScrollEnabled = isAutoScrollEnabled, displayedCurrentLineIndex = deferredCurrentLineIndex,
                                    romanizeAsMain = false, enabledLanguages = emptyList(), romanizeLyrics = false,
                                    onSizeChanged = { itemHeights[listIndex] = it },
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (selectedIndices.contains(listItem.index)) { selectedIndices.remove(listItem.index); if (selectedIndices.isEmpty()) isSelectionModeActive = false } 
                                            else selectedIndices.add(listItem.index)
                                        } else if (changeLyrics) {
                                            playerConnection.player.seekTo(listItem.entry.time)
                                            isAutoScrollEnabled = true
                                        }
                                    },
                                    onLongClick = { if (!isSelectionModeActive) { isSelectionModeActive = true; selectedIndices.add(listItem.index) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        LyricsActionOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            isAutoScrollEnabled = isAutoScrollEnabled, isSynced = isSynced, isSelectionModeActive = isSelectionModeActive, anySelected = selectedIndices.isNotEmpty(),
            onSyncClick = { isAutoScrollEnabled = true },
            onCancelSelection = { isSelectionModeActive = false; selectedIndices.clear() },
            onShareSelection = {
                val text = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
                if (text.isNotBlank()) { shareDialogData = Triple(text, mediaMetadata?.title ?: "", mediaMetadata?.artists?.joinToString { it.name } ?: ""); showShareDialog = true }
                isSelectionModeActive = false; selectedIndices.clear()
            }
        )
    }

    if (showShareDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        LyricsShareDialog(txt = txt, title = title, arts = arts, songId = mediaMetadata?.id ?: "", onDismiss = { showShareDialog = false }, onShareAsImage = { showShareDialog = false; showColorPickerDialog = true })
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        LyricsColorPickerDialog(
            txt = txt, title = title, arts = arts, thumbnailUrl = mediaMetadata?.thumbnailUrl, lyricsTextPosition = lyricsTextPosition, onDismiss = { showColorPickerDialog = false },
            onShare = { bgColor, textColor, secTextColor, style ->
                showColorPickerDialog = false
                scope.launch {
                    try {
                        val image = ComposeToImage.createLyricsImage(context, mediaMetadata?.thumbnailUrl, title, arts, txt, (configuration.screenWidthDp * density.density).toInt(), (configuration.screenHeightDp * density.density).toInt(), bgColor.toArgb(), textColor.toArgb(), secTextColor.toArgb(), null)
                        val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Lyrics"))
                    } catch (e: Exception) { Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }
}
