package com.philkes.notallyx.feature.editor.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.Res
import com.philkes.notallyx.continue_
import com.philkes.notallyx.convert_to_text
import com.philkes.notallyx.convert_to_text_note
import com.philkes.notallyx.core.ui.components.parseNoteColor
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.error
import com.philkes.notallyx.feature.editor.EditTextPlainAction
import com.philkes.notallyx.feature.editor.EditTextPlainDialogState
import com.philkes.notallyx.feature.editor.EditTextPlainUiState
import com.philkes.notallyx.save
import com.philkes.notallyx.save_as
import com.philkes.notallyx.save_to_device
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditTextPlainScreen(
    state: EditTextPlainUiState,
    onAction: (EditTextPlainAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(EditTextPlainAction.BackClicked) },
) {
    val noteState =
        EditNoteState(
            title = state.title,
            bodyValue = state.bodyValue,
        )

    val backgroundColor = parseNoteColor(state.color)

    EditNoteScreen(
        noteState = noteState,
        searchState = state.searchState,
        backgroundColor = backgroundColor,
        canUndo = state.canUndo,
        canRedo = state.canRedo,
        changeHandlers =
            object : EditChangeHandlers {
                override fun onTitleChange(title: String) {
                    onAction(EditTextPlainAction.TitleChanged(title))
                }

                override fun onBodyValueChange(bodyValue: TextFieldValue) {
                    onAction(EditTextPlainAction.BodyChanged(bodyValue))
                }

                override fun onSearchQueryChange(query: String) {
                    onAction(EditTextPlainAction.SearchQueryChanged(query))
                }
            },
        clickHandlers =
            object : EditClickHandlers {
                override fun onBackClick() {
                    onNavigateBack()
                }

                override fun onMoreOptionsClick() {}

                override fun onUndoClick() {
                    onAction(EditTextPlainAction.Undo)
                }

                override fun onRedoClick() {
                    onAction(EditTextPlainAction.Redo)
                }

                override fun onNextSearchResult() {
                    onAction(EditTextPlainAction.NextSearchResult)
                }

                override fun onPrevSearchResult() {
                    onAction(EditTextPlainAction.PrevSearchResult)
                }

                override fun onCloseSearch() {
                    onAction(EditTextPlainAction.CloseSearch)
                }
            },
        bottomLeftActions = {
            IconButton(onClick = { onAction(EditTextPlainAction.ConvertToNote) }) {
                Icon(
                    painter = painterResource(Res.drawable.convert_to_text),
                    contentDescription = stringResource(Res.string.convert_to_text_note),
                )
            }
            IconButton(onClick = { onAction(EditTextPlainAction.Save) }) {
                Icon(
                    painter = painterResource(Res.drawable.save),
                    contentDescription = stringResource(Res.string.save),
                )
            }
            IconButton(onClick = { onAction(EditTextPlainAction.RequestSaveAs) }) {
                Icon(
                    painter = painterResource(Res.drawable.save_as),
                    contentDescription = stringResource(Res.string.save_to_device),
                )
            }
        },
        modifier = modifier,
    )

    when (val dialog = state.activeDialog) {
        is EditTextPlainDialogState.Error -> {
            val title =
                dialog.title
                    ?: dialog.titleRes?.let { stringResource(it) }
                    ?: stringResource(Res.string.error)
            ConfirmDialog(
                title = title,
                message = dialog.message ?: "",
                positiveButtonText = stringResource(Res.string.continue_),
                negativeButtonText = "",
                onConfirm = { onAction(EditTextPlainAction.DismissDialog) },
                onDismissRequest = { onAction(EditTextPlainAction.DismissDialog) },
            )
        }
        null -> Unit
    }
}
