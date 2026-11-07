package com.philkes.notallyx.feature.main.routes.labels

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
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.labels.DisplayLabelViewModel
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DisplayLabelRoute(
    label: String,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayLabelViewModel: DisplayLabelViewModel = koinViewModel(),
    notallyRouteModel: NotesRouteViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyRouteModel.preferences,
) {
    val items by
        displayLabelViewModel.getNotesByLabel(label).collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyRouteModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)

    LaunchedEffect(label) {
        notallyRouteModel.setFolder(Folder.NOTES)
        notallyRouteModel.currentLabel = label
    }

    NotesGridScreen(
        items = items,
        isGrid = notesView == NotesView.GRID,
        selectedNoteIds = selectedIds,
        displayConfig = displayConfig,
        backgroundResId = R.drawable.label,
        imageRoot = notallyRouteModel.imageRoot,
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = { note, _ -> onReminderClick(note) },
        modifier = modifier,
    )
}
