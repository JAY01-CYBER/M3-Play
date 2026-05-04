/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V1     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.*
import com.j.m3play.ui.player.StyledPlaybackSlider
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlin.math.roundToInt
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(RandomThemeOnStartupKey, defaultValue = false)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    
    // PLAYER DESIGN STYLE
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(
        PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V4
    )
    
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(UseNewLibraryDesignKey, defaultValue = false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (archiveTuneCanvasEnabled, onM3PlayCanvasEnabledChange) = rememberPreference(M3PlayCanvasKey, defaultValue = false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, defaultValue = true)
    val (useSystemFont, onUseSystemFontChange) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    
    // LYRICS SETTINGS
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, defaultValue = LyricsPosition.LEFT)
    val (lyricsAnimation, onLyricsAnimationChange) = rememberEnumPreference<LyricsAnimationStyle>(
        key = LyricsAnimationStyleKey,
        defaultValue = LyricsAnimationStyle.APPLE
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (useLyricsV2, onUseLyricsV2Change) = rememberPreference(UseLyricsV2Key, defaultValue = false)

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.Standard)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, defaultValue = true)
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(SwipeSensitivityKey, defaultValue = 0.73f)
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, defaultValue = GridItemSize.SMALL)
    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(SwipeToSongKey, defaultValue = false)
    
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(ShowDownloadedPlaylistKey, defaultValue = true)
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(ShowTopPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(ShowCachedPlaylistKey, defaultValue = true)
    val (showTagsInLibrary, onShowTagsInLibraryChange) = rememberPreference(ShowTagsInLibraryKey, defaultValue = true)
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) = rememberPreference(ShowHomeCategoryChipsKey, defaultValue = true)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LibraryFilter.LIBRARY)

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.theme))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = dynamicTheme,
            onCheckedChange = onDynamicThemeChange,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.dark_theme)) },
            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            },
        )

        AnimatedVisibility(useDarkTheme) {
            SwitchPreference(
                title = { Text(stringResource(R.string.pure_black)) },
                icon = { Icon(painterResource(R.drawable.contrast), null) },
                checked = pureBlack,
                onCheckedChange = onPureBlackChange,
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.player))

        // PLAYER DESIGN STYLE SELECTOR WITH APPLE OPTION
        EnumListPreference(
            title = { Text(stringResource(R.string.player_design_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerDesignStyle,
            onValueSelected = onPlayerDesignStyleChange,
            valueText = {
                when (it) {
                    PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
                    PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
                    PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
                    PlayerDesignStyle.V4 -> stringResource(R.string.player_design_v4)
                    PlayerDesignStyle.V5 -> stringResource(R.string.player_design_v5)
                    PlayerDesignStyle.V6 -> stringResource(R.string.player_design_v6)
                    PlayerDesignStyle.APPLE -> "Apple Music Style" // Custom text for Apple
                }
            },
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_background_style)) },
            icon = { Icon(painterResource(R.drawable.gradient), null) },
            selectedValue = playerBackground,
            onValueSelected = onPlayerBackgroundChange,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.CUSTOM -> stringResource(R.string.custom)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
                    PlayerBackgroundStyle.BLUR_GRADIENT -> stringResource(R.string.blur_gradient)
                    PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
                    PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
                }
            },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description = sliderStyleLabel(sliderStyle),
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            onClick = { showSliderOptionDialog = true },
        )

        PreferenceGroupTitle(title = stringResource(R.string.lyrics))

        EnumListPreference(
          title = { Text(stringResource(R.string.lyrics_animation_style)) },
          icon = { Icon(painterResource(R.drawable.animation), null) },
          selectedValue = lyricsAnimation,
          onValueSelected = onLyricsAnimationChange,
          valueText = {
              when (it) {
                  LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                  LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                  LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                  LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                  LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                  LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
              }
          }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsScroll,
            onCheckedChange = onLyricsScrollChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.misc))

        SwitchPreference(
            title = { Text(stringResource(R.string.slim_navbar)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            checked = slimNav,
            onCheckedChange = onSlimNavChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        }
    )
    
    // Slider Style Dialog
    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = { TextButton(onClick = { showSliderOptionDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
            onDismiss = { showSliderOptionDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SliderStyle.Standard, SliderStyle.Wavy, SliderStyle.Thick, SliderStyle.Circular, SliderStyle.Simple).chunked(3).forEach { styleRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        styleRow.forEach { style ->
                            SliderStyleOptionCard(sliderStyle = style, selected = sliderStyle == style, onClick = { onSliderStyleChange(style); showSliderOptionDialog = false }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderStyleOptionCard(sliderStyle: SliderStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp)
    ) {
        StyledPlaybackSlider(sliderStyle = sliderStyle, value = sliderValue, valueRange = 0f..1f, onValueChange = { sliderValue = it }, onValueChangeFinished = {}, activeColor = MaterialTheme.colorScheme.primary, isPlaying = true, modifier = Modifier.weight(1f).fillMaxWidth())
        Text(text = sliderStyleLabel(sliderStyle), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun sliderStyleLabel(sliderStyle: SliderStyle): String {
    return when (sliderStyle) {
        SliderStyle.Standard -> stringResource(R.string.slider_style_standard)
        SliderStyle.Wavy -> stringResource(R.string.slider_style_wavy)
        SliderStyle.Thick -> stringResource(R.string.slider_style_thick)
        SliderStyle.Circular -> stringResource(R.string.slider_style_circular)
        SliderStyle.Simple -> stringResource(R.string.slider_style_simple)
    }
}

enum class DarkMode { ON, OFF, AUTO }
enum class NavigationTab { HOME, SEARCH, LIBRARY }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
