package com.philkes.notallyx.feature.main.routes.archived

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArchivedRoute(
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    archivedViewModel: ArchivedViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by archivedViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        archivedViewModel.sideEffects.collect { effect ->
            when (effect) {
                is ArchivedSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is ArchivedSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is ArchivedSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    ArchivedScreen(
        state = effectiveState,
        onAction = archivedViewModel::onAction,
        modifier = modifier,
    )
}
