package com.philkes.notallyx.feature.main.routes.deleted

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedRoute(
    noteDao: BaseNoteDao,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val notes by noteDao.getByFolder(Folder.DELETED).collectAsState(initial = emptyList())
    var activeDialog by remember { mutableStateOf<DeletedDialogState?>(null) }

    val state =
        remember(notes, activeDialog, displayConfig) {
            DeletedUiState(
                items = notes,
                isGrid = true,
                displayConfig = displayConfig,
                activeDialog = activeDialog,
            )
        }

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
                        onClick = { activeDialog = DeletedDialogState.ConfirmDeleteAll },
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DeletedScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is DeletedAction.DismissDialog -> activeDialog = null
                        is DeletedAction.RequestDeleteAll ->
                            activeDialog = DeletedDialogState.ConfirmDeleteAll
                        is DeletedAction.ConfirmDeleteAll -> {
                            scope.launch(Dispatchers.IO) { notes.forEach { noteDao.delete(it.id) } }
                            activeDialog = null
                        }
                        is DeletedAction.NoteClicked -> {}
                        is DeletedAction.NoteLongClicked -> {
                            scope.launch(Dispatchers.IO) {
                                noteDao.insertSafe(action.note.copy(folder = Folder.NOTES))
                            }
                        }
                        is DeletedAction.ReminderClicked -> {}
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
