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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.extensions.addOrUpdateLink
import com.philkes.notallyx.core.ui.extensions.clearFormatting
import com.philkes.notallyx.core.ui.extensions.removeLink
import com.philkes.notallyx.core.ui.extensions.toAnnotatedString
import com.philkes.notallyx.core.ui.extensions.toSpanRepresentations
import com.philkes.notallyx.core.ui.extensions.toggleBold
import com.philkes.notallyx.core.ui.extensions.toggleItalic
import com.philkes.notallyx.core.ui.extensions.toggleMonospace
import com.philkes.notallyx.core.ui.extensions.toggleStrikethrough
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.formatting.getFormattingState
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EditNoteRoute(
    noteId: Long,
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var existingNote by remember { mutableStateOf<BaseNote?>(null) }
    var title by remember { mutableStateOf("") }
    var bodyValue by remember { mutableStateOf(TextFieldValue("")) }
    var color by remember { mutableStateOf(BaseNote.COLOR_DEFAULT) }
    var pinned by remember { mutableStateOf(false) }
    var labels by remember { mutableStateOf(emptyList<String>()) }
    var isFormattingMenuVisible by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<EditorDialogState?>(null) }
    var activeBottomSheet by remember { mutableStateOf<EditorBottomSheetState?>(null) }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    var lastSelection by remember { mutableStateOf(TextRange.Zero) }
    var searchState by remember {
        mutableStateOf(com.philkes.notallyx.feature.editor.screens.SearchState())
    }

    fun recordUndo() {
        undoStack.add(bodyValue)
        redoStack.clear()
    }

    fun onBodyChange(newVal: TextFieldValue) {
        if (newVal.selection.min < newVal.selection.max) {
            lastSelection = newVal.selection
        }
        if (bodyValue.text != newVal.text || bodyValue.annotatedString != newVal.annotatedString) {
            recordUndo()
        }
        bodyValue = newVal
    }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val note = noteDao.get(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                bodyValue = TextFieldValue(note.body.toAnnotatedString(note.spans))
                color = note.color
                pinned = note.pinned
                labels = note.labels
            }
        }
    }

    fun saveNote() {
        if (title.isBlank() && bodyValue.text.isBlank()) return
        val now = System.currentTimeMillis()
        val spans = bodyValue.annotatedString.toSpanRepresentations()
        val noteToSave =
            existingNote?.copy(
                title = title,
                body = bodyValue.text,
                color = color,
                pinned = pinned,
                labels = labels,
                spans = spans,
                modifiedTimestamp = now,
            )
                ?: BaseNote(
                    id = if (noteId > 0L) noteId else 0L,
                    type = Type.NOTE,
                    folder = Folder.NOTES,
                    color = color,
                    title = title,
                    pinned = pinned,
                    timestamp = now,
                    modifiedTimestamp = now,
                    labels = labels,
                    body = bodyValue.text,
                    spans = spans,
                    items = emptyList(),
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

    val formattingState =
        remember(bodyValue) {
            bodyValue.annotatedString.getFormattingState(
                bodyValue.selection.min,
                bodyValue.selection.max,
            )
        }

    val preferences =
        remember(noteId) {
            val topActions =
                if (noteId > 0L) {
                    listOf(
                        EditAction.SEARCH,
                        EditAction.CHANGE_COLOR,
                        EditAction.DELETE,
                        EditAction.PIN,
                    )
                } else {
                    listOf(
                        EditAction.SEARCH,
                        EditAction.CHANGE_COLOR,
                        EditAction.PIN,
                    )
                }
            EditNotePreferences(topActions = topActions)
        }

    val uiState =
        EditNoteUiState(
            title = title,
            bodyValue = bodyValue,
            color = color,
            pinned = pinned,
            type = Type.NOTE,
            labels = labels,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            isFormattingMenuVisible = isFormattingMenuVisible,
            formattingState = formattingState,
            searchState = searchState,
            preferences = preferences,
            activeDialog = activeDialog,
            activeBottomSheet = activeBottomSheet,
        )

    EditNoteScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is EditNoteAction.TitleChanged -> {
                    title = action.title
                }
                is EditNoteAction.BodyChanged -> {
                    onBodyChange(action.bodyValue)
                }
                is EditNoteAction.ToggleFormattingMenu -> {
                    isFormattingMenuVisible = !isFormattingMenuVisible
                }
                is EditNoteAction.CloseFormattingMenu -> {
                    isFormattingMenuVisible = false
                }
                is EditNoteAction.ToggleBold -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val res = bodyValue.copy(selection = sel).toggleBold()
                    lastSelection = res.selection
                    onBodyChange(res)
                }
                is EditNoteAction.ToggleItalic -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val res = bodyValue.copy(selection = sel).toggleItalic()
                    lastSelection = res.selection
                    onBodyChange(res)
                }
                is EditNoteAction.ToggleStrikethrough -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val res = bodyValue.copy(selection = sel).toggleStrikethrough()
                    lastSelection = res.selection
                    onBodyChange(res)
                }
                is EditNoteAction.ToggleMonospace -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val res = bodyValue.copy(selection = sel).toggleMonospace()
                    lastSelection = res.selection
                    onBodyChange(res)
                }
                is EditNoteAction.ClearFormatting -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val res = bodyValue.copy(selection = sel).clearFormatting()
                    lastSelection = res.selection
                    onBodyChange(res)
                }
                is EditNoteAction.RequestLinkDialog -> {
                    val sel =
                        if (bodyValue.selection.min < bodyValue.selection.max) bodyValue.selection
                        else lastSelection
                    val selectedText =
                        if (sel.min < sel.max) {
                            bodyValue.annotatedString.text.substring(
                                sel.min,
                                sel.max,
                            )
                        } else ""
                    activeDialog =
                        EditorDialogState.EditLink(
                            urlBefore = "",
                            displayTextBefore = selectedText,
                            isNoteUrl = false,
                            onConfirm = { url, displayText ->
                                if (!url.isNullOrBlank()) {
                                    val target = bodyValue.copy(selection = sel)
                                    val res = target.addOrUpdateLink(url, displayText)
                                    lastSelection = res.selection
                                    onBodyChange(res)
                                }
                                activeDialog = null
                            },
                            onDismiss = { activeDialog = null },
                        )
                }
                is EditNoteAction.LinkClicked -> {
                    activeDialog =
                        EditorDialogState.LinkOptions(
                            url = action.url,
                            displayText = action.displayText,
                            isNoteUrl = false,
                            onOpen = { activeDialog = null },
                            onEdit = {
                                activeDialog =
                                    EditorDialogState.EditLink(
                                        urlBefore = action.url,
                                        displayTextBefore = action.displayText,
                                        isNoteUrl = false,
                                        onConfirm = { url, displayText ->
                                            if (!url.isNullOrBlank()) {
                                                onBodyChange(
                                                    bodyValue.addOrUpdateLink(
                                                        url,
                                                        displayText,
                                                        action.range,
                                                    )
                                                )
                                            } else {
                                                onBodyChange(bodyValue.removeLink(action.range))
                                            }
                                            activeDialog = null
                                        },
                                        onDismiss = { activeDialog = null },
                                    )
                            },
                            onRemove = {
                                onBodyChange(bodyValue.removeLink(action.range))
                                activeDialog = null
                            },
                        )
                }
                is EditNoteAction.Undo -> {
                    if (undoStack.isNotEmpty()) {
                        redoStack.add(bodyValue)
                        bodyValue = undoStack.removeAt(undoStack.lastIndex)
                    }
                }
                is EditNoteAction.Redo -> {
                    if (redoStack.isNotEmpty()) {
                        undoStack.add(bodyValue)
                        bodyValue = redoStack.removeAt(redoStack.lastIndex)
                    }
                }
                is EditNoteAction.StartSearch -> {
                    searchState =
                        com.philkes.notallyx.feature.editor.screens.SearchState(
                            isActive = true,
                            query = "",
                        )
                }
                is EditNoteAction.SearchQueryChanged -> {
                    val count =
                        if (action.query.isBlank()) 0
                        else {
                            var c = 0
                            var idx = bodyValue.text.indexOf(action.query, ignoreCase = true)
                            while (idx != -1) {
                                c++
                                idx =
                                    bodyValue.text.indexOf(action.query, idx + 1, ignoreCase = true)
                            }
                            c
                        }
                    searchState =
                        com.philkes.notallyx.feature.editor.screens.SearchState(
                            isActive = true,
                            query = action.query,
                            resultsCount = count,
                            resultPos = 0,
                        )
                }
                is EditNoteAction.NextSearchResult -> {
                    if (searchState.resultsCount > 0) {
                        val nextPos = (searchState.resultPos + 1) % searchState.resultsCount
                        searchState = searchState.copy(resultPos = nextPos)
                    }
                }
                is EditNoteAction.PrevSearchResult -> {
                    if (searchState.resultsCount > 0) {
                        val prevPos =
                            if (searchState.resultPos <= 0) searchState.resultsCount - 1
                            else searchState.resultPos - 1
                        searchState = searchState.copy(resultPos = prevPos)
                    }
                }
                is EditNoteAction.CloseSearch -> {
                    searchState =
                        com.philkes.notallyx.feature.editor.screens.SearchState(isActive = false)
                }
                is EditNoteAction.LabelsSelected -> {
                    labels = action.selectedLabels
                }
                is EditNoteAction.ActionClicked -> {
                    when (action.action) {
                        EditAction.PIN -> pinned = !pinned
                        EditAction.SEARCH -> {
                            searchState =
                                com.philkes.notallyx.feature.editor.screens.SearchState(
                                    isActive = !searchState.isActive,
                                    query = "",
                                )
                        }
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
                is EditNoteAction.DismissDialog -> {
                    activeDialog = null
                }
                is EditNoteAction.DismissBottomSheet -> {
                    activeBottomSheet = null
                }
                is EditNoteAction.BackClicked -> {
                    saveNote()
                    onBack()
                }
                else -> {}
            }
        },
        modifier = modifier,
    )
}

@Composable
fun EditNoteRoute(
    key: NotallyNavKey.EditNote,
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) =
    EditNoteRoute(
        noteId = key.noteId ?: 0L,
        noteDao = noteDao,
        onBack = onBack,
        modifier = modifier,
    )
