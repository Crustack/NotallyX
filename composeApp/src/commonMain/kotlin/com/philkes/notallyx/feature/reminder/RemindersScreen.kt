package com.philkes.notallyx.feature.reminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.notifications
import com.philkes.notallyx.please_grant_notally_alarm
import com.philkes.notallyx.please_grant_notally_notification
import org.jetbrains.compose.resources.stringResource

/** Screen to show notes with their reminders following MVI architecture. */
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onAction: (RemindersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (!state.isNotificationPermissionGranted) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.please_grant_notally_notification),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (!state.canScheduleExactAlarms) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.please_grant_notally_alarm),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        NotesGridScreen(
            items = state.items,
            modifier = Modifier.weight(1f),
            isGrid = state.isGrid,
            selectedNoteIds = state.selectedIds,
            displayConfig = state.displayConfig,
            backgroundDrawable = Res.drawable.notifications,
            selectedFilterOption = state.filterMode,
            isLoading = state.isLoading,
            onFilterOptionSelected = { onAction(RemindersAction.FilterOptionSelected(it)) },
            onNoteClick = { note, pos -> onAction(RemindersAction.NoteClicked(note, pos)) },
            onNoteLongClick = { note, pos, items ->
                onAction(RemindersAction.NoteLongClicked(note, pos, items))
            },
            onReminderClick = { note, _ -> onAction(RemindersAction.ReminderClicked(note)) },
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun RemindersScreenPreview() {
    NotallyXTheme {
        RemindersScreen(
            state =
                RemindersUiState(
                    items = listOf(SampleNotes.RICH_TEXT_NOTE),
                    selectedIds = setOf(),
                    isGrid = false,
                    displayConfig = NoteCardDisplayConfig(),
                ),
            onAction = {},
        )
    }
}
