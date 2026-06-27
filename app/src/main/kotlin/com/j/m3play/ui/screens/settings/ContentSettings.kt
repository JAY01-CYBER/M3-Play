/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Style: ANDROID 17 (Ultra-Rounded, M3)     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.j.m3play.innertube.YouTube
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.*
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.setAppLocale
import java.net.Proxy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (appLanguage, onAppLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)
    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (hideVideo, onHideVideoChange) = rememberPreference(key = HideVideoKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (streamBypassProxy, onStreamBypassProxyChange) = rememberPreference(key = StreamBypassProxyKey, defaultValue = false)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enableSimpMusicLyrics, onEnableSimpMusicLyricsChange) = rememberPreference(key = EnableSimpMusicLyricsKey, defaultValue = true)
    val (enableYouLyPlus, onEnableYouLyPlusChange) = rememberPreference(key = EnableYouLyPlusKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(key = PreferredLyricsProviderKey, defaultValue = PreferredLyricsProvider.LRCLIB)
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (preloadQueueLyricsEnabled, onPreloadQueueLyricsEnabledChange) = rememberPreference(PreloadQueueLyricsEnabledKey, defaultValue = true)
    val (queueLyricsPreloadCount, onQueueLyricsPreloadCountChange) = rememberPreference(QueueLyricsPreloadCountKey, defaultValue = 1)
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.content), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- GENERAL SECTION ---
            item {
                PreferenceGroupTitle(
                    title = stringResource(R.string.general),
                    modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        ListPreference(
                            title = { Text(stringResource(R.string.content_language)) },
                            icon = { Icon(painterResource(R.drawable.language), null) },
                            selectedValue = contentLanguage,
                            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                            valueText = { LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) } },
                            onValueSelected = { newValue ->
                                val locale = Locale.getDefault()
                                val languageTag = locale.toLanguageTag().replace("-Hant", "")
                                YouTube.locale = YouTube.locale.copy(
                                    hl = newValue.takeIf { it != SYSTEM_DEFAULT }
                                        ?: locale.language.takeIf { it in LanguageCodeToName }
                                        ?: languageTag.takeIf { it in LanguageCodeToName }
                                        ?: "en"
                                )
                                onContentLanguageChange(newValue)
                            }
                        )
                        ListPreference(
                            title = { Text(stringResource(R.string.content_country)) },
                            icon = { Icon(painterResource(R.drawable.location_on), null) },
                            selectedValue = contentCountry,
                            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                            valueText = { CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) } },
                            onValueSelected = { newValue ->
                                val locale = Locale.getDefault()
                                YouTube.locale = YouTube.locale.copy(
                                    gl = newValue.takeIf { it != SYSTEM_DEFAULT }
                                        ?: locale.country.takeIf { it in CountryCodeToName }
                                        ?: "US"
                                )
                                onContentCountryChange(newValue)
                            }
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.hide_explicit)) },
                            icon = { Icon(painterResource(R.drawable.explicit), null) },
                            checked = hideExplicit,
                            onCheckedChange = onHideExplicitChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.hide_video)) },
                            icon = { Icon(painterResource(R.drawable.slow_motion_video), null) },
                            checked = hideVideo,
                            onCheckedChange = onHideVideoChange,
                        )
                    }
                }
            }

            // --- APP LANGUAGE SECTION ---
            item {
                PreferenceGroupTitle(
                    title = stringResource(R.string.app_language),
                    modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.app_language)) },
                                icon = { Icon(painterResource(R.drawable.language), null) },
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri())
                                    )
                                }
                            )
                        } else {
                            ListPreference(
                                title = { Text(stringResource(R.string.app_language)) },
                                icon = { Icon(painterResource(R.drawable.language), null) },
                                selectedValue = appLanguage,
                                values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                                valueText = { LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) } },
                                onValueSelected = { langTag ->
                                    val newLocale = langTag.takeUnless { it == SYSTEM_DEFAULT }?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
                                    onAppLanguageChange(langTag)
                                    setAppLocale(context, newLocale)
                                }
                            )
                        }
                    }
                }
            }

            // --- PROXY SECTION ---
            item {
                PreferenceGroupTitle(
                    title = stringResource(R.string.proxy),
                    modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_proxy)) },
                            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                            checked = proxyEnabled,
                            onCheckedChange = onProxyEnabledChange,
                        )
                        AnimatedVisibility(
                            visible = proxyEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                ListPreference(
                                    title = { Text(stringResource(R.string.proxy_type)) },
                                    selectedValue = proxyType,
                                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                                    valueText = { it.name },
                                    onValueSelected = onProxyTypeChange,
                                )
                                EditTextPreference(
                                    title = { Text(stringResource(R.string.proxy_url)) },
                                    value = proxyUrl,
                                    onValueChange = onProxyUrlChange,
                                )
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.stream_bypass_proxy)) },
                                    description = stringResource(R.string.stream_bypass_proxy_desc),
                                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                                    checked = streamBypassProxy,
                                    onCheckedChange = {
                                        onStreamBypassProxyChange(it)
                                        YouTube.streamBypassProxy = it
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // --- LYRICS SECTION ---
            item {
                PreferenceGroupTitle(
                    title = stringResource(R.string.lyrics),
                    modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_lrclib)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = enableLrclib,
                            onCheckedChange = onEnableLrclibChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_kugou)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = enableKugou,
                            onCheckedChange = onEnableKugouChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_betterlyrics)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = enableBetterLyrics,
                            onCheckedChange = onEnableBetterLyricsChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_simpmusic_lyrics)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = enableSimpMusicLyrics,
                            onCheckedChange = onEnableSimpMusicLyricsChange,
                        )
                        SwitchPreference(
                            title = { Text("Enable YouLyPlus") },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = enableYouLyPlus,
                            onCheckedChange = onEnableYouLyPlusChange,
                        )
                        ListPreference(
                            title = { Text(stringResource(R.string.set_first_lyrics_provider)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            selectedValue = preferredProvider,
                            values = listOf(
                                PreferredLyricsProvider.LRCLIB,
                                PreferredLyricsProvider.KUGOU,
                                PreferredLyricsProvider.BETTER_LYRICS,
                                PreferredLyricsProvider.SIMPMUSIC,
                                PreferredLyricsProvider.YOULYPLUS,
                                PreferredLyricsProvider.PAXSENIX,  
                            ),
                            valueText = {
                                when (it) {
                                    PreferredLyricsProvider.LRCLIB -> "LrcLib"
                                    PreferredLyricsProvider.KUGOU -> "KuGou"
                                    PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
                                    PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
                                    PreferredLyricsProvider.YOULYPLUS -> "YouLyPlus"
                                    PreferredLyricsProvider.PAXSENIX -> "Paxsenix"
                                }
                            },
                            onValueSelected = onPreferredProviderChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = lyricsRomanizeJapanese,
                            onCheckedChange = onLyricsRomanizeJapaneseChange,
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = lyricsRomanizeKorean,
                            onCheckedChange = onLyricsRomanizeKoreanChange,
                        )
                        
                        SwitchPreference(
                            title = { Text(stringResource(R.string.preload_queue_lyrics)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = preloadQueueLyricsEnabled,
                            onCheckedChange = onPreloadQueueLyricsEnabledChange,
                        )
                        
                        AnimatedVisibility(
                            visible = preloadQueueLyricsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            NumberPickerPreference(
                                title = { Text(stringResource(R.string.queue_lyrics_preload_count)) },
                                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                                value = queueLyricsPreloadCount,
                                onValueChange = onQueueLyricsPreloadCountChange,
                                minValue = 0,
                                maxValue = 10,
                                valueText = { if (it == 0) "Off" else it.toString() },
                            )
                        }
                    }
                }
            }

            // --- MISC SECTION ---
            item {
                PreferenceGroupTitle(
                    title = stringResource(R.string.misc),
                    modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        EditTextPreference(
                            title = { Text(stringResource(R.string.top_length)) },
                            icon = { Icon(painterResource(R.drawable.trending_up), null) },
                            value = lengthTop,
                            isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                            onValueChange = onLengthTopChange,
                        )
                        ListPreference(
                            title = { Text(stringResource(R.string.set_quick_picks)) },
                            icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                            selectedValue = quickPicks,
                            values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                            valueText = {
                                when (it) {
                                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                                }
                            },
                            onValueSelected = onQuickPicksChange,
                        )
                    }
                }
            }
        }
    }
}
