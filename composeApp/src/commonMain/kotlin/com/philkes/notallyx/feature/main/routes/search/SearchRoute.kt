package com.philkes.notallyx.feature.main.routes.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    onBack: () -> Unit = {},
    initialKeyword: String = "",
    initialFolder: Folder? = Folder.NOTES,
) {
    var keyword by remember { mutableStateOf(initialKeyword) }
    var selectedFolder by remember { mutableStateOf(initialFolder ?: Folder.NOTES) }

    val notes by noteDao.getByFolder(selectedFolder).collectAsState(initial = emptyList())

    val filteredNotes =
        remember(notes, keyword) {
            if (keyword.isBlank()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(keyword, ignoreCase = true) ||
                        note.body.contains(keyword, ignoreCase = true) ||
                        note.items.any { it.body.contains(keyword, ignoreCase = true) }
                }
            }
        }

    val state =
        remember(filteredNotes, keyword, selectedFolder, displayConfig) {
            SearchUiState(
                items = filteredNotes,
                searchKeyword = keyword,
                selectedFolder = selectedFolder,
                isGrid = true,
                displayConfig = displayConfig,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_SEARCH),
        topBar = {
            TopAppBar(
                title = { Text("Search", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            SearchScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is SearchAction.KeywordChanged -> keyword = action.keyword
                        is SearchAction.FolderSelected -> selectedFolder = action.folder
                        is SearchAction.NoteClicked -> {
                            if (action.note.type == Type.LIST) {
                                onOpenList(action.note.id)
                            } else {
                                onOpenNote(action.note.id)
                            }
                        }
                        is SearchAction.Init -> {
                            action.folder?.let { selectedFolder = it }
                        }
                        is SearchAction.NoteLongClicked -> {}
                        is SearchAction.ReminderClicked -> {}
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
