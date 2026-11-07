package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.annotation.ColorInt
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel

/** BottomSheet inside list-note for all common note actions and list-item actions. */
class MoreListBottomSheet(
    model: NoteModel,
    @ColorInt color: Int?,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
) :
    ActionBottomSheet(
        createActions(model, actionHandler, topActions, bottomAction),
        color,
    ) {

    companion object {
        const val TAG = "MoreListBottomSheet"

        private fun createActions(
            model: NoteModel,
            actionHandler: NoteActionHandler,
            topActions: Collection<EditAction>,
            bottomAction: EditAction? = null,
        ) =
            MoreNoteBottomSheet.createActions(
                model,
                actionHandler,
                topActions = topActions,
                bottomAction = bottomAction,
            )
    }
}
