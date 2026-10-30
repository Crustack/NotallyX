package com.philkes.notallyx.feature.reminder

import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item

data class RemindersUiState(
    val items: List<Item> = emptyList(),
    val rawNotes: List<BaseNote> = emptyList(),
    val isGrid: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    val filterMode: FilterOptions = FilterOptions.UPCOMING,
    val isLoading: Boolean = false,
)

sealed interface RemindersAction {
    data class FilterOptionSelected(val filterOption: FilterOptions) : RemindersAction

    data class NoteClicked(val note: BaseNote, val position: Int) : RemindersAction

    data class NoteLongClicked(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : RemindersAction

    data class ReminderClicked(val note: BaseNote) : RemindersAction
}

sealed interface RemindersSideEffect {
    data class OpenNote(val note: BaseNote, val position: Int) : RemindersSideEffect

    data class SelectNote(
        val note: BaseNote,
        val position: Int,
        val items: List<Item>,
    ) : RemindersSideEffect

    data class OpenReminder(val note: BaseNote) : RemindersSideEffect
}
