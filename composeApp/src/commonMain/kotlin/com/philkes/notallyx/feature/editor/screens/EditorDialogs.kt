package com.philkes.notallyx.feature.editor.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.Res
import com.philkes.notallyx.calculating
import com.philkes.notallyx.cancel
import com.philkes.notallyx.cant_load_file
import com.philkes.notallyx.continue_
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.DualTextInputDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.LinkOptionsDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.count
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.delete
import com.philkes.notallyx.delete_note_forever
import com.philkes.notallyx.display_text
import com.philkes.notallyx.edit_link
import com.philkes.notallyx.error
import com.philkes.notallyx.feature.editor.EditorBottomSheetState
import com.philkes.notallyx.feature.editor.EditorDialogState
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.bottomsheet.ActionModalBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.ActionSelectionBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.AddBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.AddNoteBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.ExportBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreListBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreNoteBottomSheet
import com.philkes.notallyx.link
import com.philkes.notallyx.please_grant_notally_audio
import com.philkes.notallyx.settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditorDialogs(
    activeDialog: EditorDialogState?,
    onDismissRequest: () -> Unit,
    addingFilesProgress: Progress? = null,
) {
    if (addingFilesProgress?.inProgress == true) {
        val progress = addingFilesProgress
        val msg =
            when {
                progress.indeterminate -> stringResource(Res.string.calculating)
                else ->
                    stringResource(Res.string.count, progress.current, progress.total) +
                        (progress.countSuffix?.let { " $it" } ?: "")
            }
        val progressValue =
            if (progress.indeterminate || progress.total == 0) null
            else progress.current.toFloat() / progress.total.toFloat()

        ProgressDialog(
            title = "",
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
                onDismissRequest = onDismissRequest,
                onColorSelected = { selectedColor ->
                    dialogState.onColorSelected(selectedColor)
                    onDismissRequest()
                },
                onColorChanged = { oldColor, newColor ->
                    dialogState.onColorChanged?.invoke(oldColor, newColor)
                    onDismissRequest()
                },
                onColorDeleted = { colorToDelete, newColor ->
                    dialogState.onColorDeleted?.invoke(colorToDelete, newColor)
                    onDismissRequest()
                },
                onSetDefaultColor = { newDefault ->
                    dialogState.onSetDefaultColor?.invoke(newDefault)
                },
            )
        }
        is EditorDialogState.DeleteForeverConfirm -> {
            ConfirmDialog(
                message = stringResource(Res.string.delete_note_forever),
                positiveButtonText = stringResource(Res.string.delete),
                negativeButtonText = stringResource(Res.string.cancel),
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    dialogState.onConfirm()
                    onDismissRequest()
                },
            )
        }
        is EditorDialogState.AudioPermissionRationale -> {
            ConfirmDialog(
                message = stringResource(Res.string.please_grant_notally_audio),
                positiveButtonText = stringResource(Res.string.continue_),
                negativeButtonText = stringResource(Res.string.cancel),
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    dialogState.onConfirm()
                    onDismissRequest()
                },
            )
        }
        is EditorDialogState.PermissionRationale -> {
            val message =
                dialogState.message ?: dialogState.messageRes?.let { stringResource(it) } ?: ""
            ConfirmDialog(
                message = message,
                positiveButtonText = stringResource(Res.string.continue_),
                negativeButtonText = stringResource(Res.string.cancel),
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    dialogState.onConfirm()
                    onDismissRequest()
                },
            )
        }
        is EditorDialogState.RejectionConfirm -> {
            val message =
                dialogState.message ?: dialogState.msgRes?.let { stringResource(it) } ?: ""
            ConfirmDialog(
                message = message,
                positiveButtonText = stringResource(Res.string.settings),
                negativeButtonText = stringResource(Res.string.cancel),
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    dialogState.onConfirm()
                    onDismissRequest()
                },
            )
        }
        is EditorDialogState.FileErrors -> {
            ListDialog(
                title = stringResource(Res.string.cant_load_file),
                items = dialogState.errors.map { "${it.name}: ${it.description}" },
                onDismissRequest = onDismissRequest,
            )
        }
        is EditorDialogState.Error -> {
            val title =
                dialogState.title
                    ?: dialogState.titleRes?.let { stringResource(it) }
                    ?: stringResource(Res.string.error)
            ErrorDialog(
                title = title,
                message = dialogState.message,
                onDismissRequest = onDismissRequest,
            )
        }
        is EditorDialogState.EditLink -> {
            if (dialogState.isNoteUrl) {
                TextInputDialog(
                    title = stringResource(Res.string.edit_link),
                    initialValue = dialogState.displayTextBefore,
                    hint = stringResource(Res.string.display_text),
                    onDismissRequest = {
                        dialogState.onDismiss()
                        onDismissRequest()
                    },
                    onConfirm = { displayTextAfter ->
                        dialogState.onConfirm(dialogState.urlBefore, displayTextAfter)
                        onDismissRequest()
                    },
                )
            } else {
                DualTextInputDialog(
                    title = stringResource(Res.string.edit_link),
                    initialValue1 = dialogState.displayTextBefore,
                    initialValue2 = dialogState.urlBefore,
                    hint1 = stringResource(Res.string.display_text),
                    hint2 = stringResource(Res.string.link),
                    onDismissRequest = {
                        dialogState.onDismiss()
                        onDismissRequest()
                    },
                    onConfirm = { displayTextAfter, urlAfter ->
                        dialogState.onConfirm(urlAfter, displayTextAfter)
                        onDismissRequest()
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
                onDismissRequest = onDismissRequest,
            )
        }
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomSheets(
    activeBottomSheet: EditorBottomSheetState?,
    onDismissRequest: (EditorBottomSheetState) -> Unit,
    actionHandler: NoteActionHandler,
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.NOTE,
    isPinnedToStatus: Boolean = false,
    backgroundColor: Color? = null,
) {
    when (val sheetState = activeBottomSheet) {
        is EditorBottomSheetState.MoreNote -> {
            MoreNoteBottomSheet(
                actionHandler = actionHandler,
                pinned = pinned,
                viewMode = viewMode,
                folder = folder,
                type = type,
                isPinnedToStatus = isPinnedToStatus,
                topActions = sheetState.topActions,
                bottomAction = sheetState.bottomAction,
                backgroundColor = backgroundColor,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        is EditorBottomSheetState.MoreList -> {
            MoreListBottomSheet(
                actionHandler = actionHandler,
                pinned = pinned,
                viewMode = viewMode,
                folder = folder,
                type = type,
                isPinnedToStatus = isPinnedToStatus,
                topActions = sheetState.topActions,
                bottomAction = sheetState.bottomAction,
                backgroundColor = backgroundColor,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        is EditorBottomSheetState.AddNote -> {
            AddNoteBottomSheet(
                actionHandler = actionHandler,
                backgroundColor = backgroundColor,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        is EditorBottomSheetState.Add -> {
            AddBottomSheet(
                actionHandler = actionHandler,
                backgroundColor = backgroundColor,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        is EditorBottomSheetState.Export -> {
            ExportBottomSheet(
                backgroundColor = backgroundColor,
                onExport = sheetState.onExport,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        is EditorBottomSheetState.ActionSelection -> {
            ActionSelectionBottomSheet(
                actions = sheetState.actions,
                oldAction = sheetState.oldAction,
                title = sheetState.title,
                onReset = sheetState.onReset,
                backgroundColor = backgroundColor,
                pinned = pinned,
                viewMode = viewMode,
                folder = folder,
                type = type,
                onDismissRequest = { onDismissRequest(sheetState) },
                onActionSelected = { selectedAction ->
                    sheetState.onActionSelected(selectedAction)
                    onDismissRequest(sheetState)
                },
            )
        }
        is EditorBottomSheetState.Custom -> {
            ActionModalBottomSheet(
                actions = sheetState.actions,
                title = sheetState.title,
                onReset = sheetState.onReset,
                backgroundColor = backgroundColor,
                onDismissRequest = { onDismissRequest(sheetState) },
            )
        }
        null -> {}
    }
}
