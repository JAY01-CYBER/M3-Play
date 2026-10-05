package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import com.mocharealm.accompanist.lyrics.ui.internal.layout.lyricsAutoScroll
import com.mocharealm.accompanist.lyrics.ui.internal.layout.settledHeight
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.PreparedLineText
import com.mocharealm.accompanist.lyrics.ui.internal.rendering.LyricsTexturePrefetch
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.mocharealm.accompanist.lyrics.ui.internal.effects.lyricsEdgeFade
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.list.*
import com.mocharealm.accompanist.lyrics.ui.internal.layout.LyricsLazyColumn
import com.mocharealm.accompanist.lyrics.ui.internal.scene.rememberLyricsScene
import com.mocharealm.accompanist.lyrics.ui.profile.DefaultLyricsProfiles
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile
import com.mocharealm.accompanist.lyrics.ui.internal.scene.LyricsLayoutRequest
import com.mocharealm.accompanist.lyrics.ui.internal.text.isRtl

/**
 * Prepares a complete scene when content, profiles, typography or available width changes. Playback
 * writes only to the timeline and active row clocks; lazy items never measure karaoke text. Custom
 * profiles precede the defaults (first match wins).
 */
@Composable
fun KaraokeLyricsView(
    listState: LyricsLazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle =
        LocalTextStyle.current.copy(
            fontSize = 34.sp,
            lineHeight = TextUnit.Unspecified,
            fontWeight = FontWeight.Bold,
            textMotion = TextMotion.Animated,
        ),
    accompanimentLineTextStyle: TextStyle =
        LocalTextStyle.current.copy(
            fontSize = 20.sp,
            lineHeight = TextUnit.Unspecified,
            fontWeight = FontWeight.Bold,
            textMotion = TextMotion.Animated,
        ),
    textColor: Color = Color.White,
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    phoneticTextStyle: TextStyle =
        normalLineTextStyle.copy(
            fontSize = 13.sp,
            lineHeight = TextUnit.Unspecified,
            fontWeight = FontWeight.Normal,
        ),
    blendMode: BlendMode = BlendMode.Plus,
    useBlurEffect: Boolean = true,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    anchor: LyricsAnchor = LyricsAnchor.Fixed(40.dp),
    topFade: LyricsFade = LyricsFade.ToAnchor(16.dp),
    bottomFade: LyricsFade = LyricsFade.Fraction(0.5f),
    keepAliveZone: Dp = 100.dp,
    blurDelta: Float = 3f,
    showDebugRectangles: Boolean = false,
    renderProfiles: List<LyricsProfile> = DefaultLyricsProfiles,
    itemSpacing: Dp = 16.dp,
    scrollAnimationSpec: AnimationSpec<Float> = tween(650, easing = FastOutSlowInEasing),
    autoScrollResumeDelayMillis: Long = 2500,
    scrollChain: LyricsScrollChain? = LyricsScrollChain(),
) {
    val currentProvider by rememberUpdatedState(currentPosition)
    val timeProvider = remember { { currentProvider() } }
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val profiles =
        remember(renderProfiles) {
            renderProfiles + DefaultLyricsProfiles.filter { it !in renderProfiles }
        }
    BoxWithConstraints(modifier.clipToBounds()) {
        val anchorOffset = when (anchor) {
            is LyricsAnchor.Fixed -> anchor.offset.coerceAtMost(maxHeight)
            is LyricsAnchor.Fraction -> maxHeight * anchor.fraction
        }
        val width = with(density) { (maxWidth - 32.dp).toPx() }.coerceAtLeast(1f)
        val scene = rememberLyricsScene(
            LyricsLayoutRequest(
                lyrics, profiles, measurer, normalLineTextStyle, accompanimentLineTextStyle,
                phoneticTextStyle, width, density, direction,
            ),
            textColor,
            timeProvider,
        ) ?: return@BoxWithConstraints
        val timeline = scene.timeline
        // A single main-vocal row, excluding phonetics, translation and nested vocals.
        // Reuse prepared metrics so font size, line height and font scale stay consistent.
        val breathingLineHeight = remember(scene.lyrics, density) {
            val height = scene.lyrics.lines.asSequence()
                .filterNotNull()
                .filter { it.source is KaraokeLine.MainKaraokeLine }
                .flatMap { it.rows.asSequence() }
                .map { it.height - it.phoneticHeight }
                .maxOrNull()
            if (height == null) 40.dp else with(density) { height.toDp() }
        }
        val previousCaptions =
            remember(timeline, listState) { booleanArrayOf(showTranslation, showPhonetic) }
        SideEffect {
            if (previousCaptions[0] != showTranslation || previousCaptions[1] != showPhonetic) {
                listState.preserveFollowAnchorForContentChange()
                previousCaptions[0] = showTranslation
                previousCaptions[1] = showPhonetic
            }
        }
        val focus by timeline.focus
        val mapping = scene.items
        val sourceIndices = mapping.sourceIndices
        val translationShown = rememberUpdatedState(showTranslation)
        LyricsTexturePrefetch(
            mapping.lines,
            scene.resources,
            listState,
        ) {
            mapping.itemIndex(timeline.focus.value.firstIndex)
        }
        val phoneticShown = rememberUpdatedState(showPhonetic)
        SideEffect { listState.setInterludeItem(focus.activeInterludeIndex?.let(mapping::itemIndex) ?: -1) }
        val listItems =
            remember(scene.lyrics, timeline, mapping) {
                sourceIndices.map { index ->
                    val line = scene.lyrics.lines[index]
                    LyricsListItem(
                        key = "${lyrics.lines[index].start}-${lyrics.lines[index].end}-$index",
                        preserveAnchorOnHeightChange = false,
                        settledHeightPx =
                            line?.let { prepared ->
                                {
                                    prepared
                                        .settledHeight(
                                            timeline.state,
                                            translationShown.value,
                                            phoneticShown.value,
                                            density.density,
                                            index in timeline.focus.value.allIndices,
                                        )
                                        .toInt()
                                }
                            },
                        // Follow the aggregate's top, including any leading accompaniment.
                        // Offsetting by `before` would scroll those vocals above the anchor.
                        estimatedHeightPx =
                            ((line?.height ?: 0f) +
                                    (if (line?.translation != null || line?.phonetic != null)
                                        8f * density.density
                                    else 0f) +
                                    (line?.translation?.size?.height ?: 0) +
                                    (line?.phonetic?.size?.height ?: 0))
                                .toInt(),
                    )
                }
            }
        val followModifier =
            lyricsAutoScroll(
                listState,
                targetIndex = {
                    mapping.itemIndex(timeline.focus.value.firstIndex)
                },
                animationSpec = scrollAnimationSpec,
                resumeDelayMillis = autoScrollResumeDelayMillis,
                focusEndIndex = {
                    mapping.itemIndex(
                        timeline.focus.value.allIndices.lastOrNull()
                            ?: timeline.focus.value.firstIndex
                    )
                },
                playbackPosition = timeProvider,
            )
        // One shared animation, sampled only by graphics layers; no per-frame item recomposition.
        val focusBlurFactor =
            animateFloatAsState(
                targetValue = if (listState.isManualScrolling) 0f else 1f,
                animationSpec = tween(if (listState.isManualScrolling) 100 else 300),
                label = "manualScrollFocusBlur",
            )
        LyricsLazyColumn(
            items = listItems,
            state = listState,
            itemSpacing = itemSpacing,
            scrollChain = scrollChain,
            beyondBounds = keepAliveZone,
            contentPadding = PaddingValues(top = anchorOffset, bottom = maxHeight + keepAliveZone),
            modifier =
                Modifier.fillMaxSize()
                    .then(followModifier)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        this.blendMode = blendMode
                    }
                    .lyricsEdgeFade(topFade, bottomFade, anchorOffset),
        ) { itemIndex ->
            val index = sourceIndices[itemIndex]
            val line = lyrics.lines[index]
            val prepared = scene.lyrics.lines[index]
            val focused = index in focus.allIndices
            val distance =
                maxOf(
                    0,
                    (focus.allIndices.firstOrNull() ?: focus.firstIndex) - index,
                    index - (focus.allIndices.lastOrNull() ?: focus.firstIndex),
                )
            val blur by
            animateFloatAsState(if (useBlurEffect) distance * blurDelta else 0f, tween(300))
            Column {
                if (index == 0 && focus.activeIntro)
                    KaraokeBreathingDots(
                        alignment =
                            if (prepared?.rightAligned == true)
                                KaraokeAlignment
                                    .End
                            else
                                KaraokeAlignment
                                    .Start,
                        startTimeMs = 0,
                        endTimeMs = timeline.introEnd,
                        currentTimeProvider = timeProvider,
                        defaults = breathingDotsDefaults,
                        trailingSpacing = itemSpacing,
                        lineHeight = breathingLineHeight,
                    )
                if (focus.activeInterludeIndex == index && index > 0)
                    KaraokeBreathingDots(
                        alignment =
                            if (prepared?.rightAligned == true)
                                KaraokeAlignment
                                    .End
                            else
                                KaraokeAlignment
                                    .Start,
                        startTimeMs = timeline.interludeStarts[index],
                        endTimeMs = timeline.interludeEnds[index],
                        currentTimeProvider = timeProvider,
                        defaults = breathingDotsDefaults,
                        trailingSpacing = itemSpacing,
                        lineHeight = breathingLineHeight,
                    )
                if (prepared !in scene.lyrics.embeddedLines)
                    LyricsLineItem(
                        isFocused = focused,
                        isRightAligned =
                            prepared?.rightAligned
                                ?: ((line as? SyncedLine)?.content?.isRtl() == true),
                        onLineClicked = {
                            listState.resumeAutoScroll(line.start)
                            onLineClicked(line)
                        },
                        onLinePressed = { onLinePressed(line) },
                        blurRadius = { blur * focusBlurFactor.value },
                    ) {
                        if (prepared != null)
                            PreparedLineText(
                                prepared,
                                timeline.state,
                                scene.resources,
                                verticalPadding = 0.dp,
                                modifier =
                                    if (line is KaraokeLine.AccompanimentKaraokeLine)
                                        Modifier.padding(horizontal = 16.dp)
                                    else Modifier,
                                showTranslation = showTranslation,
                                showPhonetic = showPhonetic,
                                showDebugRectangles = showDebugRectangles,
                            )
                    }
            }
        }
    }
}
