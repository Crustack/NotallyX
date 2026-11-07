package com.philkes.notallyx.feature.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification

/** Overloaded screen to show notes with their reminders preserving backwards compatibility. */
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

    RemindersScreen(
        state =
            RemindersUiState(
                items = currentItems,
                selectedIds = selectedNoteIds,
                isGrid = isGrid,
                displayConfig = displayConfig,
                filterMode = filterMode,
                isNotificationPermissionGranted = true,
                canScheduleExactAlarms = true,
            ),
        onAction = { action ->
            when (action) {
                is RemindersAction.FilterOptionSelected -> filterMode = action.filterOption
                is RemindersAction.NoteClicked -> onNoteClick(action.note, action.position)
                is RemindersAction.NoteLongClicked ->
                    onNoteLongClick(action.note, action.position, action.items)
                is RemindersAction.ReminderClicked -> onReminderClick(action.note, 0)
                RemindersAction.BackClicked -> {}
            }
        },
        modifier = modifier,
    )
}
