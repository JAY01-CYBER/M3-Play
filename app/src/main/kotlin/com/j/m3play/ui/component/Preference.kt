package com.j.m3play.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j.m3play.R
import kotlin.math.roundToInt

val LocalPreferenceInGroup = compositionLocalOf { false }

@Composable
fun PreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    val inGroup = LocalPreferenceInGroup.current
    val interactionSource = remember { MutableInteractionSource() }

    val rowContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = isEnabled && onClick != null,
                    onClick = onClick ?: {},
                )
                .alpha(if (isEnabled) 1f else 0.5f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)) {
                        icon()
                    }
                }
                Spacer(Modifier.width(16.dp))
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) {
                    title()
                }
                if (description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content?.invoke()
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    trailingContent()
                }
            }
        }
    }

    if (inGroup) {
        rowContent()
    } else {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            rowContent()
        }
    }
}

@Composable
fun <T> ListPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String, // Added @Composable back
    onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = title,
        supportingContent = {
            Text(text = valueText(selectedValue))
        },
        leadingContent = icon,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { showDialog = true }
    )

    if (showDialog) {
        M3SelectorDialog(
            title = title,
            selectedValue = selectedValue,
            values = values,
            valueText = valueText,
            onValueSelected = {
                onValueSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun <T> M3SelectorDialog(
    title: @Composable () -> Unit,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String, // Added @Composable back
    onValueSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = title,
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                values.forEach { value ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueSelected(value)
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = (value == selectedValue),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = valueText(value),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
inline fun <reified T : Enum<T>> EnumListPreference(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    noinline icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    noinline valueText: @Composable (T) -> String, // Added @Composable back
    noinline onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    ListPreference(
        modifier = modifier,
        title = title,
        icon = icon,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        valueText = valueText,
        onValueSelected = onValueSelected,
        isEnabled = isEnabled,
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
                thumbContent = {
                    Icon(
                        painter = painterResource(
                            id = if (checked) R.drawable.check else R.drawable.close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            )
        },
        onClick = { onCheckedChange(!checked) },
        isEnabled = isEnabled
    )
}

@Composable
fun EditTextPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    isEnabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        TextFieldDialog(
            initialTextFieldValue =
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
            singleLine = singleLine,
            isInputValid = isInputValid,
            onDone = onValueChange,
            onDismiss = { showDialog = false },
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value,
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(value) }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_duration),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onValueChange.invoke(sliderValue)
            },
            onCancel = {
                sliderValue = value
                showDialog = false
            },
            onReset = { sliderValue = 30f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.seconds,
                            sliderValue.roundToInt(),
                            sliderValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 15f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value.roundToInt().toString(),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossfadeSliderPreference(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.audio_crossfade_dialog_title),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded = sliderValue.roundToInt().coerceIn(0, 10)
                sliderValue = rounded.toFloat()
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = value.toFloat()
                showDialog = false
            },
            onReset = { sliderValue = 0f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(0, 10)
                    Text(
                        text = if (rounded == 0) {
                            stringResource(R.string.dark_theme_off)
                        } else {
                            pluralStringResource(R.plurals.seconds, rounded, rounded)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.audio_crossfade_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it.coerceIn(0f, 10f) },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    val descriptionText =
        if (value <= 0) {
            stringResource(R.string.dark_theme_off)
        } else {
            pluralStringResource(R.plurals.seconds, value, value)
        }

    PreferenceEntry(
        modifier = modifier,
        title = { Text(stringResource(R.string.audio_crossfade_title)) },
        description = descriptionText,
        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
fun NumberPickerPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = 10,
    valueText: (Int) -> String = { it.toString() },
    isEnabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    title()
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                sliderValue = rounded.toFloat()
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = value.toFloat()
                showDialog = false
            },
            onReset = { sliderValue = minValue.toFloat() },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                    Text(
                        text = valueText(rounded),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it.coerceIn(minValue.toFloat(), maxValue.toFloat()) },
                        valueRange = minValue.toFloat()..maxValue.toFloat(),
                        steps = maxValue - minValue - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(value),
        icon = icon,
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalPreferenceInGroup provides true) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun PreferenceGroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 74.dp, end = 24.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun PreferenceGroupTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}
