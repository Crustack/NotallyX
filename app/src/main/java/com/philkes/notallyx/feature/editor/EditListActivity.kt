package com.philkes.notallyx.feature.editor

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.autoSortByCheckedEnabled
import com.philkes.notallyx.core.preference.callback
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.bottomsheet.MoreListBottomSheet
import com.philkes.notallyx.feature.editor.listitem.ListManager
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.editor.listitem.setItems
import com.philkes.notallyx.feature.editor.listitem.sorting.SortedItemsList
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.feature.editor.listitem.toMutableList
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditListActivity : EditActivity(Type.LIST) {

    private val items: MutableList<ListItem>
        get() = if (::listManager.isInitialized) listManager.items else notallyModel.items

    private var itemsChecked: SortedItemsList? = null
    private lateinit var listManager: ListManager
    private val listActionHandler: NoteListActionHandler by lazy {
        NoteListActionHandler(listManager)
    }

    override fun finish() {
        if (::listManager.isInitialized) {
            notallyModel.setItems(
                items.toMutableList() + (itemsChecked?.toMutableList() ?: listOf())
            )
        }
        super.finish()
    }

    override fun updateModel() {
        super.updateModel()
        if (::listManager.isInitialized) {
            notallyModel.setItems(
                items.toMutableList() + (itemsChecked?.toMutableList() ?: listOf())
            )
        }
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
                listActionHandler,
                topActions,
                bottomAction,
            )
            .show(supportFragmentManager, MoreListBottomSheet.TAG)
    }

    override fun selectSearchResult(resultPos: Int) {}

    override fun configureUI() {
        if (
            ::listManager.isInitialized &&
                listManager.items.isEmpty() &&
                (itemsChecked?.size() ?: 0) == 0
        ) {
            listManager.add(pushChange = false)
        }
    }

    @Composable
    override fun RenderScreen() {
        val imagesState = notallyModel.images.collectAsStateWithLifecycle(emptyList())
        val filesState = notallyModel.files.collectAsStateWithLifecycle(emptyList())
        val audiosState = notallyModel.audios.collectAsStateWithLifecycle(emptyList())
        val remindersState = notallyModel.reminders.collectAsStateWithLifecycle(emptyList())

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

        // Read stackPointerState to observe changeHistory state updates
        stackPointerState.value

        val noteState =
            EditNoteState(
                title = titleState.value,
                items =
                    if (::listManager.isInitialized) listManager.items
                    else notallyModel.items.filter { !it.checked },
                checkedItems =
                    itemsChecked?.toMutableList() ?: notallyModel.items.filter { it.checked },
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
                        val pos = listManager.items.indexOf(item)
                        if (pos != -1) {
                            listManager.changeText(pos, text, inCheckedList = false)
                        } else {
                            val checkedPos = itemsChecked?.toMutableList()?.indexOf(item) ?: -1
                            if (checkedPos != -1) {
                                listManager.changeText(checkedPos, text, inCheckedList = true)
                            }
                        }
                    }

                    override fun onItemCheckedChange(item: ListItem, checked: Boolean) {
                        val pos = listManager.items.indexOf(item)
                        if (pos != -1) {
                            listManager.changeChecked(pos, checked, item.checked)
                        } else {
                            val checkedPos = itemsChecked?.toMutableList()?.indexOf(item) ?: -1
                            if (checkedPos != -1) {
                                listManager.changeChecked(checkedPos, checked, inCheckedList = true)
                            }
                        }
                    }

                    override fun onItemDelete(item: ListItem) {
                        val pos = listManager.items.indexOf(item)
                        if (pos != -1) {
                            listManager.delete(pos)
                        } else {
                            val checkedPos = itemsChecked?.toMutableList()?.indexOf(item) ?: -1
                            if (checkedPos != -1) {
                                listManager.delete(checkedPos, inCheckedList = true)
                            }
                        }
                    }

                    override fun onItemMove(fromIndex: Int, toIndex: Int) {
                        if (
                            fromIndex in listManager.items.indices &&
                                toIndex in listManager.items.indices
                        ) {
                            listManager.move(fromIndex, toIndex)
                        }
                    }

                    override fun onItemSwipeRight(item: ListItem) {
                        val pos = listManager.items.indexOf(item)
                        if (pos > 0 && !item.isChild) {
                            listManager.changeIsChild(pos, isChild = true)
                        }
                    }

                    override fun onItemSwipeLeft(item: ListItem) {
                        val pos = listManager.items.indexOf(item)
                        if (pos != -1 && item.isChild) {
                            listManager.changeIsChild(pos, isChild = false)
                        }
                    }

                    override fun onItemMoveFinish(item: ListItem) {
                        val pos = listManager.items.indexOf(item)
                        if (pos != -1) {
                            listManager.finishMove(
                                positionTo = pos,
                                count = 1 + item.children.size,
                                parentBefore = null,
                                stateBefore = listManager.getState(),
                                pushChange = true,
                            )
                        }
                    }
                },
            clickHandlers =
                object : EditClickHandlers {
                    override fun onBackClick() {
                        finish()
                    }

                    override fun onAddItemClick() {
                        listManager.add()
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
        )
    }

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        val initializedItems = notallyModel.items.init(true)
        if (preferences.autoSortByCheckedEnabled) {
            val (checkedItems, uncheckedItems) = initializedItems.splitByChecked()
            itemsChecked =
                SortedItemsList(preferences.listItemSorting.value.callback()).apply {
                    setItems(checkedItems.toMutableList())
                }
            listManager =
                ListManager(
                    items = uncheckedItems.toMutableList(),
                    itemsChecked = itemsChecked,
                    changeHistory = changeHistory,
                    preferences = preferences,
                    endSearch = { if (isInSearchMode()) endSearch() },
                    onItemSizeChanged = { count -> updateJumpButtonsVisibility(count) },
                )
        } else {
            itemsChecked = null
            listManager =
                ListManager(
                    items = initializedItems.toMutableList(),
                    itemsChecked = null,
                    changeHistory = changeHistory,
                    preferences = preferences,
                    endSearch = { if (isInSearchMode()) endSearch() },
                    onItemSizeChanged = { count -> updateJumpButtonsVisibility(count) },
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
