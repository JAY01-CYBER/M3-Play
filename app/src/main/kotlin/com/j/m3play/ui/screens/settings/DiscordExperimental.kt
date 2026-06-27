/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │  Style: ANDROID 17 (Ultra-Rounded, M3)     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.R
import com.j.m3play.constants.*
import com.j.m3play.ui.component.ListItem
import com.j.m3play.ui.component.PreferenceEntry
import com.j.m3play.ui.component.SwitchPreference
import com.j.m3play.utils.TranslatorLanguages
import com.j.m3play.utils.rememberPreference
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordExperimental(
    navController: NavController,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.experiment_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
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
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.translator_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                )

                val (translatorEnabled, onTranslatorEnabledChange) = rememberPreference(key = EnableTranslatorKey, defaultValue = false)
                val (translatorContexts, onTranslatorContextsChange) = rememberPreference(key = TranslatorContextsKey, defaultValue = "{song}, {artist}, {album}")
                val (translatorTargetLang, onTranslatorTargetLangChange) = rememberPreference(key = TranslatorTargetLangKey, defaultValue = "ENGLISH")

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_translator)) },
                            description = stringResource(R.string.enable_translator_desc),
                            icon = { Icon(painterResource(R.drawable.translate), null) },
                            checked = translatorEnabled,
                            onCheckedChange = onTranslatorEnabledChange,
                        )

                        AnimatedVisibility(visible = translatorEnabled) {
                            Column {
                                TextField(
                                    value = translatorContexts,
                                    onValueChange = { onTranslatorContextsChange(it) },
                                    label = { Text(stringResource(R.string.context_info)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                    supportingText = {
                                        Text(stringResource(R.string.translator_info_usage))
                                    }
                                )

                                var showLangDialog by remember { mutableStateOf(false) }
                                val context = LocalContext.current
                                val languages = remember { TranslatorLanguages.load(context) }
                                val currentLangName = languages.find { it.code == translatorTargetLang }?.name ?: translatorTargetLang

                                PreferenceEntry(
                                    title = { Text(stringResource(R.string.target_language)) },
                                    description = currentLangName,
                                    icon = { Icon(painterResource(R.drawable.translate), null) },
                                    trailingContent = {
                                        TextButton(onClick = { showLangDialog = true }) {
                                            Text(stringResource(R.string.select_dialog), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                )

                                if (showLangDialog) {
                                    AlertDialog(
                                        shape = RoundedCornerShape(32.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        onDismissRequest = { showLangDialog = false },
                                        title = { Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold) },
                                        text = {
                                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                                items(languages) { lang ->
                                                    ListItem(
                                                        title = lang.name,
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            onTranslatorTargetLangChange(lang.code)
                                                            showLangDialog = false
                                                        },
                                                        thumbnailContent = {},
                                                        isActive = (lang.code == translatorTargetLang)
                                                    )
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showLangDialog = false }) {
                                                Text(stringResource(R.string.close_dialog))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.discord_button_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            item {
                val (button1Label, onButton1LabelChange) = rememberPreference(key = DiscordActivityButton1LabelKey, defaultValue = "Listen on YouTube Music")
                val (button1Enabled, onButton1EnabledChange) = rememberPreference(key = DiscordActivityButton1EnabledKey, defaultValue = true)
                val (button2Label, onButton2LabelChange) = rememberPreference(key = DiscordActivityButton2LabelKey, defaultValue = "Go to M3Play")
                val (button2Enabled, onButton2EnabledChange) = rememberPreference(key = DiscordActivityButton2EnabledKey, defaultValue = true)
                val urlOptions = listOf("songurl", "artisturl", "albumurl", "custom")
                val (button1UrlSource, onButton1UrlSourceChange) = rememberPreference(key = DiscordActivityButton1UrlSourceKey, defaultValue = "songurl")
                val (button1CustomUrl, onButton1CustomUrlChange) = rememberPreference(key = DiscordActivityButton1CustomUrlKey, defaultValue = "")
                val (button2UrlSource, onButton2UrlSourceChange) = rememberPreference(key = DiscordActivityButton2UrlSourceKey, defaultValue = "custom")
                val (button2CustomUrl, onButton2CustomUrlChange) = rememberPreference(key = DiscordActivityButton2CustomUrlKey, defaultValue = "https://github.com/JAY01-CYBER/M3-Play")

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.show_button)) },
                            description = stringResource(R.string.show_button1_description),
                            icon = { Icon(painterResource(R.drawable.buttons), null) },
                            trailingContent = { Switch(checked = button1Enabled, onCheckedChange = onButton1EnabledChange) }
                        )

                        if (button1Enabled) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                TextField(
                                    value = button1UrlSource,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.discord_activity_button_1_url)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .pointerInput(Unit) { detectTapGestures { expanded = true } }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    leadingIcon = { Icon(painterResource(R.drawable.link), null) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    urlOptions.forEach { opt ->
                                        val display = when (opt) {
                                            "songurl" -> "Song URL"
                                            "artisturl" -> "Artist URL"
                                            "albumurl" -> "Album URL"
                                            "custom" -> "Custom URL"
                                            else -> opt
                                        }
                                        DropdownMenuItem(
                                            text = { Text(display) },
                                            onClick = { onButton1UrlSourceChange(opt); expanded = false }
                                        )
                                    }
                                }
                            }
                            EditablePreference(
                                title = stringResource(R.string.discord_activity_button1_label),
                                iconRes = R.drawable.buttons,
                                value = button1Label,
                                defaultValue = "Listen on YouTube Music",
                                onValueChange = onButton1LabelChange
                            )
                            if (button1UrlSource == "custom") {
                                EditablePreference(
                                    title = stringResource(R.string.discord_activity_button1_url),
                                    iconRes = R.drawable.link,
                                    value = button1CustomUrl,
                                    defaultValue = "",
                                    onValueChange = onButton1CustomUrlChange
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.show_button)) },
                            description = stringResource(R.string.show_button2_description),
                            icon = { Icon(painterResource(R.drawable.buttons), null) },
                            trailingContent = { Switch(checked = button2Enabled, onCheckedChange = onButton2EnabledChange) }
                        )

                        if (button2Enabled) {
                            var expanded2 by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded2,
                                onExpandedChange = { expanded2 = it }
                            ) {
                                TextField(
                                    value = button2UrlSource,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.discord_activity_button_2_url)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .pointerInput(Unit) { detectTapGestures { expanded2 = true } }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    leadingIcon = { Icon(painterResource(R.drawable.link), null) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(expanded = expanded2, onDismissRequest = { expanded2 = false }) {
                                    urlOptions.forEach { opt ->
                                        val display = when (opt) {
                                            "songurl" -> "Song URL"
                                            "artisturl" -> "Artist URL"
                                            "albumurl" -> "Album URL"
                                            "custom" -> "Custom URL"
                                            else -> opt
                                        }
                                        DropdownMenuItem(
                                            text = { Text(display) },
                                            onClick = { onButton2UrlSourceChange(opt); expanded2 = false }
                                        )
                                    }
                                }
                            }
                            EditablePreference(
                                title = stringResource(R.string.discord_activity_button2_label),
                                iconRes = R.drawable.buttons,
                                value = button2Label,
                                defaultValue = "Go to M3Play",
                                onValueChange = onButton2LabelChange
                            )
                            if (button2UrlSource == "custom") {
                                EditablePreference(
                                    title = stringResource(R.string.discord_activity_button2_url),
                                    iconRes = R.drawable.link,
                                    value = button2CustomUrl,
                                    defaultValue = "",
                                    onValueChange = onButton2CustomUrlChange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
