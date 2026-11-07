package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.screens.getTitleAndIcon

/** Stateless overload of [ActionSelectionBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSelectionBottomSheet(
    actions: List<EditAction>,
    oldAction: EditAction,
    onActionSelected: (EditAction) -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.NOTE,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit = {},
) {
    val actionItems =
        remember(actions, oldAction, pinned, viewMode, folder, type, onActionSelected) {
            actions.map { action ->
                val (actionTitle, icon) =
                    action.getTitleAndIcon(
                        pinned,
                        viewMode,
                        folder,
                        type,
                    )
                ActionItem(
                    labelRes = actionTitle,
                    drawableRes = icon,
                    isSelected = action == oldAction,
                ) {
                    onActionSelected(action)
                }
            }
        }
    ActionModalBottomSheet(
        actions = actionItems,
        modifier = modifier,
        title = title,
        onReset = onReset,
        backgroundColor = backgroundColor,
        onDismissRequest = onDismissRequest,
    )
}
