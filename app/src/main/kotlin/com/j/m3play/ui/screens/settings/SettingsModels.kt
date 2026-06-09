package com.j.m3play.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

data class SettingsItem(
    val icon: Painter,
    val title: String,
    val subtitle: String? = null,
    val accentColor: Color = Color.Unspecified,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit,
)
