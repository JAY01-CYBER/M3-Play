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
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.*
import com.j.m3play.ui.player.StyledPlaybackSlider
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberEnumPreference
import com.j.m3play.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(RandomThemeOnStartupKey, defaultValue = false)
    val (useDarkTheme) = remember(darkMode, isSystemInDarkTheme()) { mutableStateOf(if (darkMode == DarkMode.AUTO) isSystemInDarkTheme() else darkMode == DarkMode.ON) }

    Column(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).verticalScroll(rememberScrollState())) {
        PreferenceGroupTitle(title = stringResource(R.string.theme))
        EnumListPreference(title = { Text(stringResource(R.string.dark_theme)) }, icon = { Icon(painterResource(R.drawable.dark_mode), null) }, selectedValue = darkMode, onValueSelected = onDarkModeChange, valueText = { it.name })
        AnimatedVisibility(useDarkTheme) { SwitchPreference(title = { Text(stringResource(R.string.pure_black)) }, icon = { Icon(painterResource(R.drawable.contrast), null) }, checked = pureBlack, onCheckedChange = onPureBlackChange) }
        PreferenceGroupTitle(title = stringResource(R.string.player))
        EnumListPreference(title = { Text(stringResource(R.string.player_design_style)) }, icon = { Icon(painterResource(R.drawable.palette), null) }, selectedValue = playerDesignStyle, onValueSelected = onPlayerDesignStyleChange, valueText = { if (it == PlayerDesignStyle.APPLE) "Apple Music Style" else it.name })
    }
    TopAppBar(title = { Text(stringResource(R.string.appearance)) }, navigationIcon = { IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) { Icon(painterResource(R.drawable.arrow_back), contentDescription = null) } })
}

enum class DarkMode { ON, OFF, AUTO }
