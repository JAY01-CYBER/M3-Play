/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Style: ANDROID 17 (Ultra-Rounded, M3)     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.PoTokenGvsKey
import com.j.m3play.constants.PoTokenPlayerKey
import com.j.m3play.constants.PoTokenSourceUrlKey
import com.j.m3play.constants.UseVisitorDataKey
import com.j.m3play.constants.VisitorDataKey
import com.j.m3play.constants.WebClientPoTokenEnabledKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.PreferenceGroupTitle
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import com.j.m3play.viewmodels.PoTokenState
import com.j.m3play.viewmodels.PoTokenViewModel

private const val DEFAULT_EXTRACT_URL = "https://youtube.com/account"

private val SUPPORTED_CLIENTS = listOf(
    "web", "mweb", "web_safari", "web_embedded", "web_creator", "web_music"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoTokenScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: PoTokenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tokenState by viewModel.state.collectAsState()
    var showRegenerateSheet by remember { mutableStateOf(false) }
    val regenerateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var (webClientPoTokenEnabled, onWebClientPoTokenEnabledChange) = rememberPreference(
        WebClientPoTokenEnabledKey,
        defaultValue = false
    )
    var (useVisitorData, onUseVisitorDataChange) = rememberPreference(
        UseVisitorDataKey,
        defaultValue = false
    )
    var (sourceUrl, onSourceUrlChange) = rememberPreference(
        PoTokenSourceUrlKey,
        defaultValue = ""
    )
    var (storedGvsToken, onStoredGvsTokenChange) = rememberPreference(
        PoTokenGvsKey,
        defaultValue = ""
    )
    var (storedPlayerToken, onStoredPlayerTokenChange) = rememberPreference(
        PoTokenPlayerKey,
        defaultValue = ""
    )
    var (storedVisitorData, onStoredVisitorDataChange) = rememberPreference(
        VisitorDataKey,
        defaultValue = ""
    )
    val (innerTubeCookie, _) = rememberPreference(
        InnerTubeCookieKey,
        defaultValue = ""
    )

    val extractionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val gvsToken = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_GVS_TOKEN).orEmpty()
            val playerToken = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_PLAYER_TOKEN).orEmpty()
            val visitorData = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_VISITOR_DATA).orEmpty()

            if (gvsToken.isNotBlank() && playerToken.isNotBlank() && visitorData.isNotBlank()) {
                viewModel.onTokensExtracted(
                    visitorData = visitorData,
                    poToken = gvsToken,
                    playerToken = playerToken,
                )
            } else {
                viewModel.onExtractionError(context.getString(R.string.token_generation_failed))
            }
        } else {
            val error = result.data?.getStringExtra(PoTokenExtractionActivity.EXTRA_ERROR).orEmpty()
            if (error.isNotBlank()) {
                viewModel.onExtractionError(error)
            }
        }
    }

    val launchExtraction: () -> Unit = {
        viewModel.resetState()
        val launchUrl = sourceUrl.takeIf { it.isNotBlank() } ?: DEFAULT_EXTRACT_URL
        val intent = Intent(context, PoTokenExtractionActivity::class.java).apply {
            putExtra(PoTokenExtractionActivity.EXTRA_SOURCE_URL, launchUrl)
        }
        extractionLauncher.launch(intent)
    }

    val hasCookie = innerTubeCookie.isNotBlank()

    LaunchedEffect(tokenState) {
        when (val state = tokenState) {
            is PoTokenState.Success -> {
                onStoredGvsTokenChange(state.gvsToken)
                onStoredPlayerTokenChange(state.playerToken)
                onStoredVisitorDataChange(state.visitorData)
                Toast.makeText(context, R.string.tokens_generated, Toast.LENGTH_SHORT).show()
            }
            is PoTokenState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    val displayGvsToken = when (val s = tokenState) {
        is PoTokenState.Success -> s.gvsToken
        else -> storedGvsToken
    }
    val displayPlayerToken = when (val s = tokenState) {
        is PoTokenState.Success -> s.playerToken
        else -> storedPlayerToken
    }
    val displayVisitorData = when (val s = tokenState) {
        is PoTokenState.Success -> s.visitorData
        else -> storedVisitorData
    }

    if (showRegenerateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRegenerateSheet = false },
            sheetState = regenerateSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.source_url),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = onSourceUrlChange,
                    label = { Text(stringResource(R.string.source_url)) },
                    placeholder = { Text(stringResource(R.string.source_url_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(20.dp),
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        showRegenerateSheet = false
                        launchExtraction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        stringResource(R.string.regenerate_token),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.po_token_generation),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.web_client_po_token)) },
                            description = stringResource(R.string.web_client_po_token_desc),
                            icon = { Icon(painterResource(R.drawable.token), null) },
                            checked = webClientPoTokenEnabled,
                            onCheckedChange = onWebClientPoTokenEnabledChange,
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = webClientPoTokenEnabled,
                    enter = expandVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PreferenceGroupTitle(
                            title = stringResource(R.string.generated_tokens),
                            modifier = Modifier.padding(start = 24.dp)
                        )

                        SelectableTokenCard(
                            label = stringResource(R.string.po_token_gvs),
                            token = displayGvsToken,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(displayGvsToken))
                                Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        SelectableTokenCard(
                            label = stringResource(R.string.po_token_player),
                            token = displayPlayerToken,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(displayPlayerToken))
                                Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        SelectableTokenCard(
                            label = stringResource(R.string.visitor_data),
                            token = displayVisitorData,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(displayVisitorData))
                                Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        PreferenceGroupTitle(
                            title = stringResource(R.string.supported_clients),
                            modifier = Modifier.padding(start = 24.dp)
                        )

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SUPPORTED_CLIENTS.forEach { client ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = client,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    border = AssistChipDefaults.assistChipBorder(
                                        enabled = true,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        PreferenceGroupTitle(
                            title = stringResource(R.string.token_settings),
                            modifier = Modifier.padding(start = 24.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(Modifier.padding(vertical = 12.dp)) {
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.use_visitor_data)) },
                                    description = stringResource(R.string.use_visitor_data_desc),
                                    icon = { Icon(painterResource(R.drawable.person), null) },
                                    checked = useVisitorData,
                                    onCheckedChange = { enabled ->
                                        if (enabled && hasCookie) {
                                            Toast.makeText(
                                                context,
                                                R.string.cookies_must_be_disabled,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            onUseVisitorDataChange(enabled)
                                        }
                                    },
                                )
                            }
                        }

                        ExtendedFloatingActionButton(
                            onClick = {
                                showRegenerateSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                                .height(56.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(24.dp),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.regenerate),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableTokenCard(
    label: String,
    token: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = token.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = if (token.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (token.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    IconButton(
                        onClick = onCopy,
                        onLongClick = onCopy,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.copy),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
