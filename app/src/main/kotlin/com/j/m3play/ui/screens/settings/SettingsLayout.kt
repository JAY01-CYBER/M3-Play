package com.j.m3play.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.j.m3play.LocalPlayerAwareWindowInsets

data class SettingsContentState(
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val latestVersion: String,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
)

// नाम बदल दिया गया है ताकि पुरानी फाइल्स से Conflict न हो
@Composable
fun PixelSettingsGroupCard(group: SettingsGroup, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = group.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
        )
        group.items.forEach { item ->
            PixelSettingsListItem(item = item)
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
        )
    }
}

// नाम बदल दिया गया है ताकि पुरानी फाइल्स से Conflict न हो
@Composable
fun PixelSettingsListItem(item: SettingsItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = item.icon,
            contentDescription = null,
            tint = if (item.accentColor != Color.Unspecified) item.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
) {
    var bannerVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(60))
        bannerVisible = true
        anim.animateTo(1f, tween(70))
        categoriesVisible = true
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            ),
        contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
    ) {
        if (!state.isSearchActive) {
            item(key = "permission") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showPermissionBanner,
                ) {
                    // अगर आपके पास SettingsPermissionBanner नाम का फंक्शन है तो यहाँ अनकमेंट कर सकते हैं
                    /*
                    SettingsPermissionBanner(
                        onRequestPermission = state.onRequestPermission,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    */
                }
            }
            item(key = "update") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showUpdateBanner,
                ) {
                    // SettingsUpdateBanner 
                    /*
                    SettingsUpdateBanner(
                        latestVersion = state.latestVersion,
                        onClick = state.onUpdateClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    */
                }
            }
        }

        if (state.isSearchActive && !state.hasSearchResults) {
            item(key = "empty") {
                Spacer(modifier = Modifier.height(24.dp))
                // SettingsSearchEmpty()
            }
        } else {
            if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                item(key = "internalSearchResults") {
                    PixelSettingsGroupCard(group = state.internalGroup)
                }
            }

            items(
                count = state.groups.size,
                key = { state.groups[it].title },
            ) { index ->
                val group = state.groups[index]
                AnimatedVisibility(
                    visible = categoriesVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 5 }),
                ) {
                    PixelSettingsGroupCard(group = group)
                }
            }
        }
    }
}
