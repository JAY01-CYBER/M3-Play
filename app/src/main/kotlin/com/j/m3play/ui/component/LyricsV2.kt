/*
 * M3Play Component Module 
 * Signature: M3PLAY::COMPONENT
 *
 * Credits: 
 * Accompanist Lyrics Library  Integration.
 */

package com.j.m3play.ui.component

import android.annotation.SuppressLint
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// Accompanist Library Imports
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView

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
private const val MIN_KARAOKE_SYLLABLE_DURATION_MS = 1
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")

private const val SMOOTH_PLAYBACK_MAX_FORWARD_DRIFT_MS = 80L
private const val SMOOTH_PLAYBACK_MAX_BACKWARD_DRIFT_MS = 180L
private const val SMOOTH_PLAYBACK_DRIFT_CORRECTION = 0.55f

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
        }
    }

    // Clean SyncedLyrics conversion for Accompanist Library
    val syncedLyrics = remember(entriesWithWords, isTtmlFormat) {
        buildSyncedLyrics(entriesWithWords, isTtmlFormat)
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

        val listState = rememberLazyListState()
        val lyricsViewportOffset = remember(constraints.maxHeight) { constraints.maxHeight * 0.38f }

        if (isSynced && syncedLyrics.lines.isNotEmpty()) {
            // Accompanist Library Karaoke View matching ArchiveTune signatures
            KaraokeLyricsView(
                listState = listState,
                lyrics = syncedLyrics,
                currentPosition = { 
                    (currentPositionMs + leadMs + LYRIC_VISUAL_TUNING_OFFSET_MS)
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt() 
                },
                onLineClicked = { line ->
                    if (isSelectionModeActive) {
                        val index = syncedLyrics.lines.indexOf(line)
                        if (index >= 0) {
                            if (selectedIndices.contains(index)) {
                                selectedIndices.remove(index)
                                if (selectedIndices.isEmpty()) isSelectionModeActive = false
                            } else {
                                if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index) else showMaxSelectionToast = true
                            }
                        }
                    } else if (lyricsClick && line.start > 0) {
                        player.seekTo(line.start.toLong())
                    }
                },
                onLinePressed = { line ->
                    val index = syncedLyrics.lines.indexOf(line)
                    if (index >= 0) {
                        if (!isSelectionModeActive) {
                            isSelectionModeActive = true
                            selectedIndices.add(index)
                        } else if (!selectedIndices.contains(index) && selectedIndices.size < maxSelectionLimit) {
                            selectedIndices.add(index)
                        } else if (!selectedIndices.contains(index)) {
                            showMaxSelectionToast = true
                        }
                    }
                },
                textColor = textColor,
                normalLineTextStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = lyricsTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                ),
                accompanimentLineTextStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (lyricsTextSize * 0.82f).sp,
                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.titleLarge.fontFamily
                ),
                phoneticTextStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (lyricsTextSize * 0.55f).sp
                ),
                blendMode = BlendMode.SrcOver,
                useBlurEffect = true,
                showTranslation = false,
                showPhonetic = romanizeJapanese || romanizeKorean,
                offset = lyricsViewportOffset,
                keepAliveZone = 72.dp,
                modifier = Modifier.fillMaxSize().smoothFadingEdge(vertical = 80.dp)
            )
        } else {
            // Fallback for Plain Lyrics
            LazyColumn(
                modifier = Modifier.fillMaxSize().smoothFadingEdge(vertical = 80.dp),
                contentPadding = PaddingValues(top = 100.dp, bottom = 150.dp)
            ) {
                itemsIndexed(entriesWithWords) { index, item ->
                    Text(
                        text = item.text,
                        color = textColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = lyricsTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                            lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                            .combinedClickable(
                                onLongClick = {
                                    if (!isSelectionModeActive) {
                                        isSelectionModeActive = true; selectedIndices.add(index)
                                    }
                                },
                                onClick = {
                                    if (isSelectionModeActive) {
                                        if (selectedIndices.contains(index)) {
                                            selectedIndices.remove(index)
                                            if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                        } else {
                                            if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                        }
                                    }
                                }
                            )
                            .background(if (selectedIndices.contains(index)) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                    )
                }
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
}

// Helper Converter for Accompanist Library Sync
private fun List<WordTimestamp>.toKaraokeSyllables(): List<KaraokeSyllable> =
    mapIndexed { index, word ->
        val start = (word.startTime * 1000.0).roundToInt().coerceAtLeast(0)
        val nextStart = getOrNull(index + 1)?.let { (it.startTime * 1000.0).roundToInt() }
        val rawEnd = (word.endTime * 1000.0).roundToInt()
        val end = nextStart?.let { minOf(rawEnd, it) } ?: rawEnd

        KaraokeSyllable(
            content = word.text,
            start = start,
            end = end.coerceAtLeast(start + MIN_KARAOKE_SYLLABLE_DURATION_MS),
            phonetic = null,
        )
    }

private fun buildSyncedLyrics(entries: List<LyricsEntry>, isTtml: Boolean): SyncedLyrics {
    if (entries.isEmpty()) return SyncedLyrics(emptyList())
    val lines = mutableListOf<ISyncedLine>()

    entries.forEachIndexed { index, entry ->
        if (entry.time < 0L) return@forEachIndexed
        if (entry.text.isBlank() && entry.words.isNullOrEmpty()) return@forEachIndexed

        if (isTtml && entry.words != null) {
            val mainWords = entry.words!!.filter { !it.isBackground }
            val bgWords = entry.words!!.filter { it.isBackground }
            val alignment =
                when (entry.agent?.lowercase()) {
                    "v2" -> KaraokeAlignment.End
                    else -> KaraokeAlignment.Start
                }

            val wordsForMain = if (mainWords.isNotEmpty()) mainWords else entry.words!!
            val mainSyllables = wordsForMain.toKaraokeSyllables()

            if (mainSyllables.isEmpty()) return@forEachIndexed
            val lineStart = mainSyllables.first().start
            val lineEnd = mainSyllables.last().end

            val accompanimentLines =
                if (mainWords.isNotEmpty() && bgWords.isNotEmpty()) {
                    val bgSyllables = bgWords.toKaraokeSyllables()
                    val bgStart = bgSyllables.firstOrNull()?.start ?: lineStart
                    val bgEnd = bgSyllables.lastOrNull()?.end ?: lineEnd
                    if (bgEnd > bgStart) {
                        listOf(
                            KaraokeLine.AccompanimentKaraokeLine(
                                syllables = bgSyllables,
                                translation = null,
                                alignment = alignment,
                                start = bgStart,
                                end = bgEnd,
                                phonetic = null,
                            ),
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }

            lines.add(
                KaraokeLine.MainKaraokeLine(
                    syllables = mainSyllables,
                    translation = null,
                    alignment = alignment,
                    start = lineStart,
                    end = lineEnd,
                    phonetic = null,
                    accompanimentLines = accompanimentLines,
                ),
            )
        } else {
            val nextEntry = entries.getOrNull(index + 1)
            val lineEnd =
                if (nextEntry != null && nextEntry.time > entry.time) {
                    (nextEntry.time - 1L).coerceAtLeast(entry.time + 1L).toInt()
                } else {
                    (entry.time + 4000L).toInt()
                }
            lines.add(
                SyncedLine(
                    content = entry.text,
                    translation = null,
                    start = entry.time.toInt(),
                    end = lineEnd,
                ),
            )
        }
    }

    return SyncedLyrics(lines = lines)
}
