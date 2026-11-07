package com.philkes.notallyx.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.components.parseNoteColor
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonEditNoteScreen(
    noteId: Long,
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var existingNote by remember { mutableStateOf<BaseNote?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(BaseNote.COLOR_DEFAULT) }
    var isPinned by remember { mutableStateOf(false) }

    var showColorDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val note = noteDao.get(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                body = note.body
                color = note.color
                isPinned = note.pinned
            }
        }
    }

    fun saveNote() {
        if (title.isBlank() && body.isBlank()) return
        val now = System.currentTimeMillis()
        val noteToSave =
            existingNote?.copy(
                title = title,
                body = body,
                color = color,
                pinned = isPinned,
                modifiedTimestamp = now,
            )
                ?: BaseNote(
                    id = if (noteId > 0L) noteId else 0L,
                    type = Type.NOTE,
                    folder = Folder.NOTES,
                    color = color,
                    title = title,
                    pinned = isPinned,
                    timestamp = now,
                    modifiedTimestamp = now,
                    labels = emptyList(),
                    body = body,
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

    val backgroundColor = parseNoteColor(color)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId > 0L) "Edit Note" else "New Note",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveNote()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showColorDialog = true },
                        modifier = Modifier.testTag(TestTags.COLOR_PICKER_BUTTON),
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Change color")
                    }
                    if (noteId > 0L) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag(TestTags.DELETE_NOTE_BUTTON),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note")
                        }
                    }
                    IconButton(
                        onClick = {
                            saveNote()
                            onBack()
                        },
                        modifier = Modifier.testTag(TestTags.SAVE_AND_CLOSE_BUTTON),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save and Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(backgroundColor)
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    saveNote()
                },
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.NOTE_TITLE_INPUT),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    saveNote()
                },
                placeholder = { Text("Note content…") },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag(TestTags.NOTE_BODY_INPUT),
            )
        }
    }

    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = color,
            onDismissRequest = { showColorDialog = false },
            onColorSelected = { newColor ->
                color = newColor
                showColorDialog = false
                saveNote()
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            message = "Are you sure you want to delete this note?",
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                scope.launch(Dispatchers.IO) { noteDao.delete(noteId) }
                onBack()
            },
        )
    }
}
