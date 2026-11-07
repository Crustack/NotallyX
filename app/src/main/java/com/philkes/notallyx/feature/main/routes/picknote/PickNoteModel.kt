package com.philkes.notallyx.feature.main.routes.picknote

import com.philkes.notallyx.data.model.BaseNote

data class PickNoteUiState(
    val notes: List<BaseNote> = emptyList(),
    val isGrid: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface PickNoteAction {
    data class Init(val excludeNoteId: Long?) : PickNoteAction

    data class NoteClicked(val note: BaseNote) : PickNoteAction

    data object BackClicked : PickNoteAction
}

sealed interface PickNoteSideEffect {
    data class NotePicked(val note: BaseNote) : PickNoteSideEffect

    data object NavigateBack : PickNoteSideEffect
}
