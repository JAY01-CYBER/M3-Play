package com.j.m3play.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R
import com.j.m3play.LocalPlayerAwareWindowInsets

data class SettingsContentState(
    val quickSettings: List<SettingsItem>,
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
)

// स्मूद एनिमेशन रैपर
@Composable
fun AnimatedListItem(index: Int, content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(durationMillis = 400, delayMillis = index * 50, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(durationMillis = 400, delayMillis = index * 50, easing = FastOutSlowInEasing)
                )
    ) {
        content()
    }
}

// 1. टॉप हेडर (Settings + Subtitle + Music Icon)
@Composable
fun SettingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize your listening experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        // Music Icon Squircle
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF8B5CF6), RoundedCornerShape(16.dp)), // Purple icon bg
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play), // Replace with music note icon
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 2. इनलाइन सर्च बार
@Composable
fun SettingsInlineSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search settings", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
    )
}

// 3. Quick Settings (2x2 ग्रिड)
@Composable
fun QuickSettingsGrid(quickSettings: List<SettingsItem>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quick Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
        )
        
        val chunks = quickSettings.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            chunks.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { item ->
                        QuickSettingCard(item = item, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSettingCard(item: SettingsItem, modifier: Modifier = Modifier) {
    val iconTint = if (item.accentColor != Color.Unspecified) item.accentColor else MaterialTheme.colorScheme.primary
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable { item.onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = iconTint.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = item.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (item.subtitle != null) {
                    Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

// 4. मेन सेटिंग्स ग्रुप
@Composable
fun PremiumSettingsGroupCard(group: SettingsGroup) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                group.items.forEachIndexed { index, item ->
                    PremiumSettingsListItem(item = item)
                    if (index < group.items.size - 1) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumSettingsListItem(item: SettingsItem) {
    val iconTint = if (item.accentColor != Color.Unspecified) item.accentColor else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = iconTint.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = item.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
    }
}

// 5. लेआउट असेंबली
@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Black/Theme background
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        contentPadding = PaddingValues(top = topPadding, bottom = 48.dp),
    ) {
        item {
            AnimatedListItem(0) { SettingsHeader() }
        }
        
        item {
            AnimatedListItem(1) {
                Column {
                    SettingsInlineSearchBar(query = query, onQueryChange = onQueryChange)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (!state.isSearchActive) {
            item {
                AnimatedListItem(2) {
                    QuickSettingsGrid(quickSettings = state.quickSettings)
                }
            }
        }

        if (state.isSearchActive && !state.hasSearchResults) {
            item { Spacer(modifier = Modifier.height(24.dp)) /* Show empty search state here */ }
        } else {
            items(
                count = state.groups.size,
                key = { state.groups[it].title },
            ) { index ->
                val group = state.groups[index]
                AnimatedListItem(index + 3) {
                    PremiumSettingsGroupCard(group = group)
                }
            }
        }
    }
}
