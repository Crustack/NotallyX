package com.philkes.notallyx.feature.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.cancel
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.extensions.formatDate
import com.philkes.notallyx.date
import com.philkes.notallyx.date_format
import com.philkes.notallyx.none
import com.philkes.notallyx.save
import com.philkes.notallyx.time
import java.util.Date
import org.jetbrains.compose.resources.stringResource

@Composable
fun DateFormat.displayLabel(): String =
    when (this) {
        DateFormat.NONE -> stringResource(Res.string.none)
        else ->
            Date(System.currentTimeMillis() - 86400000L).formatDate(this, TimeFormat.NONE) +
                textHint
    }

@Composable
fun TimeFormat.displayLabel(): String =
    when (this) {
        TimeFormat.NONE -> stringResource(Res.string.none)
        else ->
            Date(System.currentTimeMillis() - 86400000L)
                .formatDate(dateFormat = DateFormat.NONE, timeFormat = this)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeFormatDialog(
    initialDateFormat: DateFormat,
    initialTimeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirm: (DateFormat, TimeFormat) -> Unit,
) {
    var selectedDateFormat by remember { mutableStateOf(initialDateFormat) }
    var selectedTimeFormat by remember { mutableStateOf(initialTimeFormat) }

    var dateExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(Res.string.date_format)) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = dateExpanded,
                    onExpandedChange = { dateExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedDateFormat.displayLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.date)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier =
                            Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true,
                                )
                                .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = dateExpanded,
                        onDismissRequest = { dateExpanded = false },
                    ) {
                        DateFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                modifier =
                                    Modifier.testTag(
                                        TestTags.DATE_FORMAT_ITEM_PREFIX + format.name
                                    ),
                                text = { Text(format.displayLabel()) },
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
                        value = selectedTimeFormat.displayLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.time)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier =
                            Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true,
                                )
                                .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = timeExpanded,
                        onDismissRequest = { timeExpanded = false },
                    ) {
                        TimeFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                modifier =
                                    Modifier.testTag(
                                        TestTags.TIME_FORMAT_ITEM_PREFIX + format.name
                                    ),
                                text = { Text(format.displayLabel()) },
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
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
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
