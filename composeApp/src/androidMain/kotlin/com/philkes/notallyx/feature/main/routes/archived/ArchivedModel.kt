package com.philkes.notallyx.feature.main.routes.archived

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

data class ArchivedUiState(
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val isLoading: Boolean = false,
)

sealed interface ArchivedAction {
    data class NoteClicked(val note: BaseNote, val position: Int) : ArchivedAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : ArchivedAction

    data class ReminderClicked(val note: BaseNote) : ArchivedAction
}

sealed interface ArchivedSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : ArchivedSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : ArchivedSideEffect

    data class OpenReminder(val note: BaseNote) : ArchivedSideEffect
}
