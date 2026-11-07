package com.philkes.notallyx.feature.main.routes.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotesRoute(
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    notesViewModel: NotesViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by notesViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        notesViewModel.sideEffects.collect { effect ->
            when (effect) {
                is NotesSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is NotesSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is NotesSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    NotesScreen(
        state = effectiveState,
        onAction = notesViewModel::onAction,
        modifier = modifier,
    )
}
