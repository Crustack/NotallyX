package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.annotation.ColorInt
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.feature.editor.NoteModel

class ActionSelectionBottomSheet(
    actions: List<EditAction>,
    model: NoteModel,
    oldAction: EditAction,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    @ColorInt color: Int?,
    onActionSelected: (EditAction) -> Unit,
) :
    ActionBottomSheet(
        actions =
            actions.map { action ->
                val (actionTitle, icon) =
                    action.getTitleAndIcon(
                        model.pinned,
                        model.viewMode.value,
                        model.folder,
                        model.type,
                    )
                Action(
                    labelResId = actionTitle,
                    drawableResId = icon,
                    isSelected = action == oldAction,
                ) {
                    onActionSelected(action)
                    true
                }
            },
        color = color,
        sheetTitle = title,
        sheetOnReset = onReset,
    ) {

    companion object {
        const val TAG = "ActionSelectionBottomSheet"
    }
}
