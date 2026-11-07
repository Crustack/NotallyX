package com.philkes.notallyx.feature.main.routes.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.reminder.RemindersScreen
import com.philkes.notallyx.feature.reminder.RemindersSideEffect
import com.philkes.notallyx.feature.reminder.RemindersViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RemindersRoute(
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    remindersViewModel: RemindersViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by remindersViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        remindersViewModel.sideEffects.collect { effect ->
            when (effect) {
                is RemindersSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is RemindersSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is RemindersSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    RemindersScreen(
        state = effectiveState,
        onAction = remindersViewModel::onAction,
        modifier = modifier,
    )
}
