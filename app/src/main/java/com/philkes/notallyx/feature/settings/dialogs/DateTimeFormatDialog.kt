package com.philkes.notallyx.feature.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeFormatDialog(
    initialDateFormat: DateFormat,
    initialTimeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirm: (DateFormat, TimeFormat) -> Unit,
) {
    val context = LocalContext.current
    var selectedDateFormat by remember { mutableStateOf(initialDateFormat) }
    var selectedTimeFormat by remember { mutableStateOf(initialTimeFormat) }

    var dateExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }
    println("DEBUG_DIALOG: DateTimeFormatDialog composing!")

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.date_format)) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = dateExpanded,
                    onExpandedChange = { dateExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedDateFormat.getText(context),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.date)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = dateExpanded,
                        onDismissRequest = { dateExpanded = false },
                    ) {
                        DateFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.getText(context)) },
                                onClick = {
                                    selectedDateFormat = format
                                    dateExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = timeExpanded,
                    onExpandedChange = { timeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTimeFormat.getText(context),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.time)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = timeExpanded,
                        onDismissRequest = { timeExpanded = false },
                    ) {
                        TimeFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.getText(context)) },
                                onClick = {
                                    selectedTimeFormat = format
                                    timeExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedDateFormat, selectedTimeFormat)
                    onDismissRequest()
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
private fun DateTimeFormatDialogPreview() {
    NotallyXTheme {
        DateTimeFormatDialog(
            initialDateFormat = DateFormat.DD_MM_YY_GER,
            initialTimeFormat = TimeFormat.TWENTY_FOUR_H,
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
