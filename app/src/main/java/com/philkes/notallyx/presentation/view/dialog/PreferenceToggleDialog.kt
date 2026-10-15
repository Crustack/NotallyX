package com.philkes.notallyx.presentation.view.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R

@Composable
fun PreferenceBooleanDialog(
    title: String,
    message: String? = null,
    initialValue: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var selectedValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
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
                    onConfirm(selectedValue)
                    onDismissRequest()
                },
                modifier = Modifier.testTag("button1"),
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
fun <T> PreferenceEnumWithToggleDialog(
    title: String,
    hint: String? = null,
    items: List<SelectionItem<T>>,
    selectedItem: T,
    toggleLabel: String,
    initialToggleValue: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (selectedItem: T, toggleValue: Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(selectedItem) }
    var toggleState by remember { mutableStateOf(initialToggleValue) }

    AlertDialog(
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = toggleState,
                        onCheckedChange = { toggleState = it },
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
                    onConfirm(selected, toggleState)
                    onDismissRequest()
                },
                modifier = Modifier.testTag("button1"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}
