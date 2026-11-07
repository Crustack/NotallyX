package com.philkes.notallyx.feature.main.routes.archived

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val notes by noteDao.getByFolder(Folder.ARCHIVED).collectAsState(initial = emptyList())

    val state =
        remember(notes) {
            ArchivedUiState(
                items = notes,
                isGrid = true,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_ARCHIVED),
        topBar = {
            TopAppBar(
                title = { Text("Archived Notes", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    notes.firstOrNull()?.let { note ->
                                        noteDao.insertSafe(note.copy(folder = Folder.NOTES))
                                    }
                                }
                            },
                            modifier = Modifier.testTag(TestTags.RESTORE_NOTE_BUTTON),
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Unarchive note")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ArchivedScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is ArchivedAction.NoteClicked -> {
                            if (action.note.type == Type.LIST) {
                                onOpenList(action.note.id)
                            } else {
                                onOpenNote(action.note.id)
                            }
                        }
                        is ArchivedAction.NoteLongClicked -> {
                            scope.launch(Dispatchers.IO) {
                                noteDao.insertSafe(action.note.copy(folder = Folder.NOTES))
                            }
                        }
                        is ArchivedAction.ReminderClicked -> {}
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
