package com.philkes.notallyx.feature.main.routes.notes

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

data class NotesUiState(
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val isLoading: Boolean = false,
)

sealed interface NotesAction {
    data class NoteClicked(val note: BaseNote, val position: Int) : NotesAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : NotesAction

    data class ReminderClicked(val note: BaseNote) : NotesAction
}

sealed interface NotesSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : NotesSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : NotesSideEffect

    data class OpenReminder(val note: BaseNote) : NotesSideEffect
}
