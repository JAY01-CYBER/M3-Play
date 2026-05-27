package com.j.m3play.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean, // Compat parameter
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary, // Keeping parameter for compatibility, though YT uses B/W
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    // YouTube uses Solid Black or Solid White/Surface for its background
    val bgColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface

    Surface(
        color = bgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // YouTube Style Top Thin Divider Line
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            NavigationBar(
                containerColor = Color.Transparent, // Handled by parent Surface
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp, // Flat look like YouTube
                windowInsets = WindowInsets.navigationBars // Fills the bottom system bar area perfectly
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onItemClick(screen, selected) },
                        icon = {
                            Icon(
                                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                                contentDescription = stringResource(screen.titleId),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(screen.titleId),
                                // YouTube uses very small text (around 10sp)
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        },
                        // The Magic: This removes the M3 Pill and mimics YouTube's exact colors
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent, // M3 Indicator Pill removed completely
                            selectedIconColor = MaterialTheme.colorScheme.onSurface, // Black/White like YT
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
