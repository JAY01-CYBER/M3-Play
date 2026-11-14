package com.j.m3play.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.CustomThemeColorKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.theme.ThemeSeedPalette
import com.j.m3play.ui.theme.ThemeSeedPaletteCodec
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(
        CustomThemeColorKey,
        defaultValue = ThemePalettes.Default.id
    )
    
    val selectedPalette = remember(customThemeColor) {
        ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)?.toThemePalette()
            ?: ThemePalettes.findById(customThemeColor)
            ?: ThemePalettes.findByPrimaryColor(customThemeColor)
            ?: ThemePalettes.Default
    }
    
    val isDarkTheme = isSystemInDarkTheme()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty() }.getOrNull().orEmpty() }
            val imported = ThemeSeedPaletteCodec.decodeFromJson(text)
            if (imported != null) {
                val name = ThemeSeedPaletteCodec.extractNameFromJsonOrNull(text)
                onCustomThemeColorChange(ThemeSeedPaletteCodec.encodeForPreference(imported, name))
                Toast.makeText(context, context.getString(R.string.theme_import_success), Toast.LENGTH_SHORT).show()
                navController.navigate("settings/appearance/theme_creator")
            } else {
                Toast.makeText(context, context.getString(R.string.theme_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.color_palette), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
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
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.custom_theme)) },
                    icon = { Icon(painterResource(R.drawable.palette), contentDescription = null) },
                    onClick = { navController.navigate("settings/appearance/theme_creator") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.import_theme)) },
                    icon = { Icon(painterResource(R.drawable.restore), contentDescription = null) },
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ThemePreviewCard(palette = selectedPalette, isDarkTheme = isDarkTheme, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(24.dp))
            ColorPaletteSelector(
                palettes = ThemePalettes.allPalettes,
                selectedPalette = selectedPalette,
                onPaletteSelected = { palette -> onCustomThemeColorChange(palette.id) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

private fun ThemeSeedPalette.toThemePalette(): ThemePalette = ThemePalette("custom_seed", R.string.palette_custom, primary, secondary, tertiary, neutral)
private fun Color.toHexString(): String = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

@Composable
private fun ThemePreviewCard(palette: ThemePalette, isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    val animatedPrimary by animateColorAsState(targetValue = palette.primary, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = "primaryColor")
    val animatedSecondary by animateColorAsState(targetValue = palette.secondary, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = "secondaryColor")
    val animatedTertiary by animateColorAsState(targetValue = palette.tertiary, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = "tertiaryColor")
    val animatedNeutral by animateColorAsState(targetValue = palette.neutral, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = "neutralColor")
    
    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else animatedTertiary.copy(alpha = 0.3f)
    
    Card(
        modifier = modifier.fillMaxWidth().height(280.dp).shadow(16.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(colors = listOf(animatedPrimary.copy(alpha = 0.3f), Color.Transparent), center = Offset(size.width * 0.7f, size.height * 0.3f), radius = size.width * 0.8f)
                drawRect(brush = gradientBrush)
            }
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Card(modifier = Modifier.width(140.dp).height(100.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = animatedPrimary.copy(alpha = 0.15f))) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(colors = listOf(animatedPrimary, animatedSecondary))))
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(animatedNeutral.copy(alpha = 0.3f))) {
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(animatedPrimary))
                            }
                        }
                    }
                    Box(modifier = Modifier.size(56.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(animatedPrimary), contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(R.drawable.play), contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(28.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf(animatedPrimary to 48.dp, animatedSecondary to 36.dp, animatedTertiary to 28.dp).forEachIndexed { index, (color, size) ->
                        Box(modifier = Modifier.offset(x = (-12 * index).dp).size(size).shadow(4.dp, CircleShape).clip(CircleShape).background(color).border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(animatedPrimary, animatedSecondary, animatedNeutral).forEach { color ->
                        Box(modifier = Modifier.height(32.dp).width(72.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.2f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }
            Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = RoundedCornerShape(12.dp), color = animatedPrimary, shadowElevation = 4.dp) {
                Text(text = stringResource(palette.nameResId), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.onPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ColorPaletteSelector(palettes: List<ThemePalette>, selectedPalette: ThemePalette, onPaletteSelected: (ThemePalette) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val selectedIndex = palettes.indexOf(selectedPalette)
    val totalDots = (palettes.size + 3) / 4
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex / 4 } }
    var stableCurrentPage by rememberSaveable { mutableIntStateOf(0) }
    
    LaunchedEffect(currentPage) { kotlinx.coroutines.delay(50); stableCurrentPage = currentPage }
    LaunchedEffect(selectedIndex) { if (selectedIndex >= 0) listState.animateScrollToItem(index = selectedIndex, scrollOffset = -100) }
    
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(state = listState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(palettes) { palette ->
                PaletteCard(palette = palette, isSelected = palette.id == selectedPalette.id, onClick = { onPaletteSelected(palette) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CarouselDotsIndicator(totalDots = totalDots, currentPage = stableCurrentPage, selectedColor = selectedPalette.primary, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun CarouselDotsIndicator(totalDots: Int, currentPage: Int, selectedColor: Color, modifier: Modifier = Modifier) {
    val fixedDotContainerSize = 10.dp
    Row(modifier = modifier.height(fixedDotContainerSize), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(totalDots) { index ->
            val isSelected = index == currentPage
            val dotSize by animateDpAsState(targetValue = if (isSelected) 8.dp else 4.dp, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing), label = "dotSize")
            val dotColor by animateColorAsState(targetValue = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), animationSpec = tween(durationMillis = 200), label = "dotColor")
            Box(modifier = Modifier.padding(horizontal = 2.dp).size(fixedDotContainerSize), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            }
        }
    }
}

@Composable
private fun PaletteCard(palette: ThemePalette, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scaleAnimation")
    val borderWidth by animateDpAsState(targetValue = if (isSelected) 2.dp else 0.dp, animationSpec = tween(durationMillis = 200), label = "borderAnimation")
    val animatedBorderColor by animateColorAsState(targetValue = if (isSelected) palette.primary else Color.Transparent, animationSpec = tween(durationMillis = 300), label = "borderColorAnimation")
    
    Card(
        modifier = Modifier.size(80.dp).scale(scale).border(borderWidth, animatedBorderColor, RoundedCornerShape(28.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(56.dp)) {
                drawArc(color = palette.primary, startAngle = -90f, sweepAngle = 180f, useCenter = true, topLeft = Offset.Zero, size = size)
                drawArc(color = palette.primary.copy(alpha = 0.4f), startAngle = 90f, sweepAngle = 90f, useCenter = true, topLeft = Offset.Zero, size = size)
                drawArc(color = palette.primary.copy(alpha = 0.2f), startAngle = 180f, sweepAngle = 90f, useCenter = true, topLeft = Offset.Zero, size = size)
            }
            if (isSelected) {
                Box(modifier = Modifier.size(24.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.check), contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ColorPalettePicker(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showPreview) {
            ThemePreviewCard(palette = selectedPalette, isDarkTheme = isSystemInDarkTheme(), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(24.dp))
        }
        ColorPaletteSelector(palettes = palettes, selectedPalette = selectedPalette, onPaletteSelected = onPaletteSelected)
    }
}
