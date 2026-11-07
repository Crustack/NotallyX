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
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchRoute(
    initialFolder: Folder?,
    initialLabel: String?,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    notallyRouteModel: NotesRouteViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyRouteModel.preferences,
) {
    val searchKeyword by notallyRouteModel.keywordFlow.collectAsStateWithLifecycle()
    val selectedFolder by notallyRouteModel.folder.collectAsStateWithLifecycle()
    val searchResults = notallyRouteModel.searchResults
    val items by
        (searchResults?.results ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val selectedIds by
        notallyRouteModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
    val notesView by
        preferences.notesView.flow.collectAsStateWithLifecycle(preferences.notesView.value)

    LaunchedEffect(initialFolder, initialLabel) {
        notallyRouteModel.currentLabel = initialLabel
        if (initialLabel.isNullOrEmpty() && initialFolder != null) {
            notallyRouteModel.setFolder(initialFolder)
        }
    }

    NotesGridScreen(
        items = items,
        isGrid = notesView == NotesView.GRID,
        selectedNoteIds = selectedIds,
        displayConfig = displayConfig,
        backgroundResId = R.drawable.search,
        imageRoot = notallyRouteModel.imageRoot,
        showSearchBar = true,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = { notallyRouteModel.keyword = it },
        selectedFolder = selectedFolder,
        onFolderSelected = { notallyRouteModel.setFolder(it) },
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = { note, _ -> onReminderClick(note) },
        modifier = modifier,
    )
}
