package com.philkes.notallyx.feature.main.routes.deleted

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

sealed interface DeletedDialogState {
    data object ConfirmDeleteAll : DeletedDialogState
}

data class DeletedUiState(
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val isLoading: Boolean = false,
    val activeDialog: DeletedDialogState? = null,
)

sealed interface DeletedAction {
    data class NoteClicked(val note: BaseNote, val position: Int) : DeletedAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : DeletedAction

    data class ReminderClicked(val note: BaseNote) : DeletedAction

    data object RequestDeleteAll : DeletedAction

    data object ConfirmDeleteAll : DeletedAction

    data object DismissDialog : DeletedAction
}

sealed interface DeletedSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : DeletedSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : DeletedSideEffect

    data class OpenReminder(val note: BaseNote) : DeletedSideEffect
}
