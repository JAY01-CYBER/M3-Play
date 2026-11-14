package com.j.m3play.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.LocalPlayerConnection
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.together.TogetherLink
import com.j.m3play.together.TogetherRole
import com.j.m3play.together.TogetherRoomSettings
import com.j.m3play.together.TogetherSessionState
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.component.TextFieldDialog
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTogetherScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val withLocalNetworkAccess = rememberLocalNetworkAccess()
    val playerConnection = LocalPlayerConnection.current

    val (welcomeShown, setWelcomeShown) = rememberPreference(TogetherWelcomeShownKey, false)
    var welcomeDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val showWelcome = !welcomeShown && !welcomeDismissedThisSession

    if (showWelcome) {
        WelcomeDialog(
            onGotIt = { dontShowAgain ->
                welcomeDismissedThisSession = true
                if (dontShowAgain) setWelcomeShown(true)
            },
            onDismiss = { welcomeDismissedThisSession = true },
        )
    }

    val (displayName, setDisplayName) = rememberPreference(TogetherDisplayNameKey, Build.MODEL?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name))
    val (port, setPort) = rememberPreference(TogetherDefaultPortKey, 42117)
    val (allowAddTracks, setAllowAddTracksRaw) = rememberPreference(TogetherAllowGuestsToAddTracksKey, true)
    val (allowControlPlayback, setAllowControlPlaybackRaw) = rememberPreference(TogetherAllowGuestsToControlPlaybackKey, false)
    val (requireApproval, setRequireApprovalRaw) = rememberPreference(TogetherRequireHostApprovalToJoinKey, false)
    val (lastJoinLink, setLastJoinLink) = rememberPreference(TogetherLastJoinLinkKey, "")

    val sessionStateFlow = remember(playerConnection) { playerConnection?.service?.togetherSessionState ?: MutableStateFlow(TogetherSessionState.Idle) }
    val sessionState by sessionStateFlow.collectAsState()

    val isHosting = sessionState is TogetherSessionState.Hosting || sessionState is TogetherSessionState.HostingOnline
    val isJoining = sessionState is TogetherSessionState.Joining || sessionState is TogetherSessionState.JoiningOnline
    val isHostRole = when (val state = sessionState) {
        is TogetherSessionState.Hosting, is TogetherSessionState.HostingOnline -> true
        is TogetherSessionState.Joined -> state.role is TogetherRole.Host
        else -> false
    }
    val isCreatingSessionLoading = when (val state = sessionState) {
        is TogetherSessionState.Hosting -> state.roomState == null
        is TogetherSessionState.HostingOnline -> state.roomState == null
        else -> false
    }
    val isJoinedAsGuest = sessionState is TogetherSessionState.Joined && (sessionState as TogetherSessionState.Joined).role is TogetherRole.Guest
    val isWaitingApproval = isJoinedAsGuest && (sessionState as TogetherSessionState.Joined).roomState.participants.firstOrNull { it.id == (sessionState as TogetherSessionState.Joined).selfParticipantId }?.isPending == true
    val isJoinedAsAcceptedGuest = isJoinedAsGuest && !isWaitingApproval
    val disableJoinUi = isHostRole || isCreatingSessionLoading || isJoinedAsGuest

    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showPortDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showHostModeDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinModeDialog by rememberSaveable { mutableStateOf(false) }

    var hostModeOnline by rememberSaveable { mutableStateOf(false) }
    var joinModeOnline by rememberSaveable { mutableStateOf(false) }
    var joinInput by rememberSaveable { mutableStateOf(lastJoinLink) }

    val canJoin = remember(joinInput, joinModeOnline) {
        if (joinModeOnline) joinInput.trim().isNotBlank() else TogetherLink.decode(joinInput) != null
    }

    val hostingOnline = sessionState as? TogetherSessionState.HostingOnline
    var confirmKickParticipantId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmBanParticipantId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(disableJoinUi, isJoining, isHosting) {
        if (disableJoinUi || isJoining || isHosting) showJoinDialog = false
    }

    fun pushSettingsToActiveSession(addTracks: Boolean = allowAddTracks, controlPlayback: Boolean = allowControlPlayback, approval: Boolean = requireApproval) {
        if (isHosting) {
            playerConnection?.service?.updateTogetherSettings(TogetherRoomSettings(addTracks, controlPlayback, approval))
        }
    }

    // --- Dialogs ---
    if (showNameDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.together_display_name)) },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(displayName),
            isInputValid = { it.trim().isNotBlank() },
            onDone = { setDisplayName(it.trim()); showNameDialog = false },
            onDismiss = { showNameDialog = false },
        )
    }

    if (showPortDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.together_port)) },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(port.toString()),
            isInputValid = { it.trim().toIntOrNull() in 1..65535 },
            onDone = { setPort(it.trim().toInt()); showPortDialog = false },
            onDismiss = { showPortDialog = false },
        )
    }

    if (showJoinDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.join_session)) },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(joinInput),
            placeholder = { Text(if (joinModeOnline) stringResource(R.string.together_join_code_hint) else stringResource(R.string.together_join_link_hint)) },
            singleLine = false,
            maxLines = 4,
            isInputValid = { if (joinModeOnline) it.trim().isNotBlank() else TogetherLink.decode(it) != null },
            onDone = { raw ->
                val trimmed = raw.trim()
                joinInput = trimmed
                setLastJoinLink(trimmed)
                if (joinModeOnline) playerConnection?.service?.joinTogetherOnline(trimmed, displayName)
                else withLocalNetworkAccess { playerConnection?.service?.joinTogether(trimmed, displayName) }
                showJoinDialog = false
            },
            onDismiss = { showJoinDialog = false },
        )
    }

    if (showHostModeDialog) {
        EnumDialog(
            onDismiss = { showHostModeDialog = false },
            onSelect = { hostModeOnline = it == "Online"; showHostModeDialog = false },
            title = "Host Mode",
            current = if (hostModeOnline) "Online" else "LAN",
            values = listOf("LAN", "Online"),
            valueText = { if (it == "LAN") stringResource(R.string.together_lan) else stringResource(R.string.together_online) }
        )
    }

    if (showJoinModeDialog) {
        EnumDialog(
            onDismiss = { showJoinModeDialog = false },
            onSelect = { joinModeOnline = it == "Online"; showJoinModeDialog = false },
            title = "Join Mode",
            current = if (joinModeOnline) "Online" else "LAN",
            values = listOf("LAN", "Online"),
            valueText = { if (it == "LAN") stringResource(R.string.together_join_link) else stringResource(R.string.together_join_code) }
        )
    }

    // --- Kick / Ban Dialogs --- (Kept logic, simplified visually)
    if (confirmKickParticipantId != null) {
        val name = hostingOnline?.roomState?.participants?.firstOrNull { it.id == confirmKickParticipantId }?.name
        AlertDialog(
            onDismissRequest = { confirmKickParticipantId = null },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.together_kick)) },
            text = { Text(stringResource(R.string.together_kick_confirm, name ?: stringResource(R.string.unknown))) },
            confirmButton = { Button(onClick = { playerConnection?.service?.kickTogetherParticipant(confirmKickParticipantId!!); confirmKickParticipantId = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.together_kick)) } },
            dismissButton = { TextButton(onClick = { confirmKickParticipantId = null }) { Text(stringResource(R.string.dismiss)) } }
        )
    }

    if (confirmBanParticipantId != null) {
        val name = hostingOnline?.roomState?.participants?.firstOrNull { it.id == confirmBanParticipantId }?.name
        AlertDialog(
            onDismissRequest = { confirmBanParticipantId = null },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.together_ban)) },
            text = { Text(stringResource(R.string.together_ban_confirm, name ?: stringResource(R.string.unknown))) },
            confirmButton = { Button(onClick = { playerConnection?.service?.banTogetherParticipant(confirmBanParticipantId!!); confirmBanParticipantId = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.together_ban)) } },
            dismissButton = { TextButton(onClick = { confirmBanParticipantId = null }) { Text(stringResource(R.string.dismiss)) } }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_together), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            item {
                StatusCard(
                    state = sessionState,
                    onCopyText = { labelRes, value ->
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(labelRes), value))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    },
                    onShareLink = { link ->
                        val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, link) }
                        context.startActivity(Intent.createChooser(share, null))
                    },
                    onLeave = { playerConnection?.service?.leaveTogether() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (hostingOnline?.roomState != null && isHostRole) {
                item {
                    OnlineParticipantsCard(
                        participants = hostingOnline.roomState.participants,
                        hostApprovalEnabled = hostingOnline.settings.requireHostApprovalToJoin,
                        onApprove = { id, approved -> playerConnection?.service?.approveTogetherParticipant(id, approved) },
                        onKick = { confirmKickParticipantId = it },
                        onBan = { confirmBanParticipantId = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (!isJoinedAsGuest) {
                item {
                    val isStartEnabled = !isCreatingSessionLoading && !isJoining && !isHosting && sessionState !is TogetherSessionState.Joined
                    
                    Material3SettingsGroup(
                        title = stringResource(R.string.together_host_section),
                        items = buildList {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.wifi_proxy),
                                title = { Text("Network Mode") },
                                description = { Text(if (hostModeOnline) stringResource(R.string.together_online) else stringResource(R.string.together_lan)) },
                                onClick = { showHostModeDialog = true }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.person),
                                title = { Text(stringResource(R.string.together_display_name)) },
                                description = { Text(displayName) },
                                onClick = { showNameDialog = true }
                            ))
                            if (!hostModeOnline) {
                                add(Material3SettingsItem(
                                    icon = painterResource(R.drawable.link),
                                    title = { Text(stringResource(R.string.together_port)) },
                                    description = { Text(port.toString()) },
                                    onClick = { showPortDialog = true }
                                ))
                            }
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.playlist_add),
                                title = { Text(stringResource(R.string.together_allow_guests_add)) },
                                trailingContent = { Switch(checked = allowAddTracks, onCheckedChange = { v -> setAllowAddTracksRaw(v); pushSettingsToActiveSession(addTracks = v) }, thumbContent = { Icon(painterResource(if (allowAddTracks) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { val v = !allowAddTracks; setAllowAddTracksRaw(v); pushSettingsToActiveSession(addTracks = v) }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.play),
                                title = { Text(stringResource(R.string.together_allow_guests_control)) },
                                trailingContent = { Switch(checked = allowControlPlayback, onCheckedChange = { v -> setAllowControlPlaybackRaw(v); pushSettingsToActiveSession(controlPlayback = v) }, thumbContent = { Icon(painterResource(if (allowControlPlayback) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { val v = !allowControlPlayback; setAllowControlPlaybackRaw(v); pushSettingsToActiveSession(controlPlayback = v) }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.lock),
                                title = { Text(stringResource(R.string.together_require_approval)) },
                                trailingContent = { Switch(checked = requireApproval, onCheckedChange = { v -> setRequireApprovalRaw(v); pushSettingsToActiveSession(approval = v) }, thumbContent = { Icon(painterResource(if (requireApproval) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { val v = !requireApproval; setRequireApprovalRaw(v); pushSettingsToActiveSession(approval = v) }
                            ))
                        }
                    )

                    Button(
                        enabled = isStartEnabled,
                        onClick = {
                            val settings = TogetherRoomSettings(allowAddTracks, allowControlPlayback, requireApproval)
                            if (hostModeOnline) playerConnection?.service?.startTogetherOnlineHost(displayName, settings)
                            else withLocalNetworkAccess { playerConnection?.service?.startTogetherHost(port, displayName, settings) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp).height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isCreatingSessionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.loading), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(painterResource(R.drawable.fire), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.start_session), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.together_join_section),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.wifi_proxy),
                            title = { Text("Connection Mode") },
                            description = { Text(if (joinModeOnline) stringResource(R.string.together_join_code) else stringResource(R.string.together_join_link)) },
                            enabled = !disableJoinUi,
                            onClick = { showJoinModeDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.input),
                            title = { Text(stringResource(R.string.join_session)) },
                            description = { Text(joinInput.trim().ifBlank { if (joinModeOnline) stringResource(R.string.together_join_code_hint) else stringResource(R.string.together_join_link_hint) }) },
                            enabled = !disableJoinUi && !isJoining && !isJoinedAsAcceptedGuest && !isWaitingApproval,
                            onClick = { showJoinDialog = true }
                        )
                    )
                )

                FilledTonalButton(
                    enabled = canJoin && !disableJoinUi && !isJoining && !isJoinedAsAcceptedGuest && !isWaitingApproval,
                    onClick = {
                        val trimmed = joinInput.trim()
                        setLastJoinLink(trimmed)
                        if (joinModeOnline) playerConnection?.service?.joinTogetherOnline(trimmed, displayName)
                        else withLocalNetworkAccess { playerConnection?.service?.joinTogether(trimmed, displayName) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp).height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    when {
                        isJoining -> {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.connecting), fontWeight = FontWeight.Bold)
                        }
                        isWaitingApproval -> {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.together_waiting_approval), fontWeight = FontWeight.Bold)
                        }
                        isJoinedAsAcceptedGuest -> {
                            Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.joined), fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(painterResource(R.drawable.join), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.join), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Keep the rest of your dialogs (WelcomeDialog, StatusCard, OnlineParticipantsCard) below exactly as they are in your original code, as they are already custom components tailored perfectly.
