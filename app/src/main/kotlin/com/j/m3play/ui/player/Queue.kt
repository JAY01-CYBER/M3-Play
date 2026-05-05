/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.player

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.move
import com.j.m3play.extensions.togglePlayPause
import com.j.m3play.extensions.toggleRepeatMode
import com.j.m3play.models.MediaMetadata
import com.j.m3play.ui.component.*
import com.j.m3play.ui.menu.PlayerMenu
import com.j.m3play.ui.menu.SelectionMediaMetadataMenu
import com.j.m3play.ui.utils.ShowMediaInfo
import com.j.m3play.utils.makeTimeString
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.datastore.preferences.core.booleanPreferencesKey

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    onShowLyrics: () -> Unit = {},
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }
    var selection by remember { mutableStateOf(false) }

    if (selection) { BackHandler { selection = false } }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)
    var infiniteQueueEnabled by rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val togetherForcesLock = togetherSessionState is com.j.m3play.together.TogetherSessionState.Joined && (togetherSessionState as com.j.m3play.together.TogetherSessionState.Joined).role is com.j.m3play.together.TogetherRole.Guest
    val effectiveLocked = locked || togetherForcesLock

    val playerDesignStyle by rememberEnumPreference(key = PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableFloatStateOf(30f) }
    val sleepTimerEnabled = playerConnection.service.sleepTimer.isActive
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    val (showCodecOnPlayer) = rememberPreference(key = booleanPreferencesKey("show_codec_on_player"), defaultValue = false)

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        backgroundColor = Color.Unspecified,
        modifier = modifier,
        collapsedContent = {
            when (playerDesignStyle) {
                PlayerDesignStyle.V2 -> QueueCollapsedContentV2(showCodecOnPlayer, currentFormat, TextBackgroundColor, textButtonColor, iconButtonColor, sleepTimerEnabled, sleepTimerTimeLeft, repeatMode, mediaMetadata, { state.expandSoft() }, { if (sleepTimerEnabled) playerConnection.service.sleepTimer.clear() else showSleepTimerDialog = true }, onShowLyrics, { playerConnection.player.toggleRepeatMode() }, { menuState.show { PlayerMenu(mediaMetadata, navController, playerBottomSheetState, onShowDetailsDialog = { mediaMetadata?.id?.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } })
                PlayerDesignStyle.V3, PlayerDesignStyle.V5, PlayerDesignStyle.APPLE -> QueueCollapsedContentV3(showCodecOnPlayer, currentFormat, TextBackgroundColor, sleepTimerEnabled, sleepTimerTimeLeft, { state.expandSoft() }, { if (sleepTimerEnabled) playerConnection.service.sleepTimer.clear() else showSleepTimerDialog = true }, onShowLyrics, { menuState.show { PlayerMenu(mediaMetadata, navController, playerBottomSheetState, onShowDetailsDialog = { mediaMetadata?.id?.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } })
                PlayerDesignStyle.V4, PlayerDesignStyle.V6 -> QueueCollapsedContentV4(showCodecOnPlayer, currentFormat, TextBackgroundColor, textButtonColor, iconButtonColor, sleepTimerEnabled, sleepTimerTimeLeft, mediaMetadata, { state.expandSoft() }, { if (sleepTimerEnabled) playerConnection.service.sleepTimer.clear() else showSleepTimerDialog = true }, onShowLyrics)
                PlayerDesignStyle.V1 -> QueueCollapsedContentV1(showCodecOnPlayer, currentFormat, TextBackgroundColor, sleepTimerEnabled, sleepTimerTimeLeft, { state.expandSoft() }, { if (sleepTimerEnabled) playerConnection.service.sleepTimer.clear() else showSleepTimerDialog = true }, onShowLyrics)
            }
            if (showSleepTimerDialog) {
                SleepTimerDialog(onDismiss = { showSleepTimerDialog = false }, onConfirm = { showSleepTimerDialog = false; playerConnection.service.sleepTimer.start(it) }, onEndOfSong = { showSleepTimerDialog = false; playerConnection.service.sleepTimer.start(-1) }, initialValue = sleepTimerValue)
            }
        },
    ) {
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val automix by playerConnection.service.automixItems.collectAsState()
        val automixLoading by playerConnection.service.automixLoading.collectAsState()
        val automixError by playerConnection.service.automixError.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength by remember { derivedStateOf { queueWindows.sumOf { it.mediaItem.metadata!!.duration } } }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(automixError) {
            automixError?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
            playerConnection.service.automixError.value = null
        }

        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val currentPlayingUid = if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null

        val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            dragInfo = (dragInfo?.first ?: from.index) to to.index
            mutableQueueWindows.move(from.index - 1, to.index - 1)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - 1).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - 1).coerceIn(0, queueWindows.lastIndex)
                    if (!playerConnection.player.shuffleModeEnabled) playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    else playerConnection.player.setShuffleOrder(DefaultShuffleOrder(queueWindows.map { it.firstPeriodIndex }.toMutableList().move(safeFrom, safeTo).toIntArray(), System.currentTimeMillis()))
                }
                dragInfo = null
            }
        }

        LaunchedEffect(queueWindows) { mutableQueueWindows.clear(); mutableQueueWindows.addAll(queueWindows) }

        Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CurrentSongHeader(state, mediaMetadata, isPlaying, repeatMode, playerConnection.player.shuffleModeEnabled, effectiveLocked, queueWindows.size, queueLength, infiniteQueueEnabled, automixLoading, backgroundColor, onBackgroundColor, { playerConnection.service.toggleLike() }, { menuState.show { PlayerMenu(mediaMetadata, navController, playerBottomSheetState, onShowDetailsDialog = { mediaMetadata?.id?.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }, { playerConnection.player.toggleRepeatMode() }, { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled }, { if (togetherForcesLock) Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show() else locked = !locked }, { infiniteQueueEnabled = !infiniteQueueEnabled; if (infiniteQueueEnabled) playerConnection.service.onInfiniteQueueEnabled() else playerConnection.service.onInfiniteQueueDisabled() })

                LazyColumn(state = lazyListState, modifier = Modifier.weight(1f).nestedScroll(state.preUpPostDownNestedScrollConnection)) {
                    item { Spacer(modifier = Modifier.animateContentSize().height(if (selection) 48.dp else 0.dp)) }
                    itemsIndexed(items = mutableQueueWindows, key = { _, item -> item.uid.hashCode() }) { index, window ->
                        ReorderableItem(reorderableState, key = window.uid.hashCode()) {
                            val isActive = window.uid == currentPlayingUid
                            val dismissBoxState = rememberSwipeToDismissBoxState()
                            LaunchedEffect(dismissBoxState.currentValue) {
                                if (dismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                                    playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                                    snackbarHostState.showSnackbar(context.getString(R.string.removed_song_from_playlist, window.mediaItem.metadata?.title), duration = SnackbarDuration.Short)
                                }
                            }
                            MediaMetadataListItem(window.mediaItem.metadata!!, selection && window.mediaItem.metadata!! in selectedSongs, isActive, isPlaying && isActive, true, {
                                IconButton(onClick = { menuState.show { PlayerMenu(window.mediaItem.metadata!!, navController, playerBottomSheetState, true, { window.mediaItem.mediaId.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                                if (!effectiveLocked) IconButton(onClick = {}, modifier = Modifier.draggableHandle()) { Icon(painterResource(R.drawable.drag_handle), null) }
                            }, Modifier.fillMaxWidth().background(backgroundColor).combinedClickable(onClick = { if (selection) { if (window.mediaItem.metadata!! in selectedSongs) selectedSongs.remove(window.mediaItem.metadata!!) else selectedSongs.add(window.mediaItem.metadata!!) } else { playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex); playerConnection.player.playWhenReady = true } }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); selection = true; selectedSongs.add(window.mediaItem.metadata!!) }))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selection, modifier = Modifier.align(Alignment.TopCenter)) {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surface), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selection = false; selectedSongs.clear() }) { Icon(painterResource(R.drawable.close), null) }
                    Text(text = stringResource(R.string.elements_selected, selectedSongs.size), modifier = Modifier.weight(1f))
                    IconButton(onClick = { menuState.show { SelectionMediaMetadataMenu(selectedSongs, { menuState::dismiss }, { selectedSongs.clear(); selection = false }, selectedItems) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                }
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
        }
    }
}
