package com.philkes.notallyx.feature.main.routes.deleted

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
fun DeletedScreen(
    state: DeletedUiState,
    onAction: (DeletedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NotesGridScreen(
            items = state.items,
            isGrid = state.isGrid,
            selectedNoteIds = state.selectedIds,
            displayConfig = state.displayConfig,
            backgroundResId = R.drawable.delete,
            isLoading = state.isLoading,
            onNoteClick = { note, pos -> onAction(DeletedAction.NoteClicked(note, pos)) },
            onNoteLongClick = { note, pos, items ->
                onAction(DeletedAction.NoteLongClicked(note, pos, items))
            },
            onReminderClick = { note, _ -> onAction(DeletedAction.ReminderClicked(note)) },
        )

        when (state.activeDialog) {
            is DeletedDialogState.ConfirmDeleteAll -> {
                ConfirmDialog(
                    message = stringResource(R.string.delete_all_notes),
                    positiveButtonText = stringResource(R.string.delete),
                    onDismissRequest = { onAction(DeletedAction.DismissDialog) },
                    onConfirm = { onAction(DeletedAction.ConfirmDeleteAll) },
                )
            }

            null -> Unit
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DeletedScreenPreview() {
    NotallyXTheme {
        DeletedScreen(
            state =
                DeletedUiState(
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                ),
            onAction = {},
        )
    }
}
