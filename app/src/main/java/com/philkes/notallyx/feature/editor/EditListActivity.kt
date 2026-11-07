package com.philkes.notallyx.feature.editor

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.bottomsheet.MoreListBottomSheet
import com.philkes.notallyx.feature.editor.history.changes.ListItemsChange
import com.philkes.notallyx.feature.editor.listitem.checkItem
import com.philkes.notallyx.feature.editor.listitem.cloneList
import com.philkes.notallyx.feature.editor.listitem.initChildren
import com.philkes.notallyx.feature.editor.listitem.removeWithChildren
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditListActivity : EditActivity(Type.LIST) {

    override fun finish() {
        super.finish()
    }

    override fun updateModel() {
        super.updateModel()
    }

    override fun toggleCanEdit(mode: NoteViewMode) {
        super.toggleCanEdit(mode)
    }

    override fun openMoreOptionsBottomSheet() {
        val prefs = NotallyXPreferences.getInstance(this@EditListActivity)
        val topActions = prefs.getSafeEditNoteActivityTopActions()
        val bottomAction = prefs.editNoteActivityBottomAction.value

        MoreListBottomSheet(
                notallyModel,
                colorInt,
                actionHandler,
                topActions,
                bottomAction,
            )
            .show(supportFragmentManager, MoreListBottomSheet.TAG)
    }

    override fun selectSearchResult(resultPos: Int) {}

    override fun configureUI() {}

    @Composable
    override fun RenderScreen() {
        val imagesState = notallyModel.images.collectAsStateWithLifecycle(emptyList())
        val filesState = notallyModel.files.collectAsStateWithLifecycle(emptyList())
        val audiosState = notallyModel.audios.collectAsStateWithLifecycle(emptyList())
        val remindersState = notallyModel.reminders.collectAsStateWithLifecycle(emptyList())

        val itemsVersionState = remember { mutableIntStateOf(0) }
        val topActionsState = remember {
            mutableStateOf(preferences.getSafeEditNoteActivityTopActions())
        }
        val bottomActionState = remember {
            mutableStateOf(preferences.editNoteActivityBottomAction.value)
        }
        val canUndoState = changeHistory.canUndo.collectAsStateWithLifecycle(false)
        val canRedoState = changeHistory.canRedo.collectAsStateWithLifecycle(false)
        val stackPointerState = changeHistory.stackPointer.collectAsStateWithLifecycle(-1)
        val viewModeState = notallyModel.viewMode.collectAsStateWithLifecycle(NoteViewMode.EDIT)

        // Read stackPointerState and itemsVersionState to observe state updates
        stackPointerState.value
        itemsVersionState.intValue

        val listItemSort = preferences.listItemSorting.value
        val (activeItemsList, checkedItemsList) =
            when (listItemSort) {
                ListItemSort.NO_AUTO_SORT -> {
                    Pair(notallyModel.items.toList(), emptyList())
                }
                ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP -> {
                    val (checked, active) = notallyModel.items.splitByChecked()
                    Pair(active, checked.sortedByDescending { it.checkedTimestamp ?: 0L })
                }
                else -> {
                    val (checked, active) = notallyModel.items.splitByChecked()
                    Pair(active, checked)
                }
            }

        val focusFirstItem = remember { notallyModel.isNewNote }

        val noteState =
            EditNoteState(
                title = titleState.value,
                items = activeItemsList,
                checkedItems = checkedItemsList,
                createdTimestamp = notallyModel.timestamp,
                modifiedTimestamp = notallyModel.modifiedTimestamp,
                images = imagesState.value,
                mediaRoot = notallyModel.imageRoot,
                files = filesState.value,
                audios = audiosState.value,
                labels = labelsState.value,
                reminders = remindersState.value,
                pinned = notallyModel.pinned,
                viewMode = viewModeState.value,
                folder = notallyModel.folder,
                type = notallyModel.type,
                isPinnedToStatus = notallyModel.isPinnedToStatus,
            )

        val preferencesState =
            EditNotePreferences(
                notesSortBy = preferences.notesSorting.value.sortedBy,
                dateFormat = preferences.dateFormatNoteView.value,
                timeFormat = preferences.timeFormatNoteView.value,
                topActions = topActionsState.value,
                bottomAction = bottomActionState.value,
            )

        fun applyListChange(action: () -> Unit) {
            val oldList = notallyModel.items.cloneList()
            action()
            initChildren(notallyModel.items)
            val newList = notallyModel.items.cloneList()
            changeHistory.push(
                ListItemsChange(
                    notallyModel = notallyModel,
                    oldItems = oldList,
                    newItems = newList,
                    onItemsUpdated = { itemsVersionState.intValue++ },
                )
            )
            itemsVersionState.intValue++
        }

        EditListScreen(
            noteState = noteState,
            preferences = preferencesState,
            backgroundColor = Color(colorIntState.intValue),
            canUndo = canUndoState.value,
            canRedo = canRedoState.value,
            changeHandlers =
                object : EditChangeHandlers {
                    override fun onTitleChange(title: String) {
                        titleState.value = title
                        notallyModel.title = title
                    }

                    override fun onItemTextChange(item: ListItem, text: String) {
                        if (item.body != text) {
                            applyListChange { item.body = text }
                        }
                    }

                    override fun onItemCheckedChange(item: ListItem, checked: Boolean) {
                        applyListChange { notallyModel.items.checkItem(item, checked) }
                    }

                    override fun onItemDelete(item: ListItem) {
                        applyListChange { notallyModel.items.removeWithChildren(item) }
                    }

                    override fun onItemSwipeRight(item: ListItem) {
                        val sectionList = if (item.checked) checkedItemsList else activeItemsList
                        val idx = sectionList.indexOf(item)
                        if (idx > 0) {
                            applyListChange { item.isChild = true }
                        }
                    }

                    override fun onItemSwipeLeft(item: ListItem) {
                        if (item.isChild) {
                            applyListChange { item.isChild = false }
                        }
                    }
                },
            clickHandlers =
                object : EditClickHandlers {
                    override fun onBackClick() {
                        finish()
                    }

                    override fun onAddItemClick() {
                        applyListChange {
                            val maxId = notallyModel.items.maxOfOrNull { it.id } ?: -1
                            val newItem =
                                ListItem(
                                    body = "",
                                    checked = false,
                                    isChild = false,
                                    order = notallyModel.items.size,
                                    children = mutableListOf(),
                                    id = maxId + 1,
                                )
                            notallyModel.items.add(newItem)
                        }
                    }

                    override fun onMoreOptionsClick() {
                        openMoreOptionsBottomSheet()
                    }

                    override fun onActionClick(action: EditAction) {
                        actionHandler.handleAction(action)
                    }

                    override fun onActionLongClick(action: EditAction) {
                        openActionSelectionBottomSheet(
                            action = action,
                            onUpdateTopActions = { topActionsState.value = it },
                            onUpdateBottomAction = { bottomActionState.value = it },
                        )
                    }

                    override fun onUndoClick() {
                        if (changeHistory.canUndo.value) {
                            changeHistory.undo()
                        }
                    }

                    override fun onRedoClick() {
                        if (changeHistory.canRedo.value) {
                            changeHistory.redo()
                        }
                    }

                    override fun onImageClick(image: FileAttachment) {
                        actionHandler.viewImage(image)
                    }

                    override fun onFileClick(file: FileAttachment) {
                        actionHandler.viewFile(file)
                    }

                    override fun onAudioClick(audio: Audio) {
                        actionHandler.playAudio(audio)
                    }
                },
            focusFirstItem = focusFirstItem,
        )
    }

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        if (notallyModel.isNewNote && notallyModel.items.isEmpty()) {
            notallyModel.items.add(
                ListItem(
                    body = "",
                    checked = false,
                    isChild = false,
                    order = 0,
                    children = mutableListOf(),
                    id = 0,
                )
            )
        }
    }

    override suspend fun highlightSearchResults(search: String): Int {
        if (search.isEmpty()) {
            return 0
        }
        val occurrences =
            withContext(Dispatchers.Default) {
                notallyModel.items.map { it.body }.joinToString(" ").findAllOccurrences(search)
            }
        return occurrences.size
    }

    companion object {
        private const val EXTRA_ITEM_POS = "notallyx.intent.extra.EXTRA_ITEM_POS"
        private const val EXTRA_SELECTION_START = "notallyx.intent.extra.EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "notallyx.intent.extra.EXTRA_SELECTION_END"
    }
}
