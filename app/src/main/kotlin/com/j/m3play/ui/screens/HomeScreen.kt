/*
 * M3Play Project (2026)
 * Jay Chaudhary (github.com/JAY01-CYBER)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.j.m3play.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.DisableBlurKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.ShowHomeCategoryChipsKey
import com.j.m3play.innertube.utils.parseCookieString
import com.j.m3play.ui.component.ChipsRow
import com.j.m3play.ui.component.LocalBottomSheetPageState
import com.j.m3play.ui.component.LocalMenuState
import com.j.m3play.ui.component.NavigationTitle
import com.j.m3play.ui.component.TimeGreetingCard
import com.j.m3play.ui.utils.SnapLayoutInfoProvider
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()

    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.38f),
                                color1.copy(alpha = 0.24f),
                                color1.copy(alpha = 0.14f),
                                color1.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.15f, height * 0.1f),
                            radius = width * 0.55f
                        )

                        val brush2 = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.34f),
                                color2.copy(alpha = 0.2f),
                                color2.copy(alpha = 0.11f),
                                color2.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.85f, height * 0.2f),
                            radius = width * 0.65f
                        )

                        val brush3 = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.3f),
                                color3.copy(alpha = 0.17f),
                                color3.copy(alpha = 0.09f),
                                color3.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.3f, height * 0.45f),
                            radius = width * 0.6f
                        )

                        val brush4 = Brush.radialGradient(
                            colors = listOf(
                                color4.copy(alpha = 0.26f),
                                color4.copy(alpha = 0.14f),
                                color4.copy(alpha = 0.08f),
                                color4.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.7f, height * 0.5f),
                            radius = width * 0.7f
                        )

                        val brush5 = Brush.radialGradient(
                            colors = listOf(
                                color5.copy(alpha = 0.22f),
                                color5.copy(alpha = 0.12f),
                                color5.copy(alpha = 0.06f),
                                color5.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.75f),
                            radius = width * 0.8f
                        )

                        val overlayBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.22f),
                                surfaceColor.copy(alpha = 0.55f),
                                surfaceColor
                            ),
                            startY = height * 0.4f,
                            endY = height
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = brush3)
                            drawRect(brush = brush4)
                            drawRect(brush = brush5)
                            drawRect(brush = overlayBrush)
                        }
                    }
            ) {}
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            val horizontalLazyGridItemWidthFactor =
                if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val forgottenFavoritesSnapLayoutInfoProvider =
                remember(forgottenFavoritesLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = forgottenFavoritesLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        }
                    )
                }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                if (showHomeCategoryChips) {
                    item {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = {
                                viewModel.toggleChip(it)
                            }
                        )
                    }
                }

                item {
                    TimeGreetingCard(
                        onSearchClick = {
                            runCatching { navController.navigate("search") }
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Liked",
                            icon = R.drawable.favorite,
                            onClick = {
                                runCatching { navController.navigate("auto_playlist/liked") }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionCard(
                            title = "Downloads",
                            icon = R.drawable.download,
                            onClick = {
                                runCatching { navController.navigate("auto_playlist/downloaded") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            title = "History",
                            icon = R.drawable.history,
                            onClick = {
                                runCatching { navController.navigate("history") }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionCard(
                            title = if (isLoggedIn) "Account" else "Library",
                            icon = if (isLoggedIn) R.drawable.person else R.drawable.library_music,
                            onClick = {
                                if (isLoggedIn) {
                                    runCatching { navController.navigate("account") }
                                } else {
                                    runCatching { navController.navigate("library") }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    /*
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.quick_picks),
                            modifier = Modifier.animateItem()
                        )
                    }
                    */

                    item {
                        QuickPicksSection(
                            quickPicks = picks,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic
                        )
                    }
                }

                speedDialSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.speed_dial),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        SpeedDialSection(
                            speedDialSongs = songs,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic
                        )
                    }
                }

                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        KeepListeningSection(
                            keepListening = items,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic,
                            scope = scope
                        )
                    }
                }

                AccountPlaylistsContainer(
                    viewModel = viewModel,
                    accountName = accountName,
                    accountImageUrl = url,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    navController = navController,
                    playerConnection = playerConnection,
                    menuState = menuState,
                    haptic = haptic,
                    scope = scope
                )

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.forgotten_favorites),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        ForgottenFavoritesSection(
                            forgottenFavorites = favorites,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            horizontalLazyGridItemWidth = horizontalLazyGridItemWidth,
                            lazyGridState = forgottenFavoritesLazyGridState,
                            snapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic
                        )
                    }
                }

                SimilarRecommendationsContainer(
                    viewModel = viewModel,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    navController = navController,
                    playerConnection = playerConnection,
                    menuState = menuState,
                    haptic = haptic,
                    scope = scope
                )

                homePage?.sections?.forEach { section ->
                    item {
                        HomePageSectionTitle(
                            section = section,
                            navController = navController,
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        HomePageSectionContent(
                            section = section,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic,
                            scope = scope
                        )
                    }
                }

                if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                    item {
                        HomeLoadingShimmer(modifier = Modifier.animateItem())
                    }
                }
            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "action_card_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "action_card_alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
