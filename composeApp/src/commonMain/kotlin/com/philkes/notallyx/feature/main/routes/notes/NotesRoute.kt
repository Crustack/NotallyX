package com.philkes.notallyx.feature.main.routes.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.archive
import com.philkes.notallyx.cancel
import com.philkes.notallyx.change_color
import com.philkes.notallyx.checkbox
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.delete
import com.philkes.notallyx.edit
import com.philkes.notallyx.feature.editor.EditorDialogState
import com.philkes.notallyx.feature.editor.screens.EditorDialogs
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.settings.dialogs.NotesSortDialog
import com.philkes.notallyx.grid
import com.philkes.notallyx.list
import com.philkes.notallyx.make_list
import com.philkes.notallyx.notes_sorted_by
import com.philkes.notallyx.pin
import com.philkes.notallyx.take_note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    isGrid: Boolean = true,
    onToggleGrid: ((Boolean) -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    onCreateNote: () -> Unit = { onOpenNote(0L) },
    onCreateList: () -> Unit = { onOpenList(0L) },
) {
    val scope = rememberCoroutineScope()
    val notes by noteDao.getByFolder(Folder.NOTES).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var localIsGrid by remember(isGrid) { mutableStateOf(isGrid) }
    var notesSort by remember {
        mutableStateOf(NotesSort(NotesSortBy.CREATION_DATE, SortDirection.DESC))
    }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    var activeDialog by remember { mutableStateOf<EditorDialogState?>(null) }

    val filteredNotes =
        remember(notes, searchQuery, notesSort) {
            val matching =
                if (searchQuery.isBlank()) {
                    notes
                } else {
                    notes.filter { note ->
                        note.title.contains(searchQuery, ignoreCase = true) ||
                            note.body.contains(searchQuery, ignoreCase = true) ||
                            note.items.any { it.body.contains(searchQuery, ignoreCase = true) }
                    }
                }
            val comparator: Comparator<BaseNote> =
                when (notesSort.sortedBy) {
                    NotesSortBy.CREATION_DATE -> compareBy { it.timestamp }
                    NotesSortBy.MODIFIED_DATE -> compareBy { it.modifiedTimestamp }
                    NotesSortBy.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                    NotesSortBy.COLOR -> compareBy { it.color }
                }
            if (notesSort.sortDirection == SortDirection.DESC) {
                matching.sortedWith(comparator.reversed())
            } else {
                matching.sortedWith(comparator)
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_NOTES),
        topBar = {
            if (selectedNoteIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text(text = "${selectedNoteIds.size}") },
                    navigationIcon = {
                        IconButton(
                            onClick = { selectedNoteIds = emptySet() },
                            modifier = Modifier.testTag("CloseSelection"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.cancel),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val idsToToggle = selectedNoteIds.toList()
                                selectedNoteIds = emptySet()
                                scope.launch(Dispatchers.IO) {
                                    idsToToggle.forEach { id ->
                                        val n = noteDao.get(id)
                                        if (n != null) {
                                            noteDao.insertSafe(n.copy(pinned = !n.pinned))
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(Res.string.pin),
                            )
                        }
                        IconButton(
                            onClick = {
                                activeDialog =
                                    EditorDialogState.ColorPicker(
                                        currentColor = BaseNote.COLOR_DEFAULT,
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
                                            ),
                                        onColorSelected = { selectedColor ->
                                            val idsToColor = selectedNoteIds.toList()
                                            selectedNoteIds = emptySet()
                                            activeDialog = null
                                            scope.launch(Dispatchers.IO) {
                                                idsToColor.forEach { id ->
                                                    val n = noteDao.get(id)
                                                    if (n != null) {
                                                        noteDao.insertSafe(
                                                            n.copy(color = selectedColor)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                    )
                            },
                            modifier = Modifier.testTag(TestTags.COLOR_PICKER_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = stringResource(Res.string.change_color),
                            )
                        }
                        IconButton(
                            onClick = {
                                val idsToArchive = selectedNoteIds.toLongArray()
                                selectedNoteIds = emptySet()
                                scope.launch(Dispatchers.IO) {
                                    noteDao.move(idsToArchive, Folder.ARCHIVED)
                                }
                            },
                            modifier = Modifier.testTag(TestTags.NAV_ARCHIVED),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = stringResource(Res.string.archive),
                            )
                        }
                        IconButton(
                            onClick = {
                                val idsToDelete = selectedNoteIds.toLongArray()
                                selectedNoteIds = emptySet()
                                scope.launch(Dispatchers.IO) {
                                    noteDao.move(idsToDelete, Folder.DELETED)
                                }
                            },
                            modifier = Modifier.testTag(TestTags.DELETE_NOTE_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.delete),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier.testTag(TestTags.NAVIGATION),
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NotallyX",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val next = !localIsGrid
                                localIsGrid = next
                                onToggleGrid?.invoke(next)
                            },
                            modifier = Modifier.testTag(TestTags.SETTINGS_NOTES_VIEW),
                        ) {
                            Icon(
                                imageVector =
                                    if (localIsGrid) Icons.AutoMirrored.Filled.ViewList
                                    else Icons.Default.GridView,
                                contentDescription =
                                    stringResource(
                                        if (localIsGrid) Res.string.list else Res.string.grid
                                    ),
                            )
                        }
                        IconButton(
                            onClick = { showSortDialog = true },
                            modifier = Modifier.testTag(TestTags.SETTINGS_SORTING),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(Res.string.notes_sorted_by),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                )
            }
        },
        floatingActionButton = {
            if (selectedNoteIds.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = onCreateList,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.checkbox),
                            contentDescription = stringResource(Res.string.make_list),
                        )
                    }
                    FloatingActionButton(
                        onClick = onCreateNote,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.edit),
                            contentDescription = stringResource(Res.string.take_note),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NotesGridScreen(
                items = filteredNotes,
                isGrid = localIsGrid,
                displayConfig = displayConfig,
                showSearchBar = true,
                searchKeyword = searchQuery,
                onSearchKeywordChange = { searchQuery = it },
                selectedNoteIds = selectedNoteIds,
                onNoteClick = { note, _ ->
                    if (selectedNoteIds.isNotEmpty()) {
                        selectedNoteIds =
                            if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id
                            else selectedNoteIds + note.id
                    } else if (note.type == Type.LIST) {
                        onOpenList(note.id)
                    } else {
                        onOpenNote(note.id)
                    }
                },
                onNoteLongClick = { note, _, _ ->
                    selectedNoteIds =
                        if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id
                        else selectedNoteIds + note.id
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (showSortDialog) {
                NotesSortDialog(
                    currentSort = notesSort,
                    onConfirm = { sort ->
                        notesSort = sort
                        showSortDialog = false
                    },
                    onDismissRequest = { showSortDialog = false },
                )
            }

            EditorDialogs(
                activeDialog = activeDialog,
                onDismissRequest = { activeDialog = null },
            )
        }
    }
}
