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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.data.model.extensions.toRepetitionText
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

enum class FilterOptions {
    UPCOMING,
    ELAPSED,
    ALL,
}

internal sealed interface ReminderDialogState {
    data class DatePicker(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialDateMillis: Long,
    ) : ReminderDialogState

    data class TimePicker(
        val reminder: Reminder?,
        val calendar: Calendar,
    ) : ReminderDialogState

    data class Repetition(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialOption: RepetitionOption,
    ) : ReminderDialogState

    data class MonthlyAdvanced(
        val reminder: Reminder?,
        val calendar: Calendar,
        val options: List<String>,
        val selectedIndex: Int,
        val dayOfWeek: Int,
    ) : ReminderDialogState

    data class CustomRepetition(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialValue: String,
        val initialUnit: TimeUnit,
    ) : ReminderDialogState

    data class ConfirmDelete(
        val reminder: Reminder,
        val formattedDateTime: String,
        val repetitionText: String,
    ) : ReminderDialogState

    data class Rationale(
        val message: String,
        val onConfirm: () -> Unit,
    ) : ReminderDialogState
}

/** Screen to show notes with their reminders */
@Composable
fun RemindersScreen(
    notes: List<BaseNote>,
    selectedNoteIds: Set<Long>,
    isGrid: Boolean,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote, Int) -> Unit,
    modifier: Modifier = Modifier,
    initialFilterMode: FilterOptions = FilterOptions.UPCOMING,
) {
    val context = LocalContext.current
    var filterMode by remember { mutableStateOf(initialFilterMode) }

    val currentItems =
        remember(notes, filterMode) {
            val filtered =
                when (filterMode) {
                    FilterOptions.ALL ->
                        notes.sortedWith { n1, n2 ->
                            val sort =
                                n1.compareNextNotification(n2).takeIf { it != 0 }
                                    ?: n1.compareLastNotification(n2)
                            -1 * sort
                        }
                    FilterOptions.UPCOMING ->
                        notes
                            .filter { it.reminders.hasAnyUpcomingNotifications() }
                            .sortedWith { n1, n2 -> n1.compareNextNotification(n2) }
                    FilterOptions.ELAPSED ->
                        notes
                            .filter { !it.reminders.hasAnyUpcomingNotifications() }
                            .sortedWith { n1, n2 -> -1 * n1.compareLastNotification(n2) }
                }
            context.createItemsFromNotes(filtered)
        }

    NotesGridScreen(
        items = currentItems,
        modifier = modifier,
        isGrid = isGrid,
        selectedNoteIds = selectedNoteIds,
        displayConfig = displayConfig,
        backgroundResId = R.drawable.notifications,
        selectedFilterOption = filterMode,
        onFilterOptionSelected = { filterMode = it },
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = onReminderClick,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemindersScreenPreview() {
    NotallyXTheme {
        RemindersScreen(
            notes = listOf(SampleNotes.RICH_TEXT_NOTE),
            selectedNoteIds = setOf(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
        )
    }
}

/** Screen for displaying reminders of a single note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteRemindersScreen(
    reminders: List<Reminder>,
    dateFormat: PrefDateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    dialogState: ReminderDialogState? = null,
    onAddReminderClick: () -> Unit = {},
    onEditReminder: (Reminder) -> Unit = {},
    onDeleteReminder: (Reminder) -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onDateSelected: (Reminder?, Calendar, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onTimeSelected: (Reminder?, Calendar, Int, Int) -> Unit = { _, _, _, _ -> },
    onTimePickerBack: (Reminder?, Calendar) -> Unit = { _, _ -> },
    onRepetitionOptionSelected: (Reminder?, Calendar, RepetitionOption) -> Unit = { _, _, _ -> },
    onMonthlyAdvancedSelected: (Reminder?, Calendar, Int, Int) -> Unit = { _, _, _, _ -> },
    onCustomRepetitionSelected: (Reminder?, Calendar, Int, TimeUnit) -> Unit = { _, _, _, _ -> },
    onConfirmDeleteReminder: (Reminder) -> Unit = {},
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
                onClick = onAddReminderClick,
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
            if (reminders.isEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.notifications),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = reminders,
                        key = { reminder -> reminder.id },
                    ) { reminder ->
                        NoteReminderRow(
                            dateTimeText =
                                reminder.dateTime.format(
                                    dateFormat,
                                    timeFormat,
                                    ensureFullFormat = true,
                                ),
                            repetitionText = reminder.toRepetitionText(context),
                            onEdit = { onEditReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) },
                        )
                    }
                }
            }

            NoteRemindersDialogs(
                dialogState = dialogState,
                context = context,
                onDismissDialog = onDismissDialog,
                onDateSelected = onDateSelected,
                onTimeSelected = onTimeSelected,
                onTimePickerBack = onTimePickerBack,
                onRepetitionOptionSelected = onRepetitionOptionSelected,
                onMonthlyAdvancedSelected = onMonthlyAdvancedSelected,
                onCustomRepetitionSelected = onCustomRepetitionSelected,
                onConfirmDeleteReminder = onConfirmDeleteReminder,
            )
        }
    }
}

@Composable
private fun NoteRemindersDialogs(
    dialogState: ReminderDialogState?,
    context: android.content.Context,
    onDismissDialog: () -> Unit,
    onDateSelected: (Reminder?, Calendar, Int, Int, Int) -> Unit,
    onTimeSelected: (Reminder?, Calendar, Int, Int) -> Unit,
    onTimePickerBack: (Reminder?, Calendar) -> Unit,
    onRepetitionOptionSelected: (Reminder?, Calendar, RepetitionOption) -> Unit,
    onMonthlyAdvancedSelected: (Reminder?, Calendar, Int, Int) -> Unit,
    onCustomRepetitionSelected: (Reminder?, Calendar, Int, TimeUnit) -> Unit,
    onConfirmDeleteReminder: (Reminder) -> Unit,
) {
    when (dialogState) {
        is ReminderDialogState.DatePicker -> {
            ReminderDatePickerDialog(
                initialDateMillis = dialogState.initialDateMillis,
                onDismissRequest = onDismissDialog,
                onDateSelected = { year, month, day ->
                    onDateSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        year,
                        month,
                        day,
                    )
                },
            )
        }
        is ReminderDialogState.TimePicker -> {
            ReminderTimePickerDialog(
                initialHour = dialogState.calendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = dialogState.calendar.get(Calendar.MINUTE),
                is24Hour = DateFormat.is24HourFormat(context),
                onDismissRequest = onDismissDialog,
                onBack = {
                    onTimePickerBack(
                        dialogState.reminder,
                        dialogState.calendar,
                    )
                },
                onTimeSelected = { hour, minute ->
                    onTimeSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        hour,
                        minute,
                    )
                },
            )
        }
        is ReminderDialogState.ConfirmDelete -> {
            ConfirmDialog(
                message = stringResource(R.string.delete),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = onDismissDialog,
                onConfirm = { onConfirmDeleteReminder(dialogState.reminder) },
            )
        }
        is ReminderDialogState.Repetition -> {
            ReminderRepetitionDialog(
                initialOption = dialogState.initialOption,
                onDismissRequest = onDismissDialog,
                onOptionSelected = { option ->
                    onRepetitionOptionSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        option,
                    )
                },
            )
        }
        is ReminderDialogState.MonthlyAdvanced -> {
            val items =
                dialogState.options.mapIndexed { index, option -> SelectionItem(index, option) }
            SingleSelectionDialog(
                title = stringResource(R.string.monthly),
                items = items,
                selectedItem = dialogState.selectedIndex,
                onDismissRequest = {
                    onRepetitionOptionSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        RepetitionOption.CUSTOM,
                    )
                },
                onConfirm = { which ->
                    onMonthlyAdvancedSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        which,
                        dialogState.dayOfWeek,
                    )
                },
                dismissTextResId = R.string.back,
            )
        }
        is ReminderDialogState.CustomRepetition -> {
            CustomRepetitionDialog(
                initialValue = dialogState.initialValue,
                initialTimeUnit = dialogState.initialUnit,
                onDismissRequest = {
                    onRepetitionOptionSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        RepetitionOption.CUSTOM,
                    )
                },
                onConfirm = { valInt, unitEnum ->
                    onCustomRepetitionSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        valInt,
                        unitEnum,
                    )
                },
            )
        }
        is ReminderDialogState.Rationale -> {
            ConfirmDialog(
                message = dialogState.message,
                positiveButtonText = stringResource(R.string.continue_),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = onDismissDialog,
                onConfirm = {
                    onDismissDialog()
                    dialogState.onConfirm()
                },
            )
        }
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            onDateSelected = { _, _, _ -> },
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
            onTimeSelected = { _, _ -> },
        )
    }
}
