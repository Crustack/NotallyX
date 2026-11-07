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
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel

/** BottomSheet inside list-note for all common note actions and list-item actions. */
@Composable
fun MoreListBottomSheet(
    model: NoteModel,
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    val viewMode by model.viewMode.collectAsStateWithLifecycle()
    MoreListBottomSheet(
        actionHandler = actionHandler,
        modifier = modifier,
        pinned = model.pinned,
        viewMode = viewMode,
        folder = model.folder,
        type = model.type,
        isPinnedToStatus = model.isPinnedToStatus,
        topActions = topActions,
        bottomAction = bottomAction,
        backgroundColor = backgroundColor,
        onDismissRequest = onDismissRequest,
    )
}

/** Stateless overload of [MoreListBottomSheet] for easy previewing and testing. */
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
            ActionItem(
                labelResId = editAction.textResId,
                drawableResId = editAction.drawableResId,
                showDividerAbove = index == 0,
            ) {
                actionHandler.handleListAction(editAction)
            }
        }

fun createMoreListActions(
    model: NoteModel,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
): List<ActionItem> =
    createMoreListActions(
        pinned = model.pinned,
        viewMode = model.viewMode.value,
        folder = model.folder,
        type = model.type,
        isPinnedToStatus = model.isPinnedToStatus,
        actionHandler = actionHandler,
        topActions = topActions,
        bottomAction = bottomAction,
    )

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MoreListBottomSheetPreview() {
    NotallyXTheme {
        MoreListBottomSheet(
            actionHandler = object : NoteActionHandler {},
            pinned = false,
            viewMode = NoteViewMode.EDIT,
            folder = Folder.NOTES,
            type = Type.LIST,
        ) {}
    }
}
