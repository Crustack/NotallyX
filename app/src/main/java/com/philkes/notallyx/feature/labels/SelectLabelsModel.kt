package com.philkes.notallyx.feature.labels

import androidx.annotation.StringRes
import com.philkes.notallyx.data.model.Label

data class SelectLabelsUiState(
    val labels: List<Label> = emptyList(),
    val selectedLabels: Set<String> = emptySet(),
    val activeDialog: LabelDialogState? = null,
)

sealed interface SelectLabelsAction {
    data class Init(val initialLabels: List<String>) : SelectLabelsAction

    data class ToggleLabel(val label: String, val isChecked: Boolean) : SelectLabelsAction

    data object RequestAddLabel : SelectLabelsAction

    data object DismissDialog : SelectLabelsAction

    data class ConfirmAddLabel(val label: String) : SelectLabelsAction

    data object BackClicked : SelectLabelsAction
}

sealed interface SelectLabelsSideEffect {
    data class ShowToast(@StringRes val messageRes: Int) : SelectLabelsSideEffect

    data class FinishWithResult(val selectedLabels: List<String>) : SelectLabelsSideEffect
}
