package com.philkes.notallyx.feature.editor.bottomsheet

import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel

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
