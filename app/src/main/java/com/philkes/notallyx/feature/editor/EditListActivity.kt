package com.philkes.notallyx.feature.editor

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.RecyclerView
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
import com.philkes.notallyx.feature.editor.listitem.CheckedListItemAdapter
import com.philkes.notallyx.feature.editor.listitem.ListItemAdapter
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

    private var adapter: ListItemAdapter? = null
    private var adapterChecked: CheckedListItemAdapter? = null
    private val items: MutableList<ListItem>
        get() = notallyModel.items

    private var itemsChecked: SortedItemsList? = null
    private lateinit var listManager: ListManager
    private val listActionHandler: NoteListActionHandler by lazy {
        NoteListActionHandler(listManager)
    }

    override fun finish() {
        notallyModel.setItems(items.toMutableList() + (itemsChecked?.toMutableList() ?: listOf()))
        super.finish()
    }

    override fun updateModel() {
        super.updateModel()
        notallyModel.setItems(items.toMutableList() + (itemsChecked?.toMutableList() ?: listOf()))
    }

    override fun toggleCanEdit(mode: NoteViewMode) {
        super.toggleCanEdit(mode)
        adapter?.viewMode = mode
        adapterChecked?.viewMode = mode
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
        if (notallyModel.items.isEmpty()) {
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
        val viewModeState = notallyModel.viewMode.collectAsStateWithLifecycle(NoteViewMode.EDIT)

        val noteState =
            EditNoteState(
                title = titleState.value,
                items = notallyModel.items.filter { !it.checked },
                checkedItems = notallyModel.items.filter { it.checked },
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

                    override fun onItemCheckedChange(item: ListItem, checked: Boolean) {
                        val pos = notallyModel.items.indexOf(item)
                        if (pos != -1) listManager.changeChecked(pos, checked, item.checked)
                    }

                    override fun onItemDelete(item: ListItem) {
                        val pos = notallyModel.items.indexOf(item)
                        if (pos != -1) listManager.delete(pos, item.checked)
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
        val dummyRecyclerView = RecyclerView(this)
        val dummyScrollView = androidx.core.widget.NestedScrollView(this)
        val elevation = resources.displayMetrics.density * 2
        listManager =
            ListManager(
                dummyRecyclerView,
                changeHistory,
                preferences,
                inputMethodManager,
                { if (isInSearchMode()) endSearch() },
                { _ ->
                    if (isInSearchMode() && search.results.value > 0)
                        updateSearchResults(search.query)
                },
                { items -> updateJumpButtonsVisibility(items) },
            )
        adapter =
            ListItemAdapter(
                colorInt,
                notallyModel.textSize,
                elevation,
                preferences,
                listManager,
                false,
                dummyScrollView,
            )
        val initializedItems = notallyModel.items.init(true)
        if (preferences.autoSortByCheckedEnabled) {
            val (checkedItems, uncheckedItems) = initializedItems.splitByChecked()
            adapter?.submitList(uncheckedItems.toMutableList())
            adapterChecked =
                CheckedListItemAdapter(
                    colorInt,
                    notallyModel.textSize,
                    elevation,
                    preferences,
                    listManager,
                    true,
                    dummyScrollView,
                )
            itemsChecked =
                SortedItemsList(preferences.listItemSorting.value.callback(adapterChecked!!))
                    .apply { setItems(checkedItems.toMutableList()) }
            adapterChecked?.setList(itemsChecked!!)
        } else {
            adapter?.submitList(initializedItems.toMutableList())
        }
        listManager.init(adapter!!, itemsChecked, adapterChecked)
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
