package com.j.m3play.ui.screens.settings

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.j.m3play.R
import com.j.m3play.constants.PlayerCustomBlurKey
import com.j.m3play.constants.PlayerCustomBrightnessKey
import com.j.m3play.constants.PlayerCustomContrastKey
import com.j.m3play.constants.PlayerCustomImageUriKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.M3PlaySlider
import com.j.m3play.ui.theme.appBlur
import com.j.m3play.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBackground(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
) {
    val context = LocalContext.current
    val (imageUri, onImageUriChange) = rememberPreference(PlayerCustomImageUriKey, "")
    val (blur, onBlurChange) = rememberPreference(PlayerCustomBlurKey, 0f)
    val (contrast, onContrastChange) = rememberPreference(PlayerCustomContrastKey, 1f)
    val (brightness, onBrightnessChange) = rememberPreference(PlayerCustomBrightnessKey, 1f)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onImageUriChange(uri.toString())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customize_background_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val heightScale = 1.4f
            val playerPreviewHeight = (screenHeightDp * (1518f / 2400f) * heightScale).dp
            val lyricsPreviewHeight = (screenHeightDp * (1386f / 2400f) * heightScale).dp

            // --- PREVIEW CARDS (Kept exactly as your logic, just ensured 32.dp rounding) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerPreviewHeight)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri.isNotBlank()) {
                    val t = (1f - contrast) * 128f + (brightness - 1f) * 255f
                    val cm = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, t,
                            0f, contrast, 0f, 0f, t,
                            0f, 0f, contrast, 0f, t,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                    AsyncImage(
                        model = Uri.parse(imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().appBlur(blur.dp),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(cm)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                    Image(painter = painterResource(R.drawable.player_preview), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.image), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.add_image), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            FilledTonalButton(
                onClick = { launcher.launch(arrayOf("image/*")) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp) // Ultra rounded button
            ) {
                Icon(painterResource(R.drawable.image), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_image), fontWeight = FontWeight.SemiBold)
            }

            // --- CONTROLS IN A CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(stringResource(R.string.blur), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        M3PlaySlider(value = blur, onValueChange = onBlurChange, valueRange = 0f..50f, modifier = Modifier.fillMaxWidth())
                    }
                    Column {
                        Text(stringResource(R.string.contrast), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        M3PlaySlider(value = contrast, onValueChange = onContrastChange, valueRange = 0.5f..2f, modifier = Modifier.fillMaxWidth())
                    }
                    Column {
                        Text(stringResource(R.string.brightness), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        M3PlaySlider(value = brightness, onValueChange = onBrightnessChange, valueRange = 0.5f..2f, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Button(
                onClick = {
                    Toast.makeText(context, context.getString(R.string.save), Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
