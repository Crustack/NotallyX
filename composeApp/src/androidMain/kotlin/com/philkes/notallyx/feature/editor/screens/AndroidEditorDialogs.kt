package com.philkes.notallyx.feature.editor.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.EditorBottomSheetState
import com.philkes.notallyx.feature.editor.NoteModel

@Composable
fun EditorDialogs(model: NoteModel) {
    val activeDialog by model.activeDialog.collectAsStateWithLifecycle()
    val addingFilesProgress by model.addingFiles.collectAsStateWithLifecycle()
    EditorDialogs(
        activeDialog = activeDialog,
        onDismissRequest = { model.activeDialog.value = null },
        addingFilesProgress = addingFilesProgress,
    )
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
