package com.philkes.notallyx.feature.main.routes.deleted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDeletedScreen(
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val notes by noteDao.getByFolder(Folder.DELETED).collectAsState(initial = emptyList())
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_DELETED),
        topBar = {
            TopAppBar(
                title = { Text("Trash", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEmptyTrashDialog = true },
                        modifier = Modifier.padding(end = 8.dp).testTag(TestTags.DELETE_ALL_BUTTON),
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier =
                    Modifier.fillMaxSize().padding(innerPadding).testTag(TestTags.EMPTY_NOTES),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Trash is empty",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().testTag(TestTags.NOTES_GRID),
            ) {
                items(notes, key = { it.id }) { note ->
                    Box {
                        BaseNoteCard(
                            baseNote = note,
                            isGrid = true,
                            config = NoteCardDisplayConfig(),
                            modifier = Modifier.testTag(TestTags.NOTE_CARD),
                            onClick = {},
                        )
                        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        noteDao.insertSafe(note.copy(folder = Folder.NOTES))
                                    }
                                },
                                modifier = Modifier.testTag(TestTags.RESTORE_NOTE_BUTTON),
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore note")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) { noteDao.delete(note.id) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = "Delete permanently",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyTrashDialog) {
        ConfirmDialog(
            title = "Empty Trash",
            message = "Are you sure you want to permanently delete all notes in Trash?",
            positiveButtonText = "Delete All",
            onDismissRequest = { showEmptyTrashDialog = false },
            onConfirm = {
                scope.launch(Dispatchers.IO) { notes.forEach { noteDao.delete(it.id) } }
                showEmptyTrashDialog = false
            },
        )
    }
}
