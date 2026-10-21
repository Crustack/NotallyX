package com.philkes.notallyx.feature.main.routes.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel.Companion.CURRENT_LABEL_EMPTY
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotesRoute(
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    notesViewModel: NotesViewModel = koinViewModel(),
    notallyFragmentModel: NotesFragmentViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyFragmentModel.preferences,
) {
    val items by notesViewModel.baseNotes.collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyFragmentModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)

    LaunchedEffect(Unit) {
        notallyFragmentModel.setFolder(Folder.NOTES)
        notallyFragmentModel.currentLabel = CURRENT_LABEL_EMPTY
    }

    NotesGridScreen(
        items = items,
        isGrid = notesView == NotesView.GRID,
        selectedNoteIds = selectedIds,
        displayConfig = displayConfig,
        backgroundResId = R.drawable.notebook,
        imageRoot = notallyFragmentModel.imageRoot,
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = { note, _ -> onReminderClick(note) },
        modifier = modifier,
    )
}
