package com.philkes.notallyx.feature.reminder

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
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification
import com.philkes.notallyx.reminders
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersRoute(
    noteDao: BaseNoteDao,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
) {
    val notes by noteDao.getByFolder(Folder.NOTES).collectAsState(initial = emptyList())
    var filterMode by remember { mutableStateOf(FilterOptions.UPCOMING) }

    val reminderNotes = remember(notes) { notes.filter { it.reminders.isNotEmpty() } }

    val filteredItems =
        remember(reminderNotes, filterMode) {
            when (filterMode) {
                FilterOptions.ALL ->
                    reminderNotes.sortedWith { n1, n2 ->
                        val sort =
                            n1.compareNextNotification(n2).takeIf { it != 0 }
                                ?: n1.compareLastNotification(n2)
                        -1 * sort
                    }
                FilterOptions.UPCOMING ->
                    reminderNotes
                        .filter { it.reminders.hasAnyUpcomingNotifications() }
                        .sortedWith { n1, n2 -> n1.compareNextNotification(n2) }
                FilterOptions.ELAPSED ->
                    reminderNotes
                        .filter { !it.reminders.hasAnyUpcomingNotifications() }
                        .sortedWith { n1, n2 -> -1 * n1.compareLastNotification(n2) }
            }
        }

    val state =
        RemindersUiState(
            items = filteredItems,
            filterMode = filterMode,
            isGrid = true,
            displayConfig = NoteCardDisplayConfig(),
            canScheduleExactAlarms = true,
            isNotificationPermissionGranted = true,
        )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_DRAWER),
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
                            text = stringResource(Res.string.reminders),
                            style = MaterialTheme.typography.titleLarge,
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
            RemindersScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is RemindersAction.FilterOptionSelected -> filterMode = action.filterOption
                        is RemindersAction.NoteClicked -> {
                            if (action.note.type == Type.LIST) {
                                onOpenList(action.note.id)
                            } else {
                                onOpenNote(action.note.id)
                            }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
