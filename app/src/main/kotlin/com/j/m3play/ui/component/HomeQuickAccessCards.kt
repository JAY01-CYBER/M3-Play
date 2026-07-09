/*
 * ♪ M3Play Signature Component
 * File: HomeQuickAccessCards.kt
 *
 * Crafted for immersive music experience
 * Designed & maintained by JAY01-CYBER
 *
 * Signature: M3PLAY::SIGNATURE::QUICK_ACCESS::V2
 */

package com.j.m3play.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.R

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp), // Image के हिसाब से softer corners
        colors = CardDefaults.cardColors(
            // Background को हल्का रखने के लिए surfaceContainer का इस्तेमाल किया है
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeQuickAccessCards(
    onLikedClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 2x2 Grid हटाकर अब 1 ही Row इस्तेमाल कर रहे हैं
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAccessCard(
            title = stringResource(id = R.string.liked),
            subtitle = "128 songs",
            icon = Icons.Filled.Favorite,
            iconTint = Color(0xFF673AB7), // Purple
            onClick = onLikedClick,
            modifier = Modifier.weight(1f),
        )
        QuickAccessCard(
            title = stringResource(id = R.string.downloads),
            subtitle = "42 songs",
            icon = Icons.Filled.Download,
            iconTint = Color(0xFF1976D2), // Blue
            onClick = onDownloadsClick,
            modifier = Modifier.weight(1f),
        )
        QuickAccessCard(
            title = stringResource(id = R.string.history),
            subtitle = "Recently played",
            icon = Icons.Filled.History,
            iconTint = Color(0xFF388E3C), // Green
            onClick = onHistoryClick,
            modifier = Modifier.weight(1f),
        )
        QuickAccessCard(
            title = "Account", // यहाँ stringResource(id = R.string.account) कर सकते हैं
            subtitle = "View profile",
            icon = Icons.Filled.Person, // Image के हिसाब से Person icon लगाया है
            iconTint = Color(0xFFFF5722), // Orange
            onClick = onLibraryClick, 
            modifier = Modifier.weight(1f),
        )
    }
}
