package com.philkes.notallyx.feature.main.routes.labels

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

data class DisplayLabelUiState(
    val label: String = "",
    val items: List<Item> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val isLoading: Boolean = false,
)

sealed interface DisplayLabelAction {
    data class SetLabel(val label: String) : DisplayLabelAction

    data class NoteClicked(val note: BaseNote, val position: Int) : DisplayLabelAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : DisplayLabelAction

    data class ReminderClicked(val note: BaseNote) : DisplayLabelAction
}

sealed interface DisplayLabelSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : DisplayLabelSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : DisplayLabelSideEffect

    data class OpenReminder(val note: BaseNote) : DisplayLabelSideEffect
}
