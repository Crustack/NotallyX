package com.philkes.notallyx.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun PreferenceBooleanDialog(
    title: String,
    initialValue: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var selectedValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (!message.isNullOrEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(modifier = Modifier.selectableGroup()) {
                    listOf(true to R.string.enabled, false to R.string.disabled).forEach {
                        (value, labelResId) ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (value == selectedValue),
                                        onClick = { selectedValue = value },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (value == selectedValue),
                                onClick = null,
                            )
                            Text(
                                text = stringResource(labelResId),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm(selectedValue)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun <T> PreferenceEnumWithToggleDialog(
    title: String,
    items: List<SelectionItem<T>>,
    selectedItem: T,
    toggleLabel: String,
    initialToggleValue: Boolean,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (selectedItem: T, toggleValue: Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(selectedItem) }
    var toggleState by remember { mutableStateOf(initialToggleValue) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (!hint.isNullOrEmpty()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(modifier = Modifier.selectableGroup()) {
                    items.forEach { item ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (item.value == selected),
                                        onClick = { selected = item.value },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (item.value == selected),
                                onClick = null,
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .toggleable(
                                value = toggleState,
                                onValueChange = { toggleState = it },
                                role = Role.Checkbox,
                            )
                            .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = toggleState,
                        onCheckedChange = null,
                    )
                    Text(
                        text = toggleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm(selected, toggleState)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreferenceBooleanDialogPreview() {
    NotallyXTheme {
        PreferenceBooleanDialog(
            title = "Always show search bar",
            message = "When enabled, search bar will always be visible",
            initialValue = true,
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreferenceEnumWithToggleDialogPreview() {
    NotallyXTheme {
        PreferenceEnumWithToggleDialog(
            title = "Theme",
            items =
                listOf(
                    SelectionItem("LIGHT", "Light"),
                    SelectionItem("DARK", "Dark"),
                    SelectionItem("SYSTEM", "Follow System"),
                ),
            selectedItem = "SYSTEM",
            toggleLabel = "Use dynamic colors",
            initialToggleValue = true,
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
