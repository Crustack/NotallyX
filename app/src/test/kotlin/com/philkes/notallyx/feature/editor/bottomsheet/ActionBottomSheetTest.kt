package com.philkes.notallyx.feature.editor.bottomsheet

import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.EditNoteActionHandler
import com.philkes.notallyx.feature.editor.EditorBottomSheetState
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.migration.ExportMimeType
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ActionBottomSheetTest {

    @Test
    fun testCreateExportActions() {
        var exportedMimeType: ExportMimeType? = null
        val actions = createExportActions { mimeType -> exportedMimeType = mimeType }

        assertThat(actions).hasSize(ExportMimeType.entries.size)
        val pdfAction = actions.first { it.label == ExportMimeType.PDF.name }
        assertThat(pdfAction.labelResId).isEqualTo(EditAction.EXPORT.textResId)

        pdfAction.onClick()
        assertThat(exportedMimeType).isEqualTo(ExportMimeType.PDF)
    }

    @Test
    fun testCreateAddActions() {
        val actionHandler = mock<EditNoteActionHandler>()
        val actions = createAddActions(actionHandler)

        assertThat(actions).hasSize(3)
        assertThat(actions[0].labelResId).isEqualTo(R.string.add_images)
        assertThat(actions[0].drawableResId).isEqualTo(R.drawable.add_images)
        assertThat(actions[1].labelResId).isEqualTo(R.string.attach_file)
        assertThat(actions[1].drawableResId).isEqualTo(R.drawable.text_file)
        assertThat(actions[2].labelResId).isEqualTo(R.string.record_audio)
        assertThat(actions[2].drawableResId).isEqualTo(R.drawable.record_audio)

        actions[0].onClick()
        verify(actionHandler).addImages()

        actions[1].onClick()
        verify(actionHandler).attachFiles()

        actions[2].onClick()
        verify(actionHandler).recordAudio()
    }

    @Test
    fun testCreateAddNoteActions() {
        val actionHandler = mock<EditNoteActionHandler>()
        val actions = createAddNoteActions(actionHandler)

        assertThat(actions).hasSize(4)
        assertThat(actions[3].labelResId).isEqualTo(R.string.link_note)
        assertThat(actions[3].drawableResId).isEqualTo(R.drawable.notebook)

        actions[3].onClick()
        verify(actionHandler).addNoteLink()
    }

    @Test
    fun testCreateMoreNoteActionsFiltersTopAndBottomActions() {
        val model = mock<NoteModel>()
        val actionHandler = mock<EditNoteActionHandler>()

        `when`(model.pinned).thenReturn(false)
        `when`(model.viewMode).thenReturn(MutableStateFlow(NoteViewMode.EDIT))
        `when`(model.folder).thenReturn(Folder.NOTES)
        `when`(model.type).thenReturn(Type.NOTE)
        `when`(model.isPinnedToStatus).thenReturn(false)

        val topActions = listOf(EditAction.SHARE, EditAction.DELETE)
        val bottomAction = EditAction.ARCHIVE

        val actions =
            createMoreNoteActions(
                model = model,
                actionHandler = actionHandler,
                topActions = topActions,
                bottomAction = bottomAction,
            )

        val editActions = actions.map { it.labelResId }
        assertThat(editActions).doesNotContain(EditAction.SHARE.textResId)
        assertThat(editActions).doesNotContain(EditAction.DELETE.textResId)
        assertThat(editActions).doesNotContain(EditAction.ARCHIVE.textResId)
        assertThat(editActions).doesNotContain(EditAction.RESTORE.textResId)
    }

    @Test
    fun testCreateMoreNoteActionsIncludesRestoreInDeletedFolder() {
        val model = mock<NoteModel>()
        val actionHandler = mock<EditNoteActionHandler>()

        `when`(model.pinned).thenReturn(false)
        `when`(model.viewMode).thenReturn(MutableStateFlow(NoteViewMode.EDIT))
        `when`(model.folder).thenReturn(Folder.DELETED)
        `when`(model.type).thenReturn(Type.NOTE)
        `when`(model.isPinnedToStatus).thenReturn(false)

        val actions =
            createMoreNoteActions(
                model = model,
                actionHandler = actionHandler,
                topActions = emptyList(),
                bottomAction = null,
            )

        val actionLabels = actions.map { it.labelResId }
        assertThat(actionLabels).contains(EditAction.RESTORE.textResId)
    }

    @Test
    fun testEditorBottomSheetState() {
        val moreNoteState = EditorBottomSheetState.MoreNote(emptyList(), null)
        val moreListState = EditorBottomSheetState.MoreList(emptyList(), null)
        val addNoteState = EditorBottomSheetState.AddNote
        val addState = EditorBottomSheetState.Add
        val exportState = EditorBottomSheetState.Export {}
        val actionSelectionState =
            EditorBottomSheetState.ActionSelection(
                actions = emptyList(),
                oldAction = EditAction.SHARE,
                onActionSelected = {},
            )

        assertThat(moreNoteState).isNotNull
        assertThat(moreListState).isNotNull
        assertThat(addNoteState).isNotNull
        assertThat(addState).isNotNull
        assertThat(exportState).isNotNull
        assertThat(actionSelectionState).isNotNull
    }

    @Test
    fun testCreateMoreListActionsIncludesListActions() {
        val model = mock<NoteModel>()
        val actionHandler = mock<EditNoteActionHandler>()

        `when`(model.pinned).thenReturn(false)
        `when`(model.viewMode).thenReturn(MutableStateFlow(NoteViewMode.EDIT))
        `when`(model.folder).thenReturn(Folder.NOTES)
        `when`(model.type).thenReturn(Type.LIST)
        `when`(model.isPinnedToStatus).thenReturn(false)

        val actions =
            createMoreListActions(
                model = model,
                actionHandler = actionHandler,
                topActions = emptyList(),
                bottomAction = null,
            )

        val actionLabels = actions.map { it.labelResId }
        assertThat(actionLabels)
            .contains(
                R.string.delete_checked_items,
                R.string.check_all_items,
                R.string.uncheck_all_items,
            )

        val deleteCheckedAction = actions.first { it.labelResId == R.string.delete_checked_items }
        assertThat(deleteCheckedAction.showDividerAbove).isTrue()
        deleteCheckedAction.onClick()
        verify(actionHandler).handleListAction(EditListAction.DELETE_CHECKED)

        val checkAllAction = actions.first { it.labelResId == R.string.check_all_items }
        assertThat(checkAllAction.showDividerAbove).isFalse()
        checkAllAction.onClick()
        verify(actionHandler).handleListAction(EditListAction.CHECK_ALL)

        val uncheckAllAction = actions.first { it.labelResId == R.string.uncheck_all_items }
        assertThat(uncheckAllAction.showDividerAbove).isFalse()
        uncheckAllAction.onClick()
        verify(actionHandler).handleListAction(EditListAction.UNCHECK_ALL)
    }
}
