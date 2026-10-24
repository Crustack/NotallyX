package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.NoteModel

@Composable
fun ActionSelectionBottomSheet(
    actions: List<EditAction>,
    model: NoteModel,
    oldAction: EditAction,
    modifier: Modifier = Modifier,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
    onActionSelected: (EditAction) -> Unit,
) {
    val viewMode by model.viewMode.collectAsStateWithLifecycle()
    ActionSelectionBottomSheet(
        actions = actions,
        oldAction = oldAction,
        onActionSelected = onActionSelected,
        modifier = modifier,
        pinned = model.pinned,
        viewMode = viewMode,
        folder = model.folder,
        type = model.type,
        title = title,
        onReset = onReset,
        backgroundColor = backgroundColor,
        onDismissRequest = onDismissRequest,
    )
}

/** Stateless overload of [ActionSelectionBottomSheet] for easy previewing and testing. */
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
                    labelResId = actionTitle,
                    drawableResId = icon,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ActionSelectionBottomSheetPreview() {
    NotallyXTheme {
        ActionSelectionBottomSheet(
            actions = listOf(EditAction.SHARE, EditAction.DELETE, EditAction.ARCHIVE),
            oldAction = EditAction.SHARE,
            title = "Swap Action",
            onActionSelected = {},
        ) {}
    }
}
