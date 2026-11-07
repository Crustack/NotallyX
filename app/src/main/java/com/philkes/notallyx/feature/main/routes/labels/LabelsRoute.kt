package com.philkes.notallyx.feature.main.routes.labels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.feature.labels.LabelsScreen
import com.philkes.notallyx.feature.labels.LabelsSideEffect
import com.philkes.notallyx.feature.labels.LabelsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LabelsRoute(
    onNavigateToLabel: (String) -> Unit,
    onShowToast: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelsViewModel: LabelsViewModel = koinViewModel(),
) {
    val uiState by labelsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        labelsViewModel.sideEffects.collect { effect ->
            when (effect) {
                is LabelsSideEffect.NavigateToLabel -> onNavigateToLabel(effect.label)
                is LabelsSideEffect.ShowToast -> onShowToast(effect.messageRes)
            }
        }
    }

    LabelsScreen(
        state = uiState,
        onAction = labelsViewModel::onAction,
        modifier = modifier,
    )
}
