package com.j.m3play.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.j.m3play.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean, 
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (pureBlack) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black,
                            )
                        )
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                }
            )
    ) {
        NavigationBar(
            windowInsets = WindowInsets(0, 0, 0, 0), 
            containerColor = Color.Transparent, // Transparent color
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)
                
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(screen, selected) },
                    label = {
                        Text(
                            text = stringResource(screen.titleId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                            contentDescription = stringResource(screen.titleId),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = accentColor.copy(alpha = 0.2f),
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.windowInsetsPadding(
                        NavigationBarDefaults.windowInsets
                    )
                )
            }
        }
    }
}
