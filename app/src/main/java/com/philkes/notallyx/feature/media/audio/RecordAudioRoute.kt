package com.philkes.notallyx.feature.media.audio

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecordAudioRoute(
    onRecorded: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordAudioViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { viewModel.onAction(RecordAudioAction.BackClicked) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is RecordAudioSideEffect.RecordingFinished -> onRecorded()
                is RecordAudioSideEffect.NavigateBack -> onBackClick()
            }
        }
    }

    RecordAudioScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
