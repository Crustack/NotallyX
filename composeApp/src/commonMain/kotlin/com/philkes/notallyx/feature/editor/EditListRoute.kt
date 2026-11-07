package com.philkes.notallyx.feature.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EditListRoute(
    noteId: Long,
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var existingNote by remember { mutableStateOf<BaseNote?>(null) }
    var title by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<ListItem>() }
    var color by remember { mutableStateOf(BaseNote.COLOR_DEFAULT) }
    var pinned by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<EditorDialogState?>(null) }
    var activeBottomSheet by remember { mutableStateOf<EditorBottomSheetState?>(null) }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val note = noteDao.get(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                color = note.color
                pinned = note.pinned
                items.clear()
                val loadedItems =
                    note.items.mapIndexed { index, item ->
                        if (item.id < 0) item.copy(id = index) else item
                    }
                items.addAll(loadedItems)
            }
        }
    }

    fun saveList() {
        if (title.isBlank() && items.isEmpty()) return
        val now = System.currentTimeMillis()
        val noteToSave =
            existingNote?.copy(
                title = title,
                items = items.toList(),
                color = color,
                pinned = pinned,
                modifiedTimestamp = now,
            )
                ?: BaseNote(
                    id = if (noteId > 0L) noteId else 0L,
                    type = Type.LIST,
                    folder = Folder.NOTES,
                    color = color,
                    title = title,
                    pinned = pinned,
                    timestamp = now,
                    modifiedTimestamp = now,
                    labels = emptyList(),
                    body = "",
                    spans = emptyList(),
                    items = items.toList(),
                    images = emptyList(),
                    files = emptyList(),
                    audios = emptyList(),
                    reminders = emptyList(),
                    viewMode = NoteViewMode.EDIT,
                    isPinnedToStatus = false,
                )
        scope.launch(Dispatchers.IO) {
            val insertedId = noteDao.insertSafe(noteToSave)
            if (existingNote == null && insertedId > 0L) {
                existingNote = noteToSave.copy(id = insertedId)
            }
        }
    }

    val preferences =
        remember(noteId) {
            val topActions =
                if (noteId > 0L) {
                    listOf(EditAction.CHANGE_COLOR, EditAction.DELETE, EditAction.PIN)
                } else {
                    listOf(EditAction.CHANGE_COLOR, EditAction.PIN)
                }
            EditNotePreferences(topActions = topActions)
        }

    val uncheckedItems = items.filter { !it.checked }
    val checkedItems = items.filter { it.checked }

    val uiState =
        EditListUiState(
            title = title,
            items = uncheckedItems,
            checkedItems = checkedItems,
            color = color,
            pinned = pinned,
            type = Type.LIST,
            preferences = preferences,
            activeDialog = activeDialog,
            activeBottomSheet = activeBottomSheet,
        )

    EditListScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is EditListAction.TitleChanged -> {
                    title = action.title
                }
                is EditListAction.AddItemClicked -> {
                    val nextId = (items.maxOfOrNull { it.id } ?: -1) + 1
                    items.add(
                        ListItem(
                            id = nextId,
                            body = "",
                            checked = false,
                            isChild = false,
                            order = items.size,
                            children = mutableListOf(),
                        )
                    )
                }
                is EditListAction.ItemTextChanged -> {
                    val index = items.indexOfFirst {
                        if (action.item.id >= 0) it.id == action.item.id
                        else it.body == action.item.body
                    }
                    if (index != -1) {
                        items[index] = items[index].copy(body = action.newText)
                    }
                }
                is EditListAction.ItemChecked -> {
                    val index = items.indexOfFirst {
                        if (action.item.id >= 0) it.id == action.item.id
                        else it.body == action.item.body
                    }
                    if (index != -1) {
                        items[index] = items[index].copy(checked = action.checked)
                    }
                }
                is EditListAction.ItemDeleted -> {
                    if (action.item.id >= 0) {
                        items.removeAll { it.id == action.item.id }
                    } else {
                        val index = items.indexOfFirst { it.body == action.item.body }
                        if (index != -1) items.removeAt(index)
                    }
                }
                is EditListAction.ItemMove -> {
                    if (action.fromIndex in items.indices && action.toIndex in items.indices) {
                        val moved = items.removeAt(action.fromIndex)
                        items.add(action.toIndex, moved)
                    }
                }
                is EditListAction.ActionClicked -> {
                    when (action.action) {
                        EditAction.PIN -> pinned = !pinned
                        EditAction.CHANGE_COLOR -> {
                            activeDialog =
                                EditorDialogState.ColorPicker(
                                    currentColor = color,
                                    colors =
                                        setOf(
                                            BaseNote.COLOR_DEFAULT,
                                            com.philkes.notallyx.data.model.Color.CORAL
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.ORANGE
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.SAND
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.MINT
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.SAGE
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.FOG
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.STORM
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.DUSK
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.BLOSSOM
                                                .toColorString(),
                                            com.philkes.notallyx.data.model.Color.CLAY
                                                .toColorString(),
                                        ),
                                    onColorSelected = { selectedColor -> color = selectedColor },
                                )
                        }
                        EditAction.DELETE -> {
                            activeDialog =
                                EditorDialogState.DeleteForeverConfirm(
                                    onConfirm = {
                                        scope.launch(Dispatchers.IO) {
                                            if (noteId > 0L) {
                                                noteDao.delete(noteId)
                                            }
                                            onBack()
                                        }
                                    }
                                )
                        }
                        else -> {}
                    }
                }
                is EditListAction.DismissDialog -> {
                    activeDialog = null
                }
                is EditListAction.DismissBottomSheet -> {
                    activeBottomSheet = null
                }
                is EditListAction.BackClicked -> {
                    saveList()
                    onBack()
                }
                else -> {}
            }
        },
        modifier = modifier,
    )
}

@Composable
fun EditListRoute(
    key: NotallyNavKey.EditList,
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) =
    EditListRoute(
        noteId = key.noteId ?: 0L,
        noteDao = noteDao,
        onBack = onBack,
        modifier = modifier,
    )
