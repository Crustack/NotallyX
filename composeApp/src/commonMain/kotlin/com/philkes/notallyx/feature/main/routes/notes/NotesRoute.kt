package com.philkes.notallyx.feature.main.routes.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
) {
    val notes by noteDao.getByFolder(Folder.NOTES).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes =
        remember(notes, searchQuery) {
            if (searchQuery.isBlank()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(searchQuery, ignoreCase = true) ||
                        note.body.contains(searchQuery, ignoreCase = true) ||
                        note.items.any { it.body.contains(searchQuery, ignoreCase = true) }
                }
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_NOTES),
        topBar = {
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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NotesGridScreen(
                items = filteredNotes,
                isGrid = true,
                showSearchBar = true,
                searchKeyword = searchQuery,
                onSearchKeywordChange = { searchQuery = it },
                onNoteClick = { note, _ ->
                    if (note.type == Type.LIST) {
                        onOpenList(note.id)
                    } else {
                        onOpenNote(note.id)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
