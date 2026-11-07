package com.philkes.notallyx.feature.main.routes.picknote

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.PickNoteScreen as LegacyPickNoteScreen

@Composable
fun PickNoteScreen(
    state: PickNoteUiState,
    onAction: (PickNoteAction) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = { onAction(PickNoteAction.BackClicked) },
) {
    LegacyPickNoteScreen(
        notes = state.notes,
        isGrid = state.isGrid,
        onNoteClick = { onAction(PickNoteAction.NoteClicked(it)) },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PickNoteScreenPreview() {
    NotallyXTheme {
        PickNoteScreen(
            state = PickNoteUiState(notes = listOf(SampleNotes.LOREM_IPSUM_NOTE)),
            onAction = {},
        )
    }
}
