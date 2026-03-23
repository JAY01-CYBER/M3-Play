package com.j.m3play

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.WatchEndpoint
import com.j.m3play.constants.AppBarHeight
import com.j.m3play.constants.DarkModeKey
import com.j.m3play.constants.DefaultOpenTabKey
import com.j.m3play.constants.DisableScreenshotKey
import com.j.m3play.constants.DynamicThemeKey
import com.j.m3play.constants.MiniPlayerHeight
import com.j.m3play.constants.NavigationBarAnimationSpec
import com.j.m3play.constants.NavigationBarHeight
import com.j.m3play.constants.PauseSearchHistoryKey
import com.j.m3play.constants.PlayerBackgroundStyle
import com.j.m3play.constants.PlayerBackgroundStyleKey
import com.j.m3play.constants.PureBlackKey
import com.j.m3play.constants.SearchSource
import com.j.m3play.constants.SearchSourceKey
import com.j.m3play.constants.SlimNavBarKey
import com.j.m3play.constants.StopMusicOnTaskClearKey
import com.j.m3play.db.MusicDatabase
import com.j.m3play.db.entities.SearchHistory
import com.j.m3play.extensions.toEnum
import com.j.m3play.models.toMediaMetadata
import com.j.m3play.playback.DownloadUtil
import com.j.m3play.playback.MusicService
import com.j.m3play.playback.MusicService.MusicBinder
import com.j.m3play.playback.PlayerConnection
import com.j.m3play.playback.queues.YouTubeQueue
import com.j.m3play.ui.component.AvatarPreferenceManager
import com.j.m3play.ui.component.AvatarSelection
import com.j.m3play.ui.component.BottomSheetMenu
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.LocaleManager
import com.j.m3play.ui.component.Lyrics
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.ui.component.TopSearch
import com.j.m3play.ui.component.rememberBottomSheetState
import com.j.m3play.ui.component.shimmer.ShimmerTheme
import com.j.m3play.ui.menu.YouTubeSongMenu
import com.j.m3play.ui.player.BottomSheetPlayer
import com.j.m3play.ui.screens.Screens
import com.j.m3play.ui.screens.navigationBuilder
import com.j.m3play.ui.screens.search.LocalSearchScreen
import com.j.m3play.ui.screens.search.OnlineSearchScreen
import com.j.m3play.ui.screens.settings.DarkMode
import com.j.m3play.ui.screens.settings.NavigationTab
import com.j.m3play.ui.theme.ColorSaver
import com.j.m3play.ui.theme.DefaultThemeColor
import com.j.m3play.ui.theme.M3PlayTheme
import com.j.m3play.ui.theme.extractThemeColor
import com.j.m3play.ui.utils.appBarScrollBehavior
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.ui.utils.resetHeightOffset
import com.j.m3play.utils.SyncUtils
import com.j.m3play.utils.Updater
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.reportException
import com.j.m3play.viewmodels.NewReleaseViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        intent?.let { handlevideoIdIntent(it) }

        setContent {
            LaunchedEffect(Unit) {
                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
            }

            var showFullscreenLyrics by remember { mutableStateOf(false) }


            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor =
                        if (song != null) {
                            withContext(Dispatchers.IO) {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false)
                                            .build(),
                                    )
                                (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor()
                                    ?: DefaultThemeColor
                            }
                        } else {
                            DefaultThemeColor
                        }
                }
            }

            M3PlayTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
            ) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()


                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_EXPLORE -> NavigationTab.EXPLORE
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Explore.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active
                        }

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val playerAwareWindowInsets =
                        remember(
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar) bottom += NavigationBarHeight
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    val q = navBackStackEntry?.arguments?.getString("query") ?: ""
                                    if (q.contains("%")) q else URLDecoder.decode(q, "UTF-8")
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener =
                            Consumer<Intent> { intent ->
                                val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                                    ?.toUri() ?: return@Consumer
                                when (val path = uri.pathSegments.firstOrNull()) {
                                    "playlist" ->
                                        uri.getQueryParameter("list")?.let { playlistId ->
                                            if (playlistId.startsWith("OLAK5uy_")) {
                                                coroutineScope.launch {
                                                    YouTube
                                                        .albumSongs(playlistId)
                                                        .onSuccess { songs ->
                                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                navController.navigate("album/$browseId")
                                                            }
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                }
                                            } else {
                                                navController.navigate("online_playlist/$playlistId")
                                            }
                                        }

                                    "browse" ->
                                        uri.lastPathSegment?.let { browseId ->
                                            navController.navigate("album/$browseId")
                                        }

                                    "channel", "c" ->
                                        uri.lastPathSegment?.let { artistId ->
                                            navController.navigate("artist/$artistId")
                                        }

                                    else ->
                                        when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            uri.host == "youtu.be" -> path
                                            else -> null
                                        }?.let { videoId ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    YouTube.queue(listOf(videoId))
                                                }.onSuccess {
                                                    playerConnection?.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                            it.firstOrNull()?.toMediaMetadata()
                                                        )
                                                    )
                                                }.onFailure {
                                                    reportException(it)
                                                }
                                            }
                                        }
                                }
                            }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        Scaffold(
                            topBar = {
                                val playerBackground by rememberEnumPreference(
                                    key = PlayerBackgroundStyleKey,
                                    defaultValue = PlayerBackgroundStyle.DEFAULT
                                )

                                if (shouldShowTopBar) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )

                                        val safeSelectedValue = if (playerBackground == PlayerBackgroundStyle.BLUR &&
                                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                PlayerBackgroundStyle.DEFAULT
                                            } else playerBackground

                                        if (safeSelectedValue == PlayerBackgroundStyle.BLUR) {
                                            val connection = LocalPlayerConnection.current
                                            connection?.let { conn ->
                                                val mediaMetadata by conn.mediaMetadata.collectAsState()
                                                mediaMetadata?.thumbnailUrl?.let { imageUrl ->
                                                    AsyncImage(
                                                        model = imageUrl,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.FillBounds,
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .blur(35.dp)
                                                            .alpha(0.6f)
                                                            .drawWithContent {
                                                                drawContent()
                                                                drawRect(
                                                                    brush = Brush.verticalGradient(
                                                                        colors = listOf(
                                                                            Color.Black.copy(alpha = 0.5f),
                                                                            Color.Transparent
                                                                        ),
                                                                        startY = 0f,
                                                                        endY = size.height * 0.6f
                                                                    ),
                                                                    blendMode = BlendMode.DstIn
                                                                )
                                                            }
                                                    )
                                                }
                                            }
                                        }

                                        TopAppBar(
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.opentune),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(27.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.app_name),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },

                                            actions = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val context = LocalContext.current
                                                    val viewModel: NewReleaseViewModel = hiltViewModel()
                                                    val hasNewReleases by viewModel.hasNewReleases.collectAsState()

                                                    Box(modifier = Modifier.size(48.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.markNewReleasesAsSeen()
                                                                navController.navigate("new_release")
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.notification_on),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        if (hasNewReleases) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .size(10.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                                            )
                                                        }
                                                    }

                                                    IconButton(onClick = { onActiveChange(true) }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.search),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    ProfileIconWithUpdateBadge(
                                                        currentVersion = BuildConfig.VERSION_NAME,
                                                        onProfileClick = { navController.navigate("settings") }
                                                    )
                                                }
                                            },
                                            scrollBehavior = searchBarScrollBehavior,
                                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                        )
                                    }
                                }

                                val isSearchRoute = navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                if (active || isSearchRoute) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    if (searchSource == SearchSource.LOCAL) R.string.search_library else R.string.search_yt_music
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> onActiveChange(false)
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> navController.navigateUp()
                                                        else -> onActiveChange(true)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) R.drawable.arrow_back else R.drawable.search
                                                    ),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (active) {
                                                Row {
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(onClick = { onQueryChange(TextFieldValue("")) }) {
                                                            Icon(painterResource(R.drawable.close), null)
                                                        }
                                                    }
                                                    IconButton(onClick = { searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE }) {
                                                        Icon(
                                                            painterResource(if (searchSource == SearchSource.LOCAL) R.drawable.library_music else R.drawable.language),
                                                            null
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.focusRequester(searchBarFocusRequester).fillMaxWidth(),
                                        focusRequester = searchBarFocusRequester
                                    ) {
                                        Crossfade(targetState = searchSource, label = "") { currentSource ->
                                            Box(modifier = Modifier.fillMaxSize().padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp).navigationBarsPadding()) {
                                                when (currentSource) {
                                                    SearchSource.LOCAL -> LocalSearchScreen(query.text, navController, { onActiveChange(false) }, pureBlack)
                                                    SearchSource.ONLINE -> OnlineSearchScreen(query.text, onQueryChange, navController, onSearch, { onActiveChange(false) })
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                Box {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        onOpenFullscreenLyrics = { showFullscreenLyrics = true },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    AnimatedVisibility(
                                        visible = showFullscreenLyrics,
                                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                    ) {
                                        Lyrics(
                                            sliderPositionProvider = { null },
                                            onNavigateBack = { showFullscreenLyrics = false },
                                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                                        )
                                    }

                                    NavigationBar(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(15.dp))
                                            .align(Alignment.BottomCenter)
                                            .offset {
                                                if (navigationBarHeight == 0.dp) {
                                                    IntOffset(0, (bottomInset + NavigationBarHeight).roundToPx())
                                                } else {
                                                    val slideOffset = (bottomInset + NavigationBarHeight) * playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                    val hideOffset = (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                                    IntOffset(0, (slideOffset + hideOffset).roundToPx())
                                                }
                                            },
                                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        navigationItems.fastForEach { screen ->
                                            val isSelected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                            NavigationBarItem(
                                                selected = isSelected,
                                                icon = { Icon(painterResource(if (isSelected) screen.iconIdActive else screen.iconIdInactive), null) },
                                                label = { if (!slimNav) Text(stringResource(screen.titleId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                onClick = {
                                                    if (isSelected) {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                    } else {
                                                        navigateToScreen(navController, screen)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.background(insetBg).fillMaxWidth().align(Alignment.BottomCenter).height(bottomInsetDp))
                                }
                            },
                            modifier = Modifier.fillMaxSize().nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                    NavigationTab.HOME -> Screens.Home
                                    NavigationTab.EXPLORE -> Screens.Explore
                                    NavigationTab.LIBRARY -> Screens.Library
                                }.route,
                                modifier = Modifier.nestedScroll(if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } || isSearchRoute) searchBarScrollBehavior.nestedScrollConnection else topAppBarScrollBehavior.nestedScrollConnection)
                            ) {
                                navigationBuilder(navController, topAppBarScrollBehavior, latestVersionName)
                            }
                        }

                        BottomSheetMenu(state = LocalMenuState.current, modifier = Modifier.align(Alignment.BottomCenter))

                        sharedSong?.let { song ->
                            Dialog(onDismissRequest = { sharedSong = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                Surface(modifier = Modifier.padding(24.dp), shape = RoundedCornerShape(16.dp), color = AlertDialogDefaults.containerColor) {
                                    YouTubeSongMenu(song, navController, { sharedSong = null })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToScreen(navController: NavHostController, screen: Screens) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val videoId = when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }
        videoId?.let { id ->
            lifecycleScope.launch {
                YouTube.queue(listOf(id)).onSuccess {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = it.firstOrNull()?.id), it.firstOrNull()?.toMediaMetadata()))
                }
            }
        }
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.j.m3play.action.SEARCH"
        const val ACTION_EXPLORE = "com.j.m3play.action.EXPLORE"
        const val ACTION_LIBRARY = "com.j.m3play.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No connection") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No insets") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No download") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No sync") }

@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        checked = granted,
        onCheckedChange = { if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else openNotificationSettings(context) }
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
    context.startActivity(intent)
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val json = URL("https://api.github.com/repos/JAY01-CYBER/M3-Play/releases/latest").readText()
        JSONObject(json).getString("tag_name")
    } catch (e: Exception) { null }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    val r = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val c = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(r.size, c.size)) {
        val rv = r.getOrNull(i) ?: 0
        val cv = c.getOrNull(i) ?: 0
        if (rv > cv) return true
        if (rv < cv) return false
    }
    return false
}

@Composable
fun ProfileIconWithUpdateBadge(currentVersion: String, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val selection by avatarManager.getAvatarSelection.collectAsState(AvatarSelection.Default)
    var hasUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val latest = withContext(Dispatchers.IO) { checkForUpdates() }
        hasUpdate = latest?.let { isNewerVersion(it, currentVersion) } ?: false
    }

    Box(modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
        when (val s = selection) {
            is AvatarSelection.Custom -> AsyncImage(model = s.uri.toUri(), contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            is AvatarSelection.DiceBear -> AsyncImage(model = s.url, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else -> Icon(painterResource(R.drawable.person), null)
        }
        if (hasUpdate) {
            Box(modifier = Modifier.align(Alignment.TopEnd).size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).border(1.dp, MaterialTheme.colorScheme.surface, CircleShape))
        }
    }
}
