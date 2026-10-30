package com.philkes.notallyx.feature.main.routes.labels

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
fun UnlabeledRoute(
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    unlabeledViewModel: UnlabeledViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by unlabeledViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        unlabeledViewModel.sideEffects.collect { effect ->
            when (effect) {
                is UnlabeledSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is UnlabeledSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is UnlabeledSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    UnlabeledScreen(
        state = effectiveState,
        onAction = unlabeledViewModel::onAction,
        modifier = modifier,
    )
}
