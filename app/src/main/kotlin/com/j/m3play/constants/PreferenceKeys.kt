package com.j.m3play.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val CustomThemeColorKey = stringPreferencesKey("customThemeColor")
val RandomThemeOnStartupKey = booleanPreferencesKey("randomThemeOnStartup")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
val UseSystemFontKey = booleanPreferencesKey("useSystemFont")
val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val GridItemsSizeKey = stringPreferencesKey("gridItemSize")
val SliderStyleKey = stringPreferencesKey("sliderStyle")
val SwipeToSongKey = booleanPreferencesKey("SwipeToSong")
val PlayerDesignStyleKey = stringPreferencesKey("playerDesignStyle")
val UseNewLibraryDesignKey = booleanPreferencesKey("useNewLibraryDesign")
val UseNewMiniPlayerDesignKey = booleanPreferencesKey("useNewMiniPlayerDesign")
val HidePlayerThumbnailKey = booleanPreferencesKey("hidePlayerThumbnail")
val M3PlayCanvasKey = booleanPreferencesKey("archiveTuneCanvas")
val ThumbnailCornerRadiusKey = floatPreferencesKey("thumbnailCornerRadius")
val CropThumbnailToSquareKey = booleanPreferencesKey("cropThumbnailToSquare")
val SeekExtraSeconds = booleanPreferencesKey("seekExtraSeconds")
val DisableBlurKey = booleanPreferencesKey("disableBlur")
val MiniPlayerLastAnchorKey = intPreferencesKey("miniPlayerLastAnchor")


val HistoryDuration = floatPreferencesKey("historyDuration")
val TopSize = stringPreferencesKey("topSize")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val MiniPlayerBackgroundStyleKey = stringPreferencesKey("mini_player_background_style")
val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")

enum class SliderStyle { Standard, Wavy, Thick, Circular, Simple }

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val AppLanguageKey = stringPreferencesKey("appLanguage")
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrcLib")
val EnableBetterLyricsKey = booleanPreferencesKey("enableBetterLyrics")
val EnableSimpMusicLyricsKey = booleanPreferencesKey("enableSimpMusicLyrics")
val EnableYouLyPlusKey = booleanPreferencesKey("enableYouLyPlus")
val EnablePaxsenixKey = booleanPreferencesKey("enablePaxsenix")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val HideVideoKey = booleanPreferencesKey("hideVideo")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val StreamBypassProxyKey = booleanPreferencesKey("streamBypassProxy")
val YtmSyncKey = booleanPreferencesKey("ytmSync")
val SelectedYtmPlaylistsKey = stringPreferencesKey("ytm_selected_playlists")

val TogetherDisplayNameKey = stringPreferencesKey("together_display_name")
val TogetherClientIdKey = stringPreferencesKey("together_client_id")
val TogetherDefaultPortKey = intPreferencesKey("together_default_port")
val TogetherAllowGuestsToAddTracksKey = booleanPreferencesKey("together_allow_guests_add_tracks")
val TogetherAllowGuestsToControlPlaybackKey = booleanPreferencesKey("together_allow_guests_control_playback")
val TogetherRequireHostApprovalToJoinKey = booleanPreferencesKey("together_require_host_approval_to_join")
val TogetherLastJoinLinkKey = stringPreferencesKey("together_last_join_link")
val TogetherWelcomeShownKey = booleanPreferencesKey("together_welcome_shown")
    
val ListenBrainzEnabledKey = booleanPreferencesKey("listenbrainz_enabled")
val ListenBrainzTokenKey = stringPreferencesKey("listenbrainz_token")

val LastFMSessionKey = stringPreferencesKey("lastfmSession")
val LastFMUsernameKey = stringPreferencesKey("lastfmUsername")
val EnableLastFMScrobblingKey = booleanPreferencesKey("lastfmScrobblingEnable")
val LastFMUseNowPlaying = booleanPreferencesKey("lastfmUseNowPlaying")
val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")

val SpotifyConnectedKey = booleanPreferencesKey("spotify_connected")
val SpotifyTokenKey = stringPreferencesKey("spotify_token")
val SpotifyRefreshTokenKey = stringPreferencesKey("spotify_refresh_token")
val SpotifyUserNameKey = stringPreferencesKey("spotify_user_name")
val ShowSpotifyPlaylistKey = booleanPreferencesKey("show_spotify_playlist")

val SpotifySpDcKey = stringPreferencesKey("spotify_sp_dc")
val SpotifySpKeyKey = stringPreferencesKey("spotify_sp_key")
val SpotifyAccessTokenKey = stringPreferencesKey("spotify_access_token")
val SpotifyAccessTokenExpiresAtKey = longPreferencesKey("spotify_access_token_expires_at")
val SpotifyAccountNameKey = stringPreferencesKey("spotify_account_name")
val SpotifyAccountAvatarUrlKey = stringPreferencesKey("spotify_account_avatar_url")
val ShowSpotifyPlaylistsKey = booleanPreferencesKey("show_spotify_playlists")
val SpotifyLibraryPlaylistsCacheKey = stringPreferencesKey("spotify_library_playlists_cache")

val AudioQualityKey = stringPreferencesKey("audioQuality")
val NetworkMeteredKey = booleanPreferencesKey("networkMetered")

enum class AudioQuality { AUTO, HIGH, HIGHEST, LOW }

val PlayerStreamClientKey = stringPreferencesKey("playerStreamClient")
enum class PlayerStreamClient { ANDROID_VR, WEB_REMIX, IOS, TVHTML5, ANDROID_MUSIC }

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val PermanentShuffleKey = booleanPreferencesKey("permanentShuffle")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")
val AudioOffload = booleanPreferencesKey("audioOffload")
val AudioCrossfadeDurationKey = intPreferencesKey("audioCrossfadeDuration")
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val AutoDownloadOnLikeKey = booleanPreferencesKey("autoDownloadOnLike")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val PauseOnDeviceMuteKey = booleanPreferencesKey("pauseOnDeviceMute")
val AutoStartOnBluetoothKey = booleanPreferencesKey("autoStartOnBluetooth")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")
val WakelockKey = booleanPreferencesKey("wakelock")
val ArtistSeparatorsKey = stringPreferencesKey("artistSeparators")
val ExternalDownloaderEnabledKey = booleanPreferencesKey("externalDownloaderEnabled")
val ExternalDownloaderPackageKey = stringPreferencesKey("externalDownloaderPackage")
val PlaylistTagsFilterKey = stringPreferencesKey("playlistTagsFilter")
val ShowHomeCategoryChipsKey = booleanPreferencesKey("showHomeCategoryChips")
val ShowTagsInLibraryKey = booleanPreferencesKey("showTagsInLibrary")

val EqualizerEnabledKey = booleanPreferencesKey("equalizerEnabled")
val EqualizerBandLevelsMbKey = stringPreferencesKey("equalizerBandLevelsMb")
val EqualizerOutputGainEnabledKey = booleanPreferencesKey("equalizerOutputGainEnabled")
val EqualizerOutputGainMbKey = intPreferencesKey("equalizerOutputGainMb")
val EqualizerBassBoostEnabledKey = booleanPreferencesKey("equalizerBassBoostEnabled")
val EqualizerBassBoostStrengthKey = intPreferencesKey("equalizerBassBoostStrength")
val EqualizerVirtualizerEnabledKey = booleanPreferencesKey("equalizerVirtualizerEnabled")
val EqualizerVirtualizerStrengthKey = intPreferencesKey("equalizerVirtualizerStrength")
val EqualizerSelectedProfileIdKey = stringPreferencesKey("equalizerSelectedProfileId")
val EqualizerCustomProfilesJsonKey = stringPreferencesKey("equalizerCustomProfilesJson")

val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")
val SmartTrimmerKey = booleanPreferencesKey("smartTrimmer")
val MaxSongCacheSizeKey = intPreferencesKey("maxSongCacheSize")
val MaxCanvasCacheSizeKey = intPreferencesKey("maxCanvasCacheSize")

val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")
val DisableScreenshotKey = booleanPreferencesKey("disableScreenshot")

val DiscordTokenKey = stringPreferencesKey("discordToken")
val DiscordInfoDismissedKey = booleanPreferencesKey("discordInfoDismissed")
val DiscordUsernameKey = stringPreferencesKey("discordUsername")
val DiscordNameKey = stringPreferencesKey("discordName")
val EnableDiscordRPCKey = booleanPreferencesKey("discordRPCEnable")
val DiscordActivityNameKey = stringPreferencesKey("discordActivityName")
val DiscordActivityDetailsKey = stringPreferencesKey("discordActivityDetails")
val DiscordActivityStateKey = stringPreferencesKey("discordActivityState")
val DiscordActivityButton1LabelKey = stringPreferencesKey("discordActivityButton1Label")
val DiscordActivityButton1UrlSourceKey = stringPreferencesKey("discordActivityButton1UrlSource")
val DiscordActivityButton1CustomUrlKey = stringPreferencesKey("discordActivityButton1CustomUrl")
val DiscordActivityButton2LabelKey = stringPreferencesKey("discordActivityButton2Label")
val DiscordActivityButton2UrlSourceKey = stringPreferencesKey("discordActivityButton2UrlSource")
val DiscordActivityButton2CustomUrlKey = stringPreferencesKey("discordActivityButton2CustomUrl")
val DiscordActivityButton1EnabledKey = booleanPreferencesKey("discordActivityButton1Enabled")
val DiscordActivityButton2EnabledKey = booleanPreferencesKey("discordActivityButton2Enabled")
val DiscordShowWhenPausedKey = booleanPreferencesKey("discordShowWhenPaused")
val DiscordActivityTypeKey = stringPreferencesKey("discordActivityType")
val DiscordPresenceIntervalValueKey = intPreferencesKey("discordPresenceIntervalValue")
val DiscordPresenceIntervalUnitKey = stringPreferencesKey("discordPresenceIntervalUnit") 
val DiscordPresenceStatusKey = stringPreferencesKey("discordPresenceStatus") 
val DiscordLargeImageTypeKey = stringPreferencesKey("discordLargeImageType")
val DiscordLargeTextSourceKey = stringPreferencesKey("discordLargeTextSource")
val DiscordLargeTextCustomKey = stringPreferencesKey("discordLargeTextCustom")
val DiscordLargeImageCustomUrlKey = stringPreferencesKey("discordLargeImageCustomUrl")
val DiscordSmallImageTypeKey = stringPreferencesKey("discordSmallImageType")
val DiscordSmallImageCustomUrlKey = stringPreferencesKey("discordSmallImageCustomUrl")
val DiscordActivityPlatformKey = stringPreferencesKey("discordActivityPlatform")

val TranslatorContextsKey = stringPreferencesKey("translatorContexts")
val TranslatorTargetLangKey = stringPreferencesKey("translatorTargetLang")
val EnableTranslatorKey = booleanPreferencesKey("enableTranslator")

val ChipSortTypeKey = stringPreferencesKey("chipSortType")
val SongSortTypeKey = stringPreferencesKey("songSortType")
val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val PlaylistSongSortTypeKey = stringPreferencesKey("playlistSongSortType")
val PlaylistSongSortDescendingKey = booleanPreferencesKey("playlistSongSortDescending")
val AutoPlaylistSongSortTypeKey = stringPreferencesKey("autoPlaylistSongSortType")
val AutoPlaylistSongSortDescendingKey = booleanPreferencesKey("autoPlaylistSongSortDescending")
val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val ArtistSongSortTypeKey = stringPreferencesKey("artistSongSortType")
val ArtistSongSortDescendingKey = booleanPreferencesKey("artistSongSortDescending")
val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")

val SongFilterKey = stringPreferencesKey("songFilter")
val ArtistFilterKey = stringPreferencesKey("artistFilter")
val AlbumFilterKey = stringPreferencesKey("albumFilter")

val LastLikeSongSyncKey = longPreferencesKey("last_like_song_sync")
val LastLibSongSyncKey = longPreferencesKey("last_library_song_sync")
val LastAlbumSyncKey = longPreferencesKey("last_album_sync")
val LastArtistSyncKey = longPreferencesKey("last_artist_sync")
val LastPlaylistSyncKey = longPreferencesKey("last_playlist_sync")

val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")

val PlaylistEditLockKey = booleanPreferencesKey("playlistEditLock")
val QuickPicksKey = stringPreferencesKey("discover")
val SpeedDialSongIdsKey = stringPreferencesKey("speedDialSongIds")
val PreferredLyricsProviderKey = stringPreferencesKey("lyricsProvider")
val QueueEditLockKey = booleanPreferencesKey("queueEditLock")

val ShowLikedPlaylistKey = booleanPreferencesKey("show_liked_playlist")
val ShowDownloadedPlaylistKey = booleanPreferencesKey("show_downloaded_playlist")
val ShowTopPlaylistKey = booleanPreferencesKey("show_top_playlist")
val ShowCachedPlaylistKey = booleanPreferencesKey("show_cached_playlist")

// 🔥 GLOSSY LYRICS & TRANSLATION ENGINE KEYS 🔥
val LyricsAnimationStyleKey = stringPreferencesKey("lyricsAnimationStyle")
enum class LyricsAnimationStyle { NONE, FADE, GLOW, SLIDE, KARAOKE, APPLE }

val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
enum class LyricsPosition { LEFT, CENTER, RIGHT }

val LyricsTextSizeKey = floatPreferencesKey("lyricsTextSize")
val LyricsLineSpacingKey = floatPreferencesKey("lyricsLineSpacing")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")
val HideStatusBarOnFullscreenKey = booleanPreferencesKey("hideStatusBarOnFullscreen")
val LyricsRomanizeAsMainKey = booleanPreferencesKey("lyricsRomanizeAsMain")
val LyricsRomanizeCyrillicByLineKey = booleanPreferencesKey("lyricsRomanizeCyrillicByLine")
val RespectAgentPositioningKey = booleanPreferencesKey("respectAgentPositioning")
val ShowIntervalIndicatorKey = booleanPreferencesKey("showIntervalIndicator")
val ExperimentalLyricsKey = booleanPreferencesKey("experimentalLyrics")
val LyricsGlowEffectKey = booleanPreferencesKey("lyricsGlowEffect")
val LyricsRomanizeList = stringPreferencesKey("lyricsRomanizeList")

// Romanization missing keys added here 
val LyricsRomanizeJapaneseKey = booleanPreferencesKey("lyricsRomanizeJapanese")
val LyricsRomanizeKoreanKey = booleanPreferencesKey("lyricsRomanizeKorean")

// AI Translation Keys
val OpenRouterApiKey = stringPreferencesKey("openRouterApiKey")
val AiProviderKey = stringPreferencesKey("aiProvider")
val OpenRouterBaseUrlKey = stringPreferencesKey("openRouterBaseUrl")
val OpenRouterModelKey = stringPreferencesKey("openRouterModel")
const val OpenRouterDefaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions"
const val OpenRouterDefaultModel = "google/gemini-2.5-flash-lite"
val TranslateModeKey = stringPreferencesKey("translateMode")
val TranslateLanguageKey = stringPreferencesKey("translateLanguage")
val DeeplApiKey = stringPreferencesKey("deeplApiKey")
val DeeplFormalityKey = stringPreferencesKey("deeplFormality")
val AiSystemPromptKey = stringPreferencesKey("aiSystemPrompt")

const val DEFAULT_AI_SYSTEM_PROMPT = """You are a precise lyrics translation assistant. Your output must ALWAYS be a valid JSON array of strings."""

enum class LibraryViewType { LIST, GRID ; fun toggle() = when (this) { LIST -> GRID; GRID -> LIST } }
enum class SongFilter { LIBRARY, LIKED, DOWNLOADED }
enum class ArtistFilter { LIBRARY, LIKED }
enum class AlbumFilter { LIBRARY, LIKED, DOWNLOADED, DOWNLOADED_FULL }
enum class SongSortType { CREATE_DATE, NAME, ARTIST, PLAY_TIME }
enum class PlaylistSongSortType { CUSTOM, CREATE_DATE, NAME, ARTIST, PLAY_TIME }
enum class AutoPlaylistSongSortType { CREATE_DATE, NAME, ARTIST, PLAY_TIME }
enum class ArtistSortType { CREATE_DATE, NAME, SONG_COUNT, PLAY_TIME }
enum class ArtistSongSortType { CREATE_DATE, NAME, PLAY_TIME }
enum class AlbumSortType { CREATE_DATE, NAME, ARTIST, YEAR, SONG_COUNT, LENGTH, PLAY_TIME }
enum class PlaylistSortType { CREATE_DATE, NAME, SONG_COUNT, LAST_UPDATED, CUSTOM }
enum class MixSortType { CREATE_DATE, NAME, LAST_UPDATED }
enum class GridItemSize { BIG, SMALL }
enum class MyTopFilter {
    ALL_TIME, DAY, WEEK, MONTH, YEAR;
    fun toTimeMillis(): Long = when (this) {
        DAY -> LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        WEEK -> LocalDateTime.now().minusWeeks(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        MONTH -> LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        YEAR -> LocalDateTime.now().minusMonths(12).toInstant(ZoneOffset.UTC).toEpochMilli()
        ALL_TIME -> 0
    }
}
enum class QuickPicks { QUICK_PICKS, LAST_LISTEN }
enum class PreferredLyricsProvider { LRCLIB, KUGOU, BETTER_LYRICS, SIMPMUSIC, YOULYPLUS, PAXSENIX }

// Enums fixed for player styles
enum class PlayerButtonsStyle { DEFAULT, SECONDARY, PRIMARY, TERTIARY }
enum class PlayerDesignStyle { V1, V2, V3, V4, V5, V6 }
enum class PlayerBackgroundStyle { DEFAULT, GRADIENT, CUSTOM, BLUR, COLORING, BLUR_GRADIENT, GLOW, GLOW_ANIMATED, APPLE_MUSIC, LIVE_MESH }

val PlayerCustomImageUriKey = stringPreferencesKey("playerCustomImageUri")
val PlayerCustomBlurKey = floatPreferencesKey("playerCustomBlur")
val PlayerCustomContrastKey = floatPreferencesKey("playerCustomContrast")
val PlayerCustomBrightnessKey = floatPreferencesKey("playerCustomBrightness")

val PreloadQueueLyricsEnabledKey = booleanPreferencesKey("preload_queue_lyrics_enabled")
val QueueLyricsPreloadCountKey = intPreferencesKey("queue_lyrics_preload_count")
val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val RepeatModeKey = intPreferencesKey("repeatMode")
val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")
val SwipeSensitivityKey = floatPreferencesKey("swipeSensitivity")

enum class SearchSource {
    LOCAL, ONLINE;
    fun toggle() = when (this) { LOCAL -> ONLINE; ONLINE -> LOCAL }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val PoTokenKey = stringPreferencesKey("poToken")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

val WebClientPoTokenEnabledKey = booleanPreferencesKey("webClientPoTokenEnabled")
val PoTokenGvsKey = stringPreferencesKey("poTokenGvs")
val PoTokenPlayerKey = stringPreferencesKey("poTokenPlayer")
val UseVisitorDataKey = booleanPreferencesKey("useVisitorData")
val PoTokenSourceUrlKey = stringPreferencesKey("poTokenSourceUrl")

val LaunchCountKey = intPreferencesKey("launch_count")
val HasPressedStarKey = booleanPreferencesKey("has_pressed_star")
val RemindAfterKey = intPreferencesKey("remind_after")
val EnableUpdateNotificationKey = booleanPreferencesKey("enableUpdateNotification")
val UpdateChannelKey = stringPreferencesKey("updateChannel")
val LastUpdateCheckKey = longPreferencesKey("lastUpdateCheck")
val LastNotifiedVersionKey = stringPreferencesKey("lastNotifiedVersion")

enum class UpdateChannel { STABLE, NIGHTLY }
