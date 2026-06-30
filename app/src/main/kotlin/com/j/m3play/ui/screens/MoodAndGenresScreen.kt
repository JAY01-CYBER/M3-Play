package com.j.m3play.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
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
                        shape = RoundedCornerShape(20.dp),
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
    val isLightBackground = baseColor.luminance() > 0.4f
    val contentColor = if (isLightBackground) Color.Black else Color.White
    val genreData = getGenreVisuals(title)
    
    // Grayscale converter for exact color blending
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Box(
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(baseColor)
            .clickable(onClick = onClick)
    ) {
        // 1. Load Background Image
        if (genreData.bgImageUrl != null) {
            AsyncImage(
                model = genreData.bgImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.65f) // Keeps text readable by only taking right side
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix) // B&W magic
            )
        }

        // 2. Smooth Blending Gradient over the image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to baseColor, 
                        0.4f to baseColor, 
                        0.7f to baseColor.copy(alpha = 0.65f), 
                        1.0f to baseColor.copy(alpha = 0.15f) 
                    )
                )
        )

        // 3. Text and Icons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)),
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
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

val MoodAndGenresButtonHeight = 100.dp

data class GenreVisuals(
    val subtitle: String,
    val textIcon: String? = null,
    val icon: ImageVector? = null,
    val bgImageUrl: String? = null
)

fun getGenreVisuals(title: String): GenreVisuals {
    val lowerTitle = title.lowercase()
    return when {
        lowerTitle.contains("hindi") -> GenreVisuals(
            subtitle = "Bollywood hits & more",
            textIcon = "अ",
            bgImageUrl = "https://images.unsplash.com/photo-1548013146-72479768bada?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("2010") -> GenreVisuals(
            subtitle = "The best of the 2010s",
            textIcon = "2010s",
            bgImageUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=800&auto=format&fit=crop" // Cassette Tape
        )
        lowerTitle.contains("jazz") -> GenreVisuals(
            subtitle = "Smooth & soulful",
            icon = Icons.Rounded.Album,
            bgImageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?q=80&w=800&auto=format&fit=crop" // Saxophone
        )
        lowerTitle.contains("classical") -> GenreVisuals(
            subtitle = "Timeless masterpieces",
            icon = Icons.Rounded.LibraryMusic,
            bgImageUrl = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?q=80&w=800&auto=format&fit=crop" // Piano keys
        )
        lowerTitle.contains("workout") -> GenreVisuals(
            subtitle = "Push your limits",
            icon = Icons.Rounded.DirectionsRun,
            bgImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=800&auto=format&fit=crop" // Gym / Shoes
        )
        lowerTitle.contains("indie") -> GenreVisuals(
            subtitle = "Fresh local sounds",
            icon = Icons.Rounded.GraphicEq,
            bgImageUrl = "https://images.unsplash.com/photo-1516280440502-869f4e41bf17?q=80&w=800&auto=format&fit=crop" // Acoustic guitar vibes
        )
        lowerTitle.contains("family") -> GenreVisuals(
            subtitle = "Songs for everyone",
            icon = Icons.Rounded.People,
            bgImageUrl = "https://images.unsplash.com/photo-1511895426328-dc8714191300?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("romance") -> GenreVisuals(
            subtitle = "Feel the love",
            icon = Icons.Rounded.Favorite,
            bgImageUrl = "https://images.unsplash.com/photo-1518621736915-f346c4136e28?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("pop") -> GenreVisuals(
            subtitle = "Today's top hits",
            icon = Icons.Rounded.Mic,
            bgImageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("hip-hop") -> GenreVisuals(
            subtitle = "Beats that move you",
            icon = Icons.Rounded.LibraryMusic,
            bgImageUrl = "https://images.unsplash.com/photo-1519750783826-e2420f4d687f?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("party") -> GenreVisuals(
            subtitle = "Get the party started",
            icon = Icons.Rounded.Celebration,
            bgImageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("focus") -> GenreVisuals(
            subtitle = "Stay in the zone",
            icon = Icons.Rounded.CenterFocusStrong,
            bgImageUrl = "https://images.unsplash.com/photo-1520112185208-a54d5b248eb3?q=80&w=800&auto=format&fit=crop"
        )
        lowerTitle.contains("sleep") || lowerTitle.contains("relax") -> GenreVisuals(
            subtitle = "Unwind and drift away",
            icon = Icons.Rounded.Nightlight,
            bgImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop" // Beach/Sunset
        )
        else -> GenreVisuals(
            subtitle = "Explore top tracks",
            icon = Icons.Rounded.MusicNote
        )
    }
}
