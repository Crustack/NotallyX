package com.philkes.notallyx.feature.main.routes.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.reminder.RemindersScreen
import com.philkes.notallyx.feature.reminder.RemindersViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RemindersRoute(
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    remindersViewModel: RemindersViewModel = koinViewModel(),
    notallyRouteModel: NotesRouteViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyRouteModel.preferences,
) {
    val allReminderNotes by
        remindersViewModel.reminderNotes.collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyRouteModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)

    LaunchedEffect(Unit) { notallyRouteModel.currentLabel = CURRENT_LABEL_EMPTY }

    RemindersScreen(
        notes = allReminderNotes,
        selectedNoteIds = selectedIds,
        isGrid = notesView == NotesView.GRID,
        displayConfig = displayConfig,
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = { note, _ -> onReminderClick(note) },
    )
}
