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
fun DisplayLabelRoute(
    label: String,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    displayLabelViewModel: DisplayLabelViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by displayLabelViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(label) { displayLabelViewModel.onAction(DisplayLabelAction.SetLabel(label)) }

    LaunchedEffect(Unit) {
        displayLabelViewModel.sideEffects.collect { effect ->
            when (effect) {
                is DisplayLabelSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is DisplayLabelSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is DisplayLabelSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    DisplayLabelScreen(
        state = effectiveState,
        onAction = displayLabelViewModel::onAction,
        modifier = modifier,
    )
}
