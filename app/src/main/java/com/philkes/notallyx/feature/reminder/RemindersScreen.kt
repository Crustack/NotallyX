package com.philkes.notallyx.feature.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification

enum class FilterOptions {
    UPCOMING,
    ELAPSED,
    ALL,
}

/** Screen to show notes with their reminders following MVI architecture. */
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onAction: (RemindersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        modifier = modifier,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundResId = R.drawable.notifications,
        selectedFilterOption = state.filterMode,
        isLoading = state.isLoading,
        onFilterOptionSelected = { onAction(RemindersAction.FilterOptionSelected(it)) },
        onNoteClick = { note, pos -> onAction(RemindersAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(RemindersAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(RemindersAction.ReminderClicked(note)) },
    )
}

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
            state =
                RemindersUiState(
                    items = listOf(SampleNotes.RICH_TEXT_NOTE),
                    selectedIds = setOf(),
                    isGrid = false,
                    displayConfig = NoteCardDisplayConfig(),
                ),
            onAction = {},
        )
    }
}
