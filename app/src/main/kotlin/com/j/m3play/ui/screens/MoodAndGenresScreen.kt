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
        columns = GridCells.Adaptive(minSize = 320.dp), // Single column like screenshot
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
                // Same to same sub-heading
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
    val genreData = getGenreVisuals(title)

    Box(
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(baseColor)
            .clickable(onClick = onClick)
    ) {
        // 1. Background Image on the Right (If Available)
        if (genreData.bgImageRes != null) {
            Image(
                painter = painterResource(id = genreData.bgImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f) // Take up 60% of right side
                    .align(Alignment.CenterEnd)
            )
        }

        // 2. Gradient Overlay for Smooth Blending (Fades image left edge into solid color)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to baseColor,
                        0.4f to baseColor.copy(alpha = 0.9f), // Strong color on text side
                        0.7f to baseColor.copy(alpha = 0.3f), // Fading smoothly
                        1.0f to Color.Transparent // Fully transparent on the right edge
                    )
                )
        )

        // 3. Foreground Content (Icons & Texts)
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
                    .background(Color.White.copy(alpha = 0.2f)), // Translucent white circle
                contentAlignment = Alignment.Center
            ) {
                if (genreData.icon != null) {
                    Icon(
                        imageVector = genreData.icon,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = genreData.textIcon ?: title.take(1),
                        color = Color.Black.copy(alpha = 0.7f),
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
                    color = Color.Black.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = genreData.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f),
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
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

val MoodAndGenresButtonHeight = 110.dp

// Helper class & function to map design exact details
data class GenreVisuals(
    val subtitle: String,
    val textIcon: String? = null,
    val icon: ImageVector? = null,
    val bgImageRes: Int? = null
)

fun getGenreVisuals(title: String): GenreVisuals {
    return when (title.lowercase()) {
        "hindi" -> GenreVisuals(
            subtitle = "Bollywood hits & more",
            textIcon = "अ",
            // TODO: Niche diya gaya code tabhi chalega jab tum ek image res/drawable me add karoge
            // bgImageRes = R.drawable.taj_mahal_bg
        )
        "2010s" -> GenreVisuals(
            subtitle = "The best of the 2010s",
            textIcon = "2010s",
            // bgImageRes = R.drawable.cassette_bg
        )
        "family" -> GenreVisuals(
            subtitle = "Songs for the whole family",
            icon = Icons.Rounded.People,
            // bgImageRes = R.drawable.family_bg
        )
        "romance" -> GenreVisuals(
            subtitle = "Feel the love",
            icon = Icons.Rounded.Favorite,
            // bgImageRes = R.drawable.roses_bg
        )
        "pop" -> GenreVisuals(
            subtitle = "Today's top hits",
            icon = Icons.Rounded.Mic,
            // bgImageRes = R.drawable.concert_bg
        )
        "hip-hop" -> GenreVisuals(
            subtitle = "Beats that move you",
            icon = Icons.Rounded.LibraryMusic,
            // bgImageRes = R.drawable.graffiti_bg
        )
        else -> GenreVisuals(
            subtitle = "Explore top tracks",
            icon = Icons.Rounded.MusicNote
        )
    }
}
