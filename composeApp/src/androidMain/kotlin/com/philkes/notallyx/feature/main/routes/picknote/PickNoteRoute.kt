package com.philkes.notallyx.feature.main.routes.picknote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.main.PickNoteScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PickNoteRoute(
    excludeNoteId: Long?,
    onNotePicked: (BaseNote) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickNoteViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(excludeNoteId) { viewModel.onAction(PickNoteAction.Init(excludeNoteId)) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is PickNoteSideEffect.NotePicked -> onNotePicked(effect.note)
                is PickNoteSideEffect.NavigateBack -> onBackClick()
            }
        }
    }

    PickNoteScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
