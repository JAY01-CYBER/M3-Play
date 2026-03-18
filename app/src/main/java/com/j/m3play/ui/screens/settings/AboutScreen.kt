package com.j.m3play.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.j.m3play.BuildConfig
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.utils.backToMain

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerEffect")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerEffect"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun UserCard(
    imageUrl: String,
    name: String,
    role: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    val uriHandler = LocalUriHandler.current
    val shimmerBrush = shimmerEffect()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {

            Image(
                painter = painterResource(R.drawable.opentune_monochrome),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shimmerBrush)
            )
        }

        Text(
            text = "M3 Play",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Dev By Jay 亗",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp)
        ) {

            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                IconButton(
                    onClick = { uriHandler.openUri("https://www.facebook.com/") },
                    onLongClick = {}
                ) {
                    Icon(
                        painterResource(R.drawable.facebook),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { uriHandler.openUri("https://www.instagram.com/") },
                    onLongClick = {}
                ) {
                    Icon(
                        painterResource(R.drawable.instagram),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { uriHandler.openUri("https://github.com/JAY01-CYBER/M3-Play") },
                    onLongClick = {}
                ) {
                    Icon(
                        painterResource(R.drawable.github),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                /* 🔴 DONATE BUTTON (UPI) */

                IconButton(
                    onClick = {
                        uriHandler.openUri("upi://pay?pa=6354471314@upi&pn=Jay&cu=INR")
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painterResource(R.drawable.paypal),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }

            }
        }

        Spacer(Modifier.height(20.dp))

        UserCards(uriHandler)

        Spacer(Modifier.height(20.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), null)
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun UserCards(uriHandler: UriHandler) {
    Column {

        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/82433052?v=4",
            name = "亗 Jay",
            role = "Developer",
            onClick = {
                uriHandler.openUri("https://github.com/JAY01-CYBER")
            }
        )
    }
}
