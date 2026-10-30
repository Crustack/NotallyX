package com.philkes.notallyx.feature.main.routes.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
fun NotesScreen(
    state: NotesUiState,
    onAction: (NotesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundResId = R.drawable.notebook,
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(NotesAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(NotesAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(NotesAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun NotesScreenPreview() {
    NotallyXTheme {
        NotesScreen(
            state =
                NotesUiState(
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                ),
            onAction = {},
        )
    }
}
