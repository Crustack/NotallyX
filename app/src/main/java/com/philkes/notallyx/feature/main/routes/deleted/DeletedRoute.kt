package com.philkes.notallyx.feature.main.routes.deleted

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
fun DeletedRoute(
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    deletedViewModel: DeletedViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by deletedViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        deletedViewModel.sideEffects.collect { effect ->
            when (effect) {
                is DeletedSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is DeletedSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is DeletedSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    DeletedScreen(
        state = effectiveState,
        onAction = deletedViewModel::onAction,
        modifier = modifier,
    )
}
