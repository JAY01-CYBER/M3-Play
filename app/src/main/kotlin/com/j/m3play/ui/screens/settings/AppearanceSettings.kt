package com.j.m3play.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.EnumListPreference
import com.j.m3play.ui.component.ListPreference
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.component.PreferenceGroupTitle
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.ui.component.ThumbnailCornerRadiusSelectorButton
import com.j.m3play.ui.player.StyledPlaybackSlider
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference
import kotlin.math.roundToInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, true)
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(RandomThemeOnStartupKey, false)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(PlayerDesignStyleKey, PlayerDesignStyle.V4)
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(UseNewMiniPlayerDesignKey, true)
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(UseNewLibraryDesignKey, false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (archiveTuneCanvasEnabled, onM3PlayCanvasEnabledChange) = rememberPreference(M3PlayCanvasKey, false)
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(ThumbnailCornerRadiusKey, 16f)
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) = rememberPreference(CropThumbnailToSquareKey, false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, true)
    val (useSystemFont, onUseSystemFontChange) = rememberPreference(UseSystemFontKey, false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
    
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, 34f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, 1.3f)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, false)
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, true)
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, true)
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, true)
    
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, true)
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(SwipeSensitivityKey, 0.73f)
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)
    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, false)
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(SwipeToSongKey, false)
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showTagsInLibrary, onShowTagsInLibraryChange) = rememberPreference(ShowTagsInLibraryKey, true)
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsTextSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by rememberSaveable { mutableStateOf(false) }

    if (showSliderOptionDialog) {
        val sliderStyles = remember {
            listOf(SliderStyle.Standard, SliderStyle.Wavy, SliderStyle.Thick, SliderStyle.Circular, SliderStyle.Simple)
        }
        DefaultDialog(
            buttons = {
                TextButton(onClick = { showSliderOptionDialog = false }) { Text(text = stringResource(android.R.string.cancel)) }
            },
            onDismiss = { showSliderOptionDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sliderStyles.chunked(3).forEach { styleRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        styleRow.forEach { style ->
                            SliderStyleOptionCard(
                                sliderStyle = style,
                                selected = sliderStyle == style,
                                onClick = { onSliderStyleChange(style); showSliderOptionDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - styleRow.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    
    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        DefaultDialog(
            onDismiss = { tempTextSize = lyricsTextSize; showLyricsTextSizeDialog = false },
            buttons = {
                TextButton(onClick = { tempTextSize = 34f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempTextSize = lyricsTextSize; showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsTextSizeChange(tempTextSize); showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = "Lyrics Text Size", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "${tempTextSize.roundToInt()} sp", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempTextSize, onValueChange = { tempTextSize = it }, valueRange = 16f..48f, steps = 31, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    
    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        DefaultDialog(
            onDismiss = { tempLineSpacing = lyricsLineSpacing; showLyricsLineSpacingDialog = false },
            buttons = {
                TextButton(onClick = { tempLineSpacing = 1.3f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempLineSpacing = lyricsLineSpacing; showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsLineSpacingChange(tempLineSpacing); showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = "Lyrics Line Spacing", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "${String.format(Locale.US, "%.1f", tempLineSpacing)}x", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempLineSpacing, onValueChange = { tempLineSpacing = it }, valueRange = 1.0f..2.0f, steps = 19, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.appearance), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PreferenceGroupTitle(title = stringResource(R.string.theme), modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            checked = dynamicTheme, onCheckedChange = onDynamicThemeChange,
                        )

                        AnimatedVisibility(visible = !dynamicTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            Column {
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.random_theme_on_startup)) },
                                    description = stringResource(R.string.random_theme_on_startup_desc),
                                    icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                                    checked = randomThemeOnStartup, onCheckedChange = onRandomThemeOnStartupChange,
                                )
                                PreferenceEntry(
                                    title = { Text(stringResource(R.string.color_palette)) },
                                    description = stringResource(R.string.customize_theme_colors),
                                    icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                                    onClick = { navController.navigate("settings/appearance/palette_picker") }
                                )
                            }
                        }

                        EnumListPreference(
                            title = { Text(stringResource(R.string.dark_theme)) },
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                            selectedValue = darkMode, onValueSelected = onDarkModeChange,
                            valueText = { when (it) { DarkMode.ON -> stringResource(R.string.dark_theme_on); DarkMode.OFF -> stringResource(R.string.dark_theme_off); DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system) } }
                        )

                        AnimatedVisibility(useDarkTheme) {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.pure_black)) },
                                icon = { Icon(imageVector = Icons.Default.AccountBox, contentDescription = null) },
                                checked = pureBlack, onCheckedChange = onPureBlackChange,
                            )
                        }

                        SwitchPreference(
                            title = { Text(stringResource(R.string.disable_blur)) },
                            description = stringResource(R.string.disable_blur_desc),
                            icon = { Icon(imageVector = Icons.Default.Clear, contentDescription = null) },
                            checked = disableBlur, onCheckedChange = onDisableBlurChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.use_system_font)) },
                            description = stringResource(R.string.use_system_font_desc),
                            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                            checked = useSystemFont, onCheckedChange = onUseSystemFontChange,
                        )
                    }
                }
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.player), modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        EnumListPreference(
                            title = { Text(stringResource(R.string.player_design_style)) },
                            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = null) },
                            selectedValue = playerDesignStyle, onValueSelected = onPlayerDesignStyleChange,
                            valueText = { it.name }
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.new_mini_player_design)) },
                            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = null) },
                            checked = useNewMiniPlayerDesign, onCheckedChange = onUseNewMiniPlayerDesignChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.new_library_design)) },
                            description = stringResource(R.string.new_library_design_description),
                            icon = { Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null) },
                            checked = useNewLibraryDesign, onCheckedChange = onUseNewLibraryDesignChange,
                        )

                        ListPreference(
                            title = { Text(stringResource(R.string.player_background_style)) },
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            selectedValue = playerBackground,
                            values = listOf(PlayerBackgroundStyle.DEFAULT, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.CUSTOM, PlayerBackgroundStyle.COLORING, PlayerBackgroundStyle.GLOW, PlayerBackgroundStyle.GLOW_ANIMATED),
                            valueText = { it.name },
                            onValueSelected = onPlayerBackgroundChange,
                        )

                        ListPreference(
                            title = { Text("Mini player background style") },
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            selectedValue = miniPlayerBackground,
                            values = listOf(PlayerBackgroundStyle.DEFAULT, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.BLUR_GRADIENT, PlayerBackgroundStyle.COLORING, PlayerBackgroundStyle.GLOW, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH),
                            valueText = { it.name },
                            onValueSelected = onMiniPlayerBackgroundChange,
                        )

                        if (playerBackground == PlayerBackgroundStyle.CUSTOM) {
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.customized_background)) },
                                icon = { Icon(imageVector = Icons.Default.AccountBox, contentDescription = null) },
                                onClick = { navController.navigate("customize_background") }
                            )
                        }

                        SwitchPreference(
                            title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                            description = stringResource(R.string.hide_player_thumbnail_desc),
                            icon = { Icon(imageVector = Icons.Default.Clear, contentDescription = null) },
                            checked = hidePlayerThumbnail, onCheckedChange = onHidePlayerThumbnailChange
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.m3play_canvas)) },
                            description = stringResource(R.string.m3play_canvas_desc),
                            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                            checked = archiveTuneCanvasEnabled, onCheckedChange = onM3PlayCanvasEnabledChange
                        )

                        ThumbnailCornerRadiusSelectorButton(
                            modifier = Modifier.padding(16.dp), onRadiusSelected = {}
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.crop_thumbnail_to_square)) },
                            description = stringResource(R.string.crop_thumbnail_to_square_desc),
                            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                            checked = cropThumbnailToSquare, onCheckedChange = onCropThumbnailToSquareChange
                        )

                        EnumListPreference(
                            title = { Text(stringResource(R.string.player_buttons_style)) },
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            selectedValue = playerButtonsStyle, onValueSelected = onPlayerButtonsStyleChange,
                            valueText = { it.name }
                        )

                        PreferenceEntry(
                            title = { Text(stringResource(R.string.player_slider_style)) },
                            description = sliderStyleLabel(sliderStyle),
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                            onClick = { showSliderOptionDialog = true },
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                            icon = { Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null) },
                            checked = swipeThumbnail, onCheckedChange = onSwipeThumbnailChange,
                        )

                        AnimatedVisibility(swipeThumbnail) {
                            var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
                            
                            if (showSensitivityDialog) {
                                var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
                                DefaultDialog(
                                    onDismiss = { tempSensitivity = swipeSensitivity; showSensitivityDialog = false },
                                    buttons = {
                                        TextButton(onClick = { tempSensitivity = 0.73f }) { Text(stringResource(R.string.reset)) }
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = { onSwipeSensitivityChange(tempSensitivity); showSensitivityDialog = false }) { Text(stringResource(android.R.string.ok)) }
                                    }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                        Text("Swipe Sensitivity", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                                        Text("${(tempSensitivity * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                                        Slider(value = tempSensitivity, onValueChange = { tempSensitivity = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                description = "${(swipeSensitivity * 100).roundToInt()}%",
                                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                                onClick = { showSensitivityDialog = true }
                            )
                        }
                    }
                }
            }

        
            item {
                PreferenceGroupTitle(title = stringResource(R.string.lyrics), modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        
                        EnumListPreference(
                            title = { Text("Animation Style") },
                            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                            selectedValue = lyricsAnimationStyle, onValueSelected = onLyricsAnimationStyleChange,
                            valueText = { it.name }
                        )

                        EnumListPreference(
                            title = { Text("Text Position") },
                            icon = { Icon(imageVector = Icons.Default.List, contentDescription = null) },
                            selectedValue = lyricsPosition, onValueSelected = onLyricsPositionChange,
                            valueText = { it.name }
                        )

                        PreferenceEntry(
                            title = { Text("Text Size") }, description = "${lyricsTextSize.roundToInt()} sp",
                            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) }, onClick = { showLyricsTextSizeDialog = true }
                        )

                        PreferenceEntry(
                            title = { Text("Line Spacing") }, description = "${String.format(Locale.US, "%.1f", lyricsLineSpacing)}x",
                            icon = { Icon(imageVector = Icons.Default.List, contentDescription = null) }, onClick = { showLyricsLineSpacingDialog = true }
                        )

                        SwitchPreference(
                            title = { Text("Glow Effect") }, description = "Add glow to highlighted lyrics",
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            checked = lyricsGlowEffect, onCheckedChange = onLyricsGlowEffectChange,
                        )

                        SwitchPreference(
                            title = { Text("Respect Agent Positioning") }, description = "Align duets (v1/v2) to left/right",
                            icon = { Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = null) },
                            checked = respectAgentPositioning, onCheckedChange = onRespectAgentPositioningChange,
                        )

                        SwitchPreference(
                            title = { Text("Click to Seek") },
                            icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = null) },
                            checked = lyricsClick, onCheckedChange = onLyricsClickChange,
                        )

                        SwitchPreference(
                            title = { Text("Auto Scroll") },
                            icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                            checked = lyricsScroll, onCheckedChange = onLyricsScrollChange,
                        )

                        SwitchPreference(
                            title = { Text("Hide Status Bar") }, description = "Hide status bar on lyrics full screen",
                            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = null) },
                            checked = hideStatusBarOnFullscreen, onCheckedChange = onHideStatusBarOnFullscreenChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
                            icon = { Icon(imageVector = Icons.Default.Share, contentDescription = null) },
                            checked = lyricsRomanizeJapanese, onCheckedChange = onLyricsRomanizeJapaneseChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
                            icon = { Icon(imageVector = Icons.Default.Share, contentDescription = null) },
                            checked = lyricsRomanizeKorean, onCheckedChange = onLyricsRomanizeKoreanChange,
                        )
                    }
                }
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.misc), modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        EnumListPreference(
                            title = { Text(stringResource(R.string.default_open_tab)) },
                            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = null) },
                            selectedValue = defaultOpenTab, onValueSelected = onDefaultOpenTabChange,
                            valueText = { it.name }
                        )

                        ListPreference(
                            title = { Text(stringResource(R.string.default_lib_chips)) },
                            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = null) },
                            selectedValue = defaultChip,
                            values = listOf(LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS, LibraryFilter.ALBUMS, LibraryFilter.ARTISTS),
                            valueText = { it.name }, onValueSelected = onDefaultChipChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_home_category_chips)) },
                            description = stringResource(R.string.show_home_category_chips_desc),
                            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                            checked = showHomeCategoryChips, onCheckedChange = onShowHomeCategoryChipsChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_tags_in_library)) },
                            description = stringResource(R.string.show_tags_in_library_desc),
                            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            checked = showTagsInLibrary, onCheckedChange = onShowTagsInLibraryChange,
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.swipe_song_to_add)) },
                            icon = { Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null) },
                            checked = swipeToSong, onCheckedChange = onSwipeToSongChange
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.slim_navbar)) },
                            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = null) },
                            checked = slimNav, onCheckedChange = onSlimNavChange
                        )

                        EnumListPreference(
                            title = { Text(stringResource(R.string.grid_cell_size)) },
                            icon = { Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null) },
                            selectedValue = gridItemSize, onValueSelected = onGridItemSizeChange,
                            valueText = { it.name }
                        )
                    }
                }
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.auto_playlists), modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_liked_playlist)) },
                            icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = null) },
                            checked = showLikedPlaylist, onCheckedChange = onShowLikedPlaylistChange
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                            icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                            checked = showDownloadedPlaylist, onCheckedChange = onShowDownloadedPlaylistChange
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_top_playlist)) },
                            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                            checked = showTopPlaylist, onCheckedChange = onShowTopPlaylistChange
                        )

                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_cached_playlist)) },
                            icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                            checked = showCachedPlaylist, onCheckedChange = onShowCachedPlaylistChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderStyleOptionCard(
    sliderStyle: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .aspectRatio(1f)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        StyledPlaybackSlider(
            sliderStyle = sliderStyle, value = sliderValue, valueRange = 0f..1f,
            onValueChange = { sliderValue = it }, onValueChangeFinished = {},
            activeColor = MaterialTheme.colorScheme.primary, isPlaying = true,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Text(
            text = sliderStyleLabel(sliderStyle), style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
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
