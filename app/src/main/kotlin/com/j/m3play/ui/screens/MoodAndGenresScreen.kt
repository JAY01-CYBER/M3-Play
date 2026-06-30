package com.j.m3play.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.shimmer.ShimmerHost
import com.j.m3play.ui.component.shimmer.TextPlaceholder
import com.j.m3play.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val topPadding = with(density) { windowInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { windowInsets.getBottom(this).toDp() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            gridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = topPadding,
            end = 16.dp,
            bottom = bottomPadding + 80.dp,
        ),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.animateItem().padding(bottom = 16.dp)) {
                NavigationTitle(
                    title = stringResource(R.string.mood_and_genres),
                )
                Text(
                    text = "Music for every vibe",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }

        if (moodAndGenres == null) {
            items(12) {
                ShimmerHost {
                    TextPlaceholder(
                        height = MoodAndGenresButtonHeight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        } else {
            items(
                items = moodAndGenres.orEmpty(),
                key = { item -> "${item.title}:${item.endpoint.browseId}:${item.endpoint.params}" },
            ) { item ->
                MoodAndGenresButton(
                    title = item.title,
                    stripeColor = item.stripeColor,
                    onClick = {
                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .animateItem(),
                )
            }
        }
    }
}

@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = Color(stripeColor)
    
    // Yahan check kar rahe hain ki background dark hai ya light
    val isLightBackground = baseColor.luminance() > 0.4f 
    // Light bg pe Black text, Dark bg pe White text
    val contentColor = if (isLightBackground) Color.Black else Color.White 

    val genreData = getGenreVisuals(title)

    Box(
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Naya Gradient: Bina image ke bhi card premium lagega
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to baseColor.copy(alpha = 0.6f), // Left side slightly lighter
                        1.0f to baseColor // Right side full color
                    )
                )
        )

        // Agar future me Image add karni ho (Commented)
        if (genreData.bgImageRes != null) {
            Image(
                painter = painterResource(id = genreData.bgImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .align(Alignment.CenterEnd)
            )
            // Gradient Overlay for Image blending
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to baseColor,
                            0.5f to baseColor.copy(alpha = 0.85f),
                            0.8f to baseColor.copy(alpha = 0.3f),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circular Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)), // Translucent matching circle
                contentAlignment = Alignment.Center
            ) {
                if (genreData.icon != null) {
                    Icon(
                        imageVector = genreData.icon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = genreData.textIcon ?: title.take(1),
                        color = contentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and Subtitle Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = contentColor.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = genreData.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Arrow Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    // Arrow ka color dark cards par base color hoga, light cards par dark gray
                    tint = if (isLightBackground) Color.Black.copy(alpha = 0.7f) else baseColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

val MoodAndGenresButtonHeight = 110.dp

data class GenreVisuals(
    val subtitle: String,
    val textIcon: String? = null,
    val icon: ImageVector? = null,
    val bgImageRes: Int? = null
)

// Logic update: Ab directly compare karne ke bajaye "contains" check karega
fun getGenreVisuals(title: String): GenreVisuals {
    val lowerTitle = title.lowercase()
    return when {
        lowerTitle.contains("hindi") -> GenreVisuals(
            subtitle = "Bollywood hits & more",
            textIcon = "अ",
        )
        lowerTitle.contains("2010") -> GenreVisuals(
            subtitle = "The best of the 2010s",
            textIcon = "2010s",
        )
        lowerTitle.contains("family") -> GenreVisuals(
            subtitle = "Songs for the whole family",
            icon = Icons.Rounded.People,
        )
        lowerTitle.contains("romance") -> GenreVisuals(
            subtitle = "Feel the love",
            icon = Icons.Rounded.Favorite,
        )
        lowerTitle.contains("pop") -> GenreVisuals(
            subtitle = "Today's top hits",
            icon = Icons.Rounded.Mic,
        )
        lowerTitle.contains("hip-hop") -> GenreVisuals(
            subtitle = "Beats that move you",
            icon = Icons.Rounded.LibraryMusic,
        )
        lowerTitle.contains("haryanvi") -> GenreVisuals(
            subtitle = "Top Haryanvi bangers",
            icon = Icons.Rounded.MusicNote,
        )
        else -> GenreVisuals(
            subtitle = "Explore top tracks",
            icon = Icons.Rounded.MusicNote
        )
    }
}
