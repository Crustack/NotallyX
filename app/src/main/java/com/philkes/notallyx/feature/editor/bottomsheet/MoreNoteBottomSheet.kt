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
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel

/** BottomSheet inside note for all common note actions. */
@Composable
fun MoreNoteBottomSheet(
    model: NoteModel,
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    val viewMode by model.viewMode.collectAsStateWithLifecycle()
    MoreNoteBottomSheet(
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

/** Stateless overload of [MoreNoteBottomSheet] for easy previewing and testing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreNoteBottomSheet(
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.NOTE,
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
            createMoreNoteActions(
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

fun createMoreNoteActions(
    pinned: Boolean = false,
    viewMode: NoteViewMode = NoteViewMode.EDIT,
    folder: Folder = Folder.NOTES,
    type: Type = Type.NOTE,
    isPinnedToStatus: Boolean = false,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
): List<ActionItem> {
    val allPossibleActions = EditAction.entries

    val actionsInBottomSheet = allPossibleActions.filter {
        (it !in topActions) &&
            (it != bottomAction) &&
            (it != EditAction.RESTORE || folder == Folder.DELETED)
    }

    return actionsInBottomSheet.map { editAction ->
        val (title, icon) =
            editAction.getTitleAndIcon(
                pinned,
                viewMode,
                folder,
                type,
                isPinnedToStatus,
            )
        ActionItem(
            labelResId = title,
            drawableResId = icon,
        ) {
            actionHandler.handleAction(editAction)
        }
    }
}

fun createMoreNoteActions(
    model: NoteModel,
    actionHandler: NoteActionHandler,
    topActions: Collection<EditAction> = listOf(),
    bottomAction: EditAction? = null,
): List<ActionItem> =
    createMoreNoteActions(
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
private fun MoreNoteBottomSheetPreview() {
    NotallyXTheme {
        MoreNoteBottomSheet(
            actionHandler = object : NoteActionHandler {},
            pinned = false,
            viewMode = NoteViewMode.EDIT,
            folder = Folder.NOTES,
            type = Type.NOTE,
        ) {}
    }
}
