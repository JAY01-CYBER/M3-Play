package com.j.m3play.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R

val SectionTitleGap = 18.dp

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = SectionTitleGap)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(32.dp)
                    index == 0 -> RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    index == items.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 32.dp, bottomEnd = 32.dp)
                    else -> RoundedCornerShape(8.dp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (!useLowContrast) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Material3SettingsItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(16.dp))
        } else if (item.icon != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (item.isHighlighted) 0.15f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.showBadge) {
                    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                )
            ) { item.title() }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { desc() }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }

        if (item.showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val showChevron: Boolean = false,
    val onClick: (() -> Unit)? = null
)
