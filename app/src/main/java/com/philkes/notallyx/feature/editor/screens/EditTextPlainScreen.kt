package com.philkes.notallyx.feature.editor.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.editor.EditTextPlainAction
import com.philkes.notallyx.feature.editor.EditTextPlainDialogState
import com.philkes.notallyx.feature.editor.EditTextPlainUiState

@Composable
fun EditTextPlainScreen(
    state: EditTextPlainUiState,
    onAction: (EditTextPlainAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(EditTextPlainAction.BackClicked) },
) {
    val context = LocalContext.current
    val noteState =
        EditNoteState(
            title = state.title,
            bodyValue = state.bodyValue,
        )

    EditNoteScreen(
        noteState = noteState,
        searchState = state.searchState,
        backgroundColor = Color(context.extractColor(state.color)),
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
                    painter = painterResource(id = R.drawable.convert_to_text),
                    contentDescription = stringResource(id = R.string.convert_to_text_note),
                )
            }
            IconButton(onClick = { onAction(EditTextPlainAction.Save) }) {
                Icon(
                    painter = painterResource(id = R.drawable.save),
                    contentDescription = stringResource(id = R.string.save),
                )
            }
            IconButton(onClick = { onAction(EditTextPlainAction.RequestSaveAs) }) {
                Icon(
                    painter = painterResource(id = R.drawable.save_as),
                    contentDescription = stringResource(id = R.string.save_to_device),
                )
            }
        },
        modifier = modifier,
    )

    when (val dialog = state.activeDialog) {
        is EditTextPlainDialogState.Error -> {
            ConfirmDialog(
                title = stringResource(dialog.titleResId),
                message = dialog.message ?: "",
                positiveButtonText = stringResource(android.R.string.ok),
                negativeButtonText = "",
                onConfirm = { onAction(EditTextPlainAction.DismissDialog) },
                onDismissRequest = { onAction(EditTextPlainAction.DismissDialog) },
            )
        }
        null -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTextPlainScreenPreview() {
    NotallyXTheme {
        EditTextPlainScreen(
            state = EditTextPlainUiState(title = "test.txt"),
            onAction = {},
        )
    }
}
