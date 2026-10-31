package com.philkes.notallyx.feature.main.routes.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
fun DisplayLabelScreen(
    state: DisplayLabelUiState,
    onAction: (DisplayLabelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundResId = R.drawable.label,
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(DisplayLabelAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(DisplayLabelAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(DisplayLabelAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DisplayLabelScreenPreview() {
    NotallyXTheme {
        DisplayLabelScreen(
            state =
                DisplayLabelUiState(
                    label = "Personal",
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                ),
            onAction = {},
        )
    }
}
