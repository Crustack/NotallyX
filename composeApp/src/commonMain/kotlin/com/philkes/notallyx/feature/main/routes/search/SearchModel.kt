package com.philkes.notallyx.feature.main.routes.search

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import java.io.File

data class SearchUiState(
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val searchKeyword: String = "",
    val selectedFolder: Folder = Folder.NOTES,
    val currentLabel: String? = null,
    val imageRoot: File? = null,
    val isLoading: Boolean = false,
)

sealed interface SearchAction {
    data class Init(val folder: Folder?, val label: String?) : SearchAction

    data class KeywordChanged(val keyword: String) : SearchAction

    data class FolderSelected(val folder: Folder) : SearchAction

    data class NoteClicked(val note: BaseNote, val position: Int) : SearchAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : SearchAction

    data class ReminderClicked(val note: BaseNote) : SearchAction
}

sealed interface SearchSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : SearchSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : SearchSideEffect

    data class OpenReminder(val note: BaseNote) : SearchSideEffect
}
