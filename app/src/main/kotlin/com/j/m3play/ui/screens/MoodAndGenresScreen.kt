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
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DriveEta
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
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
    
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Box(
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(baseColor)
            .clickable(onClick = onClick)
    ) {
        if (genreData.bgImageUrl != null) {
            AsyncImage(
                model = genreData.bgImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.65f)
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
            )
        }

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

// 🔥 THE ULTIMATE MEGA LIST (All 10 Screenshots Covered) 🔥
fun getGenreVisuals(title: String): GenreVisuals {
    val lowerTitle = title.lowercase()
    
    // Yaha par checking order zaroori hai. Specific categories pehle hain!
    return when {
        // --- 1. Regional Indian Languages ---
        lowerTitle.contains("hindi") -> GenreVisuals("Bollywood hits & more", textIcon = "अ", bgImageUrl = "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("tamil") -> GenreVisuals("South super hits", textIcon = "த", bgImageUrl = "https://images.unsplash.com/photo-1582510003544-4d00b7f7415e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("telugu") -> GenreVisuals("Tollywood chartbusters", textIcon = "తె", bgImageUrl = "https://images.unsplash.com/photo-1582510003544-4d00b7f7415e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("kannada") -> GenreVisuals("Sandalwood top tracks", textIcon = "ಕ", bgImageUrl = "https://images.unsplash.com/photo-1582510003544-4d00b7f7415e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("malayalam") -> GenreVisuals("Mollywood essentials", textIcon = "മ", bgImageUrl = "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("marathi") -> GenreVisuals("Marathi bangers", textIcon = "म", bgImageUrl = "https://images.unsplash.com/photo-1570168007204-dfb528c6958f?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("bhojpuri") -> GenreVisuals("Desi vibes", textIcon = "भ", bgImageUrl = "https://images.unsplash.com/photo-1598816434455-1f92e805f3da?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("gujarati") -> GenreVisuals("Garba & hits", textIcon = "ગુ", bgImageUrl = "https://images.unsplash.com/photo-1587132137056-bfbf0137329b?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("bengali") -> GenreVisuals("Soulful melodies", textIcon = "বা", bgImageUrl = "https://images.unsplash.com/photo-1558431382-27e303142255?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("punjabi") -> GenreVisuals("Bhangra beats & hits", textIcon = "ਪੰ", bgImageUrl = "https://images.unsplash.com/photo-1533682805518-48d1f5a8bb58?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("haryanvi") -> GenreVisuals("Top Haryanvi bangers", textIcon = "ह", bgImageUrl = "https://images.unsplash.com/photo-1586227740560-8cf2732c1531?q=80&w=800&auto=format&fit=crop")
        
        // --- 2. Indian Sub-Genres ---
        lowerTitle.contains("desi hip-hop") || lowerTitle.contains("desi hip hop") -> GenreVisuals("Beats that move you", icon = Icons.Rounded.Headphones, bgImageUrl = "https://images.unsplash.com/photo-1519750783826-e2420f4d687f?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("hindustani") || lowerTitle.contains("carnatic") || lowerTitle.contains("classical") -> GenreVisuals("Timeless masterpieces", icon = Icons.Rounded.LibraryMusic, bgImageUrl = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("indian pop") -> GenreVisuals("Today's top hits", icon = Icons.Rounded.Mic, bgImageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("indian indie") -> GenreVisuals("Fresh local sounds", icon = Icons.Rounded.GraphicEq, bgImageUrl = "https://images.unsplash.com/photo-1516280440502-869f4e41bf17?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("ghazal") || lowerTitle.contains("sufi") -> GenreVisuals("Soulful poetry", icon = Icons.Rounded.Nightlight, bgImageUrl = "https://images.unsplash.com/photo-1582643503204-747f48037b51?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("devotional") || lowerTitle.contains("bhakti") -> GenreVisuals("Divine melodies", icon = Icons.Rounded.Spa, bgImageUrl = "https://images.unsplash.com/photo-1582643503204-747f48037b51?q=80&w=800&auto=format&fit=crop")

        // --- 3. Decades ---
        lowerTitle.contains("1960") || lowerTitle.contains("60s") -> GenreVisuals("The classic 60s", textIcon = "60s", bgImageUrl = "https://images.unsplash.com/photo-1557766131-d88cb2cb705c?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("1970") || lowerTitle.contains("70s") -> GenreVisuals("Groovy times", textIcon = "70s", bgImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("1980") || lowerTitle.contains("80s") -> GenreVisuals("Retro vibes", textIcon = "80s", bgImageUrl = "https://images.unsplash.com/photo-1550684376-efcbd6e3f031?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("1990") || lowerTitle.contains("90s") -> GenreVisuals("The golden era", textIcon = "90s", bgImageUrl = "https://images.unsplash.com/photo-1621360841013-c76831f12282?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("2000") || lowerTitle.contains("00s") -> GenreVisuals("Y2K hits", textIcon = "00s", bgImageUrl = "https://images.unsplash.com/photo-1516280440502-869f4e41bf17?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("2010") || lowerTitle.contains("10s") -> GenreVisuals("The best of the 2010s", textIcon = "10s", bgImageUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=800&auto=format&fit=crop")

        // --- 4. Moods & Activities ---
        lowerTitle.contains("feel good") || lowerTitle.contains("happy") -> GenreVisuals("Boost your mood", icon = Icons.Rounded.SentimentSatisfiedAlt, bgImageUrl = "https://images.unsplash.com/photo-1490730141153-62ce8912d8a4?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("energize") || lowerTitle.contains("energy") -> GenreVisuals("Boost your energy", icon = Icons.Rounded.FlashOn, bgImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("chill") || lowerTitle.contains("relax") -> GenreVisuals("Unwind and drift away", icon = Icons.Rounded.Coffee, bgImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("sleep") -> GenreVisuals("Peaceful dreams", icon = Icons.Rounded.Nightlight, bgImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("sad") || lowerTitle.contains("heartbreak") -> GenreVisuals("In your feelings", icon = Icons.Rounded.SentimentDissatisfied, bgImageUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("focus") -> GenreVisuals("Stay in the zone", icon = Icons.Rounded.CenterFocusStrong, bgImageUrl = "https://images.unsplash.com/photo-1520112185208-a54d5b248eb3?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("monsoon") || lowerTitle.contains("rain") -> GenreVisuals("Cozy rainy days", icon = Icons.Rounded.Cloud, bgImageUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("romance") -> GenreVisuals("Feel the love", icon = Icons.Rounded.Favorite, bgImageUrl = "https://images.unsplash.com/photo-1518621736915-f346c4136e28?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("workout") -> GenreVisuals("Push your limits", icon = Icons.Rounded.DirectionsRun, bgImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("gaming") -> GenreVisuals("Level up your play", icon = Icons.Rounded.Computer, bgImageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("commute") -> GenreVisuals("On the move", icon = Icons.Rounded.DriveEta, bgImageUrl = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("party") -> GenreVisuals("Get the party started", icon = Icons.Rounded.Celebration, bgImageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("family") -> GenreVisuals("Songs for everyone", icon = Icons.Rounded.People, bgImageUrl = "https://images.unsplash.com/photo-1511895426328-dc8714191300?q=80&w=800&auto=format&fit=crop")

        // --- 5. Global Genres & Languages ---
        lowerTitle.contains("k-pop") || lowerTitle.contains("j-pop") -> GenreVisuals("Global pop phenomenon", icon = Icons.Rounded.Mic, bgImageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("latin") || lowerTitle.contains("español") -> GenreVisuals("Ritmo y pasión", icon = Icons.Rounded.Public, bgImageUrl = "https://images.unsplash.com/photo-1516280440502-869f4e41bf17?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("arabic") || lowerTitle.contains("iraqi") -> GenreVisuals("Middle Eastern vibes", icon = Icons.Rounded.Public, bgImageUrl = "https://images.unsplash.com/photo-1542384701-c0e46e0eda04?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("african") -> GenreVisuals("Afrobeats & more", icon = Icons.Rounded.Public, bgImageUrl = "https://images.unsplash.com/photo-1523805009056-3348518e28cc?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("reggae") || lowerTitle.contains("caribbean") -> GenreVisuals("Island vibes", icon = Icons.Rounded.WbSunny, bgImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop")
        
        // --- 6. Other Main Genres ---
        lowerTitle.contains("country") || lowerTitle.contains("americana") -> GenreVisuals("Roots & storytelling", icon = Icons.Rounded.Landscape, bgImageUrl = "https://images.unsplash.com/photo-1510486835266-96b65313936a?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("metal") -> GenreVisuals("Heavy hits", icon = Icons.Rounded.GraphicEq, bgImageUrl = "https://images.unsplash.com/photo-1598387993441-a364f854c3e1?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("rock") -> GenreVisuals("Classic & modern", icon = Icons.Rounded.GraphicEq, bgImageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("r&b") || lowerTitle.contains("soul") -> GenreVisuals("Smooth vibes", icon = Icons.Rounded.Mic, bgImageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("dance") || lowerTitle.contains("electronic") -> GenreVisuals("Drop the bass", icon = Icons.Rounded.Celebration, bgImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("jazz") -> GenreVisuals("Smooth & soulful", icon = Icons.Rounded.Album, bgImageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("indie") || lowerTitle.contains("alternative") -> GenreVisuals("Fresh alternative sounds", icon = Icons.Rounded.GraphicEq, bgImageUrl = "https://images.unsplash.com/photo-1516280440502-869f4e41bf17?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("folk") || lowerTitle.contains("acoustic") -> GenreVisuals("Unplugged", icon = Icons.Rounded.Nature, bgImageUrl = "https://images.unsplash.com/photo-1510486835266-96b65313936a?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("pop") -> GenreVisuals("Today's top hits", icon = Icons.Rounded.Mic, bgImageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=800&auto=format&fit=crop")
        lowerTitle.contains("hip-hop") -> GenreVisuals("Beats that move you", icon = Icons.Rounded.LibraryMusic, bgImageUrl = "https://images.unsplash.com/photo-1519750783826-e2420f4d687f?q=80&w=800&auto=format&fit=crop")

        // Default Fallback
        else -> GenreVisuals("Explore top tracks", icon = Icons.Rounded.MusicNote)
    }
}
