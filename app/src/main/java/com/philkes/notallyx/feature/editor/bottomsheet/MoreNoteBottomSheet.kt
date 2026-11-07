package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.annotation.ColorInt
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel

/** BottomSheet inside list-note for all common note actions. */
class MoreNoteBottomSheet(
    model: NoteModel,
    @ColorInt color: Int?,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
) : ActionBottomSheet(createActions(model, actionHandler, topActions, bottomAction), color) {

    companion object {
        const val TAG = "MoreNoteBottomSheet"

        internal fun createActions(
            model: NoteModel,
            actionHandler: NoteActionHandler,
            topActions: Collection<EditAction>,
            bottomAction: EditAction? = null,
        ): List<Action> {
            val allPossibleActions = EditAction.entries

            val actionsInBottomSheet = allPossibleActions.filter {
                it !in topActions &&
                    it != bottomAction &&
                    (it != EditAction.RESTORE || model.folder == Folder.DELETED)
            }

            return actionsInBottomSheet.map { editAction ->
                val (title, icon) =
                    editAction.getTitleAndIcon(
                        model.pinned,
                        model.viewMode.value,
                        model.folder,
                        model.type,
                        model.isPinnedToStatus,
                    )
                Action(title, icon) { _ ->
                    actionHandler.handleAction(editAction)
                    true
                }
            }
        }
    }
}
