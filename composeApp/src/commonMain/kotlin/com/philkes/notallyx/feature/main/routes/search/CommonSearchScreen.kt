package com.philkes.notallyx.feature.main.routes.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonSearchScreen(
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    initialKeyword: String = "",
    initialFolder: Folder = Folder.NOTES,
    modifier: Modifier = Modifier,
) {
    var keyword by remember { mutableStateOf(initialKeyword) }
    var selectedFolder by remember { mutableStateOf(initialFolder) }

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

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_SEARCH),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = { Text("Search in $selectedFolder…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (keyword.isNotEmpty()) {
                                IconButton(
                                    onClick = { keyword = "" },
                                    modifier = Modifier.testTag(TestTags.SEARCH_CLEAR_BUTTON),
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth().height(52.dp).testTag(TestTags.SEARCH_INPUT),
                    )
                },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Folder filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(Folder.NOTES, Folder.ARCHIVED, Folder.DELETED).forEach { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { selectedFolder = folder },
                        label = {
                            Text(folder.name.lowercase().replaceFirstChar { it.uppercase() })
                        },
                    )
                }
            }

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            if (keyword.isBlank()) "No notes in $selectedFolder"
                            else "No matching results",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag(TestTags.NOTES_GRID),
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        BaseNoteCard(
                            baseNote = note,
                            isGrid = true,
                            config = NoteCardDisplayConfig(),
                            modifier = Modifier.testTag(TestTags.NOTE_CARD),
                            onClick = {
                                if (note.type == Type.LIST) {
                                    onOpenList(note.id)
                                } else {
                                    onOpenNote(note.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
