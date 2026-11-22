package com.j.m3play.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    val (displayName, setDisplayName) = rememberPreference(TogetherDisplayNameKey, android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: "User")
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

    if (showNameDialog) {
        TextFieldDialog(
            title = { Text("Display Name") },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(displayName),
            isInputValid = { it.trim().isNotBlank() },
            onDone = { setDisplayName(it.trim()); showNameDialog = false },
            onDismiss = { showNameDialog = false },
        )
    }

    if (showPortDialog) {
        TextFieldDialog(
            title = { Text("Port Number") },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(port.toString()),
            isInputValid = { it.trim().toIntOrNull() in 1..65535 },
            onDone = { setPort(it.trim().toInt()); showPortDialog = false },
            onDismiss = { showPortDialog = false },
        )
    }

    if (showJoinDialog) {
        TextFieldDialog(
            title = { Text("Join Session") },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(joinInput),
            placeholder = { Text(if (joinModeOnline) "Enter Room Code" else "Enter LAN URL/Link") },
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
            valueText = { if (it == "LAN") "Local Wi-Fi (LAN)" else "Internet (Online)" }
        )
    }

    if (showJoinModeDialog) {
        EnumDialog(
            onDismiss = { showJoinModeDialog = false },
            onSelect = { joinModeOnline = it == "Online"; showJoinModeDialog = false },
            title = "Join Mode",
            current = if (joinModeOnline) "Online" else "LAN",
            values = listOf("LAN", "Online"),
            valueText = { if (it == "LAN") "Via LAN Link" else "Via Room Code" }
        )
    }

    if (confirmKickParticipantId != null) {
        val name = hostingOnline?.roomState?.participants?.firstOrNull { it.id == confirmKickParticipantId }?.name
        AlertDialog(
            onDismissRequest = { confirmKickParticipantId = null },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Kick Participant") },
            text = { Text("Are you sure you want to kick ${name ?: "this user"}?") },
            confirmButton = { Button(onClick = { playerConnection?.service?.kickTogetherParticipant(confirmKickParticipantId!!); confirmKickParticipantId = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Kick") } },
            dismissButton = { TextButton(onClick = { confirmKickParticipantId = null }) { Text("Cancel") } }
        )
    }

    if (confirmBanParticipantId != null) {
        val name = hostingOnline?.roomState?.participants?.firstOrNull { it.id == confirmBanParticipantId }?.name
        AlertDialog(
            onDismissRequest = { confirmBanParticipantId = null },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Ban Participant") },
            text = { Text("Are you sure you want to ban ${name ?: "this user"}?") },
            confirmButton = { Button(onClick = { playerConnection?.service?.banTogetherParticipant(confirmBanParticipantId!!); confirmBanParticipantId = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Ban") } },
            dismissButton = { TextButton(onClick = { confirmBanParticipantId = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Music Together", fontWeight = FontWeight.Bold) },
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
        val bottomInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomInsets + 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    state = sessionState,
                    onCopyText = { _, value ->
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", value))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
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
                        title = "Host a Session",
                        items = buildList {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.wifi_proxy),
                                title = { Text("Network Mode") },
                                description = { Text(if (hostModeOnline) "Online" else "LAN") },
                                onClick = { showHostModeDialog = true }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.person),
                                title = { Text("Display Name") },
                                description = { Text(displayName) },
                                onClick = { showNameDialog = true }
                            ))
                            if (!hostModeOnline) {
                                add(Material3SettingsItem(
                                    icon = painterResource(R.drawable.link),
                                    title = { Text("Port Number") },
                                    description = { Text(port.toString()) },
                                    onClick = { showPortDialog = true }
                                ))
                            }
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.playlist_add),
                                title = { Text("Allow Guests to Add Tracks") },
                                trailingContent = { Switch(checked = allowAddTracks, onCheckedChange = { v -> setAllowAddTracksRaw(v); pushSettingsToActiveSession(addTracks = v) }, thumbContent = { Icon(painterResource(if (allowAddTracks) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { val v = !allowAddTracks; setAllowAddTracksRaw(v); pushSettingsToActiveSession(addTracks = v) }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.play),
                                title = { Text("Allow Guests to Control Playback") },
                                trailingContent = { Switch(checked = allowControlPlayback, onCheckedChange = { v -> setAllowControlPlaybackRaw(v); pushSettingsToActiveSession(controlPlayback = v) }, thumbContent = { Icon(painterResource(if (allowControlPlayback) R.drawable.check else R.drawable.close), null, modifier = Modifier.size(SwitchDefaults.IconSize)) }) },
                                onClick = { val v = !allowControlPlayback; setAllowControlPlaybackRaw(v); pushSettingsToActiveSession(controlPlayback = v) }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.lock),
                                title = { Text("Require Approval to Join") },
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
                            Text("Loading...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(painterResource(R.drawable.fire), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Session", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = "Join a Session",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.wifi_proxy),
                            title = { Text("Connection Mode") },
                            description = { Text(if (joinModeOnline) "Join Code" else "LAN Link") },
                            enabled = !disableJoinUi,
                            onClick = { showJoinModeDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.input),
                            title = { Text("Join Session") },
                            description = { Text(joinInput.trim().ifBlank { if (joinModeOnline) "Enter Join Code" else "Enter Join Link" }) },
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
                            Text("Connecting...", fontWeight = FontWeight.Bold)
                        }
                        isWaitingApproval -> {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text("Waiting for approval...", fontWeight = FontWeight.Bold)
                        }
                        isJoinedAsAcceptedGuest -> {
                            Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Joined", fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(painterResource(R.drawable.join), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Join", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeDialog(onGotIt: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(text = "Music Together", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Listen to music synchronously with your friends across different devices in real-time.")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("Don't show this again")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGotIt(dontShowAgain) }) { Text("Got it") } }
    )
}

@Composable
fun StatusCard(state: TogetherSessionState, onCopyText: (Int, String) -> Unit, onShareLink: (String) -> Unit, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    if (state is TogetherSessionState.Idle) return

    val titleText = when (state) {
        is TogetherSessionState.Hosting -> "Hosting on LAN"
        is TogetherSessionState.HostingOnline -> "Hosting Online"
        is TogetherSessionState.Joining -> "Joining on LAN"
        is TogetherSessionState.JoiningOnline -> "Joining Online"
        is TogetherSessionState.Joined -> if (state.role is TogetherRole.Host) "Active Hosting" else "Joined Session"
        else -> "Unknown Status"
    }

    val color = when (state) {
        is TogetherSessionState.Joined -> Color(0xFF43B581)
        is TogetherSessionState.HostingOnline -> Color(0xFF43B581)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = titleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                if (state is TogetherSessionState.Joined || state is TogetherSessionState.HostingOnline) {
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                }
            }

            if (state is TogetherSessionState.Hosting) {
                Text("Session started. Wait for others to join.", style = MaterialTheme.typography.bodyMedium)
            }
            if (state is TogetherSessionState.HostingOnline) {
                val code = (try {
                    state.roomState?.javaClass?.getMethod("getJoinCode")?.invoke(state.roomState) as? String
                } catch (e: Exception) { null }) ?: ""

                if (code.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Room Code:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { onCopyText(0, code) }) { Icon(painterResource(R.drawable.copy), null) }
                        IconButton(onClick = { onShareLink(code) }) { Icon(painterResource(R.drawable.share), null) }
                    }
                }
            }
            Button(onClick = onLeave, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
                Text("Leave Session")
            }
        }
    }
}

@Composable
fun OnlineParticipantsCard(
    participants: List<com.j.m3play.together.TogetherParticipant>,
    hostApprovalEnabled: Boolean,
    onApprove: (String, Boolean) -> Unit,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Participants (${participants.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            participants.forEach { p ->
                val pRoleName = try { p.javaClass.getMethod("getRole").invoke(p)?.toString() } catch (e: Exception) { null }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    if (p.isPending) {
                        Row {
                            IconButton(onClick = { onApprove(p.id, true) }) { Icon(painterResource(R.drawable.check), null, tint = Color(0xFF43B581)) }
                            IconButton(onClick = { onApprove(p.id, false) }) { Icon(painterResource(R.drawable.close), null, tint = MaterialTheme.colorScheme.error) }
                        }
                    } else if (pRoleName != "Host") {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) { Icon(painterResource(R.drawable.more_vert), null) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Kick") }, onClick = { expanded = false; onKick(p.id) })
                                DropdownMenuItem(text = { Text("Ban") }, onClick = { expanded = false; onBan(p.id) })
                            }
                        }
                    } else {
                        Text("Host", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
