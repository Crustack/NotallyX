package com.philkes.notallyx.feature.reminder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.extensions.toRepetitionText
import java.util.Calendar

internal sealed interface ReminderDialogState {
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
}

/** Composable replacing activity_reminders.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemindersScreen(
    reminders: List<Reminder>,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    dialogState: ReminderDialogState? = null,
    onAddReminderClick: () -> Unit = {},
    onEditReminder: (Reminder) -> Unit = {},
    onDeleteReminder: (Reminder) -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onRepetitionOptionSelected: (Reminder?, Calendar, RepetitionOption) -> Unit = { _, _, _ -> },
    onMonthlyAdvancedSelected: (Reminder?, Calendar, Int, Int) -> Unit = { _, _, _, _ -> },
    onCustomRepetitionSelected: (Reminder?, Calendar, Int, TimeUnit) -> Unit = { _, _, _, _ -> },
    onConfirmDeleteReminder: (Reminder) -> Unit = {},
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_reminders)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("Back"),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddReminderClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = stringResource(R.string.add_reminder),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(reminders, key = { it.id }) { reminder ->
                        val dateTimeText =
                            reminder.dateTime.format(
                                dateFormat,
                                timeFormat,
                                ensureFullFormat = true,
                            )
                        val repetitionText = reminder.toRepetitionText(context)
                        ReminderChipItem(
                            dateTimeText = dateTimeText,
                            repetitionText = repetitionText,
                            onEdit = { onEditReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    when (dialogState) {
        is ReminderDialogState.Repetition -> {
            ReminderRepetitionDialog(
                initialOption = dialogState.initialOption,
                onDismissRequest = onDismissDialog,
                onOptionSelected = { selected ->
                    onRepetitionOptionSelected(
                        dialogState.reminder,
                        dialogState.calendar,
                        selected,
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
        is ReminderDialogState.ConfirmDelete -> {
            ConfirmDialog(
                title = stringResource(R.string.delete_reminder_question),
                message = "${dialogState.formattedDateTime}\n${dialogState.repetitionText}",
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = onDismissDialog,
                onConfirm = { onConfirmDeleteReminder(dialogState.reminder) },
            )
        }
        null -> {}
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersScreenPreview() {
    NotallyXTheme {
        RemindersScreen(
            reminders = emptyList(),
            dateFormat = DateFormat.FULL,
            timeFormat = TimeFormat.AM_PM,
        )
    }
}
