package com.philkes.notallyx.feature.main.routes.archived

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonArchivedScreen(
    noteDao: BaseNoteDao,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val notes by noteDao.getByFolder(Folder.ARCHIVED).collectAsState(initial = emptyList())

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
                    text = "No archived notes",
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
                            onClick = {
                                if (note.type == Type.LIST) {
                                    onOpenList(note.id)
                                } else {
                                    onOpenNote(note.id)
                                }
                            },
                        )
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    noteDao.insertSafe(note.copy(folder = Folder.NOTES))
                                }
                            },
                            modifier =
                                Modifier.align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .testTag(TestTags.RESTORE_NOTE_BUTTON),
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Unarchive note")
                        }
                    }
                }
            }
        }
    }
}
