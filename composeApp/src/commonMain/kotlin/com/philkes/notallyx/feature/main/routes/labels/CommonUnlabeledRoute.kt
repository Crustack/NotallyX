package com.philkes.notallyx.feature.main.routes.labels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.unlabeled
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlabeledRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    onOpenDrawer: () -> Unit = {},
) {
    val notes by noteDao.getByFolder(Folder.NOTES).collectAsState(initial = emptyList())
    val unlabeledNotes = remember(notes) { notes.filter { it.labels.isEmpty() } }

    val state =
        remember(unlabeledNotes, displayConfig) {
            UnlabeledUiState(
                items = unlabeledNotes,
                isGrid = true,
                displayConfig = displayConfig,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_NOTES),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.unlabeled),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            UnlabeledScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is UnlabeledAction.NoteClicked -> {
                            if (action.note.type == Type.LIST) {
                                onOpenList(action.note.id)
                            } else {
                                onOpenNote(action.note.id)
                            }
                        }
                        else -> Unit
                    }
                },
            )
        }
    }
}
