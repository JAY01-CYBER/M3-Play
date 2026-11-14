package com.j.m3play.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.EnumDialog
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.component.Material3SettingsGroup
import com.j.m3play.ui.component.Material3SettingsItem
import com.j.m3play.ui.component.TextFieldDialog
import com.j.m3play.utils.TranslatorLanguages
import com.j.m3play.utils.rememberPreference
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordExperimental(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val (translatorEnabled, onTranslatorEnabledChange) = rememberPreference(EnableTranslatorKey, false)
    val (translatorContexts, onTranslatorContextsChange) = rememberPreference(TranslatorContextsKey, "{song}, {artist}, {album}")
    val (translatorTargetLang, onTranslatorTargetLangChange) = rememberPreference(TranslatorTargetLangKey, "ENGLISH")

    val (button1Label, onButton1LabelChange) = rememberPreference(DiscordActivityButton1LabelKey, "Listen on YouTube Music")
    val (button1Enabled, onButton1EnabledChange) = rememberPreference(DiscordActivityButton1EnabledKey, true)
    val (button1UrlSource, onButton1UrlSourceChange) = rememberPreference(DiscordActivityButton1UrlSourceKey, "songurl")
    val (button1CustomUrl, onButton1CustomUrlChange) = rememberPreference(DiscordActivityButton1CustomUrlKey, "")
    
    val (button2Label, onButton2LabelChange) = rememberPreference(DiscordActivityButton2LabelKey, "Go to M3Play")
    val (button2Enabled, onButton2EnabledChange) = rememberPreference(DiscordActivityButton2EnabledKey, true)
    val (button2UrlSource, onButton2UrlSourceChange) = rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
    val (button2CustomUrl, onButton2CustomUrlChange) = rememberPreference(DiscordActivityButton2CustomUrlKey, "https://github.com/JAY01-CYBER/M3-Play")

    var showContextDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    
    var showBtn1LabelDialog by remember { mutableStateOf(false) }
    var showBtn1SourceDialog by remember { mutableStateOf(false) }
    var showBtn1CustomUrlDialog by remember { mutableStateOf(false) }
    
    var showBtn2LabelDialog by remember { mutableStateOf(false) }
    var showBtn2SourceDialog by remember { mutableStateOf(false) }
    var showBtn2CustomUrlDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val languages = remember { TranslatorLanguages.load(context) }
    val currentLangName = languages.find { it.code == translatorTargetLang }?.name ?: translatorTargetLang
    val urlOptions = listOf("songurl", "artisturl", "albumurl", "custom")

    // --- Dialogs ---
    if (showContextDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.context_info)) },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(translatorContexts),
            onDone = { onTranslatorContextsChange(it); showContextDialog = false },
            onDismiss = { showContextDialog = false },
            extraContent = { Text(stringResource(R.string.translator_info_usage), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        )
    }

    if (showLangDialog) {
        EnumDialog(
            onDismiss = { showLangDialog = false },
            onSelect = { onTranslatorTargetLangChange(it); showLangDialog = false },
            title = stringResource(R.string.target_language),
            current = translatorTargetLang,
            values = languages.map { it.code },
            valueText = { code -> languages.find { it.code == code }?.name ?: code }
        )
    }
    
    if (showBtn1LabelDialog) {
        TextFieldDialog(title = { Text(stringResource(R.string.discord_activity_button1_label)) }, initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(button1Label), onDone = { onButton1LabelChange(it.ifBlank { "Listen on YouTube Music" }); showBtn1LabelDialog = false }, onDismiss = { showBtn1LabelDialog = false })
    }
    if (showBtn1SourceDialog) {
        EnumDialog(onDismiss = { showBtn1SourceDialog = false }, onSelect = { onButton1UrlSourceChange(it); showBtn1SourceDialog = false }, title = stringResource(R.string.discord_activity_button_1_url), current = button1UrlSource, values = urlOptions, valueText = { when (it) { "songurl" -> "Song URL"; "artisturl" -> "Artist URL"; "albumurl" -> "Album URL"; "custom" -> "Custom URL"; else -> it } })
    }
    if (showBtn1CustomUrlDialog) {
        TextFieldDialog(title = { Text(stringResource(R.string.discord_activity_button1_url)) }, initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(button1CustomUrl), onDone = { onButton1CustomUrlChange(it); showBtn1CustomUrlDialog = false }, onDismiss = { showBtn1CustomUrlDialog = false })
    }

    if (showBtn2LabelDialog) {
        TextFieldDialog(title = { Text(stringResource(R.string.discord_activity_button2_label)) }, initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(button2Label), onDone = { onButton2LabelChange(it.ifBlank { "Go to M3Play" }); showBtn2LabelDialog = false }, onDismiss = { showBtn2LabelDialog = false })
    }
    if (showBtn2SourceDialog) {
        EnumDialog(onDismiss = { showBtn2SourceDialog = false }, onSelect = { onButton2UrlSourceChange(it); showBtn2SourceDialog = false }, title = stringResource(R.string.discord_activity_button_2_url), current = button2UrlSource, values = urlOptions, valueText = { when (it) { "songurl" -> "Song URL"; "artisturl" -> "Artist URL"; "albumurl" -> "Album URL"; "custom" -> "Custom URL"; else -> it } })
    }
    if (showBtn2CustomUrlDialog) {
        TextFieldDialog(title = { Text(stringResource(R.string.discord_activity_button2_url)) }, initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(button2CustomUrl), onDone = { onButton2CustomUrlChange(it); showBtn2CustomUrlDialog = false }, onDismiss = { showBtn2CustomUrlDialog = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.experiment_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.translator_options),
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.translate),
                            title = { Text(stringResource(R.string.enable_translator)) },
                            description = { Text(stringResource(R.string.enable_translator_desc)) },
                            trailingContent = { Switch(checked = translatorEnabled, onCheckedChange = onTranslatorEnabledChange) },
                            onClick = { onTranslatorEnabledChange(!translatorEnabled) }
                        ))
                        if (translatorEnabled) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.text_fields),
                                title = { Text(stringResource(R.string.context_info)) },
                                description = { Text(translatorContexts) },
                                onClick = { showContextDialog = true }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.language),
                                title = { Text(stringResource(R.string.target_language)) },
                                description = { Text(currentLangName) },
                                onClick = { showLangDialog = true }
                            ))
                        }
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.discord_button_options) + " 1",
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.buttons),
                            title = { Text(stringResource(R.string.show_button)) },
                            description = { Text(stringResource(R.string.show_button1_description)) },
                            trailingContent = { Switch(checked = button1Enabled, onCheckedChange = onButton1EnabledChange) },
                            onClick = { onButton1EnabledChange(!button1Enabled) }
                        ))
                        if (button1Enabled) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.buttons),
                                title = { Text(stringResource(R.string.discord_activity_button1_label)) },
                                description = { Text(button1Label) },
                                onClick = { showBtn1LabelDialog = true }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = { Text(stringResource(R.string.discord_activity_button_1_url)) },
                                description = { Text(when(button1UrlSource) { "songurl" -> "Song URL"; "artisturl" -> "Artist URL"; "albumurl" -> "Album URL"; "custom" -> "Custom URL"; else -> button1UrlSource }) },
                                onClick = { showBtn1SourceDialog = true }
                            ))
                            if (button1UrlSource == "custom") {
                                add(Material3SettingsItem(
                                    icon = painterResource(R.drawable.link),
                                    title = { Text(stringResource(R.string.discord_activity_button1_url)) },
                                    description = { Text(if (button1CustomUrl.isBlank()) "No URL set" else button1CustomUrl) },
                                    onClick = { showBtn1CustomUrlDialog = true }
                                ))
                            }
                        }
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.discord_button_options) + " 2",
                    items = buildList {
                        add(Material3SettingsItem(
                            icon = painterResource(R.drawable.buttons),
                            title = { Text(stringResource(R.string.show_button)) },
                            description = { Text(stringResource(R.string.show_button2_description)) },
                            trailingContent = { Switch(checked = button2Enabled, onCheckedChange = onButton2EnabledChange) },
                            onClick = { onButton2EnabledChange(!button2Enabled) }
                        ))
                        if (button2Enabled) {
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.buttons),
                                title = { Text(stringResource(R.string.discord_activity_button2_label)) },
                                description = { Text(button2Label) },
                                onClick = { showBtn2LabelDialog = true }
                            ))
                            add(Material3SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = { Text(stringResource(R.string.discord_activity_button_2_url)) },
                                description = { Text(when(button2UrlSource) { "songurl" -> "Song URL"; "artisturl" -> "Artist URL"; "albumurl" -> "Album URL"; "custom" -> "Custom URL"; else -> button2UrlSource }) },
                                onClick = { showBtn2SourceDialog = true }
                            ))
                            if (button2UrlSource == "custom") {
                                add(Material3SettingsItem(
                                    icon = painterResource(R.drawable.link),
                                    title = { Text(stringResource(R.string.discord_activity_button2_url)) },
                                    description = { Text(if (button2CustomUrl.isBlank()) "No URL set" else button2CustomUrl) },
                                    onClick = { showBtn2CustomUrlDialog = true }
                                ))
                            }
                        }
                    }
                )
            }
        }
    }
}
