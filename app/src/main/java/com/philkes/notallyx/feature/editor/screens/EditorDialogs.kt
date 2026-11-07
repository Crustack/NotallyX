package com.philkes.notallyx.feature.editor.screens

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.DualTextInputDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.LinkOptionsDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.EditorBottomSheetState
import com.philkes.notallyx.feature.editor.EditorDialogState
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.bottomsheet.ActionModalBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.ActionSelectionBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.AddBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.AddNoteBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.ExportBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreListBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreNoteBottomSheet

@Composable
fun EditorDialogs(model: NoteModel) {
    val activeDialog by model.activeDialog.collectAsStateWithLifecycle()
    val addingFilesProgress by model.addingFiles.collectAsStateWithLifecycle()

    if (addingFilesProgress?.inProgress == true) {
        val progress = addingFilesProgress!!
        val msg =
            when {
                progress.indeterminate -> stringResource(R.string.calculating)
                else ->
                    stringResource(R.string.count, progress.current, progress.total) +
                        (progress.countSuffix?.let { " $it" } ?: "")
            }
        val progressValue =
            if (progress.indeterminate || progress.total == 0) null
            else progress.current.toFloat() / progress.total.toFloat()

        ProgressDialog(
            title = stringResource(progress.titleId),
            message = msg,
            progress = progressValue,
        )
    }

    when (val dialogState = activeDialog) {
        is EditorDialogState.ColorPicker -> {
            ColorPickerDialog(
                currentColor = dialogState.currentColor,
                colors = dialogState.colors,
                defaultColor = dialogState.defaultColor,
                onDismissRequest = { model.activeDialog.value = null },
                onColorSelected = { selectedColor ->
                    dialogState.onColorSelected(selectedColor)
                    model.activeDialog.value = null
                },
                onColorChanged = { oldColor, newColor ->
                    dialogState.onColorChanged?.invoke(oldColor, newColor)
                    model.activeDialog.value = null
                },
                onColorDeleted = { colorToDelete, newColor ->
                    dialogState.onColorDeleted?.invoke(colorToDelete, newColor)
                    model.activeDialog.value = null
                },
                onSetDefaultColor = { newDefault ->
                    dialogState.onSetDefaultColor?.invoke(newDefault)
                },
            )
        }
        is EditorDialogState.DeleteForeverConfirm -> {
            ConfirmDialog(
                message = stringResource(R.string.delete_note_forever),
                positiveButtonText = stringResource(R.string.delete),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { model.activeDialog.value = null },
                onConfirm = {
                    dialogState.onConfirm()
                    model.activeDialog.value = null
                },
            )
        }
        is EditorDialogState.AudioPermissionRationale -> {
            ConfirmDialog(
                message = stringResource(R.string.please_grant_notally_audio),
                positiveButtonText = stringResource(R.string.continue_),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { model.activeDialog.value = null },
                onConfirm = {
                    dialogState.onConfirm()
                    model.activeDialog.value = null
                },
            )
        }
        is EditorDialogState.PermissionRationale -> {
            ConfirmDialog(
                message = stringResource(dialogState.messageResId),
                positiveButtonText = stringResource(R.string.continue_),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { model.activeDialog.value = null },
                onConfirm = {
                    dialogState.onConfirm()
                    model.activeDialog.value = null
                },
            )
        }
        is EditorDialogState.RejectionConfirm -> {
            ConfirmDialog(
                message = stringResource(dialogState.msgResId),
                positiveButtonText = stringResource(R.string.settings),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { model.activeDialog.value = null },
                onConfirm = {
                    dialogState.onConfirm()
                    model.activeDialog.value = null
                },
            )
        }
        is EditorDialogState.FileErrors -> {
            ListDialog(
                title = stringResource(R.string.cant_load_file),
                items = dialogState.errors.map { "${it.name}: ${it.description}" },
                onDismissRequest = { model.activeDialog.value = null },
            )
        }
        is EditorDialogState.Error -> {
            ErrorDialog(
                title = stringResource(dialogState.titleResId),
                message = dialogState.message,
                onDismissRequest = { model.activeDialog.value = null },
            )
        }
        is EditorDialogState.EditLink -> {
            if (dialogState.isNoteUrl) {
                TextInputDialog(
                    title = stringResource(R.string.edit_link),
                    initialValue = dialogState.displayTextBefore,
                    hint = stringResource(R.string.display_text),
                    onDismissRequest = {
                        dialogState.onDismiss()
                        model.activeDialog.value = null
                    },
                    onConfirm = { displayTextAfter ->
                        dialogState.onConfirm(dialogState.urlBefore, displayTextAfter)
                        model.activeDialog.value = null
                    },
                )
            } else {
                DualTextInputDialog(
                    title = stringResource(R.string.edit_link),
                    initialValue1 = dialogState.displayTextBefore,
                    initialValue2 = dialogState.urlBefore,
                    hint1 = stringResource(R.string.display_text),
                    hint2 = stringResource(R.string.link),
                    onDismissRequest = {
                        dialogState.onDismiss()
                        model.activeDialog.value = null
                    },
                    onConfirm = { displayTextAfter, urlAfter ->
                        dialogState.onConfirm(urlAfter, displayTextAfter)
                        model.activeDialog.value = null
                    },
                )
            }
        }
        is EditorDialogState.LinkOptions -> {
            LinkOptionsDialog(
                url = dialogState.url,
                displayText = dialogState.displayText,
                isNoteUrl = dialogState.isNoteUrl,
                onOpen = dialogState.onOpen,
                onEdit = dialogState.onEdit,
                onRemove = dialogState.onRemove,
                onDismissRequest = { model.activeDialog.value = null },
            )
        }
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomSheets(
    model: NoteModel,
    actionHandler: NoteActionHandler,
    backgroundColor: Color? = null,
) {
    val activeBottomSheet by model.activeBottomSheet.collectAsStateWithLifecycle()

    when (val sheetState = activeBottomSheet) {
        is EditorBottomSheetState.MoreNote -> {
            MoreNoteBottomSheet(
                model = model,
                actionHandler = actionHandler,
                topActions = sheetState.topActions,
                bottomAction = sheetState.bottomAction,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        is EditorBottomSheetState.MoreList -> {
            MoreListBottomSheet(
                model = model,
                actionHandler = actionHandler,
                topActions = sheetState.topActions,
                bottomAction = sheetState.bottomAction,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        is EditorBottomSheetState.AddNote -> {
            AddNoteBottomSheet(
                actionHandler = actionHandler,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        is EditorBottomSheetState.Add -> {
            AddBottomSheet(
                actionHandler = actionHandler,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        is EditorBottomSheetState.Export -> {
            ExportBottomSheet(
                backgroundColor = backgroundColor,
                onExport = sheetState.onExport,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        is EditorBottomSheetState.ActionSelection -> {
            ActionSelectionBottomSheet(
                actions = sheetState.actions,
                model = model,
                oldAction = sheetState.oldAction,
                title = sheetState.title,
                onReset = sheetState.onReset,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
                onActionSelected = { selectedAction ->
                    sheetState.onActionSelected(selectedAction)
                    model.activeBottomSheet.value = null
                },
            )
        }
        is EditorBottomSheetState.Custom -> {
            ActionModalBottomSheet(
                actions = sheetState.actions,
                title = sheetState.title,
                onReset = sheetState.onReset,
                backgroundColor = backgroundColor,
                onDismissRequest = {
                    if (model.activeBottomSheet.value == sheetState) {
                        model.activeBottomSheet.value = null
                    }
                },
            )
        }
        null -> {}
    }
}

fun openActionSelectionBottomSheet(
    context: Context,
    model: NoteModel,
    preferences: NotallyXPreferences,
    action: EditAction,
    onUpdateTopActions: (List<EditAction>) -> Unit,
    onUpdateBottomAction: (EditAction) -> Unit,
) {
    val topActions = preferences.getSafeEditNoteActivityTopActions()
    val bottomAction = preferences.editNoteActivityBottomAction.value

    val isTopAction = action in topActions
    val index = if (isTopAction) topActions.indexOf(action) else -1

    val allPossibleActions = EditAction.entries
    val availableActions = allPossibleActions.filter {
        it !in topActions &&
            it != bottomAction &&
            (it != EditAction.RESTORE || model.folder == Folder.DELETED)
    }
    val selectableActions = (listOf(action) + availableActions).distinct()

    model.activeBottomSheet.value =
        EditorBottomSheetState.ActionSelection(
            actions = selectableActions,
            oldAction = action,
            title = context.getString(R.string.swap_action),
            onReset = {
                if (isTopAction) {
                    preferences.editNoteActivityTopActions.save(
                        NotallyXPreferences.DEFAULT_EDIT_NOTE_TOP_ACTIONS
                    )
                    onUpdateTopActions(NotallyXPreferences.DEFAULT_EDIT_NOTE_TOP_ACTIONS)
                } else {
                    preferences.editNoteActivityBottomAction.save(
                        NotallyXPreferences.DEFAULT_EDIT_NOTE_BOTTOM_ACTION
                    )
                    onUpdateBottomAction(NotallyXPreferences.DEFAULT_EDIT_NOTE_BOTTOM_ACTION)
                }
            },
            onActionSelected = { selectedAction ->
                if (isTopAction) {
                    val updated = topActions.toMutableList()
                    if (index != -1 && index in updated.indices) {
                        updated[index] = selectedAction
                        preferences.editNoteActivityTopActions.save(updated)
                        onUpdateTopActions(updated)
                    }
                } else {
                    preferences.editNoteActivityBottomAction.save(selectedAction)
                    onUpdateBottomAction(selectedAction)
                }
            },
        )
}
