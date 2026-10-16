package com.philkes.notallyx.feature.reminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification
import java.util.Calendar

enum class FilterOptions {
    UPCOMING,
    ELAPSED,
    ALL,
}

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

@Composable
fun RemindersScreen(
    notes: List<BaseNote>,
    selectedNoteIds: Set<Long>,
    isGrid: Boolean,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int) -> Unit,
    onReminderClick: (BaseNote, Int) -> Unit,
    initialFilterMode: FilterOptions = FilterOptions.UPCOMING,
) {
    val context = LocalContext.current
    var filterMode by remember { mutableStateOf(initialFilterMode) }
    var currentItems by remember { mutableStateOf<List<Item>>(emptyList()) }

    LaunchedEffect(notes, filterMode) {
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
        currentItems = context.createItemsFromNotes(filtered)
    }

    NotesGridScreen(
        items = currentItems,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.reminders)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.cancel),
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
                    items(reminders) { reminder ->
                        NoteReminderRow(
                            title = reminder.dateTime.format(dateFormat, timeFormat),
                            nextNotificationText = "",
                            remindersCountText = "",
                            onClick = { onEditReminder(reminder) },
                        )
                    }
                }
            }

            when (dialogState) {
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
                else -> {}
            }
        }
    }
}
