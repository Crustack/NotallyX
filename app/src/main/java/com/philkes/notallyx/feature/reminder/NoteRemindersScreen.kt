package com.philkes.notallyx.feature.reminder

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat as PrefDateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.toRepetitionText
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Screen for displaying reminders of a single note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteRemindersScreen(
    state: NoteRemindersUiState,
    onAction: (NoteRemindersAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.reminders)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(NoteRemindersAction.AddReminderClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = stringResource(R.string.add_reminder),
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (state.reminders.isEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.notifications),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = state.reminders,
                        key = { reminder -> reminder.id },
                    ) { reminder ->
                        NoteReminderRow(
                            dateTimeText =
                                reminder.dateTime.format(
                                    state.dateFormat,
                                    state.timeFormat,
                                    ensureFullFormat = true,
                                ),
                            repetitionText = reminder.toRepetitionText(context),
                            onEdit = {
                                onAction(NoteRemindersAction.EditReminderClicked(reminder))
                            },
                            onDelete = {
                                onAction(NoteRemindersAction.RequestDeleteReminder(reminder))
                            },
                        )
                    }
                }
            }

            NoteRemindersDialogs(
                dialogState = state.activeDialog,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun NoteRemindersDialogs(
    dialogState: NoteRemindersDialogState?,
    onAction: (NoteRemindersAction) -> Unit,
) {
    val context = LocalContext.current

    when (dialogState) {
        is NoteRemindersDialogState.DatePicker -> {
            ReminderDatePickerDialog(
                initialDateMillis = dialogState.initialDateMillis,
                onDismissRequest = { onAction(NoteRemindersAction.DismissDialog) },
                onDateSelected = { year: Int, month: Int, day: Int ->
                    onAction(
                        NoteRemindersAction.DateSelected(
                            year,
                            month,
                            day,
                            dialogState.reminder,
                            dialogState.calendar,
                        )
                    )
                },
            )
        }
        is NoteRemindersDialogState.TimePicker -> {
            ReminderTimePickerDialog(
                initialHour = dialogState.calendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = dialogState.calendar.get(Calendar.MINUTE),
                is24Hour = DateFormat.is24HourFormat(context),
                onDismissRequest = { onAction(NoteRemindersAction.DismissDialog) },
                onBack = { onAction(NoteRemindersAction.TimePickerBack) },
                onTimeSelected = { hour: Int, minute: Int ->
                    onAction(
                        NoteRemindersAction.TimeSelected(
                            hour,
                            minute,
                            dialogState.reminder,
                            dialogState.calendar,
                        )
                    )
                },
            )
        }
        is NoteRemindersDialogState.ConfirmDelete -> {
            ConfirmDialog(
                message = stringResource(R.string.delete),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = { onAction(NoteRemindersAction.DismissDialog) },
                onConfirm = {
                    onAction(NoteRemindersAction.ConfirmDeleteReminder(dialogState.reminder))
                },
            )
        }
        is NoteRemindersDialogState.Repetition -> {
            ReminderRepetitionDialog(
                initialOption = dialogState.initialOption,
                onDismissRequest = { onAction(NoteRemindersAction.DismissDialog) },
                onOptionSelected = { option ->
                    onAction(
                        NoteRemindersAction.RepetitionOptionSelected(
                            option,
                            dialogState.reminder,
                            dialogState.calendar,
                        )
                    )
                },
            )
        }
        is NoteRemindersDialogState.MonthlyAdvanced -> {
            val items =
                dialogState.options.mapIndexed { index, option -> SelectionItem(index, option) }
            SingleSelectionDialog(
                title = stringResource(R.string.monthly),
                items = items,
                selectedItem = dialogState.selectedIndex,
                onDismissRequest = {
                    onAction(
                        NoteRemindersAction.RepetitionOptionSelected(
                            RepetitionOption.CUSTOM,
                            dialogState.reminder,
                            dialogState.calendar,
                        )
                    )
                },
                onConfirm = { which ->
                    onAction(
                        NoteRemindersAction.MonthlyAdvancedSelected(
                            which = which,
                            dayOfWeek = dialogState.dayOfWeek,
                            reminder = dialogState.reminder,
                            calendar = dialogState.calendar,
                        )
                    )
                },
                dismissTextResId = R.string.back,
            )
        }
        is NoteRemindersDialogState.CustomRepetition -> {
            CustomRepetitionDialog(
                initialValue = dialogState.initialValue,
                initialTimeUnit = dialogState.initialUnit,
                onDismissRequest = {
                    onAction(
                        NoteRemindersAction.RepetitionOptionSelected(
                            RepetitionOption.CUSTOM,
                            dialogState.reminder,
                            dialogState.calendar,
                        )
                    )
                },
                onConfirm = { valInt, unitEnum ->
                    onAction(
                        NoteRemindersAction.CustomRepetitionSelected(
                            value = valInt,
                            timeUnit = unitEnum,
                            reminder = dialogState.reminder,
                            calendar = dialogState.calendar,
                        )
                    )
                },
            )
        }
        is NoteRemindersDialogState.PermissionRationale -> {
            ConfirmDialog(
                message = stringResource(dialogState.messageResId),
                positiveButtonText = stringResource(R.string.continue_),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { onAction(NoteRemindersAction.DismissDialog) },
                onConfirm = {
                    onAction(
                        NoteRemindersAction.ConfirmPermissionRationale(dialogState.permissionType)
                    )
                },
            )
        }
        null -> {}
    }
}

@Composable
internal fun ReminderDatePickerDialog(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    DatePickerDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalendar.timeInMillis = millis
                        onDateSelected(
                            utcCalendar.get(Calendar.YEAR),
                            utcCalendar.get(Calendar.MONTH),
                            utcCalendar.get(Calendar.DAY_OF_MONTH),
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.next))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismissRequest: () -> Unit,
    onBack: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = is24Hour,
        )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
                Text(stringResource(R.string.next))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NoteRemindersScreenPreview() {
    NotallyXTheme {
        NoteRemindersScreen(
            state =
                NoteRemindersUiState(
                    reminders =
                        listOf(
                            Reminder(
                                id = 1L,
                                dateTime = Date(),
                                repetition = Repetition(1, RepetitionTimeUnit.DAYS),
                            ),
                            Reminder(
                                id = 2L,
                                dateTime = Date(System.currentTimeMillis() + 86400000L),
                                repetition = null,
                            ),
                        ),
                    dateFormat = PrefDateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                ),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReminderDatePickerDialogPreview() {
    NotallyXTheme {
        ReminderDatePickerDialog(
            initialDateMillis = System.currentTimeMillis(),
            onDismissRequest = {},
            onDateSelected = { _: Int, _: Int, _: Int -> },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReminderTimePickerDialogPreview() {
    NotallyXTheme {
        ReminderTimePickerDialog(
            initialHour = 9,
            initialMinute = 0,
            is24Hour = true,
            onDismissRequest = {},
            onBack = {},
            onTimeSelected = { _: Int, _: Int -> },
        )
    }
}
