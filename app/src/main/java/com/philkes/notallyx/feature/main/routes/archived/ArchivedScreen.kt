package com.philkes.notallyx.feature.main.routes.archived

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
fun ArchivedScreen(
    state: ArchivedUiState,
    onAction: (ArchivedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundResId = R.drawable.archive,
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(ArchivedAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(ArchivedAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(ArchivedAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun ArchivedScreenPreview() {
    NotallyXTheme {
        ArchivedScreen(
            state =
                ArchivedUiState(
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                ),
            onAction = {},
        )
    }
}
