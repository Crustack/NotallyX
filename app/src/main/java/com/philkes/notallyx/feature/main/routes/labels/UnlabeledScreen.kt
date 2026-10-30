package com.philkes.notallyx.feature.main.routes.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
fun UnlabeledScreen(
    state: UnlabeledUiState,
    onAction: (UnlabeledAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundResId = R.drawable.label_off,
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(UnlabeledAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(UnlabeledAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(UnlabeledAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun UnlabeledScreenPreview() {
    NotallyXTheme {
        UnlabeledScreen(
            state =
                UnlabeledUiState(
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                ),
            onAction = {},
        )
    }
}
