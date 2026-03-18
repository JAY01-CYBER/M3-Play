package com.j.m3play.ui.component

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.AsyncImage
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object AppConfig {

    private val THUMBNAIL_CORNER_RADIUS_KEY =
        floatPreferencesKey("thumbnail_corner_radius")

    suspend fun saveThumbnailCornerRadius(context: Context, radius: Float) {
        context.dataStore.edit {
            it[THUMBNAIL_CORNER_RADIUS_KEY] = radius
        }
    }

    suspend fun getThumbnailCornerRadius(
        context: Context,
        defaultValue: Float = 16f
    ): Float {
        return context.dataStore.data
            .map { it[THUMBNAIL_CORNER_RADIUS_KEY] ?: defaultValue }
            .first()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThumbnailCornerRadiusSelectorButton(
    modifier: Modifier = Modifier,
    onRadiusSelected: (Float) -> Unit
) {

    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var currentRadius by remember { mutableStateOf(24f) }

    LaunchedEffect(Unit) {
        currentRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showBottomSheet = true }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.line_curve),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    id = R.string.customize_thumbnail_corner_radius,
                    currentRadius.roundToInt()
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showBottomSheet) {
        ThumbnailCornerRadiusBottomSheet(
            initialRadius = currentRadius,
            onDismiss = { showBottomSheet = false },
            onRadiusSelected = {
                currentRadius = it
                onRadiusSelected(it)
                showBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThumbnailCornerRadiusBottomSheet(
    initialRadius: Float,
    onDismiss: () -> Unit,
    onRadiusSelected: (Float) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var radius by remember { mutableStateOf(initialRadius) }
    val presetValues = listOf(0f, 8f, 16f, 24f, 32f, 40f)

    var customValue by remember { mutableStateOf(initialRadius.roundToInt().toString()) }
    var customSelected by remember { mutableStateOf(radius !in presetValues) }

    val animatedRadius by animateFloatAsState(
        targetValue = radius,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "radius_anim"
    )

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(
                    id = R.string.customize_thumbnail_corner_radius,
                    radius.roundToInt()
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(animatedRadius.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(animatedRadius.dp)
                    )
            ) {

                if (mediaMetadata != null) {

                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Image(
                        painter = painterResource(R.drawable.previewalbum),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${radius.roundToInt()}dp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            ChipsGrid(
                values = presetValues,
                selectedValue = if (customSelected) null else radius,
                onValueSelected = {
                    radius = it
                    customSelected = false
                    customValue = it.roundToInt().toString()
                }
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = customValue,
                onValueChange = {

                    if (it.all(Char::isDigit) || it.isEmpty()) {

                        customValue = it

                        it.toIntOrNull()?.let { number ->

                            val limited = number.coerceIn(0, 45)
                            radius = limited.toFloat()
                            customSelected = true
                        }
                    }
                },
                enabled = customSelected,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.custom_value)) },
                trailingIcon = { Text("dp") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Slider(
                value = radius,
                onValueChange = {

                    radius = it
                    customSelected = radius !in presetValues
                    customValue = radius.roundToInt().toString()
                },
                valueRange = 0f..45f
            )

            Text(
                text = stringResource(
                    id = R.string.corner_radius_label,
                    radius.roundToInt().toString()
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(28.dp))

            HorizontalDivider()

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {

                        scope.launch {

                            AppConfig.saveThumbnailCornerRadius(
                                context,
                                radius
                            )

                            onRadiusSelected(radius)
                        }
                    }
                ) {

                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

@Composable
private fun ChipsGrid(
    values: List<Float>,
    selectedValue: Float?,
    onValueSelected: (Float) -> Unit
) {

    val rows = values.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        rows.forEach { row ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                row.forEach { value ->

                    FilterChip(
                        selected = selectedValue == value,
                        onClick = { onValueSelected(value) },
                        label = {
                            Text("${value.roundToInt()}")
                        }
                    )
                }
            }
        }
    }
}