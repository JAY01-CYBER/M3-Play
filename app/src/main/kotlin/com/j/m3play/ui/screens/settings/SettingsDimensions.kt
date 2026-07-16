package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

object SettingsDimensions {
    val GroupCardCornerRadius = 28.dp 
    val QuickActionCardCornerRadius = 24.dp
    val IntegrationPillCornerRadius = 100.dp 
    val BannerCardCornerRadius = 28.dp
    val HeroCardCornerRadius = 32.dp
    val RowIconCornerRadius = 16.dp 

    val ScreenHorizontalPadding = 16.dp
    val CardInternalPadding = 16.dp
    val SectionSpacing = 16.dp 
    val RowVerticalPadding = 16.dp
    val RowHorizontalPadding = 20.dp 

    val RowIconSize = 42.dp
    val RowIconInnerSize = 22.dp
    val QuickActionIconSize = 44.dp
    val QuickActionIconInnerSize = 24.dp
    val HeroIconSize = 64.dp
    val HeroIconInnerSize = 32.dp
    val IntegrationIconSize = 30.dp
    val IntegrationIconInnerSize = 18.dp
    val BannerIconSize = 48.dp
    val BannerIconInnerSize = 24.dp
    val ChevronSize = 20.dp

    val DividerThickness = 1.dp
    val DividerStartIndent = 72.dp 

    val SectionHeaderBottomPadding = 8.dp
    val SectionHeaderHorizontalPadding = 24.dp 

    val QuickActionTileAspectRatio = 1.3f

    val CompactColumns = 2
    val MediumColumns = 4
    val ExpandedColumns = 4

    val MediumPaneLeftWeight = 0.42f
    val MediumPaneRightWeight = 0.58f
    val ExpandedListPaneWidth = 380.dp
}

// Bouncy aur "boosted" animations ko completely normal (1.0f) kar diya hai.
// Taki agar app ke kisi aur hisse me ye use ho raha ho toh crash na ho, 
// par Material 3 jaisa normal feel aaye (no extra bounce).
object SettingsAnimations {
    val PressScale = 1.0f
    val TilePressScale = 1.0f
    val PillPressScale = 1.0f
    val IconPressRotation = 0f
    val PillPressLift = 0.dp

    val EntranceFadeDuration = 300
    val EntranceSlideDuration = 350
    val StaggerDelayPerItem = 80
    val ExitFadeDuration = 200

    fun <T> pressSpring() = spring<T>(stiffness = Spring.StiffnessHigh)
    fun <T> entranceSpring() = spring<T>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = 0.85f,
    )
}
