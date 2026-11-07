package com.philkes.notallyx.feature.main.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchRoute(
    initialFolder: Folder?,
    initialLabel: String?,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    notallyFragmentModel: NotesFragmentViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyFragmentModel.preferences,
) {
    val searchKeyword by notallyFragmentModel.keywordFlow.collectAsStateWithLifecycle()
    val selectedFolder by notallyFragmentModel.folder.collectAsStateWithLifecycle()
    val searchResults = notallyFragmentModel.searchResults
    val items by
        (searchResults?.results ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyFragmentModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)

    LaunchedEffect(initialFolder, initialLabel) {
        notallyFragmentModel.currentLabel = initialLabel
        if (initialLabel.isNullOrEmpty() && initialFolder != null) {
            notallyFragmentModel.setFolder(initialFolder)
        }
    }

    NotesGridScreen(
        items = items,
        isGrid = notesView == NotesView.GRID,
        selectedNoteIds = selectedIds,
        displayConfig = displayConfig,
        backgroundResId = R.drawable.search,
        imageRoot = notallyFragmentModel.imageRoot,
        showSearchBar = true,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = { notallyFragmentModel.keyword = it },
        selectedFolder = selectedFolder,
        onFolderSelected = { notallyFragmentModel.setFolder(it) },
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = { note, _ -> onReminderClick(note) },
        modifier = modifier,
    )
}
