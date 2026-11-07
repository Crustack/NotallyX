package com.philkes.notallyx.feature.reminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

internal enum class RepetitionOption(val labelResId: Int) {
    NONE(R.string.none),
    DAILY(R.string.daily),
    WEEKLY(R.string.weekly),
    MONTHLY(R.string.monthly),
    YEARLY(R.string.yearly),
    CUSTOM(R.string.custom),
}

internal enum class TimeUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

@Composable
internal fun ReminderRepetitionDialog(
    modifier: Modifier = Modifier,
    initialOption: RepetitionOption = RepetitionOption.NONE,
    onDismissRequest: () -> Unit,
    onOptionSelected: (RepetitionOption) -> Unit,
) {
    var selectedOption by remember { mutableStateOf(initialOption) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.repetition)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                RepetitionOption.entries.forEach { option ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(44.dp)
                                .selectable(
                                    selected = (option == selectedOption),
                                    onClick = { selectedOption = option },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null,
                        )
                        Text(
                            text = stringResource(option.labelResId),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOptionSelected(selectedOption) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun CustomRepetitionDialog(
    modifier: Modifier = Modifier,
    initialValue: String = "1",
    initialTimeUnit: TimeUnit = TimeUnit.WEEKS,
    onDismissRequest: () -> Unit,
    onConfirm: (value: Int, timeUnit: TimeUnit) -> Unit,
) {
    var valueText by remember { mutableStateOf(initialValue) }
    var selectedUnit by remember { mutableStateOf(initialTimeUnit) }

    val units =
        listOf(
            TimeUnit.MINUTES to R.string.minutes,
            TimeUnit.HOURS to R.string.hours,
            TimeUnit.DAYS to null, // uses plural
            TimeUnit.WEEKS to R.string.weeks,
            TimeUnit.MONTHS to R.string.months,
            TimeUnit.YEARS to R.string.years,
        )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(stringResource(R.string.repetition_value_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    units.forEach { (unit, labelResId) ->
                        val unitLabel =
                            if (labelResId != null) {
                                stringResource(labelResId)
                            } else {
                                pluralStringResource(R.plurals.days, 10)
                            }

                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (unit == selectedUnit),
                                        onClick = { selectedUnit = unit },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (unit == selectedUnit),
                                onClick = null,
                            )
                            Text(
                                text = unitLabel,
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
                    val parsedVal = valueText.toIntOrNull() ?: 1
                    onConfirm(parsedVal, selectedUnit)
                },
                enabled = valueText.toIntOrNull() != null && valueText.toInt() > 0,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun ReminderRepetitionDialogPreview() {
    NotallyXTheme {
        ReminderRepetitionDialog(
            initialOption = RepetitionOption.DAILY,
            onDismissRequest = {},
            onOptionSelected = {},
        )
    }
}

@Preview
@Composable
private fun CustomRepetitionDialogPreview() {
    NotallyXTheme {
        CustomRepetitionDialog(
            initialValue = "2",
            initialTimeUnit = TimeUnit.WEEKS,
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
