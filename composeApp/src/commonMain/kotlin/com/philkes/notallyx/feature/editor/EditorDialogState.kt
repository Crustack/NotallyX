package com.philkes.notallyx.feature.editor

import com.philkes.notallyx.core.common.result.FileError
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.editor.bottomsheet.ActionItem
import com.philkes.notallyx.feature.migration.ExportMimeType
import org.jetbrains.compose.resources.StringResource

sealed interface EditorDialogState {
    data class ColorPicker(
        val currentColor: String,
        val colors: Set<String>,
        val defaultColor: String = BaseNote.COLOR_DEFAULT,
        val onColorSelected: (selectedColor: String) -> Unit,
        val onColorChanged: ((oldColor: String, newColor: String) -> Unit)? = null,
        val onColorDeleted: ((colorToDelete: String, newColor: String) -> Unit)? = null,
        val onSetDefaultColor: ((String) -> Unit)? = null,
    ) : EditorDialogState

    data class DeleteForeverConfirm(val onConfirm: () -> Unit) : EditorDialogState

    data class AudioPermissionRationale(val onConfirm: () -> Unit) : EditorDialogState

    data class PermissionRationale(
        val messageResId: Int = 0,
        val messageRes: StringResource? = null,
        val message: String? = null,
        val onConfirm: () -> Unit,
    ) : EditorDialogState {
        constructor(
            messageResId: Int,
            onConfirm: () -> Unit,
        ) : this(
            messageResId = messageResId,
            messageRes = null,
            message = null,
            onConfirm = onConfirm,
        )
    }

    data class RejectionConfirm(
        val msgResId: Int = 0,
        val msgRes: StringResource? = null,
        val message: String? = null,
        val onConfirm: () -> Unit,
    ) : EditorDialogState

    data class FileErrors(val errors: List<FileError>) : EditorDialogState

    data class Error(
        val titleResId: Int = 0,
        val titleRes: StringResource? = null,
        val title: String? = null,
        val message: String?,
    ) : EditorDialogState

    data class EditLink(
        val urlBefore: String,
        val displayTextBefore: String,
        val isNoteUrl: Boolean,
        val onConfirm: (urlAfter: String?, displayTextAfter: String) -> Unit,
        val onDismiss: () -> Unit = {},
    ) : EditorDialogState

    data class LinkOptions(
        val url: String,
        val displayText: String,
        val isNoteUrl: Boolean,
        val onOpen: () -> Unit,
        val onEdit: () -> Unit,
        val onRemove: () -> Unit,
    ) : EditorDialogState
}

sealed interface EditorBottomSheetState {
    data class MoreNote(
        val topActions: Collection<EditAction>,
        val bottomAction: EditAction?,
    ) : EditorBottomSheetState

    data class MoreList(
        val topActions: Collection<EditAction>,
        val bottomAction: EditAction?,
    ) : EditorBottomSheetState

    data object AddNote : EditorBottomSheetState

    data object Add : EditorBottomSheetState

    data class Export(val onExport: (ExportMimeType) -> Unit) : EditorBottomSheetState

    data class ActionSelection(
        val actions: List<EditAction>,
        val oldAction: EditAction,
        val title: String? = null,
        val onReset: (() -> Unit)? = null,
        val onActionSelected: (EditAction) -> Unit,
    ) : EditorBottomSheetState

    data class Custom(
        val actions: List<ActionItem>,
        val title: String? = null,
        val onReset: (() -> Unit)? = null,
    ) : EditorBottomSheetState
}
