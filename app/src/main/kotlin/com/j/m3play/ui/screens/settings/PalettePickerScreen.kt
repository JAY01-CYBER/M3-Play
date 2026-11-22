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

data class ThemePalette(
    val id: String,
    val nameResId: Int,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

object ThemePalettes {
    val Default = ThemePalette("default", R.string.palette_default, Color(0xFFED5564), Color(0xFFED5564), Color(0xFFED5564), Color(0xFFED5564))
    val OceanBlue = ThemePalette("ocean_blue", R.string.palette_ocean_blue, Color(0xFF4A90D9), Color(0xFF4A90D9), Color(0xFF4A90D9), Color(0xFF4A90D9))
    val ArcticBlue = ThemePalette("arctic_blue", R.string.palette_arctic_blue, Color(0xFF00BFFF), Color(0xFF00BFFF), Color(0xFF00BFFF), Color(0xFF00BFFF))
    val MidnightNavy = ThemePalette("midnight_navy", R.string.palette_midnight_navy, Color(0xFF2C3E50), Color(0xFF2C3E50), Color(0xFF2C3E50), Color(0xFF2C3E50))
    val SkyBlue = ThemePalette("sky_blue", R.string.palette_sky_blue, Color(0xFF87CEEB), Color(0xFF87CEEB), Color(0xFF87CEEB), Color(0xFF87CEEB))
    val CobaltBlue = ThemePalette("cobalt_blue", R.string.palette_cobalt_blue, Color(0xFF0047AB), Color(0xFF0047AB), Color(0xFF0047AB), Color(0xFF0047AB))
    val ElectricBlue = ThemePalette("electric_blue", R.string.palette_electric_blue, Color(0xFF7DF9FF), Color(0xFF7DF9FF), Color(0xFF7DF9FF), Color(0xFF7DF9FF))
    val EmeraldGreen = ThemePalette("emerald_green", R.string.palette_emerald_green, Color(0xFF2ECC71), Color(0xFF2ECC71), Color(0xFF2ECC71), Color(0xFF2ECC71))
    val TealWave = ThemePalette("teal_wave", R.string.palette_teal_wave, Color(0xFF1ABC9C), Color(0xFF1ABC9C), Color(0xFF1ABC9C), Color(0xFF1ABC9C))
    val ForestGreen = ThemePalette("forest_green", R.string.palette_forest_green, Color(0xFF228B22), Color(0xFF228B22), Color(0xFF228B22), Color(0xFF228B22))
    val SpotifyGreen = ThemePalette("spotify_green", R.string.palette_spotify_green, Color(0xFF1DB954), Color(0xFF1DB954), Color(0xFF1DB954), Color(0xFF1DB954))
    val MintFresh = ThemePalette("mint_fresh", R.string.palette_mint_fresh, Color(0xFF98FF98), Color(0xFF98FF98), Color(0xFF98FF98), Color(0xFF98FF98))
    val OliveGarden = ThemePalette("olive_garden", R.string.palette_olive_garden, Color(0xFF808000), Color(0xFF808000), Color(0xFF808000), Color(0xFF808000))
    val SageGreen = ThemePalette("sage_green", R.string.palette_sage_green, Color(0xFF9CAF88), Color(0xFF9CAF88), Color(0xFF9CAF88), Color(0xFF9CAF88))
    val SunsetOrange = ThemePalette("sunset_orange", R.string.palette_sunset_orange, Color(0xFFE67E22), Color(0xFFE67E22), Color(0xFFE67E22), Color(0xFFE67E22))
    val GoldenHour = ThemePalette("golden_hour", R.string.palette_golden_hour, Color(0xFFF39C12), Color(0xFFF39C12), Color(0xFFF39C12), Color(0xFFF39C12))
    val WarmAmber = ThemePalette("warm_amber", R.string.palette_warm_amber, Color(0xFFFFBF00), Color(0xFFFFBF00), Color(0xFFFFBF00), Color(0xFFFFBF00))
    val TangerineBlast = ThemePalette("tangerine_blast", R.string.palette_tangerine_blast, Color(0xFFFF9800), Color(0xFFFF9800), Color(0xFFFF9800), Color(0xFFFF9800))
    val Peach = ThemePalette("peach", R.string.palette_peach, Color(0xFFFFDAB9), Color(0xFFFFDAB9), Color(0xFFFFDAB9), Color(0xFFFFDAB9))
    val Mango = ThemePalette("mango", R.string.palette_mango, Color(0xFFFF8243), Color(0xFFFF8243), Color(0xFFFF8243), Color(0xFFFF8243))
    val RoyalPurple = ThemePalette("royal_purple", R.string.palette_royal_purple, Color(0xFF9B59B6), Color(0xFF9B59B6), Color(0xFF9B59B6), Color(0xFF9B59B6))
    val LavenderDream = ThemePalette("lavender_dream", R.string.palette_lavender_dream, Color(0xFFB39DDB), Color(0xFFB39DDB), Color(0xFFB39DDB), Color(0xFFB39DDB))
    val GrapePurple = ThemePalette("grape_purple", R.string.palette_grape_purple, Color(0xFF6B5B95), Color(0xFF6B5B95), Color(0xFF6B5B95), Color(0xFF6B5B95))
    val Violet = ThemePalette("violet", R.string.palette_violet, Color(0xFFEE82EE), Color(0xFFEE82EE), Color(0xFFEE82EE), Color(0xFFEE82EE))
    val Amethyst = ThemePalette("amethyst", R.string.palette_amethyst, Color(0xFF9966CC), Color(0xFF9966CC), Color(0xFF9966CC), Color(0xFF9966CC))
    val UltraViolet = ThemePalette("ultra_violet", R.string.palette_ultra_violet, Color(0xFF645394), Color(0xFF645394), Color(0xFF645394), Color(0xFF645394))
    val CherryBlossom = ThemePalette("cherry_blossom", R.string.palette_cherry_blossom, Color(0xFFFFB7C5), Color(0xFFFFB7C5), Color(0xFFFFB7C5), Color(0xFFFFB7C5))
    val RoseQuartz = ThemePalette("rose_quartz", R.string.palette_rose_quartz, Color(0xFFF7CAC9), Color(0xFFF7CAC9), Color(0xFFF7CAC9), Color(0xFFF7CAC9))
    val MagentaPop = ThemePalette("magenta_pop", R.string.palette_magenta_pop, Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF))
    val HotPink = ThemePalette("hot_pink", R.string.palette_hot_pink, Color(0xFFFF69B4), Color(0xFFFF69B4), Color(0xFFFF69B4), Color(0xFFFF69B4))
    val Blush = ThemePalette("blush", R.string.palette_blush, Color(0xFFDE5D83), Color(0xFFDE5D83), Color(0xFFDE5D83), Color(0xFFDE5D83))
    val Coral = ThemePalette("coral", R.string.palette_coral, Color(0xFFFF7F50), Color(0xFFFF7F50), Color(0xFFFF7F50), Color(0xFFFF7F50))
    val Bubblegum = ThemePalette("bubblegum", R.string.palette_bubblegum, Color(0xFFFFC1CC), Color(0xFFFFC1CC), Color(0xFFFFC1CC), Color(0xFFFFC1CC))
    val CrimsonRed = ThemePalette("crimson_red", R.string.palette_crimson_red, Color(0xFFDC143C), Color(0xFFDC143C), Color(0xFFDC143C), Color(0xFFDC143C))
    val YouTubeRed = ThemePalette("youtube_red", R.string.palette_youtube_red, Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000))
    val WineRed = ThemePalette("wine_red", R.string.palette_wine_red, Color(0xFF722F37), Color(0xFF722F37), Color(0xFF722F37), Color(0xFF722F37))
    val RubyRed = ThemePalette("ruby_red", R.string.palette_ruby_red, Color(0xFFE0115F), Color(0xFFE0115F), Color(0xFFE0115F), Color(0xFFE0115F))
    val Scarlet = ThemePalette("scarlet", R.string.palette_scarlet, Color(0xFFFF2400), Color(0xFFFF2400), Color(0xFFFF2400), Color(0xFFFF2400))
    val Charcoal = ThemePalette("charcoal", R.string.palette_charcoal, Color(0xFF36454F), Color(0xFF36454F), Color(0xFF36454F), Color(0xFF36454F))
    val Silver = ThemePalette("silver", R.string.palette_silver, Color(0xFFC0C0C0), Color(0xFFC0C0C0), Color(0xFFC0C0C0), Color(0xFFC0C0C0))
    val Slate = ThemePalette("slate", R.string.palette_slate, Color(0xFF708090), Color(0xFF708090), Color(0xFF708090), Color(0xFF708090))
    val Graphite = ThemePalette("graphite", R.string.palette_graphite, Color(0xFF474747), Color(0xFF474747), Color(0xFF474747), Color(0xFF474747))
    val Terracotta = ThemePalette("terracotta", R.string.palette_terracotta, Color(0xFFE2725B), Color(0xFFE2725B), Color(0xFFE2725B), Color(0xFFE2725B))
    val Coffee = ThemePalette("coffee", R.string.palette_coffee, Color(0xFF6F4E37), Color(0xFF6F4E37), Color(0xFF6F4E37), Color(0xFF6F4E37))
    val Mocha = ThemePalette("mocha", R.string.palette_mocha, Color(0xFF967969), Color(0xFF967969), Color(0xFF967969), Color(0xFF967969))
    val Sand = ThemePalette("sand", R.string.palette_sand, Color(0xFFC2B280), Color(0xFFC2B280), Color(0xFFC2B280), Color(0xFFC2B280))
    val Clay = ThemePalette("clay", R.string.palette_clay, Color(0xFFB66A50), Color(0xFFB66A50), Color(0xFFB66A50), Color(0xFFB66A50))
    val PastelPink = ThemePalette("pastel_pink", R.string.palette_pastel_pink, Color(0xFFFFD1DC), Color(0xFFFFD1DC), Color(0xFFFFD1DC), Color(0xFFFFD1DC))
    val PastelBlue = ThemePalette("pastel_blue", R.string.palette_pastel_blue, Color(0xFFAEC6CF), Color(0xFFAEC6CF), Color(0xFFAEC6CF), Color(0xFFAEC6CF))
    val PastelGreen = ThemePalette("pastel_green", R.string.palette_pastel_green, Color(0xFF77DD77), Color(0xFF77DD77), Color(0xFF77DD77), Color(0xFF77DD77))
    val PastelYellow = ThemePalette("pastel_yellow", R.string.palette_pastel_yellow, Color(0xFFFDFD96), Color(0xFFFDFD96), Color(0xFFFDFD96), Color(0xFFFDFD96))
    val PastelPurple = ThemePalette("pastel_purple", R.string.palette_pastel_purple, Color(0xFFB19CD9), Color(0xFFB19CD9), Color(0xFFB19CD9), Color(0xFFB19CD9))
    val NeonGreen = ThemePalette("neon_green", R.string.palette_neon_green, Color(0xFF39FF14), Color(0xFF39FF14), Color(0xFF39FF14), Color(0xFF39FF14))
    val NeonPink = ThemePalette("neon_pink", R.string.palette_neon_pink, Color(0xFFFF10F0), Color(0xFFFF10F0), Color(0xFFFF10F0), Color(0xFFFF10F0))
    val NeonBlue = ThemePalette("neon_blue", R.string.palette_neon_blue, Color(0xFF00F5FF), Color(0xFF00F5FF), Color(0xFF00F5FF), Color(0xFF00F5FF))
    val NeonOrange = ThemePalette("neon_orange", R.string.palette_neon_orange, Color(0xFFFF5F1F), Color(0xFFFF5F1F), Color(0xFFFF5F1F), Color(0xFFFF5F1F))
    val Cyberpunk = ThemePalette("cyberpunk", R.string.palette_cyberpunk, Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF))
    val Synthwave = ThemePalette("synthwave", R.string.palette_synthwave, Color(0xFFFF6EC7), Color(0xFFFF6EC7), Color(0xFFFF6EC7), Color(0xFFFF6EC7))
    val Ocean = ThemePalette("ocean", R.string.palette_ocean, Color(0xFF006994), Color(0xFF006994), Color(0xFF006994), Color(0xFF006994))
    val Forest = ThemePalette("forest", R.string.palette_forest, Color(0xFF0B3D0B), Color(0xFF0B3D0B), Color(0xFF0B3D0B), Color(0xFF0B3D0B))
    val Autumn = ThemePalette("autumn", R.string.palette_autumn, Color(0xFFD2691E), Color(0xFFD2691E), Color(0xFFD2691E), Color(0xFFD2691E))
    val Winter = ThemePalette("winter", R.string.palette_winter, Color(0xFFADD8E6), Color(0xFFADD8E6), Color(0xFFADD8E6), Color(0xFFADD8E6))
    val Spring = ThemePalette("spring", R.string.palette_spring, Color(0xFF98FB98), Color(0xFF98FB98), Color(0xFF98FB98), Color(0xFF98FB98))
    val Summer = ThemePalette("summer", R.string.palette_summer, Color(0xFFFFD700), Color(0xFFFFD700), Color(0xFFFFD700), Color(0xFFFFD700))
    val Twilight = ThemePalette("twilight", R.string.palette_twilight, Color(0xFF4B0082), Color(0xFF4B0082), Color(0xFF4B0082), Color(0xFF4B0082))
    val Aurora = ThemePalette("aurora", R.string.palette_aurora, Color(0xFF00FF7F), Color(0xFF00FF7F), Color(0xFF00FF7F), Color(0xFF00FF7F))
    val Candy = ThemePalette("candy", R.string.palette_candy, Color(0xFFFF69B4), Color(0xFFFF69B4), Color(0xFFFF69B4), Color(0xFFFF69B4))
    val Rainbow = ThemePalette("rainbow", R.string.palette_rainbow, Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000))

    val allPalettes: List<ThemePalette> = listOf(
        Default, OceanBlue, ArcticBlue, MidnightNavy, SkyBlue, CobaltBlue, ElectricBlue, EmeraldGreen,
        TealWave, ForestGreen, SpotifyGreen, MintFresh, OliveGarden, SageGreen, SunsetOrange, GoldenHour,
        WarmAmber, TangerineBlast, Peach, Mango, RoyalPurple, LavenderDream, GrapePurple, Violet, Amethyst,
        UltraViolet, CherryBlossom, RoseQuartz, MagentaPop, HotPink, Blush, Coral, Bubblegum, CrimsonRed,
        YouTubeRed, WineRed, RubyRed, Scarlet, Charcoal, Silver, Slate, Graphite, Terracotta, Coffee, Mocha,
        Sand, Clay, PastelPink, PastelBlue, PastelGreen, PastelYellow, PastelPurple, NeonGreen, NeonPink,
        NeonBlue, NeonOrange, Cyberpunk, Synthwave, Ocean, Forest, Autumn, Winter, Spring, Summer, Twilight,
        Aurora, Candy, Rainbow
    )

    fun findByPrimaryColor(colorHex: String): ThemePalette? = allPalettes.find { it.primary.toHexString() == colorHex }
    fun findById(id: String): ThemePalette? = allPalettes.find { it.id == id }
    
    fun getRandomPalette(): ThemePalette {
        return allPalettes.random()
    }
    
    fun generateRandomPalette(): ThemePalette {
        val random = java.util.Random()
        
        val primaryHue = random.nextFloat() * 360f
        val primarySaturation = 0.5f + random.nextFloat() * 0.4f
        val primaryLightness = 0.4f + random.nextFloat() * 0.25f
        
        val primary = hctToColor(primaryHue, primarySaturation, primaryLightness)
        
        val secondaryHue = (primaryHue + 30f + random.nextFloat() * 60f) % 360f
        val secondary = hctToColor(secondaryHue, primarySaturation * 0.9f, primaryLightness * 1.1f)
        
        val tertiaryHue = (primaryHue - 30f - random.nextFloat() * 60f + 360f) % 360f
        val tertiary = hctToColor(tertiaryHue, primarySaturation * 0.8f, primaryLightness * 0.95f)
        
        val neutralHue = (primaryHue + random.nextFloat() * 20f - 10f) % 360f
        val neutral = hctToColor(neutralHue, 0.1f, primaryLightness * 0.8f)
        
        return ThemePalette(
            id = "random_" + System.currentTimeMillis(),
            nameResId = R.string.palette_custom,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            neutral = neutral
        )
    }

    private fun hctToColor(hue: Float, saturation: Float, lightness: Float): Color {
        val hsv = floatArrayOf(hue, saturation, lightness)
        val argb = android.graphics.Color.HSVToColor(hsv)
        return Color(argb)
    }
}

private fun Color.toHexString(): String = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

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
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
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
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

private fun ThemeSeedPalette.toThemePalette(): ThemePalette = ThemePalette("custom_seed", R.string.palette_custom, primary, secondary, tertiary, neutral)

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
                        Icon(painter = painterResource(R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(28.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf(animatedPrimary to 48.dp, animatedSecondary to 36.dp, animatedTertiary to 28.dp).forEachIndexed { index, (color, s) ->
                        Box(modifier = Modifier.offset(x = (-12 * index).dp).size(s).shadow(4.dp, CircleShape).clip(CircleShape).background(color).border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape))
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
