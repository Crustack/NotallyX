package com.philkes.notallyx.feature.main.routes.picknote

import com.philkes.notallyx.data.model.BaseNote
import java.io.File

data class PickNoteUiState(
    val notes: List<BaseNote> = emptyList(),
    val isGrid: Boolean = false,
    val isLoading: Boolean = false,
    val imageRoot: File? = null,
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
