package com.philkes.notallyx.feature.labels

import androidx.annotation.StringRes

data class LabelsUiState(
    val labels: List<LabelData> = emptyList(),
    val activeDialog: LabelDialogState? = null,
)

sealed interface LabelDialogState {
    data object Add : LabelDialogState

    data class Edit(val label: String) : LabelDialogState

    data class Delete(val label: String) : LabelDialogState
}

sealed interface LabelsAction {
    data class LabelClicked(val label: LabelData) : LabelsAction

    data object RequestAddLabel : LabelsAction

    data class RequestEditLabel(val label: LabelData) : LabelsAction

    data class RequestDeleteLabel(val label: LabelData) : LabelsAction

    data object DismissDialog : LabelsAction

    data class ToggleVisibility(val label: LabelData) : LabelsAction

    data class ConfirmAddLabel(val value: String) : LabelsAction

    data class ConfirmEditLabel(val oldLabel: String, val newLabel: String) : LabelsAction

    data class ConfirmDeleteLabel(val label: String) : LabelsAction

    data class ReorderLabels(val reorderedLabels: List<LabelData>) : LabelsAction
}

sealed interface LabelsSideEffect {
    data class NavigateToLabel(val label: String) : LabelsSideEffect

    data class ShowToast(@StringRes val messageRes: Int) : LabelsSideEffect
}
