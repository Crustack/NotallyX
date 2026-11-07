package com.philkes.notallyx.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.screens.ChecklistItemRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonEditListScreen(
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
    var isPinned by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }

    var showColorDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val note = noteDao.get(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                color = note.color
                isPinned = note.pinned
                items.clear()
                items.addAll(note.items)
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
                pinned = isPinned,
                modifiedTimestamp = now,
            )
                ?: BaseNote(
                    id = if (noteId > 0L) noteId else 0L,
                    type = Type.LIST,
                    folder = Folder.NOTES,
                    color = color,
                    title = title,
                    pinned = isPinned,
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

    fun addItem() {
        val trimmed = newItemText.trim()
        if (trimmed.isNotEmpty()) {
            items.add(
                ListItem(
                    body = trimmed,
                    checked = false,
                    isChild = false,
                    order = items.size,
                    children = mutableListOf(),
                )
            )
            newItemText = ""
            saveList()
        }
    }

    val backgroundColor = parseNoteColor(color)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId > 0L) "Edit Checklist" else "New Checklist",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveList()
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete checklist")
                        }
                    }
                    IconButton(
                        onClick = {
                            saveList()
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
                    saveList()
                },
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.NOTE_TITLE_INPUT),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Add item field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("Add list item…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag(TestTags.CHECKLIST_NEW_ITEM_INPUT),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { addItem() },
                    modifier = Modifier.testTag(TestTags.CHECKLIST_ADD_BUTTON),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checklist items
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = { it.hashCode() }) { item ->
                    ChecklistItemRow(
                        value = item.body,
                        onValueChange = { newText ->
                            val index = items.indexOf(item)
                            if (index != -1) {
                                items[index] = item.copy(body = newText)
                                saveList()
                            }
                        },
                        isChecked = item.checked,
                        onCheckedChange = { checked ->
                            val index = items.indexOf(item)
                            if (index != -1) {
                                items[index] =
                                    item.copy(
                                        checked = checked,
                                        checkedTimestamp =
                                            if (checked) System.currentTimeMillis() else null,
                                    )
                                saveList()
                            }
                        },
                        onDelete = {
                            items.remove(item)
                            saveList()
                        },
                        backgroundColor = backgroundColor,
                        modifier = Modifier.testTag(TestTags.CHECKLIST_ITEM_ROW),
                    )
                }
            }
        }
    }

    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = color,
            onDismissRequest = { showColorDialog = false },
            onColorSelected = { newColor ->
                color = newColor
                showColorDialog = false
                saveList()
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            message = "Are you sure you want to delete this checklist?",
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                scope.launch(Dispatchers.IO) { noteDao.delete(noteId) }
                onBack()
            },
        )
    }
}
