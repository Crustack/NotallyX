package com.philkes.notallyx.feature.editor

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.feature.editor.screens.SearchState

data class EditTextPlainUiState(
    val title: String = "",
    val bodyValue: TextFieldValue = TextFieldValue(),
    val fileUri: Uri? = null,
    val isLoading: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val color: ColorString = BaseNote.COLOR_DEFAULT,
    val searchState: SearchState = SearchState(),
    val activeDialog: EditTextPlainDialogState? = null,
)

sealed interface EditTextPlainDialogState {
    data class Error(
        @StringRes val titleResId: Int,
        val message: String?,
    ) : EditTextPlainDialogState
}

sealed interface EditTextPlainAction {
    data class Init(val fileUriString: String?) : EditTextPlainAction

    data class TitleChanged(val title: String) : EditTextPlainAction

    data class BodyChanged(val bodyValue: TextFieldValue) : EditTextPlainAction

    data class SearchQueryChanged(val query: String) : EditTextPlainAction

    data object NextSearchResult : EditTextPlainAction

    data object PrevSearchResult : EditTextPlainAction

    data object CloseSearch : EditTextPlainAction

    data object StartSearch : EditTextPlainAction

    data object Undo : EditTextPlainAction

    data object Redo : EditTextPlainAction

    data object Save : EditTextPlainAction

    data class SaveToUri(val uri: Uri) : EditTextPlainAction

    data object RequestSaveAs : EditTextPlainAction

    data object ConvertToNote : EditTextPlainAction

    data object DismissDialog : EditTextPlainAction

    data object BackClicked : EditTextPlainAction
}

sealed interface EditTextPlainSideEffect {
    data class ShowToast(@StringRes val messageResId: Int) : EditTextPlainSideEffect

    data class LaunchSaveAs(val defaultFileName: String) : EditTextPlainSideEffect

    data class NavigateToEditNote(val noteId: Long) : EditTextPlainSideEffect

    data object NavigateBack : EditTextPlainSideEffect
}
