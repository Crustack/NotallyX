package com.philkes.notallyx.feature.main.routes.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchRoute(
    initialFolder: Folder?,
    initialLabel: String?,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = koinViewModel(),
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialFolder, initialLabel) {
        searchViewModel.onAction(SearchAction.Init(initialFolder, initialLabel))
    }

    LaunchedEffect(Unit) {
        searchViewModel.sideEffects.collect { effect ->
            when (effect) {
                is SearchSideEffect.OpenNote -> onNoteClick(effect.note, effect.position)
                is SearchSideEffect.SelectNote ->
                    onNoteLongClick(effect.note, effect.position, effect.items)
                is SearchSideEffect.OpenReminder -> onReminderClick(effect.note)
            }
        }
    }

    val effectiveState =
        if (displayConfig != NoteCardDisplayConfig()) {
            uiState.copy(displayConfig = displayConfig)
        } else {
            uiState
        }

    SearchScreen(
        state = effectiveState,
        onAction = searchViewModel::onAction,
        modifier = modifier,
    )
}
