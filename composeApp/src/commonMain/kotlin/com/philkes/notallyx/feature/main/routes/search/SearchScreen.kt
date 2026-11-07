package com.philkes.notallyx.feature.main.routes.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.search

@Composable
fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundDrawable = Res.drawable.search,
        imageRoot = state.imageRoot,
        showSearchBar = true,
        searchKeyword = state.searchKeyword,
        onSearchKeywordChange = { onAction(SearchAction.KeywordChanged(it)) },
        selectedFolder = state.selectedFolder,
        onFolderSelected = { onAction(SearchAction.FolderSelected(it)) },
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(SearchAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(SearchAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(SearchAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun SearchScreenPreview() {
    NotallyXTheme {
        SearchScreen(
            state =
                SearchUiState(
                    items = listOf(SampleNotes.LOREM_IPSUM_NOTE, SampleNotes.LIST_NOTE),
                    isGrid = true,
                    searchKeyword = "meeting",
                ),
            onAction = {},
        )
    }
}
