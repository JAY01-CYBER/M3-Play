package com.j.m3play.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.DefaultDialog
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.M3PlaySlider
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
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
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, true)
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(RandomThemeOnStartupKey, false)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(PlayerDesignStyleKey, PlayerDesignStyle.V4)
    val (miniPlayerStyle, onMiniPlayerStyleChange) = rememberEnumPreference(MiniPlayerStyleKey, MiniPlayerStyle.MODERN)
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(com.j.m3play.constants.UseNewLibraryDesignKey, false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (archiveTuneCanvasEnabled, onM3PlayCanvasEnabledChange) = rememberPreference(M3PlayCanvasKey, false)
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(ThumbnailCornerRadiusKey, 16f)
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) = rememberPreference(CropThumbnailToSquareKey, false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, true)
    
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val (lyricsAnimation, onLyricsAnimationChange) = rememberEnumPreference<LyricsAnimationStyle>(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, 26f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, 1.3f)
    val (useLyricsV2, onUseLyricsV2Change) = rememberPreference(UseLyricsV2Key, true)
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

    // --- DIALOG STATES ---
    var showDarkModeDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerDesignDialog by rememberSaveable { mutableStateOf(false) }
    var showMiniPlayerStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerButtonsDialog by rememberSaveable { mutableStateOf(false) }
    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsAnimationDialog by rememberSaveable { mutableStateOf(false) }
    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsTextSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by rememberSaveable { mutableStateOf(false) }
    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

    // --- DIALOGS ---
    if (showDarkModeDialog) {
        EnumDialog(
            onDismiss = { showDarkModeDialog = false },
            onSelect = { onDarkModeChange(it); showDarkModeDialog = false },
            title = stringResource(R.string.dark_theme),
            current = darkMode,
            values = DarkMode.values().toList(),
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            }
        )
    }

    if (showPlayerDesignDialog) {
        EnumDialog(
            onDismiss = { showPlayerDesignDialog = false },
            onSelect = { onPlayerDesignStyleChange(it); showPlayerDesignDialog = false },
            title = stringResource(R.string.player_design_style),
            current = playerDesignStyle,
            values = PlayerDesignStyle.values().toList(),
            valueText = { it.name }
        )
    }

    if (showMiniPlayerStyleDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerStyleDialog = false },
            onSelect = { onMiniPlayerStyleChange(it); showMiniPlayerStyleDialog = false },
            title = "Mini player style",
            current = miniPlayerStyle,
            values = MiniPlayerStyle.values().toList(),
            valueText = { it.name }
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = { onPlayerBackgroundChange(it); showPlayerBackgroundDialog = false },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = PlayerBackgroundStyle.values().toList(),
            valueText = { it.name }
        )
    }
    
    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = { onMiniPlayerBackgroundChange(it); showMiniPlayerBackgroundDialog = false },
            title = "Mini player background style",
            current = miniPlayerBackground,
            values = PlayerBackgroundStyle.values().toList(),
            valueText = { it.name }
        )
    }

    if (showPlayerButtonsDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsDialog = false },
            onSelect = { onPlayerButtonsStyleChange(it); showPlayerButtonsDialog = false },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = { it.name }
        )
    }
    
    if (showSliderOptionDialog) {
        EnumDialog(
            onDismiss = { showSliderOptionDialog = false },
            onSelect = { onSliderStyleChange(it); showSliderOptionDialog = false },
            title = stringResource(R.string.player_slider_style),
            current = sliderStyle,
            values = SliderStyle.values().toList(),
            valueText = { it.name }
        )
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = { onLyricsPositionChange(it); showLyricsPositionDialog = false },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = { it.name }
        )
    }

    if (showLyricsAnimationDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationDialog = false },
            onSelect = { onLyricsAnimationChange(it); showLyricsAnimationDialog = false },
            title = stringResource(R.string.lyrics_animation_style),
            current = lyricsAnimation,
            values = LyricsAnimationStyle.values().toList(),
            valueText = { it.name }
        )
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = { onDefaultOpenTabChange(it); showDefaultOpenTabDialog = false },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = { it.name }
        )
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = { onDefaultChipChange(it); showDefaultChipDialog = false },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = listOf(LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS, LibraryFilter.ALBUMS, LibraryFilter.ARTISTS),
            valueText = { it.name }
        )
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = { onGridItemSizeChange(it); showGridSizeDialog = false },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = { it.name }
        )
    }

    // Custom Dialogs for Sliders
    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        DefaultDialog(
            onDismiss = { showLyricsTextSizeDialog = false },
            buttons = {
                TextButton(onClick = { tempTextSize = 26f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsTextSizeChange(tempTextSize); showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = "${tempTextSize.roundToInt()} sp", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                M3PlaySlider(value = tempTextSize, onValueChange = { tempTextSize = it }, valueRange = 16f..36f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        DefaultDialog(
            onDismiss = { showLyricsLineSpacingDialog = false },
            buttons = {
                TextButton(onClick = { tempLineSpacing = 1.3f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsLineSpacingChange(tempLineSpacing); showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = "${String.format("%.1f", tempLineSpacing)}x", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                M3PlaySlider(value = tempLineSpacing, onValueChange = { tempLineSpacing = it }, valueRange = 1.0f..2.0f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSensitivityDialog) {
        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
        DefaultDialog(
            onDismiss = { showSensitivityDialog = false },
            buttons = {
                TextButton(onClick = { tempSensitivity = 0.73f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showSensitivityDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSwipeSensitivityChange(tempSensitivity); showSensitivityDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = "${(tempSensitivity * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                M3PlaySlider(value = tempSensitivity, onValueChange = { tempSensitivity = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) { Icon(painterResource(R.drawable.arrow_back), null) }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
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
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.theme),
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                            trailingContent = { Switch(checked = dynamicTheme, onCheckedChange = onDynamicThemeChange, thumbContent = { Icon(painterResource(if (dynamicTheme) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onDynamicThemeChange(!dynamicTheme) }
                        ))
                        if (!dynamicTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.shuffle),
                                title = { Text(stringResource(R.string.random_theme_on_startup)) },
                                description = { Text(stringResource(R.string.random_theme_on_startup_desc)) },
                                trailingContent = { Switch(checked = randomThemeOnStartup, onCheckedChange = onRandomThemeOnStartupChange, thumbContent = { Icon(painterResource(if (randomThemeOnStartup) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { onRandomThemeOnStartupChange(!randomThemeOnStartup) }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.format_paint),
                                title = { Text(stringResource(R.string.color_palette)) },
                                description = { Text(stringResource(R.string.customize_theme_colors)) },
                                showChevron = true,
                                onClick = { navController.navigate("settings/appearance/palette_picker") }
                            ))
                        }
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.dark_mode),
                            title = { Text(stringResource(R.string.dark_theme)) },
                            description = { Text(darkMode.name) },
                            onClick = { showDarkModeDialog = true }
                        ))
                        if (useDarkTheme) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.contrast),
                                title = { Text(stringResource(R.string.pure_black)) },
                                trailingContent = { Switch(checked = pureBlack, onCheckedChange = onPureBlackChange, thumbContent = { Icon(painterResource(if (pureBlack) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { onPureBlackChange(!pureBlack) }
                            ))
                        }
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.blur_off),
                            title = { Text(stringResource(R.string.enable_blur)) },
                            description = { Text(stringResource(R.string.enable_blur_description)) },
                            trailingContent = { Switch(checked = !disableBlur, onCheckedChange = { onDisableBlurChange(!it) }, thumbContent = { Icon(painterResource(if (!disableBlur) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onDisableBlurChange(!disableBlur) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text("App Font") },
                            description = { Text("Configure typography") },
                            showChevron = true,
                            onClick = { navController.navigate("settings/appearance/font") }
                        ))
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.player),
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.player_design_style)) },
                            description = { Text(playerDesignStyle.name) },
                            onClick = { showPlayerDesignDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text("Mini player style") },
                            description = { Text(miniPlayerStyle.name) },
                            onClick = { showMiniPlayerStyleDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.grid_view),
                            title = { Text(stringResource(R.string.new_library_design)) },
                            description = { Text(stringResource(R.string.new_library_design_description)) },
                            trailingContent = { Switch(checked = useNewLibraryDesign, onCheckedChange = onUseNewLibraryDesignChange, thumbContent = { Icon(painterResource(if (useNewLibraryDesign) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onUseNewLibraryDesignChange(!useNewLibraryDesign) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.gradient),
                            title = { Text(stringResource(R.string.player_background_style)) },
                            description = { Text(playerBackground.name) },
                            onClick = { showPlayerBackgroundDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.gradient),
                            title = { Text("Mini player background style") },
                            description = { Text(miniPlayerBackground.name) },
                            onClick = { showMiniPlayerBackgroundDialog = true }
                        ))
                        if (playerBackground == PlayerBackgroundStyle.CUSTOM) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.image),
                                title = { Text(stringResource(R.string.customized_background)) },
                                showChevron = true,
                                onClick = { navController.navigate("customize_background") }
                            ))
                        }
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.hide_image),
                            title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                            description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                            trailingContent = { Switch(checked = hidePlayerThumbnail, onCheckedChange = onHidePlayerThumbnailChange, thumbContent = { Icon(painterResource(if (hidePlayerThumbnail) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.motion_photos_on),
                            title = { Text(stringResource(R.string.m3play_canvas)) },
                            description = { Text(stringResource(R.string.m3play_canvas_desc)) },
                            trailingContent = { Switch(checked = archiveTuneCanvasEnabled, onCheckedChange = onM3PlayCanvasEnabledChange, thumbContent = { Icon(painterResource(if (archiveTuneCanvasEnabled) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onM3PlayCanvasEnabledChange(!archiveTuneCanvasEnabled) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.image),
                            title = { Text(stringResource(R.string.crop_thumbnail_to_square)) },
                            description = { Text(stringResource(R.string.crop_thumbnail_to_square_desc)) },
                            trailingContent = { Switch(checked = cropThumbnailToSquare, onCheckedChange = onCropThumbnailToSquareChange, thumbContent = { Icon(painterResource(if (cropThumbnailToSquare) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onCropThumbnailToSquareChange(!cropThumbnailToSquare) }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.player_buttons_style)) },
                            description = { Text(playerButtonsStyle.name) },
                            onClick = { showPlayerButtonsDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.sliders),
                            title = { Text(stringResource(R.string.player_slider_style)) },
                            description = { Text(sliderStyle.name) },
                            onClick = { showSliderOptionDialog = true }
                        ))
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                            trailingContent = { Switch(checked = swipeThumbnail, onCheckedChange = onSwipeThumbnailChange, thumbContent = { Icon(painterResource(if (swipeThumbnail) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onSwipeThumbnailChange(!swipeThumbnail) }
                        ))
                        if (swipeThumbnail) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.tune),
                                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                description = { Text(stringResource(R.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt())) },
                                onClick = { showSensitivityDialog = true }
                            ))
                        }
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.lyrics),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text("Lyrics V2 (Experimental)") },
                            description = { Text("Use the new fluid word-synced lyrics engine") },
                            trailingContent = { Switch(checked = useLyricsV2, onCheckedChange = onUseLyricsV2Change, thumbContent = { Icon(painterResource(if (useLyricsV2) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onUseLyricsV2Change(!useLyricsV2) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_text_position)) },
                            description = { Text(lyricsPosition.name) },
                            onClick = { showLyricsPositionDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.animation),
                            title = { Text(stringResource(R.string.lyrics_animation_style)) },
                            description = { Text(lyricsAnimation.name) },
                            onClick = { showLyricsAnimationDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_click_change)) },
                            trailingContent = { Switch(checked = lyricsClick, onCheckedChange = onLyricsClickChange, thumbContent = { Icon(painterResource(if (lyricsClick) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onLyricsClickChange(!lyricsClick) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                            trailingContent = { Switch(checked = lyricsScroll, onCheckedChange = onLyricsScrollChange, thumbContent = { Icon(painterResource(if (lyricsScroll) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onLyricsScrollChange(!lyricsScroll) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.lyrics_text_size)) },
                            description = { Text("${lyricsTextSize.roundToInt()} sp") },
                            onClick = { showLyricsTextSizeDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.text_fields),
                            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                            description = { Text("${String.format("%.1f", lyricsLineSpacing)}x") },
                            onClick = { showLyricsLineSpacingDialog = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.misc),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.default_open_tab)) },
                            description = { Text(defaultOpenTab.name) },
                            onClick = { showDefaultOpenTabDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.tab),
                            title = { Text(stringResource(R.string.default_lib_chips)) },
                            description = { Text(defaultChip.name) },
                            onClick = { showDefaultChipDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.home_outlined),
                            title = { Text(stringResource(R.string.show_home_category_chips)) },
                            description = { Text(stringResource(R.string.show_home_category_chips_desc)) },
                            trailingContent = { Switch(checked = showHomeCategoryChips, onCheckedChange = onShowHomeCategoryChipsChange, thumbContent = { Icon(painterResource(if (showHomeCategoryChips) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowHomeCategoryChipsChange(!showHomeCategoryChips) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.filter_alt),
                            title = { Text(stringResource(R.string.show_tags_in_library)) },
                            description = { Text(stringResource(R.string.show_tags_in_library_desc)) },
                            trailingContent = { Switch(checked = showTagsInLibrary, onCheckedChange = onShowTagsInLibraryChange, thumbContent = { Icon(painterResource(if (showTagsInLibrary) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowTagsInLibraryChange(!showTagsInLibrary) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.swipe_song_to_add)) },
                            trailingContent = { Switch(checked = swipeToSong, onCheckedChange = onSwipeToSongChange, thumbContent = { Icon(painterResource(if (swipeToSong) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onSwipeToSongChange(!swipeToSong) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.slim_navbar)) },
                            trailingContent = { Switch(checked = slimNav, onCheckedChange = onSlimNavChange, thumbContent = { Icon(painterResource(if (slimNav) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onSlimNavChange(!slimNav) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.grid_view),
                            title = { Text(stringResource(R.string.grid_cell_size)) },
                            description = { Text(gridItemSize.name) },
                            onClick = { showGridSizeDialog = true }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.auto_playlists),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.favorite),
                            title = { Text(stringResource(R.string.show_liked_playlist)) },
                            trailingContent = { Switch(checked = showLikedPlaylist, onCheckedChange = onShowLikedPlaylistChange, thumbContent = { Icon(painterResource(if (showLikedPlaylist) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.offline),
                            title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                            trailingContent = { Switch(checked = showDownloadedPlaylist, onCheckedChange = onShowDownloadedPlaylistChange, thumbContent = { Icon(painterResource(if (showDownloadedPlaylist) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.trending_up),
                            title = { Text(stringResource(R.string.show_top_playlist)) },
                            trailingContent = { Switch(checked = showTopPlaylist, onCheckedChange = onShowTopPlaylistChange, thumbContent = { Icon(painterResource(if (showTopPlaylist) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowTopPlaylistChange(!showTopPlaylist) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.cached),
                            title = { Text(stringResource(R.string.show_cached_playlist)) },
                            trailingContent = { Switch(checked = showCachedPlaylist, onCheckedChange = onShowCachedPlaylistChange, thumbContent = { Icon(painterResource(if (showCachedPlaylist) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                            onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) }
                        )
                    )
                )
            }
        }
    }
}
