/*
 * ╭────────────────────────────────────────────╮
 * │            M3Play Core Engine              │
 * │--------------------------------------------│
 * │  Java Serialization Queue Persistence      │
 * │  Signature: M3PLAY::CORE::ENGINE::V6       │
 * ╰────────────────────────────────────────────╯
 */

@file:Suppress("DEPRECATION")

package com.j.m3play.playback

import android.app.PendingIntent
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothClass
import android.content.pm.PackageManager
import android.database.SQLException
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioAttributes as LegacyAudioAttributes
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.MediaCodecList
import android.media.audiofx.Virtualizer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.j.m3play.innertube.YouTube
import com.j.m3play.innertube.models.YouTubeClient
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.lyrics.LyricsPreloadManager
import com.j.m3play.innertube.models.WatchEndpoint
import com.j.m3play.MainActivity
import com.j.m3play.R
import com.j.m3play.constants.AudioNormalizationKey
import com.j.m3play.constants.AudioOffload
import com.j.m3play.constants.AudioCrossfadeDurationKey
import com.j.m3play.constants.AudioQualityKey
import com.j.m3play.constants.AutoLoadMoreKey
import com.j.m3play.constants.AutoDownloadOnLikeKey
import com.j.m3play.constants.AutoSkipNextOnErrorKey
import com.j.m3play.constants.AutoStartOnBluetoothKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.DiscordTokenKey
import com.j.m3play.constants.EqualizerBandLevelsMbKey
import com.j.m3play.constants.EqualizerBassBoostEnabledKey
import com.j.m3play.constants.EqualizerBassBoostStrengthKey
import com.j.m3play.constants.EqualizerEnabledKey
import com.j.m3play.constants.EqualizerOutputGainEnabledKey
import com.j.m3play.constants.EqualizerOutputGainMbKey
import com.j.m3play.constants.EqualizerSelectedProfileIdKey
import com.j.m3play.constants.EqualizerVirtualizerEnabledKey
import com.j.m3play.constants.EqualizerVirtualizerStrengthKey
import com.j.m3play.constants.EnableDiscordRPCKey
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.HideVideoKey
import com.j.m3play.constants.HistoryDuration
import com.j.m3play.constants.MediaSessionConstants.CommandToggleLike
import com.j.m3play.constants.MediaSessionConstants.CommandToggleStartRadio
import com.j.m3play.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.j.m3play.constants.MediaSessionConstants.CommandToggleShuffle
import com.j.m3play.constants.PauseListenHistoryKey
import com.j.m3play.constants.PauseOnDeviceMuteKey
import com.j.m3play.constants.PermanentShuffleKey
import com.j.m3play.constants.PersistentQueueKey
import com.j.m3play.constants.PlayerStreamClient
import com.j.m3play.constants.PlayerStreamClientKey
import com.j.m3play.constants.PlayerVolumeKey
import com.j.m3play.constants.RepeatModeKey
import com.j.m3play.constants.ShowLyricsKey
import com.j.m3play.constants.SkipSilenceKey
import com.j.m3play.constants.MaxSongCacheSizeKey
import com.j.m3play.constants.SmartTrimmerKey
import com.j.m3play.constants.StopMusicOnTaskClearKey
import com.j.m3play.constants.WakelockKey
import com.j.m3play.constants.YtmSyncKey
import com.j.m3play.db.MusicDatabase
import com.j.m3play.db.entities.Event
import com.j.m3play.db.entities.FormatEntity
import com.j.m3play.db.entities.LyricsEntity
import com.j.m3play.db.entities.RelatedSongMap
import com.j.m3play.db.entities.Song
import com.j.m3play.db.entities.SongEntity
import com.j.m3play.db.entities.ArtistEntity
import com.j.m3play.db.entities.AlbumEntity
import com.j.m3play.di.DownloadCache
import com.j.m3play.di.PlayerCache
import com.j.m3play.extensions.SilentHandler
import com.j.m3play.extensions.collect
import com.j.m3play.extensions.collectLatest
import com.j.m3play.extensions.currentMetadata
import com.j.m3play.extensions.directorySizeBytes
import com.j.m3play.extensions.findNextMediaItemById
import com.j.m3play.extensions.mediaItems
import com.j.m3play.extensions.metadata
import com.j.m3play.extensions.setOffloadEnabled
import com.j.m3play.extensions.toMediaItem
import com.j.m3play.extensions.toPersistQueue
import com.j.m3play.extensions.toQueue
import com.j.m3play.lyrics.LyricsHelper
import com.j.m3play.models.PersistQueue
import com.j.m3play.models.PersistPlayerState
import com.j.m3play.models.QueueData
import com.j.m3play.models.QueueType
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.queues.EmptyQueue
import com.j.m3play.playback.queues.ListQueue
import com.j.m3play.playback.queues.Queue
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.playback.queues.filterExplicit
import com.j.m3play.playback.queues.filterVideo
import com.j.m3play.utils.CoilBitmapLoader
import com.j.m3play.utils.DiscordRPC
import com.j.m3play.ui.screens.settings.DiscordPresenceManager
import com.j.m3play.utils.SyncUtils
import com.j.m3play.utils.YTPlayerUtils
import com.j.m3play.utils.StreamClientUtils
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.enumPreference
import com.j.m3play.utils.get
import com.j.m3play.utils.getAsync
import com.j.m3play.utils.isInternetAvailable
import com.j.m3play.utils.getPresenceIntervalMillis
import com.j.m3play.utils.reportException
import com.j.m3play.utils.NetworkConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import com.j.m3play.ui.screens.settings.ListenBrainzManager
import com.j.m3play.constants.ListenBrainzEnabledKey
import com.j.m3play.constants.ListenBrainzTokenKey
import com.j.m3play.lastfm.LastFM
import com.j.m3play.constants.EnableLastFMScrobblingKey
import com.j.m3play.constants.LastFMSessionKey
import com.j.m3play.constants.LastFMUseNowPlaying
import com.j.m3play.constants.ScrobbleDelayPercentKey
import com.j.m3play.constants.ScrobbleMinSongDurationKey
import com.j.m3play.constants.ScrobbleDelaySecondsKey
import com.j.m3play.constants.TogetherClientIdKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var pauseOnDeviceMuteEnabled = false
    private var wasAutoPausedByDeviceMute = false
    private var hasAudioFocus = false
    private var autoStartOnBluetoothEnabled = false
    private var bluetoothReceiverRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakelockEnabled = false

    private var scopeJob = Job()
    private var scope = CoroutineScope(Dispatchers.Main + scopeJob)
    private var ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val binder = MusicBinder()
    private var hasBoundClients = false
    private var idleStopJob: Job? = null

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.j.m3play.constants.AudioQuality.AUTO
    )
    private val preferredStreamClient by enumPreference(
        this,
        PlayerStreamClientKey,
        PlayerStreamClient.ANDROID_VR
    )
    private val playbackUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val streamRecoveryState = ConcurrentHashMap<String, Pair<Int, Long>>()
    @Volatile
    private var pendingStreamRefreshValidationMediaId: String? = null
    @Volatile
    private var refreshValidatedPlayingMediaId: String? = null
    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }
    private val mediaOkHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .proxy(YouTube.streamProxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }.build()
    }

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null
    private val persistentStateLock = Any()
    @Volatile
    private var suppressAutoPlayback = false
    private var lastPresenceToken: String? = null
    @Volatile
    private var lastPresenceUpdateTime = 0L
    @Volatile
    private var lastLoginRecoveryPrompt: Pair<String, Long>? = null

    val currentMediaMetadata = MutableStateFlow<com.j.m3play.models.MediaMetadata?>(null)
    val queueRestoreCompleted = MutableStateFlow(false)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }.flowOn(Dispatchers.IO)

    private val normalizeFactor = MutableStateFlow(1f)
    var playerVolume = MutableStateFlow(1f)
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val crossfadeDurationMs = MutableStateFlow(0)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private var crossfadeAudio: CrossfadeAudio? = null
    private var lyricsPreloadManager: LyricsPreloadManager? = null

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        return appProcesses.any { processInfo ->
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                processInfo.processName == packageName
        }
    }

    private fun promptLoginRecovery(mediaId: String, targetUrl: String) {
        if (!isAppInForeground()) return

        val now = System.currentTimeMillis()
        val lastPrompt = lastLoginRecoveryPrompt
        if (lastPrompt?.first == mediaId && now - lastPrompt.second < 10000L) return
        lastLoginRecoveryPrompt = mediaId to now

        val deepLink = Uri.parse("m3play://login?url=${Uri.encode(targetUrl)}")
        val intent = Intent(Intent.ACTION_VIEW, deepLink, this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            Timber.e(it, "Failed to open login recovery for %s", mediaId)
        }
    }

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: Cache

    @Inject
    @DownloadCache
    lateinit var downloadCache: Cache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false
    private var openedAudioSessionId: Int? = null
    val eqCapabilities = MutableStateFlow<EqCapabilities?>(null)
    private val desiredEqSettings =
        MutableStateFlow(
            EqSettings(
                enabled = false,
                bandLevelsMb = emptyList(),
                outputGainEnabled = false,
                outputGainMb = 0,
                bassBoostEnabled = false,
                bassBoostStrength = 0,
                virtualizerEnabled = false,
                virtualizerStrength = 0,
            ),
        )

    private var audioEffectsSessionId: Int? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var discordRpc: DiscordRPC? = null
    private var lastDiscordUpdateTime = 0L

    private var scrobbleManager: com.j.m3play.utils.ScrobbleManager? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val automixLoading = MutableStateFlow(false)
    val automixError = MutableStateFlow<String?>(null)
    private var automixJob: Job? = null
    private var automixSeedMediaId: String? = null

    val autoAddedMediaIds: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())

    private var consecutivePlaybackErr = 0

    val maxSafeGainFactor = 1.414f 
    @Volatile
    private var hasCalledStartForeground = false

    val togetherSessionState = MutableStateFlow<com.j.m3play.together.TogetherSessionState>(
        com.j.m3play.together.TogetherSessionState.Idle,
    )
    private var togetherServer: com.j.m3play.together.TogetherServer? = null
    private var togetherOnlineHost: com.j.m3play.together.TogetherOnlineHost? = null
    private var togetherClient: com.j.m3play.together.TogetherClient? = null
    private var togetherBroadcastJob: Job? = null
    private var togetherOnlineConnectJob: Job? = null
    private var togetherClientEventsJob: Job? = null
    private var togetherHeartbeatJob: Job? = null
    private var togetherClock: com.j.m3play.together.TogetherClock? = null
    private var togetherSelfParticipantId: String? = null
    private var togetherLastAppliedQueueHash: String? = null
    private var togetherIsOnlineSession: Boolean = false
    @Volatile
    private var togetherApplyingRemote: Boolean = false
    @Volatile
    private var togetherSuppressEchoUntilElapsedMs: Long = 0L
    @Volatile
    private var togetherLastAppliedRoomStateSentAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastRemoteAppliedPlayWhenReady: Boolean? = null
    @Volatile
    private var togetherLastRemoteAppliedIndex: Int = -1
    @Volatile
    private var togetherLastSentControlAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastSentControlAction: com.j.m3play.together.ControlAction? = null
    @Volatile
    private var togetherPendingGuestControl: TogetherPendingGuestControl? = null

    private fun isTogetherApplyingRemote(): Boolean = togetherApplyingRemote
    private val togetherHostId: String = "host"
    private var lastTogetherNoticeAtElapsedMs: Long = 0L
    private var lastTogetherNoticeKey: String? = null

    private data class TogetherPendingGuestControl(
        val desiredIsPlaying: Boolean? = null,
        val desiredIndex: Int? = null,
        val desiredTrackId: String? = null,
        val requestedAtElapsedMs: Long,
        val expiresAtElapsedMs: Long,
    )

    private fun showTogetherNotice(message: String, key: String? = null) {
        val now = android.os.SystemClock.elapsedRealtime()
        val normalizedKey = key ?: message
        if (normalizedKey == lastTogetherNoticeKey && now - lastTogetherNoticeAtElapsedMs < 1200L) return
        lastTogetherNoticeKey = normalizedKey
        lastTogetherNoticeAtElapsedMs = now
        scope.launch(SilentHandler) {
            Toast.makeText(this@MusicService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getOrCreateTogetherClientId(): String {
        val existing = dataStore.getAsync(TogetherClientIdKey)?.trim().orEmpty()
        if (existing.isNotBlank()) return existing
        val generated = java.util.UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[TogetherClientIdKey] = generated }
        return generated
    }

    private fun ensureStartedAsForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (hasCalledStartForeground) return

        val notification =
            try {
                val contentIntent =
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.small_icon)
                    .setContentTitle(getString(R.string.music_player))
                    .setContentText(getString(R.string.app_name))
                    .setContentIntent(contentIntent)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
            } catch (e: Exception) {
                reportException(e)
                return
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            hasCalledStartForeground = true
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun promoteToStartedService() {
        runCatching { startService(Intent(this, MusicService::class.java)) }
            .onFailure { reportException(it) }
    }

    private fun cancelIdleStop() {
        idleStopJob?.cancel()
        idleStopJob = null
    }

    private fun stopForegroundAndSelf() {
        cancelIdleStop()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
        }
        hasCalledStartForeground = false
        stopSelf()
    }

    private fun scheduleStopIfIdle() {
        if (hasBoundClients) return
        val state = player.playbackState
        val keepAlive =
            player.isPlaying ||
            (player.playWhenReady && (state == Player.STATE_BUFFERING || state == Player.STATE_READY))
        if (keepAlive) {
            cancelIdleStop()
            return
        }
        val togetherIdle = togetherSessionState.value is com.j.m3play.together.TogetherSessionState.Idle
        if (!togetherIdle) {
            cancelIdleStop()
            return
        }

        val delayMs =
            when (state) {
                Player.STATE_READY -> 5 * 60_000L
                Player.STATE_ENDED, Player.STATE_IDLE -> 30_000L
                else -> 60_000L
            }

        cancelIdleStop()
        idleStopJob =
            scope.launch {
                delay(delayMs)
                if (hasBoundClients) return@launch
                val currentState = player.playbackState
                val shouldKeep =
                    player.isPlaying ||
                    (player.playWhenReady && (currentState == Player.STATE_BUFFERING || currentState == Player.STATE_READY))
                if (shouldKeep) return@launch
                if (togetherSessionState.value !is com.j.m3play.together.TogetherSessionState.Idle) return@launch
                stopForegroundAndSelf()
            }
    }

    override fun onCreate() {
        super.onCreate()
        ensureScopesActive()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.music_player),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        } catch (e: Exception) {
            reportException(e)
        }

        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .setDeviceVolumeControlEnabled(true)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                    setOffloadEnabled(false)
                }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "M3Play:Playback")
            .also { it.setReferenceCounted(false) }
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            ).apply {
                setSmallIcon(R.drawable.small_icon)
            }
        )
        
        updateNotification()
        player.repeatMode = REPEAT_MODE_OFF

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
        scope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            val repeatMode = prefs[RepeatModeKey] ?: REPEAT_MODE_OFF
            val volume = (prefs[PlayerVolumeKey] ?: 1f).coerceIn(0f, 1f)
            val offload = prefs[AudioOffload] ?: false
            withContext(Dispatchers.Main) {
                player.repeatMode = repeatMode
                playerVolume.value = volume
                updateAudioOffload(offload)
            }
        }

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady &&
                        player.playbackState == Player.STATE_IDLE
                    ) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        combine(playerVolume, normalizeFactor, audioFocusVolumeFactor, playbackFadeFactor) { playerVolume, normalizeFactor, audioFocusVolumeFactor, playbackFadeFactor ->
            playerVolume * normalizeFactor * audioFocusVolumeFactor * playbackFadeFactor
        }.collectLatest(scope) { finalVolume ->
            player.volume = finalVolume
        }

        playerVolume.debounce(1000).collect(ioScope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(300).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                ensurePresenceManager()
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(ioScope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val result = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = result.lyrics,
                            provider = result.providerName
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        dataStore.data
            .map { it[PauseOnDeviceMuteKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                pauseOnDeviceMuteEnabled = enabled
                if (!enabled) {
                    wasAutoPausedByDeviceMute = false
                } else {
                    handleDeviceMuteStateChanged()
                }
            }

        dataStore.data
            .map { it[AutoStartOnBluetoothKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                autoStartOnBluetoothEnabled = enabled
                if (enabled) {
                    registerBluetoothReceiver()
                } else {
                    unregisterBluetoothReceiver()
                }
            }

        dataStore.data
            .map { it[AudioOffload] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                updateAudioOffload(enabled)
                if (enabled) {
                    val skipSilenceEnabled = dataStore.get(SkipSilenceKey, false)
                    if (skipSilenceEnabled) {
                        dataStore.edit { it[SkipSilenceKey] = false }
                        player.skipSilenceEnabled = false
                    }
                    val crossfadeSeconds = dataStore.get(AudioCrossfadeDurationKey, 0)
                    if (crossfadeSeconds != 0) {
                        dataStore.edit { it[AudioCrossfadeDurationKey] = 0 }
                    }
                }
            }
        
        dataStore.data
            .map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }
            .distinctUntilChanged()
            .collectLatest(scope) {
                crossfadeDurationMs.value = it
            }

        dataStore.data
            .map { it[WakelockKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                wakelockEnabled = enabled
                updateWakeLock()
            }

        crossfadeAudio =
            CrossfadeAudio(
                player = player,
                database = database,
                crossfadeDurationMs = crossfadeDurationMs,
                playbackFadeFactor = playbackFadeFactor,
                playerVolume = playerVolume,
                audioFocusVolumeFactor = audioFocusVolumeFactor,
                audioNormalizationEnabled = audioNormalizationEnabled,
                maxSafeGainFactor = maxSafeGainFactor,
                overlapPlayerFactory = {
                    ExoPlayer
                        .Builder(this)
                        .setMediaSourceFactory(createMediaSourceFactory())
                        .setRenderersFactory(createRenderersFactory())
                        .setHandleAudioBecomingNoisy(false)
                        .setWakeMode(C.WAKE_MODE_NETWORK)
                        .setAudioAttributes(
                            AudioAttributes
                                .Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                            false,
                        ).setSeekBackIncrementMs(5000)
                        .setSeekForwardIncrementMs(5000)
                        .build()
                },
                onCrossfadeStart = { mediaItem ->
                    val metadata = mediaItem.metadata
                    currentMediaMetadata.value = metadata
                    scope.launch {
                        try {
                            val token = dataStore.get(DiscordTokenKey, "")
                            if (token.isNotBlank() && DiscordPresenceManager.isRunning()) {
                                val mediaId = mediaItem.mediaId
                                val song = if (mediaId != null) withContext(Dispatchers.IO) { database.song(mediaId).first() } else null
                                val finalSong = song ?: metadata?.let { createTransientSongFromMedia(it) }

                                if (canUpdatePresence()) {
                                    DiscordPresenceManager.updateNow(
                                        context = this@MusicService,
                                        token = token,
                                        song = finalSong,
                                        positionMs = 0L,
                                        isPaused = false
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            ).also { it.start(scope) }

        lyricsPreloadManager = LyricsPreloadManager(
            context = this,
            database = database,
            networkConnectivity = connectivityObserver,
        )

        dataStore.data
            .map(::readEqSettingsFromPrefs)
            .distinctUntilChanged()
            .collectLatest(scope) { settings ->
                desiredEqSettings.value = settings
                applyEqSettingsToEffects(settings)
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            audioNormalizationEnabled.value = normalizeAudio
            Timber.tag("AudioNormalization").d("Audio normalization enabled: $normalizeAudio")
            
            normalizeFactor.value =
                if (normalizeAudio) {
                    val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb
                    if (loudness != null) {
                        val loudnessDb = loudness.toFloat()
                        var factor = 10f.pow(-loudnessDb / 20)
                        if (factor > 1f) {
                            factor = min(factor, maxSafeGainFactor)
                        }
                        factor
                    } else {
                        1f
                    }
                } else {
                    1f
                }
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest(scope) { (key, enabled) ->
                val newRpc =
                    withContext(Dispatchers.IO) {
                        if (!key.isNullOrBlank() && enabled) {
                            runCatching { DiscordRPC(this@MusicService, key) }
                                .onFailure { Timber.tag("MusicService").e(it, "failed to create DiscordRPC client") }
                                .getOrNull()
                        } else {
                            null
                        }
                    }

                try {
                    if (discordRpc?.isRpcRunning() == true) {
                        withContext(Dispatchers.IO) { discordRpc?.closeRPC() }
                    }
                } catch (_: Exception) {}
                discordRpc = newRpc

                if (discordRpc != null) {
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            ensurePresenceManager()
                        }
                    }
                } else {
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                }
            }

        dataStore.data
            .map { prefs ->
                (prefs[SmartTrimmerKey] ?: false) to (prefs[MaxSongCacheSizeKey] ?: 1024)
            }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest(ioScope) { (enabled, maxSongCacheSizeMb) ->
                if (!enabled) return@collectLatest
                if (maxSongCacheSizeMb <= 0 || maxSongCacheSizeMb == -1) return@collectLatest
                val bytesPerMb = 1024L * 1024L
                val safeSizeMb = maxSongCacheSizeMb.toLong().coerceAtMost(Long.MAX_VALUE / bytesPerMb)
                val limitBytes = safeSizeMb * bytesPerMb
                trimPlayerCacheToBytes(limitBytes)
            }

        dataStore.data
            .map { it[EnableLastFMScrobblingKey] ?: false }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && scrobbleManager == null) {
                    val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                    val minSongDuration = dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                    val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                    
                    scrobbleManager = com.j.m3play.utils.ScrobbleManager(
                        ioScope,
                        minSongDuration = minSongDuration,
                        scrobbleDelayPercent = delayPercent,
                        scrobbleDelaySeconds = delaySeconds
                    )
                    scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                } else if (!enabled && scrobbleManager != null) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map { it[LastFMUseNowPlaying] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                scrobbleManager?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                )
            }
            .distinctUntilChanged()
            .collect(scope) { (delayPercent, minSongDuration, delaySeconds) ->
                scrobbleManager?.let {
                    it.scrobbleDelayPercent = delayPercent
                    it.minSongDuration = minSongDuration
                    it.scrobbleDelaySeconds = delaySeconds
                }
            }

        scope.launch(Dispatchers.IO) {
            if (dataStore.get(PersistentQueueKey, true)) {
                readPersistentObject<PersistQueue>(PERSISTENT_QUEUE_FILE)
                    ?.let { persistedQueue ->
                    restorePersistentQueue(persistedQueue)
                }
                
                readPersistentObject<PersistQueue>(PERSISTENT_AUTOMIX_FILE)
                    ?.let { persistedAutomix ->
                    val items = persistedAutomix.items.map { it.toMediaItem() }
                    withContext(Dispatchers.Main) {
                        automixItems.value = items
                        automixSeedMediaId = player.currentMetadata?.id?.trim()?.takeIf { it.isNotBlank() }
                    }
                }
                
                readPersistentObject<PersistPlayerState>(PERSISTENT_PLAYER_STATE_FILE)
                    ?.let { playerState ->
                    withContext(Dispatchers.Main) {
                        player.repeatMode = playerState.repeatMode
                        player.shuffleModeEnabled = playerState.shuffleModeEnabled
                        playerVolume.value = playerState.volume
                        
                        if (player.mediaItemCount > 0) {
                            val index =
                                if (playerState.currentMediaItemIndex in 0 until player.mediaItemCount) {
                                    playerState.currentMediaItemIndex
                                } else {
                                    player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1)
                                }
                            player.seekTo(index, playerState.currentPosition)
                        }
                        
                        if (currentMediaMetadata.value == null) {
                            currentMediaMetadata.value = player.currentMetadata
                        }
                        updateNotification()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                queueRestoreCompleted.value = true
            }
        }

        scope.launch {
            while (isActive) {
                val interval = if (player.isPlaying) 10.seconds else 30.seconds
                delay(interval)
                val shouldSave = withContext(Dispatchers.IO) { dataStore.get(PersistentQueueKey, true) }
                if (shouldSave) {
                    saveQueueToDisk()
                }
            }
        }
    }

    private fun ensureScopesActive() {
        if (!scopeJob.isActive) {
            scopeJob = Job()
        }
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + scopeJob)
        }
        if (!ioScope.isActive) {
            ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
        }
    }

    private suspend fun restorePersistentQueue(persistedQueue: PersistQueue) {
        val restoredQueue = persistedQueue.toQueue()
        val hideExplicit = dataStore.get(HideExplicitKey, false)
        val hideVideo = dataStore.get(HideVideoKey, false)
        val initialStatus =
            restoredQueue
                .getInitialStatus()
                .filterExplicit(hideExplicit)
                .filterVideo(hideVideo)

        withContext(Dispatchers.Main) {
            currentQueue = restoredQueue
            queueTitle = initialStatus.title

            val items = initialStatus.items
            if (items.isEmpty()) {
                Timber.tag("Persist").e("Queue restore failed: items empty")
                return@withContext
            }

            val fullIndex = initialStatus.mediaItemIndex.coerceIn(0, items.lastIndex)

            val savedMetadata = persistedQueue.items.getOrNull(fullIndex)
            if (savedMetadata != null) {
                currentMediaMetadata.value = savedMetadata
            }

            delay(300)

            player.setMediaItems(items, fullIndex, initialStatus.position)
            player.prepare()
            player.playWhenReady = false
            
            updateNotification()
        }
    }

    private fun ensurePresenceManager() {
        if (DiscordPresenceManager.isRunning() && lastPresenceToken != null) return

        scope.launch {
            if (!dataStore.get(EnableDiscordRPCKey, false)) {
                if (DiscordPresenceManager.isRunning()) {
                    Timber.tag("MusicService").d("Discord RPC disabled → stopping presence manager")
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            val key: String = dataStore.get(DiscordTokenKey, "")
            if (key.isNullOrBlank()) {
                if (DiscordPresenceManager.isRunning()) {
                    Timber.tag("MusicService").d("No Discord token → stopping presence manager")
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            if (DiscordPresenceManager.isRunning() && lastPresenceToken == key) {
                return@launch
            }

            try {
                DiscordPresenceManager.stop()
                DiscordPresenceManager.start(
                    context = this@MusicService,
                    token = key,
                    songProvider = { player.currentMetadata?.let { createTransientSongFromMedia(it) } ?: currentSong.value },
                    positionProvider = { player.currentPosition },
                    isPausedProvider = { !player.isPlaying },
                    intervalProvider = { getPresenceIntervalMillis(this@MusicService) }
                )
                Timber.tag("MusicService").d("Presence manager started with token=$key")
                lastPresenceToken = key
            } catch (ex: Exception) {
                Timber.tag("MusicService").e(ex, "Failed to start presence manager")
            }
        }
    }

    private fun canUpdatePresence(): Boolean {
        val now = System.currentTimeMillis()
        synchronized(this) {
            return if (now - lastPresenceUpdateTime > MIN_PRESENCE_UPDATE_INTERVAL) {
                lastPresenceUpdateTime = now
                true
            } else false
        }
    }

    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                audioFocusVolumeFactor.value = 1f

                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                audioFocusVolumeFactor.value = 1f
                wasPlayingBeforeAudioFocusLoss = false

                if (player.isPlaying) {
                    player.pause()
                }

                abandonAudioFocus()

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                audioFocusVolumeFactor.value = 1f
                wasPlayingBeforeAudioFocusLoss = player.isPlaying

                if (player.isPlaying) {
                    player.pause()
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                audioFocusVolumeFactor.value = 0.2f
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true
                audioFocusVolumeFactor.value = 1f

                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }
        
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                audioFocusVolumeFactor.value = 1f
                lastAudioFocusState = focusChange
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
    
        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun isDeviceMutedNow(): Boolean {
        return player.isDeviceMuted || player.deviceVolume <= 0
    }

    private fun isTogetherGuestSession(): Boolean {
        val joined = togetherSessionState.value as? com.j.m3play.together.TogetherSessionState.Joined
        return joined?.role is com.j.m3play.together.TogetherRole.Guest
    }

    private fun handleDeviceMuteStateChanged() {
        if (!pauseOnDeviceMuteEnabled || isTogetherGuestSession()) {
            wasAutoPausedByDeviceMute = false
            return
        }

        if (isDeviceMutedNow()) {
            val canPauseNow =
                player.currentMediaItem != null &&
                    player.playWhenReady &&
                    player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED

            if (canPauseNow) {
                player.pause()
                wasAutoPausedByDeviceMute = true
            }
            return
        }

        if (!wasAutoPausedByDeviceMute) return

        wasAutoPausedByDeviceMute = false
        val canResumeNow =
            player.currentMediaItem != null &&
                player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED
        if (canResumeNow) {
            player.play()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
            if (!autoStartOnBluetoothEnabled) return

            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return

            val isAudioDevice = try {
                val majorClass = device.bluetoothClass?.majorDeviceClass
                majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO || majorClass == BluetoothClass.Device.Major.WEARABLE
            } catch (_: SecurityException) {
                true
            }

            if (!isAudioDevice) return

            scope.launch {
                delay(1500)
                handleBluetoothAutoStart()
            }
        }
    }

    private fun handleBluetoothAutoStart() {
        if (isTogetherGuestSession()) return

        if (player.currentMediaItem != null &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        ) {
            if (!player.playWhenReady) {
                player.play()
            }
            return
        }

        if (player.mediaItemCount > 0) {
            player.prepare()
            player.play()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerBluetoothReceiver() {
        if (bluetoothReceiverRegistered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return

        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
        bluetoothReceiverRegistered = true
    }

    private fun unregisterBluetoothReceiver() {
        if (!bluetoothReceiverRegistered) return
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        bluetoothReceiverRegistered = false
    }

    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        try {
            val customLayout = listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> R.string.repeat_mode_off
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> R.drawable.repeat
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
            )
            mediaSession.setCustomLayout(customLayout)
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun refreshPlaybackNotification() {
        updateNotification()
        runCatching { super.onUpdateNotification(mediaSession, player.isPlaying) }
            .onFailure { reportException(it) }
    }
