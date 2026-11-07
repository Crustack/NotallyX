package com.philkes.notallyx.feature.labels

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.extensions.showToast
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectLabelsRoute(
    initialLabels: List<String>,
    onLabelsSelected: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectLabelsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialLabels) { viewModel.onAction(SelectLabelsAction.Init(initialLabels)) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SelectLabelsSideEffect.ShowToast -> {
                    context.showToast(effect.messageRes)
                }

                is SelectLabelsSideEffect.FinishWithResult -> {
                    onLabelsSelected(effect.selectedLabels)
                    onBackClick()
                }
            }
        }
    }

    BackHandler { viewModel.onAction(SelectLabelsAction.BackClicked) }

    SelectLabelsScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.EXTRA_SELECTED_LABELS"
