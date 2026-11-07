package com.philkes.notallyx.feature.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
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
    var activeDialog by remember { mutableStateOf<EditorDialogState?>(null) }
    var activeBottomSheet by remember { mutableStateOf<EditorBottomSheetState?>(null) }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val note = noteDao.get(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                bodyValue = TextFieldValue(note.body)
                color = note.color
                pinned = note.pinned
            }
        }
    }

    fun saveNote() {
        if (title.isBlank() && bodyValue.text.isBlank()) return
        val now = System.currentTimeMillis()
        val noteToSave =
            existingNote?.copy(
                title = title,
                body = bodyValue.text,
                color = color,
                pinned = pinned,
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
                    labels = emptyList(),
                    body = bodyValue.text,
                    spans = emptyList(),
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

    val uiState =
        EditNoteUiState(
            title = title,
            bodyValue = bodyValue,
            color = color,
            pinned = pinned,
            type = Type.NOTE,
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
                    bodyValue = action.bodyValue
                }
                is EditNoteAction.ActionClicked -> {
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
