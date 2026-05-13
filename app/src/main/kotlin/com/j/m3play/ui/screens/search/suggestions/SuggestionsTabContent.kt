package com.j.m3play.ui.screens.search.suggestions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.j.m3play.R
import com.j.m3play.LocalPlayerConnection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsTabContent(
    navController: NavController,
    viewModel: SuggestionsViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val suggestionTracks by viewModel.suggestionTracks.collectAsState()
    val suggestionArtists by viewModel.suggestionArtists.collectAsState()
    val suggestionAlbums by viewModel.suggestionAlbums.collectAsState()
    val suggestionVideos by viewModel.suggestionVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val regionCode = "in" // Defaulting to India for Apple Music Top 100

    LaunchedEffect(regionCode) { viewModel.refresh(regionCode) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        if (isLoading && suggestionTracks == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        suggestionTracks?.let { tracks ->
            item {
                TrendingAppleMusicSection(
                    tracks = tracks,
                    onTrackClick = { track ->
                        android.widget.Toast.makeText(context, "Loading ${track.title}...", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.playTrack(track, playerConnection)
                    }
                )
            }
        }

        suggestionArtists?.let { artists ->
            item {
                TopArtistsSection(artists = artists, onArtistClick = { artist ->
                    android.widget.Toast.makeText(context, "Loading ${artist.name}...", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.navigateToArtist(artist, navController)
                })
            }
        }

        suggestionAlbums?.let { albums ->
            item {
                TrendingAlbumsSection(albums = albums, onAlbumClick = { album ->
                    android.widget.Toast.makeText(context, "Loading ${album.title}...", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.navigateToAlbum(album, navController)
                })
            }
        }

        if (suggestionTracks != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Data from Apple Music", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(text = "M3-Play", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingAppleMusicSection(tracks: List<SuggestionTrack>, onTrackClick: (SuggestionTrack) -> Unit) {
    if (tracks.isEmpty()) return
    val displayTracks = tracks.take(30)
    val pagerState = rememberPagerState(pageCount = { (displayTracks.size + 4) / 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Apple Music Top 100", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 32.dp))
        
        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().animateContentSize(tween(300, easing = FastOutSlowInEasing))) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val startIdx = page * 5
                val endIdx = minOf(startIdx + 5, displayTracks.size)
                for (i in startIdx until endIdx) {
                    val isTop = i == startIdx
                    val isBottom = i == endIdx - 1
                    val shape = when {
                        isTop && isBottom -> RoundedCornerShape(24.dp)
                        isTop -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isBottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    val track = displayTracks[i]
                    Row(modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer).clickable { onTrackClick(track) }) {
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)) {
                                Text("#${track.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (track.thumbnailUrl != null) {
                            AsyncImage(model = track.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.padding(16.dp).clip(MaterialTheme.shapes.large).size(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopArtistsSection(artists: List<SuggestionArtist>, onArtistClick: (SuggestionArtist) -> Unit) {
    if (artists.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Trending Artists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(artists) { artist ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).clickable { onArtistClick(artist) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(model = artist.thumbnailUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) { Text(artist.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(artist.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun TrendingAlbumsSection(albums: List<SuggestionAlbum>, onAlbumClick: (SuggestionAlbum) -> Unit) {
    if (albums.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Trending Albums", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
            items(albums) { album ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable { onAlbumClick(album) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(model = album.thumbnailUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) { Text(album.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(album.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
