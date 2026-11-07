package com.philkes.notallyx.feature.main.routes.deleted

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel.Companion.CURRENT_LABEL_EMPTY
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeletedRoute(
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    deletedViewModel: DeletedViewModel = koinViewModel(),
    notallyFragmentModel: NotesFragmentViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyFragmentModel.preferences,
) {
    val items by deletedViewModel.deletedNotes.collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyFragmentModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)
    val showConfirmDeleteAllDialog by
        deletedViewModel.showConfirmDeleteAllDialog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        notallyFragmentModel.setFolder(Folder.DELETED)
        notallyFragmentModel.currentLabel = CURRENT_LABEL_EMPTY
    }

    Box(modifier = modifier.fillMaxSize()) {
        NotesGridScreen(
            items = items,
            isGrid = notesView == NotesView.GRID,
            selectedNoteIds = selectedIds,
            displayConfig = displayConfig,
            backgroundResId = R.drawable.delete,
            imageRoot = notallyFragmentModel.imageRoot,
            onNoteClick = onNoteClick,
            onNoteLongClick = onNoteLongClick,
            onReminderClick = { note, _ -> onReminderClick(note) },
        )

        if (showConfirmDeleteAllDialog) {
            ConfirmDialog(
                message = stringResource(R.string.delete_all_notes),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = { deletedViewModel.dismissDeleteAllDialog() },
                onConfirm = { deletedViewModel.confirmDeleteAll() },
            )
        }
    }
}
