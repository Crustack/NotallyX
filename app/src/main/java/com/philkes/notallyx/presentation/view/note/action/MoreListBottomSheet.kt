package com.philkes.notallyx.presentation.view.note.action

import androidx.annotation.ColorInt
import com.philkes.notallyx.presentation.viewmodel.NotallyModel
import com.philkes.notallyx.presentation.viewmodel.preference.EditAction
import com.philkes.notallyx.presentation.viewmodel.preference.EditListAction

/** BottomSheet inside list-note for all common note actions and list-item actions. */
class MoreListBottomSheet(
    model: NotallyModel,
    @ColorInt color: Int?,
    handleAction: (EditAction) -> Unit,
    handleListAction: (EditListAction) -> Unit,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
) :
    ActionBottomSheet(
        createActions(model, handleAction, handleListAction, topActions, bottomAction),
        color,
    ) {

    companion object {
        const val TAG = "MoreListBottomSheet"

        private fun createActions(
            model: NotallyModel,
            handleAction: (EditAction) -> Unit,
            handleListAction: (EditListAction) -> Unit,
            topActions: Collection<EditAction>,
            bottomAction: EditAction? = null,
        ) =
            MoreNoteBottomSheet.createActions(
                model,
                handleAction,
                topActions = topActions,
                bottomAction = bottomAction,
            ) +
                EditListAction.entries.mapIndexed { index, editAction ->
                    Action(
                        editAction.textResId,
                        editAction.drawableResId,
                        showDividerAbove = index == 0,
                    ) { _ ->
                        handleListAction(editAction)
                        true
                    }
                }
    }
}
