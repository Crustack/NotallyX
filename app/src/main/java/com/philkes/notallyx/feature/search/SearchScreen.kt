package com.philkes.notallyx.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.main.NotesGridScreen

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
        dateFormat = dateFormat,
        timeFormat = timeFormat,
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
