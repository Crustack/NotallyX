package com.philkes.notallyx.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

/** Search screen composable wrapping NotesGridScreen for search functionality. */
@Composable
internal fun SearchScreen(
    items: List<Item>,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    searchKeyword: String = "",
    onSearchKeywordChange: (String) -> Unit = {},
    selectedFolder: Folder? = Folder.NOTES,
    onFolderSelected: (Folder) -> Unit = {},
    isLoading: Boolean = false,
    dateFormat: DateFormat = DateFormat.RELATIVE,
    timeFormat: TimeFormat = TimeFormat.AM_PM,
    onNoteClick: (BaseNote, Int) -> Unit = { _, _ -> },
    onNoteLongClick: (BaseNote, Int) -> Unit = { _, _ -> },
) {
    NotesGridScreen(
        items = items,
        modifier = modifier,
        isGrid = isGrid,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = onSearchKeywordChange,
        showSearchBar = true,
        selectedFolder = selectedFolder,
        onFolderSelected = onFolderSelected,
        backgroundResId = R.drawable.search,
        isLoading = isLoading,
        displayConfig = NoteCardDisplayConfig(dateFormat = dateFormat, timeFormat = timeFormat),
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    NotallyXTheme {
        SearchScreen(
            items = emptyList(),
            isGrid = true,
            searchKeyword = "Test",
        )
    }
}
