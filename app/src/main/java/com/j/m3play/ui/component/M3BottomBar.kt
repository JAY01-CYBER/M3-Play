package com.j.m3play.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.j.m3play.R

@Composable
fun M3BottomBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 6.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                BottomItem(
                    selected = currentRoute == "home",
                    iconRes = if (currentRoute == "home") R.drawable.home_filled else R.drawable.home_outlined,
                    label = "Home",
                    onClick = onHomeClick
                )

                Spacer(modifier = Modifier.width(72.dp))

                BottomItem(
                    selected = currentRoute == "library",
                    iconRes = if (currentRoute == "library") R.drawable.library_music_filled else R.drawable.library_music_outlined,
                    label = "Library",
                    onClick = onLibraryClick
                )
            }
        }

        // Floating Search Button
        Surface(
            modifier = Modifier
                .size(70.dp)
                .clickable(onClick = onSearchClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {

    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .background(
                if (selected) activeBg else Color.Transparent,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor
        )

        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = activeColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
