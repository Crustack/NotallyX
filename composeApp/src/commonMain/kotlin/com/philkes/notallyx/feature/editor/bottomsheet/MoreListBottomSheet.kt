package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.screens.getTitleAndIcon

/** Stateless [MoreListBottomSheet] for list-note actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreListBottomSheet(
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.LIST,
    isPinnedToStatus: Boolean = false,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    val actions =
        remember(
            actionHandler,
            pinned,
            viewMode,
            folder,
            type,
            isPinnedToStatus,
            topActions,
            bottomAction,
        ) {
            createMoreListActions(
                pinned = pinned,
                viewMode = viewMode,
                folder = folder,
                type = type,
                isPinnedToStatus = isPinnedToStatus,
                actionHandler = actionHandler,
                topActions = topActions,
                bottomAction = bottomAction,
            )
        }
    ActionModalBottomSheet(
        actions = actions,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismissRequest = onDismissRequest,
    )
}

fun createMoreListActions(
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.LIST,
    isPinnedToStatus: Boolean = false,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
): List<ActionItem> =
    createMoreNoteActions(
        pinned = pinned,
        viewMode = viewMode,
        folder = folder,
        type = type,
        isPinnedToStatus = isPinnedToStatus,
        actionHandler = actionHandler,
        topActions = topActions,
        bottomAction = bottomAction,
    ) +
        EditListAction.entries.mapIndexed { index, editAction ->
            val (title, icon) = editAction.getTitleAndIcon()
            ActionItem(
                labelRes = title,
                drawableRes = icon,
                showDividerAbove = index == 0,
            ) {
                actionHandler.handleListAction(editAction)
            }
        }
