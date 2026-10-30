package com.philkes.notallyx.feature.main.routes.labels

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

data class UnlabeledUiState(
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val isLoading: Boolean = false,
)

sealed interface UnlabeledAction {
    data class NoteClicked(val note: BaseNote, val position: Int) : UnlabeledAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : UnlabeledAction

    data class ReminderClicked(val note: BaseNote) : UnlabeledAction
}

sealed interface UnlabeledSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : UnlabeledSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : UnlabeledSideEffect

    data class OpenReminder(val note: BaseNote) : UnlabeledSideEffect
}
